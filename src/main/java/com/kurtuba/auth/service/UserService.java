package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.MetaChangeType;
import com.kurtuba.auth.data.model.RegisteredClient;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
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
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kurtuba.auth.utils.Utils.generateRandomAlphanumericString;
import static com.kurtuba.auth.utils.Utils.generateValidationCode;

@Service
public class UserService {

    @Value("${kurtuba.meta-change.validity.password-reset-code.minutes}")
    private int passwordResetCodeValidityMinutes;

    @Value("${kurtuba.meta-change.max-try-count}")
    private int metaChangeMaxTryCount;

    @Value("${kurtuba.meta-change.validity.email.activation-code.minutes}")
    private int activationCodeValidityMinutes;

    @Value("${kurtuba.meta-change.validity.email.change-code.minutes}")
    private int emailChangeCodeValidityMinutes;

    private final UserRepository userRepository;

    private final UserRoleRepository userRoleRepository;

    private final UserTokenService userTokenService;

    private final EmailJobService emailJobService;

    private final UserMetaChangeService userMetaChangeService;

    private final RegisteredClientRepository registeredClientRepository;

    private final EntityManagerFactory entityManagerFactory;

    private EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                       UserTokenService userTokenService, EmailJobService emailJobService,
                       UserMetaChangeService userMetaChangeService,
                       RegisteredClientRepository registeredClientRepository,
                       EntityManagerFactory entityManagerFactory) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTokenService = userTokenService;
        this.emailJobService = emailJobService;
        this.userMetaChangeService = userMetaChangeService;
        this.registeredClientRepository = registeredClientRepository;
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
            newUser.setAuthProvider(AuthProviderType.GOOGLE);
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
    public TokenResponseDto generateTokensForLogin(String emailUsername, String pass,
                                                   String registeredClientId, String registeredClientSecret) {
        // validate client credentials
        RegisteredClient client = registeredClientRepository.findByClientId(registeredClientId);
        if (client == null) {
            throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);
        }
        if (StringUtils.hasLength(client.getClientSecret())) {
            if (!StringUtils.hasLength(registeredClientSecret)) {
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if (!new BCryptPasswordEncoder().matches(registeredClientSecret, client.getClientSecret())) {
                throw new BusinessLogicException(ErrorEnum.LOGIN_INVALID_CREDENTIALS);
            }
        }

        // authenticate user
        User user = authenticate(emailUsername, pass);

        //set token params
        Duration accessTokenValidity = Duration.ofMinutes(client.getAccessTokenTtlMinutes());
        Duration refreshTokenValidity = null;
        if (client.isRefreshTokenEnabled()) {
            refreshTokenValidity = Duration.ofMinutes(client.getRefreshTokenTtlMinutes());
        }

        Set<String> roles = null;
        if (client.isScopeEnabled()) {
            roles = user.getUserRoles().stream().map(role -> role.getRole().name()).collect(Collectors.toSet());
        }

        //create token(s)
        return userTokenService.createAndSaveTokens(user.getId(), client.getClientId(), Set.of(client.getClientName()),
                roles, accessTokenValidity, refreshTokenValidity);
    }

    @Transactional
    public String register(@Valid UserRegistrationDto newUser) {
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
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(newUser.isEmailValidationByCode() ? metaChangeMaxTryCount : null)
                .tryCount(newUser.isEmailValidationByCode() ? 0 : null)
                .code(newUser.isEmailValidationByCode() ? generateValidationCode() : null)
                .linkParam(!newUser.isEmailValidationByCode() ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();

        userMetaChangeService.create(metaChange);
        user.setUserRoles(List.of(
                UserRole.builder()
                        .userId(user.getId())
                        .role(AuthoritiesType.USER)
                        .createdDate(LocalDateTime.now())
                        .build()));
        userRoleRepository.saveAll(user.getUserRoles());

        if (newUser.isEmailValidationByCode()) {
            emailJobService.sendRegistrationValidationCodeMail(user.getEmail(), metaChange.getCode());
        } else {
            emailJobService.sendRegistrationValidationLinkMail(user.getEmail(), metaChange.getLinkParam());
        }

        return metaChange.getId();
    }


    @Transactional
    public UserDto validateEmailByLink(@NotEmpty String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam);

        if (userMetaChange == null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_CODE_INVALID);
        }

        User user = userRepository.getUserById(userMetaChange.getUserId());
        return saveNewEmail(userMetaChange, user);
    }

    /**
     * Validate email by rest request. User must enter the code mailed to them
     *
     * @param userMetaChangeId
     * @param code             is random numeric string
     * @return
     */
    @Transactional
    public UserDto validateEmailByCode(@NotEmpty String userMetaChangeId, @NotEmpty String code) {

        UserMetaChange userMetaChange = userMetaChangeService
                .findById(userMetaChangeId).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_CODE_INVALID));
        User user = userRepository.getUserById(userMetaChange.getUserId());

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()){
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATE_CODE_EXPIRED);
        }

        if (!userMetaChange.getCode().equals(code)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        return saveNewEmail(userMetaChange, user);
    }

    private UserDto saveNewEmail(UserMetaChange userMetaChange, User user) {
        if (!userMetaChange.getMetaChangeType().equals(MetaChangeType.EMAIL)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATION_STATUS_INVALID);
        }

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()){
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATE_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VALIDATE_CODE_EXPIRED);
        }

        if (user.isEmailValidated()) {
            //this is a change operation
            //send change notification mail to old e-mail
            emailJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaChangeType.EMAIL);
        } else {
            // this is activation
            user.setActivated(true);
        }
        user.setEmail(userMetaChange.getMeta());
        user.setEmailValidated(true);

        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.create(userMetaChange);

        return UserDto.fromUser(user);
    }

    /**
     * User registered for the first time and no email is validated(account activation)
     * Assumes the email address is in user table and also an expired code exist in user_meta_change table
     */
    @Transactional
    public void sendRegistrationEmailValidationCode(@NotEmpty String email, boolean byCode) {

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
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateValidationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);
        if (byCode) {
            emailJobService.sendRegistrationValidationCodeMail(user.getEmail(), metaChange.getCode());
        } else {
            emailJobService.sendRegistrationValidationLinkMail(user.getEmail(), metaChange.getLinkParam());
        }


    }

    @Transactional
    public UserRegistrationDto registerByAnotherProvider(@Valid UserRegistrationOtherProviderDto newUserByOtherProvider) {

        UserRegistrationDto decodedUser = null;
        if (newUserByOtherProvider.getProvider().equals(AuthProviderType.GOOGLE)) {
            try {
                decodedUser = TokenUtils.decodeGoogleToken(newUserByOtherProvider.getToken(), newUserByOtherProvider.getClientId());
            } catch (Exception e) {
                e.printStackTrace();
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN);
            }
        }
        if (newUserByOtherProvider.getProvider().equals(AuthProviderType.FACEBOOK)) {
            try {

                JsonObject jsonUser = TokenUtils.decodeTokenPayload(newUserByOtherProvider.getToken());
                decodedUser = new UserRegistrationDto();
                decodedUser.setEmail(jsonUser.get("email").getAsString());
                decodedUser.setName(jsonUser.get("given_name").getAsString());
                decodedUser.setSurname(jsonUser.get("family_name").getAsString());
                decodedUser.setAuthProvider(AuthProviderType.FACEBOOK);
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
                            .role(AuthoritiesType.USER)
                            .build()));
            userRoleRepository.saveAll(user.getUserRoles());
            decodedUser.setPassword(pass);
            return decodedUser;
        }

        if (existingUser.getAuthProvider().equals(AuthProviderType.KURTUBA)) {
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

        //This email with different provider exists. TODO Check active, lock etc fields and return a token
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
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
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
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }
        if (!new BCryptPasswordEncoder().matches(passwordChangeDto.getOldPassword(), user.getPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_WRONG_PASSWORD);
        }
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getRepeatNewPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }
        user.setPassword(new BCryptPasswordEncoder().encode(passwordChangeDto.getNewPassword()));
        userRepository.save(user);
        userMetaChangeService.create(UserMetaChange.builder()
                .metaChangeType(MetaChangeType.PASSWORD_CHANGE)
                .userId(user.getId())
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .executed(true)
                .expirationDate(LocalDateTime.now())
                .build());
        emailJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaChangeType.PASSWORD_CHANGE);
    }


    /**
     * If password reset request is responded with a link, link has unique code.
     *
     * @param passwordResetByLinkDto
     */
    @Transactional
    public void resetPasswordByLink(@Valid PasswordResetByLinkDto passwordResetByLinkDto) {

        UserMetaChange userMetaChange = validatePasswordResetLinkParam(passwordResetByLinkDto.getLinkParam());

        saveNewPassword(userMetaChange, passwordResetByLinkDto.getNewPassword(), passwordResetByLinkDto.getRepeatNewPassword());

    }

    public UserMetaChange validatePasswordResetLinkParam(String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam);
        validatePasswordResetUserMetaChange(userMetaChange);
        return userMetaChange;
    }

    private void saveNewPassword(UserMetaChange userMetaChange, String newPassword, String repeatNewPassword) {
        User user = userRepository.getUserById(userMetaChange.getUserId());

        if (!newPassword.equals(repeatNewPassword)) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.create(userMetaChange);
        emailJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaChangeType.PASSWORD_RESET);
    }


    /**
     * If password reset request is responded with a code rather than a link, generated code is random-not unique.
     * Hence, userMetaChangeId is required
     *
     * @param passwordResetByCodeDto
     */
    @Transactional
    public void resetPasswordByCode(@Valid PasswordResetByCodeDto passwordResetByCodeDto) {

        UserMetaChange userMetaChange = userMetaChangeService
                .findById(passwordResetByCodeDto.getUserMetaChangeId())
                .orElse(null);

        validatePasswordResetUserMetaChange(userMetaChange);

        if (!userMetaChange.getCode().equals(passwordResetByCodeDto.getCode())) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        saveNewPassword(userMetaChange, passwordResetByCodeDto.getNewPassword(), passwordResetByCodeDto.getRepeatNewPassword());

    }

    @Transactional
    public UserMetaChange requestResetPassword(@NotEmpty String usernameEmail, boolean byCode) {
        User user = userRepository.getUserByEmailOrUsername(usernameEmail);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if (!user.isEmailValidated()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_NOT_VALIDATED);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .metaChangeType(MetaChangeType.PASSWORD_RESET)
                .executed(false)
                .expirationDate(LocalDateTime.now().plusMinutes(passwordResetCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateValidationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .createdDate(LocalDateTime.now())
                .userId(user.getId())
                .build();

        // inactive users cannot carry out any operations
        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }
        userMetaChangeService.create(metaChange);
        if (byCode == true) {
            emailJobService.sendPasswordResetCodeMail(user.getEmail(), metaChange.getCode());
        } else {
            emailJobService.sendPasswordResetLinkMail(user.getEmail(), metaChange.getLinkParam());
        }

        return metaChange;

    }

    private void validatePasswordResetUserMetaChange(UserMetaChange userMetaChange) {
        if (userMetaChange == null) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_INVALID);
        }

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }

        if (!userMetaChange.getMetaChangeType().equals(MetaChangeType.PASSWORD_RESET)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }
    }

    /**
     * User registered and there is already a validated email(email change operation)
     *
     * @param userId
     * @param email
     * @return
     */
    public UserMetaChange requestChangeEmail(String userId, String email, boolean byCode) {
        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if(userRepository.getUserByEmail(email) != null){
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        // user must be in a valid state
        if (!user.isEmailValidated() || user.isLocked() || user.isShowCaptcha() || !user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaChangeType(MetaChangeType.EMAIL)
                .meta(email)
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(emailChangeCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateValidationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);

        if (byCode) {
            emailJobService.sendUserEmailChangeCodeMail(email, metaChange.getCode());
        } else {
            emailJobService.sendUserEmailChangeLinkMail(email, metaChange.getLinkParam());
        }

        return metaChange;

    }

    @Transactional
    public void updateEmailChangeTryCount(@Valid EmailValidationDto emailValidationDto) {
        UserMetaChange userMetaChange = userMetaChangeService.findById(emailValidationDto.getUserMetaChangeId())
                .get();
        updateUserMetaChangeTryCount(userMetaChange);
    }

    @Transactional
    public void updatePasswordResetTryCount(PasswordResetByCodeDto passwordResetByCodeDto) {
        UserMetaChange userMetaChange = userMetaChangeService
                .findById(passwordResetByCodeDto.getUserMetaChangeId()).get();
        updateUserMetaChangeTryCount(userMetaChange);
    }

    @Transactional
    public void updateUserMetaChangeTryCount(UserMetaChange userMetaChange) {
        userMetaChange.setTryCount(userMetaChange.getTryCount() + 1);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);
    }
}
