package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
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
import jakarta.validation.constraints.NotNull;
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
import static com.kurtuba.auth.utils.Utils.generateVerificationCode;

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

    private final MessageJobService messageJobService;

    private final UserMetaChangeService userMetaChangeService;

    private final RegisteredClientRepository registeredClientRepository;

    private final EntityManagerFactory entityManagerFactory;

    private EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository,
                       UserTokenService userTokenService, MessageJobService messageJobService,
                       UserMetaChangeService userMetaChangeService,
                       RegisteredClientRepository registeredClientRepository,
                       EntityManagerFactory entityManagerFactory) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userTokenService = userTokenService;
        this.messageJobService = messageJobService;
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
        // authenticate user
        User user = authenticate(emailUsername, pass);
        return validateRegisteredClientAndGetTokens(user, registeredClientId, registeredClientSecret);


    }

    @Transactional
    public String register(@Valid UserRegistrationDto newUser) {
        if (StringUtils.hasLength(newUser.getEmail()) && userRepository.getUserByEmail(newUser.getEmail()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (StringUtils.hasLength(newUser.getMobile()) && userRepository.getUserByMobile(newUser.getMobile()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_MOBILE_ALREADY_EXISTS);
        }

        if (StringUtils.hasLength(newUser.getUsername()) && userRepository.getUserByUsername(newUser.getUsername()) != null) {
            throw new BusinessLogicException(ErrorEnum.USER_USERNAME_ALREADY_EXISTS);
        }

        if(!StringUtils.hasLength(newUser.getEmail()) && !StringUtils.hasLength(newUser.getMobile())){
            throw new BusinessLogicException(ErrorEnum.USER_CONTACT_REQUIRED);
        }

        if(newUser.getPreferredVerificationContact().equals(ContactType.EMAIL) && !StringUtils.hasLength(newUser.getEmail())){
            throw new BusinessLogicException(ErrorEnum.USER_CONTACT_REQUIRED);
        }

        String pass = newUser.getPassword();
        newUser.setPassword(new BCryptPasswordEncoder().encode(pass));

        User user = newUser.toUser();
        if(StringUtils.hasLength(user.getUsername())){
            user.setCanChangeUsername(false);
        }else{
            user.setCanChangeUsername(true);
        }

        userRepository.save(user);

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(newUser.getPreferredVerificationContact())
                .meta(newUser.getPreferredVerificationContact().equals(ContactType.EMAIL) ? user.getEmail() : user.getMobile())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(newUser.isVerificationByCode() ? metaChangeMaxTryCount : null)
                .tryCount(newUser.isVerificationByCode() ? 0 : null)
                .code(newUser.isVerificationByCode() ? generateVerificationCode() : null)
                .linkParam(!newUser.isVerificationByCode() ?
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

        // in case there are both email and mobile contacts, only one can be used to activate account.
        if(newUser.getPreferredVerificationContact().equals(ContactType.EMAIL)){
            if (newUser.isVerificationByCode()) {
                messageJobService.sendAccountActivationCodeMail(user.getEmail(), metaChange.getCode());
            } else {
                messageJobService.sendAccountActivationLinkMail(user.getEmail(), metaChange.getLinkParam());
            }
        }else{
            // todo implement send sms
            throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
        }

        return metaChange.getId();
    }


    @Transactional
    public UserMetaChange verifyEmailByLink(@NotEmpty String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam);
        validateEmailChangeUserMetaChange(userMetaChange);
        User user = userRepository.getUserById(userMetaChange.getUserId());
        saveNewEmail(userMetaChange, user);
        return userMetaChange;
    }

    /**
     * Verify email by rest request. User must enter the code mailed to them
     *
     * @param userMetaChangeId
     * @param code
     * @return
     */
    @Transactional
    public UserDto verifyEmailByCode(@NotEmpty String userMetaChangeId, @NotEmpty String code) {

        UserMetaChange userMetaChange = userMetaChangeService
                .findById(userMetaChangeId).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_INVALID));
        User user = userRepository.getUserById(userMetaChange.getUserId());

        validateEmailChangeUserMetaChange(userMetaChange);

        if (!userMetaChange.getCode().equals(code)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        return saveNewEmail(userMetaChange, user);
    }

    private UserDto saveNewEmail(UserMetaChange userMetaChange, User user) {

        if(StringUtils.hasLength(user.getEmail())){
            //send change notification mail to old e-mail
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.EMAIL_CHANGE);
        }

        user.setEmail(userMetaChange.getMeta());
        user.setEmailVerified(true);
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);

        return UserDto.fromUser(user);
    }

    private void validateEmailChangeUserMetaChange(UserMetaChange userMetaChange) {
        if (userMetaChange == null) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_INVALID);
        }

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_EXPIRED);
        }

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.EMAIL_CHANGE)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        if (userMetaChange.isExecuted()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_EXPIRED);
        }

        if (userMetaChange.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_EXPIRED);
        }
    }


    @Transactional
    public String sendAccountActivationMessage(@NotEmpty String emailMobile, boolean byCode){
        if(emailMobile.contains("@")){
            //email
            return sendAccountActivationMail(emailMobile, byCode);
        }else{
            return sendAccountActivationSMS(emailMobile, byCode);
        }

    }
    /**
     * Send activation SMS to the user's mobile number
     * May contain code or link
     *
     * @return
     */
    @Transactional
    public String sendAccountActivationSMS(@NotEmpty String mobile, boolean byCode) {
        throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
    }

    /**
     * Send activation MAIL to the user's email address
     * May contain code or link
     *
     * @return
     */
    @Transactional
    public String sendAccountActivationMail(@NotEmpty String email, boolean byCode) {

        User user = userRepository.getUserByEmail(email);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if(user.isActivated()){
            // already activated
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(ContactType.EMAIL)
                .meta(user.getEmail())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);
        if (byCode) {
            messageJobService.sendAccountActivationCodeMail(user.getEmail(), metaChange.getCode());
        } else {
            messageJobService.sendAccountActivationLinkMail(user.getEmail(), metaChange.getLinkParam());
        }

        return metaChange.getId();


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
            user.setEmailVerified(true);
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

    public boolean isMobileAvailable(String mobile) {
        return userRepository.getUserByMobile(mobile) == null;
    }

    @Transactional
    public void changePassword(@Valid PasswordChangeDto passwordChangeDto, @NotEmpty String userId) {
        User user = userRepository.getUserById(userId);
        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if(!user.isActivated()){
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
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
                .metaOperationType(MetaOperationType.PASSWORD_CHANGE)
                .userId(user.getId())
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .executed(true)
                .expirationDate(LocalDateTime.now())
                .build());
        messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_CHANGE);
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

    /**
     * If password reset request is responded with a code rather than a link, generated code is random-not unique.
     * Hence, userMetaChangeId is required
     *
     * @param passwordResetByCodeDto
     */
    @Transactional
    public TokenResponseDto resetPasswordByCode(@Valid PasswordResetByCodeDto passwordResetByCodeDto) {

        UserMetaChange userMetaChange = userMetaChangeService
                .findById(passwordResetByCodeDto.getUserMetaChangeId())
                .orElse(null);

        validatePasswordResetUserMetaChange(userMetaChange);

        if (!userMetaChange.getCode().equals(passwordResetByCodeDto.getCode())) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        saveNewPassword(userMetaChange, passwordResetByCodeDto.getNewPassword(), passwordResetByCodeDto.getRepeatNewPassword());
        User user = userRepository.getUserById( userMetaChange.getUserId());

        return validateRegisteredClientAndGetTokens(user, passwordResetByCodeDto.getClientId(), passwordResetByCodeDto.getClientSecret());

    }

    private void saveNewPassword(@NotNull UserMetaChange userMetaChange, String newPassword, String repeatNewPassword) {
        User user = userRepository.getUserById(userMetaChange.getUserId());

        if(!user.isActivated()){
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (!newPassword.equals(repeatNewPassword)) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.create(userMetaChange);

        if(StringUtils.hasLength(user.getEmail())){
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_RESET);
        }

        //todo uncomment after SMS integration
        /*if(StringUtils.hasLength(user.getMobile())){
            messageJobService.sendUserMetaChangeNotificationSMS(user.getEmail(), MetaOperationType.PASSWORD_RESET);
        }*/

    }

    private TokenResponseDto validateRegisteredClientAndGetTokens(User user, String clientId, String clientSecret){
        if(StringUtils.hasLength(clientId)){
            RegisteredClient client = registeredClientRepository.findByClientId(clientId);
            if(client == null){
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
            }
            if(StringUtils.hasLength(client.getClientSecret())){
                if(!StringUtils.hasLength(clientSecret) || !new BCryptPasswordEncoder()
                        .matches(clientSecret, client.getClientSecret())){
                    throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID_CREDENTIALS);
                }
            }

            // credentials validated
            // generate tokens and return
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

        return null;
    }

    @Transactional
    public UserMetaChange requestResetPassword(@NotEmpty String emailMobile, boolean byCode) {
        User user = userRepository.getUserByEmailOrMobile(emailMobile);

        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if(emailMobile.contains("@")) {
            //email
            if (!user.isEmailVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_EMAIL_NOT_VERIFIED);
            }

        }else{
            //mobile
            if (!user.isMobileVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_MOBILE_NOT_VERIFIED);
            }
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .metaOperationType(MetaOperationType.PASSWORD_RESET)
                .contactType(emailMobile.contains("@") ? ContactType.EMAIL : ContactType.MOBILE)
                .executed(false)
                .expirationDate(LocalDateTime.now().plusMinutes(passwordResetCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .createdDate(LocalDateTime.now())
                .userId(user.getId())
                .build();

        if(emailMobile.contains("@")){
            //email
            if (byCode == true) {
                messageJobService.sendPasswordResetCodeMail(user.getEmail(), metaChange.getCode());
            } else {
                messageJobService.sendPasswordResetLinkMail(user.getEmail(), metaChange.getLinkParam());
            }
        }else{
            //mobile
            // todo implement send sms
            throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
        }
        userMetaChangeService.create(metaChange);

        return metaChange;

    }

    private void validatePasswordResetUserMetaChange(UserMetaChange userMetaChange) {
        if (userMetaChange == null) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_INVALID);
        }

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_RESET_CODE_EXPIRED);
        }

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.PASSWORD_RESET)) {
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
     * User registered and there is already a verified email(email change operation)
     *
     * @param userId
     * @param email
     * @return
     */
    @Transactional
    public UserMetaChange requestChangeEmail(String userId, String email, boolean byCode) {
        User user = userRepository.getUserById(userId);

        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if (user.isLocked() || user.isShowCaptcha() || !user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if(userRepository.getUserByEmail(email) != null){
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.EMAIL_CHANGE)
                .contactType(ContactType.EMAIL)
                .meta(email)
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(emailChangeCodeValidityMinutes))
                .maxTryCount(byCode ? metaChangeMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);

        if (byCode) {
            messageJobService.sendUserEmailChangeCodeMail(email, metaChange.getCode());
        } else {
            messageJobService.sendUserEmailChangeLinkMail(email, metaChange.getLinkParam());
        }

        return metaChange;

    }

    @Transactional
    public void updateEmailChangeTryCount(@Valid EmailVerificationDto emailVerificationDto) {
        UserMetaChange userMetaChange = userMetaChangeService.findById(emailVerificationDto.getUserMetaChangeId())
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
    public void updateAccountActivationTryCount(AccountActivationDto accountActivationDto) {
        UserMetaChange userMetaChange = userMetaChangeService
                .findById(userRepository.getUserByEmailOrMobile(accountActivationDto.getEmailMobile()).getId()).get();
        updateUserMetaChangeTryCount(userMetaChange);
    }

    @Transactional
    public void updateUserMetaChangeTryCount(UserMetaChange userMetaChange) {
        userMetaChange.setTryCount(userMetaChange.getTryCount() + 1);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChangeService.update(userMetaChange);
    }

    @Transactional
    public UserMetaChange activateAccountByLink(String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findActiveAccountActivationByLinkParam(linkParam);
        User user = userRepository.getUserById(userMetaChange.getUserId());
        activateAccount(user, userMetaChange, null, null);
        return userMetaChange;
    }

    @Transactional
    public TokenResponseDto activateAccountByCode(String emailMobile, String code, String clientId, String clientSecret) {
        User user = userRepository.getUserByEmailOrMobile(emailMobile);
        UserMetaChange userMetaChange = userMetaChangeService.findActiveAccountActivationByUserId(user.getId());

        if(userMetaChange == null){
            throw new BusinessLogicException(ErrorEnum.USER_ACTIVATION_INVALID);
        }

        if (userMetaChange.getMaxTryCount() != null && userMetaChange.getTryCount() >= userMetaChange.getMaxTryCount()){
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_VERIFICATION_CODE_EXPIRED);
        }

        if(!userMetaChange.getCode().equals(code)){
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_CODE_MISMATCH);
        }

        return activateAccount(user, userMetaChange, clientId, clientSecret);
    }

    private TokenResponseDto activateAccount(User user, UserMetaChange userMetaChange, String clientId, String clientSecret) {

        if(user == null){
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if(user.isActivated()){
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        // assuming userMetaChange passed to this method is acquired from
        // "findActiveAccountActivationByLinkParam" or "findActiveAccountActivationByUserId" method which
        // already does validation checks for executed, expirationDate and MetaOperationType
        if(userMetaChange == null){
            throw new BusinessLogicException(ErrorEnum.USER_ACTIVATION_INVALID);
        }

        user.setActivated(true);
        if(userMetaChange.getContactType().equals(ContactType.EMAIL)){
            user.setEmailVerified(true);
        }else {
            user.setMobileVerified(true);
        }
        userRepository.save(user);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChange.setExecuted(true);
        userMetaChangeService.update(userMetaChange);

        return validateRegisteredClientAndGetTokens(user, clientId, clientSecret);

    }
}
