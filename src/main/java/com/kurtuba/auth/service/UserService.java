package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.model.dto.*;
import com.kurtuba.auth.data.repository.UserMetaChangeRepository;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.data.repository.UserRoleRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.kurtuba.auth.utils.Utils.generateRandomAlphanumericString;

@Service
public class UserService {

    @Value("${kurtuba.mobile-client.access-token-validity.minutes}")
    private int mobileClientAccessTokenValidityMinutes;
    @Value("${kurtuba.mobile-client.refresh-token-validity.minutes}")
    private int mobileClientRefreshTokenValidityMinutes;
    @Value("${kurtuba.web-client.access-token-validity.minutes}")
    private int webClientAccessTokenValidityMinutes;
    @Value("${kurtuba.web-client.refresh-token-validity.minutes}")
    private int webClientRefreshTokenValidityMinutes;
    @Value("${kurtuba.password-reset.code.validity.minutes}")
    private int passwordResetCodeValidityMinutes;

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserTokenService userTokenService;

    private final EmailService emailService;

    private final UserMetaChangeRepository userMetaChangeRepository;

    private final EntityManagerFactory entityManagerFactory;

    private EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                       UserTokenService userTokenService, EmailService emailService,
                       UserMetaChangeRepository userMetaChangeRepository, EntityManagerFactory entityManagerFactory) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTokenService = userTokenService;
        this.emailService = emailService;
        this.userMetaChangeRepository = userMetaChangeRepository;
        this.entityManagerFactory = entityManagerFactory;
    }


    /**
     * Runs when sign in with Google
     *
     * @param username
     */
    @Transactional
    public void processOAuthPostLogin(String username) {
        User existUser = userRepository.getUserByUsername(username);

        if (existUser == null) {
            User newUser = new User();
            newUser.setEmail(username);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            newUser.setActivated(true);

            userRepository.save(newUser);

            System.out.println("Created new user: " + username);
        }

    }

    /**
     * Temporary method. Only user for local development. Will be removed
     *
     * @param user
     */
    @Transactional
    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User getUserByUsernameOrEmail(String email) {
        return userRepository.getUserByEmailOrUsername(email);
    }


    /**
     * Used by both authorization server login(CustomAuthenticationProvider) and
     * custom rest request login(EmailPassLoginController)
     * <p>
     * transaction is managed manually because we may save data to db and then throw BusinessLogicException
     */


    public User authenticate(String emailUsername, String pass) {
        User user = userRepository.getUserByEmailOrUsername(emailUsername);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);
        }
        long timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
        if (user.isLocked() && LocalDateTime.now().isBefore(user.getLastLoginAttempt().plusMinutes(timeToWait))) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
        }

        user.setLastLoginAttempt(LocalDateTime.now());
        String dbPass = user.getPassword();
        EntityTransaction transaction = null;
        try (EntityManager em = em()) {
            transaction = em.getTransaction();
            transaction.begin();
            user = em.find(User.class, user.getId());
            if (!BCrypt.checkpw(pass, dbPass)) {
                user.setFailedLoginCount(user.getFailedLoginCount() + 1);

                if (user.getFailedLoginCount() >= 5) {
                    user.setShowCaptcha(true);
                }

                if (user.getFailedLoginCount() >= 10) {
                    user.setLocked(true);
                    em.persist(user);
                    transaction.commit();
                    timeToWait = Double.valueOf(Math.pow(2, user.getFailedLoginCount() - 10)).longValue() * 15;
                    throw new BusinessLogicException(ErrorEnum.LOGIN_USER_LOCKED.getCode(), "Account locked until " + user.getLastLoginAttempt().plusMinutes(timeToWait));
                }
                em.persist(user);
                transaction.commit();
                throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);

            }
            user.setFailedLoginCount(0);
            user.setShowCaptcha(false);
            user.setLocked(false);
            em.persist(user);
            transaction.commit();
            return user;

        }

    }

    @Transactional
    public TokensDto generateTokensForLoginByRestRequest(String emailUsername, String pass, Set<ClientType> clientTypes) {
        Duration accessTokenValidity;
        Duration refreshTokenValidity;
        User user = authenticate(emailUsername, pass);
        if (clientTypes.stream().filter(aud -> aud.equals(ClientType.ADM_WEB_CLIENT)).findFirst().orElse(null) != null) {
            accessTokenValidity = Duration.ofMinutes(webClientAccessTokenValidityMinutes);
            refreshTokenValidity = Duration.ofMinutes(webClientRefreshTokenValidityMinutes);
        } else {
            // mobile client
            accessTokenValidity = Duration.ofMinutes(mobileClientAccessTokenValidityMinutes);
            refreshTokenValidity = Duration.ofMinutes(mobileClientRefreshTokenValidityMinutes);
        }
        return userTokenService.createAndSaveTokens(user.getId(), clientTypes, accessTokenValidity, refreshTokenValidity);
    }

    @Transactional
    public UserDto register(@Valid UserRegistrationDto newUser) {
        if (userRepository.getUserByEmail(newUser.getEmail()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (userRepository.getUserByUsername(newUser.getUsername()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_USERNAME_ALREADY_EXISTS);
        }

        String pass = newUser.getPassword();
        newUser.setPassword(new BCryptPasswordEncoder().encode(pass));

        User user = newUser.toUser();
        user.setCanChangeUsername(false);

        userRepository.save(user);

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaChangeType(MetaChangeType.EMAIL)
                .meta(user.getEmail())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusHours(24))
                .code(newUser.isEmailValidationByCode() ? generateRandomAlphanumericString(6) :
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()))
                .build();

        userMetaChangeRepository.save(metaChange);
        user.setUserRoles(List.of(
                UserRole.builder()
                        .userId(user.getId())
                        .role(AuthoritiesEnum.USER)
                        .build()));
        userRoleRepository.saveAll(user.getUserRoles());

        try {
            if (newUser.isEmailValidationByCode()) {
                emailService.sendValidationCodeMail(user.getEmail(), metaChange.getCode());
            } else {
                emailService.sendValidationLinkMail(user.getEmail(), metaChange.getCode());
            }

        } catch (BusinessLogicException e) {
            //TODO let email scheduler handle
            e.printStackTrace();
        }

        return UserDto.fromUser(user);
    }

    /**
     *
     * @param code is unique
     */
    @Transactional
    public void validateEmailByLink(@NotEmpty String code) {
        UserMetaChange userMetaChange = userMetaChangeRepository.findByCode(code);
        if (userMetaChange == null || !userMetaChange.getMetaChangeType().equals(MetaChangeType.EMAIL)) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_CODE_INVALID);
        }
        User user = userRepository.getUserById(userMetaChange.getUserId());
        validateEmailByCode(user.getEmail(), code);
    }

    /**
     * Validate email by rest request. User must enter the code mailed to them
     *
     * @param email
     * @param code is random alphanumeric string
     * @return
     */
    @Transactional
    public UserDto validateEmailByCode(@NotEmpty String email, @NotEmpty String code) {
        User user  = userRepository.getUserByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("Invalid user");
        }

        UserMetaChange userMetaChange = userMetaChangeRepository
                .findByUserIdAndCode(user.getId(), code);

        if (userMetaChange == null || !userMetaChange.getMetaChangeType().equals(MetaChangeType.EMAIL)) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_CODE_INVALID);
        }
        if (userMetaChange.isExecuted()) {
            //already executed
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_STATUS_INVALID);
        }
        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATE_CODE_EXPIRED);
        }

        if(user.isEmailValidated()){
            //this is a change operation
            //send change notification mail to old e-mail
            emailService.sendUserMetaChangeMail(user.getEmail(), MetaChangeType.EMAIL.name());
        }
        user.setEmail(userMetaChange.getMeta());
        user.setEmailValidated(true);// in case user is registering
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeRepository.save(userMetaChange);

        return UserDto.fromUser(user);
    }

    /**
     * Used when a user registered but didn't verify the mail by clicking the link and the link is expired
     * Assumes the email address is in user table and also an expired code exist in user_meta_change table
     */
    @Transactional
    public void resendValidationCode(@NotEmpty String email, boolean byCode) {
        // a single user in pending email verification state with email address must exist
        User user = userRepository.getUserByEmailAndEmailValidatedIsFalse(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_STATUS_INVALID);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaChangeType(MetaChangeType.EMAIL)
                .meta(user.getEmail())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusHours(24))
                .code(byCode == true ? generateRandomAlphanumericString(6) :
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()))
                .build();
        try {
            if (byCode) {
                emailService.sendValidationCodeMail(user.getEmail(), metaChange.getCode());
            } else {
                emailService.sendValidationLinkMail(user.getEmail(), metaChange.getCode());
            }

        } catch (BusinessLogicException e) {
            //TODO let email scheduler handle
        }

    }

    @Transactional
    public UserRegistrationDto registerByAnotherProvider(@Valid UserRegistrationOtherProviderDto newUserByOtherProvider) {

        UserRegistrationDto decodedUser = null;
        if (newUserByOtherProvider.getProvider().equals(AuthProvider.GOOGLE)) {
            try {
                decodedUser = TokenUtils.decodeGoogleToken(newUserByOtherProvider.getToken(), newUserByOtherProvider.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN);
            }
        }
        if (newUserByOtherProvider.getProvider().equals(AuthProvider.FACEBOOK)) {
            try {

                JsonObject jsonUser = TokenUtils.decodeTokenPayload(newUserByOtherProvider.getToken());
                decodedUser = new UserRegistrationDto();
                decodedUser.setEmail(jsonUser.get("email").getAsString());
                decodedUser.setName(jsonUser.get("given_name").getAsString());
                decodedUser.setSurname(jsonUser.get("family_name").getAsString());
                decodedUser.setAuthProvider(AuthProvider.FACEBOOK);
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN);
            }
        }

        //else check twitter etc. then...

        //check if user already exists
        User existingUser = userRepository.getUserByEmail(decodedUser.getEmail());

        if (existingUser == null) {
            //this user never existed, let make one and return a token
            User user = decodedUser.toUser();
            user.setEmailValidated(true);
            String pass = UUID.randomUUID().toString();
            user.setPassword(new BCryptPasswordEncoder().encode(pass));
            String provisionalUsername = user.getEmail().split("@")[0];
            if (provisionalUsername.length() > 25) {
                provisionalUsername = provisionalUsername.substring(0, 25);
            }
            user.setUsername(provisionalUsername + "." + generateRandomAlphanumericString(6));
            user.setCanChangeUsername(true);
            userRepository.save(user);
            user.setUserRoles(List.of(
                    UserRole.builder()
                            .userId(user.getId())
                            .role(AuthoritiesEnum.USER)
                            .build()));
            userRoleRepository.saveAll(user.getUserRoles());
            decodedUser.setPassword(pass);
            return decodedUser;
        }

        if (existingUser.getAuthProvider().equals(AuthProvider.KURTUBA)) {
            //this is a regular user and we cannot return a token without changing pass so have to log in properly. Throw error
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (existingUser.getAuthProvider().equals(newUserByOtherProvider.getProvider())) {
            //this email with given provider exists. check active, lock etc fields and return a token
            if (existingUser.isActivated() && !existingUser.isLocked()) {
                String pass = UUID.randomUUID().toString();
                existingUser.setPassword(new BCryptPasswordEncoder().encode(pass));
                decodedUser.setPassword(pass);
                userRepository.save(existingUser);
                return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);
            }

        }

        //This email with different provider exists. Check active, lock etc fields and return a token
        //That also means as long as user uses other providers with same email, same user will be logged in
        String pass = UUID.randomUUID().toString();
        existingUser.setPassword(new BCryptPasswordEncoder().encode(pass));
        existingUser.setAuthProvider(decodedUser.getAuthProvider());
        decodedUser.setPassword(pass);
        userRepository.save(existingUser);
        return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);

    }


    public UserDto getUserByEmail(String email) {
        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.RESOURCE_NOT_FOUND);
        }
        return UserDto.fromUser(user);
    }

    public UserDto getUserById(String id) {
        User user = userRepository.getUserById(id);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid user id");
        }
        return UserDto.fromUser(user);
    }

    public boolean isUsernameAvailable(String username) {
        return userRepository.getUserByUsername(username) == null;
    }

    public boolean isEmailAvailable(String email) {
        return userRepository.getUserByEmail(email) == null;
    }

    @Transactional
    public void changePassword(@Valid PasswordChangeDto passwordChangeDto, @NotEmpty String userId) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid user");
        }
        if (!new BCryptPasswordEncoder().matches(passwordChangeDto.getOldPassword(), user.getPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_WRONG_PASSWORD);
        }
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getRepeatNewPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        emailService.sendUserMetaChangeMail(user.getEmail(), "password");
    }


    /**
     * If password reset request is responded with a link, link has unique code.
     * @param passwordResetDto
     */
    @Transactional
    public void resetPasswordByLink(@Valid PasswordResetDto passwordResetDto) {
        UserMetaChange userMetaChange = validatePasswordResetCode(passwordResetDto.getCode());
        passwordResetDto.setUserMetaChangeId(userMetaChange.getId());
        resetPasswordByCode(passwordResetDto.toPasswordResetByCodeDto());
    }


    /**
     * If password reset request is responded with a code rather than a link, generated code is random-not unique.
     * Hence, userMetaChangeId is required
     * @param passwordResetByCodeDto
     */
    @Transactional
    public void resetPasswordByCode(@Valid PasswordResetByCodeDto passwordResetByCodeDto) {

        UserMetaChange userMetaChange = userMetaChangeRepository.findByIdAndCode(passwordResetByCodeDto.getUserMetaChangeId(),
                passwordResetByCodeDto.getCode());

        validatePasswordResetCode(userMetaChange);

        User user = userRepository.getUserById(userMetaChange.getUserId());
        if (user == null) {
            throw new UsernameNotFoundException("Invalid user");
        }

        if (!passwordResetByCodeDto.getNewPassword().equals(passwordResetByCodeDto.getRepeatNewPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(passwordResetByCodeDto.getNewPassword()));
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeRepository.save(userMetaChange);

    }

    @Transactional
    public void requestResetPassword(@NotEmpty String usernameEmail, boolean byCode) {
        User user = userRepository.getUserByEmailOrUsername(usernameEmail);
        if (user == null) {
            throw new UsernameNotFoundException("Invalid user");
        }

        if (!user.isEmailValidated()) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_EMAIL_NOT_VALIDATED);
        }
        String code = byCode == true ? generateRandomAlphanumericString(6) :
                Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
        UserMetaChange metaChange = UserMetaChange.builder()
                .metaChangeType(MetaChangeType.PASSWORD_RESET)
                .executed(false)
                .code(code)
                .expirationDate(LocalDateTime.now().plusMinutes(passwordResetCodeValidityMinutes))
                .createdDate(LocalDateTime.now())
                .userId(user.getId())
                .build();

        //todo check user state for locked/active etc?
        userMetaChangeRepository.save(metaChange);
        if (byCode == true) {
            emailService.sendPasswordResetCodeMail(user.getEmail(), code);
        } else {
            emailService.sendPasswordResetLinkMail(user.getEmail(), code);
        }

    }

    public UserMetaChange validatePasswordResetCode(String code) {
        UserMetaChange userMetaChange = userMetaChangeRepository.findByCode(code);

        validatePasswordResetCode(userMetaChange);

        return userMetaChange;
    }

    private void validatePasswordResetCode(UserMetaChange userMetaChange) {
        if (userMetaChange == null || !userMetaChange.getMetaChangeType().equals(MetaChangeType.PASSWORD_RESET)) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_INVALID);
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }
    }
}
