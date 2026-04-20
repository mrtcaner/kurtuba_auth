package com.kurtuba.auth.service;

import com.kurtuba.adm.data.dto.AdmUserDto;
import com.kurtuba.adm.data.dto.AdmUserFcmTokenDto;
import com.kurtuba.adm.data.dto.AdmUserFcmTokenSearchCriteria;
import com.kurtuba.adm.data.dto.UserAdminSearchCriteria;
import com.kurtuba.auth.data.dto.*;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.*;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.data.repository.RegisteredClientRepository;
import com.kurtuba.auth.data.repository.UserFcmTokenRepository;
import com.kurtuba.auth.data.repository.UserRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.ServiceUtils;
import com.kurtuba.auth.utils.annotation.EmailMobile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jakarta.persistence.criteria.JoinType;
import java.util.stream.Collectors;


import static com.kurtuba.auth.utils.Utils.generateVerificationCode;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserTokenService userTokenService;
    private final MessageJobService messageJobService;
    private final UserMetaChangeService userMetaChangeService;
    private final ServiceUtils serviceUtils;
    private final LocalizationSupportedCountryRepository localizationSupportedCountryRepository;
    private final LocalizationSupportedLangRepository localizationSupportedLangRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final UserMapper userMapper;

    @Value("${kurtuba.meta-change.validity.password-reset-code.minutes}")
    private int passwordResetCodeValidityMinutes;
    @Value("${kurtuba.meta-change.email-max-try-count}")
    private int metaChangeEmailMaxTryCount;
    @Value("${kurtuba.meta-change.sms-max-try-count}")
    private int metaChangeSmsMaxTryCount;
    @Value("${kurtuba.meta-change.validity.email.change-code.minutes}")
    private int emailChangeCodeValidityMinutes;



    @Transactional
    public void changePassword(@Valid PasswordChangeDto passwordChangeDto, @NotBlank String userId) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if(user.getAuthProvider().equals(AuthProviderType.KURTUBA)){
            if (!new BCryptPasswordEncoder().matches(passwordChangeDto.getOldPassword(), user.getPassword())) {
                throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_WRONG_OLD_PASSWORD);
            }
        } else {
            LOGGER.info("User {} with {} auth provider changing password", user.getId(), user.getAuthProvider());
        }

        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getRepeatNewPassword())) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(passwordChangeDto.getNewPassword()));
        user.setAuthProvider(AuthProviderType.KURTUBA);
        userRepository.save(user);
        UserMetaChange metaChange = userMetaChangeService.create(UserMetaChange.builder()
                .metaOperationType(MetaOperationType.PASSWORD_CHANGE)
                .contactType(ContactType.EMAIL)
                .userId(user.getId())
                .createdDate(Instant.now())
                .updatedDate(Instant.now())
                .executed(true)
                .expirationDate(Instant.now())
                .build());

        //todo: send sms if no email?
        if(StringUtils.hasLength(user.getEmail())){
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_CHANGE,
                    user.getUserSetting().getLanguageCode(), metaChange.getId());
        }
    }


    public Optional<User> getUserByEmail(String email) {
        return userRepository.getUserByEmail(email);
    }

    public Optional<User> getUserByEmailOrMobile(String emailMobile) {
        return userRepository.getUserByEmailOrMobile(emailMobile);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.getUserById(id);
    }

    public List<UserBasicDto> getActiveAdminUsers() {
        return userRepository.getAdminUsers().stream().map(userMapper::mapToUserBasicDto).toList();
    }

    public List<User> getUsersByIds(List<String> ids) {
        return userRepository.findAllById(ids);
    }

    public Optional<User> getUserByMobile(String mobile) {
        return userRepository.getUserByMobile(mobile);
    }

    public Optional<User> getUserByUsername(String userName) {
        return userRepository.getUserByUsername(userName);
    }

    public Optional<User> getUserByUsernameOrEmail(String email) {
        return userRepository.getUserByEmailOrUsername(email);
    }

    public List<User> getAllUsers() {
        return (List<User>) userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> searchUsers(UserAdminSearchCriteria criteria) {
        return userRepository.findAll(buildUserAdminSpecification(criteria), Sort.by(Sort.Direction.DESC, "createdDate"));
    }

    @Transactional(readOnly = true)
    public List<AdmUserDto> searchAdmUsers(UserAdminSearchCriteria criteria) {
        List<User> users = searchUsers(criteria);
        Map<String, Instant> latestTokenCreatedDatesByUserId = userTokenService.findLatestCreatedDatesByUserIds(
                users.stream().map(User::getId).collect(Collectors.toSet()));

        return users.stream()
                .map(user -> userMapper.mapToAdmUserDto(user, latestTokenCreatedDatesByUserId.get(user.getId())))
                .toList();
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.getUserByEmail(email).isPresent();
    }

    public boolean isMobileAvailable(String mobile) {
        return !userRepository.getUserByMobile(mobile).isPresent();
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.getUserByUsername(username).isPresent();
    }

    /**
     * Runs when sign in with Google
     *
     * @param username
     */
    @Transactional
    public void processOAuthPostLogin(String username) {
        User existUser = userRepository.getUserByUsername(username).orElse(null);

        if (existUser == null) {
            User newUser = new User();
            newUser.setEmail(username);
            newUser.setAuthProvider(AuthProviderType.GOOGLE);
            newUser.setActivated(true);

            userRepository.save(newUser);
            LOGGER.info("Created new OAuth user for username {}", username);
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
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked() || user.isLocked() || user.isShowCaptcha() || !user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (userRepository.getUserByEmail(email).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.EMAIL_CHANGE)
                .contactType(ContactType.EMAIL)
                .meta(email)
                .executed(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plus(Duration.ofMinutes(emailChangeCodeValidityMinutes)))
                .maxTryCount(byCode ? metaChangeEmailMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        userMetaChangeService.create(metaChange);

        if (byCode) {
            messageJobService.sendUserEmailChangeCodeMail(email, metaChange.getCode(),
                    user.getUserSetting().getLanguageCode(), metaChange.getId());
        } else {
            messageJobService.sendUserEmailChangeLinkMail(email, metaChange.getLinkParam(),
                    user.getUserSetting().getLanguageCode(), metaChange.getId());
        }

        return metaChange;

    }

    @Transactional
    public UserMetaChange requestResetPassword(@EmailMobile String emailMobile, boolean byCode) {
        User user = userRepository.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (emailMobile.contains("@")) {
            //email
            if (!user.isEmailVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_EMAIL_NOT_VERIFIED);
            }

        } else {
            //mobile
            if (!user.isMobileVerified()) {
                throw new BusinessLogicException(ErrorEnum.USER_MOBILE_NOT_VERIFIED);
            }
        }

        String linkParam = null;
        String code = null;
        Integer maxTryCount = null;
        if(emailMobile.contains("@")){
            if(byCode) {
                maxTryCount = metaChangeEmailMaxTryCount;
                code = generateVerificationCode();
            }else{
                // linkParam is only available for email
                linkParam = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
            }
        }else{
            // if ContactType.MOBILE then twilio will create the code and only user and twilio will know it. In that case
            // during code verification, auth server will dispatch the code verification to twilio and get the result
            // maxTryCount cannot be null for MOBILE
            // twilio's default max try count
            maxTryCount = metaChangeSmsMaxTryCount;
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .metaOperationType(MetaOperationType.PASSWORD_RESET)
                .contactType(emailMobile.contains("@") ? ContactType.EMAIL : ContactType.MOBILE)
                .executed(false)
                .expirationDate(Instant.now().plus(Duration.ofMinutes(passwordResetCodeValidityMinutes)))
                .maxTryCount(maxTryCount)
                .tryCount(maxTryCount == null ? null : 0)
                .code(code)
                .linkParam(linkParam)
                .createdDate(Instant.now())
                .userId(user.getId())
                .build();
        userMetaChangeService.create(metaChange);

        if (emailMobile.contains("@")) {
            //email
            if (byCode) {
                messageJobService.sendPasswordResetCodeMail(user.getEmail(), metaChange.getCode(),
                        user.getUserSetting().getLanguageCode(), metaChange.getId());
            } else {
                messageJobService.sendPasswordResetLinkMail(user.getEmail(), metaChange.getLinkParam(),
                        user.getUserSetting().getLanguageCode(), metaChange.getId());
            }
        } else {
            //mobile
            messageJobService.sendVerificationCodeSMSViaTwilio(user.getMobile(), metaChange.getId());
        }

        return metaChange;

    }


    @Transactional
    public TokensResponseDto resetPasswordByCode(@Valid PasswordResetByCodeDto passwordResetByCodeDto) {
        String registeredClientId;
        String registeredClientSecret;
        if(passwordResetByCodeDto.getClientId() != null){
            registeredClientId = passwordResetByCodeDto.getClientId();
            registeredClientSecret = passwordResetByCodeDto.getClientSecret();
        }else{
            List<RegisteredClient> defaultClientList =
                    registeredClientRepository.findByClientType(RegisteredClientType.DEFAULT);
            if(defaultClientList.isEmpty()){
                throw new BusinessLogicException(ErrorEnum.AUTH_CLIENT_INVALID);
            }
            registeredClientId = defaultClientList.getFirst().getClientId();
            registeredClientSecret = defaultClientList.getFirst().getClientSecret();
        }
        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(userRepository
                        .getUserByEmailOrMobile(passwordResetByCodeDto.getEmailMobile()).orElseThrow(() ->
                                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST)
                        ).getId(), MetaOperationType.PASSWORD_RESET).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));

        validatePasswordResetUserMetaChange(userMetaChange, passwordResetByCodeDto.getCode());

        saveNewPassword(userMetaChange, passwordResetByCodeDto.getNewPassword(), passwordResetByCodeDto.getRepeatNewPassword());
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        return userTokenService.validateRegisteredClientAndGetTokens(user, registeredClientId, registeredClientSecret);

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

    @Transactional
    public void updateUserPersonalInfo(String userId, @Valid  UserPersonalInfoDto userPersonalInfoDto) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        user.setName(userPersonalInfoDto.getName());
        user.setSurname(userPersonalInfoDto.getSurname());
        user.setBirthdate(userPersonalInfoDto.getBirthdate() != null ?
                          LocalDate.parse(userPersonalInfoDto.getBirthdate(),
                DateTimeFormatter.ofPattern("dd/MM/yyyy",
                Locale.ENGLISH)).atStartOfDay().toInstant(ZoneOffset.UTC) : null);
        user.setGender(userPersonalInfoDto.getGender());
        userRepository.save(user);
    }

    @Transactional
    public void updateUserLang(@NotBlank String userId,@NotBlank String langCode) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        String normalizedLanguageCode = normalizeCode(langCode);
        localizationSupportedLangRepository.findByLanguageCode(normalizedLanguageCode)
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_UNSUPPORTED_LANGUAGE));

        user.getUserSetting().setLanguageCode(normalizedLanguageCode);
        userRepository.save(user);
    }

    @Transactional
    public void upsertUserFcmToken(@NotBlank String userId, @NotBlank String fcmToken,
                                   @NotBlank String jti, @NotBlank String firebaseInstallationId) {
        getUserById(userId).orElseThrow(() -> new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        String registeredClientId = userTokenService.findByJTI(jti).orElseThrow().getClientId();

        try {
        // delete existing records with the same token
        Optional<UserFcmToken> existingFcmToken =
                userFcmTokenRepository.findByFcmToken(fcmToken);
        if (existingFcmToken.isPresent()) {
            UserFcmToken existing = existingFcmToken.get();
            if(existing.getUserId().equals(userId) && existing.getFirebaseInstallationId().equals(firebaseInstallationId)) {
                // nothing to update
                return;
            }
            if (!existing.getFirebaseInstallationId().equals(firebaseInstallationId)) {
                userFcmTokenRepository.delete(existing);
                userFcmTokenRepository.flush(); // Force the delete before the update
            }
        }

        // now we are sure the token is unique
        // This blocks other threads from touching this specific installationId
        UserFcmToken fcmEntry = userFcmTokenRepository.findByInstallationIdForUpdate(firebaseInstallationId)
                                          .orElseGet(() -> UserFcmToken.builder()
                                                                       .firebaseInstallationId(firebaseInstallationId)
                                                                       .build());

        // The "Eventual Setter" logic: Just overwrite with the latest
        fcmEntry.setUserId(userId);
        fcmEntry.setFcmToken(fcmToken);
        fcmEntry.setRegisteredClientId(registeredClientId);
        fcmEntry.setUpdatedAt(Instant.now());

        userFcmTokenRepository.save(fcmEntry);
        } catch (PessimisticLockingFailureException e) {
            // Log it—this means the "Take Turns" queue was too long
            LOGGER.warn("Lock timeout for installationId {}. Skipping update because another update is in progress.",
                    firebaseInstallationId);
        }
    }

    @Transactional(readOnly = true)
    public List<UserFcmTokenResponseDto> getUserFcmTokens(@NotBlank String userId) {
        List<UserFcmToken> userFcmTokenOpt = userFcmTokenRepository.findByUserId(userId);
        return userFcmTokenOpt.stream().map(userFcmToken -> UserFcmTokenResponseDto.builder()
                                                                                   .fcmToken(userFcmToken.getFcmToken())
                                                                                   .userId(userFcmToken.getUserId())
                                                                                   .clientId(userFcmToken.getRegisteredClientId())
                                                                                   .clientType(registeredClientRepository.findByClientId(userFcmToken.getRegisteredClientId()).get().getClientType().name())
                                                                                   .updatedAt(userFcmToken.getUpdatedAt())
                                                                                   .build()).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<UserFcmTokenResponseDto>> getUsersFcmTokens(@NotEmpty List<String> userIds) {
        List<UserFcmToken> userFcmTokenOpt = userFcmTokenRepository.findByUserIdIn(userIds);
        return userFcmTokenOpt.stream().map(userFcmToken -> UserFcmTokenResponseDto.builder()
                                                                                   .fcmToken(userFcmToken.getFcmToken())
                                                                                   .userId(userFcmToken.getUserId())
                                                                                   .clientId(userFcmToken.getRegisteredClientId())
                                                                                   .clientType(registeredClientRepository.findByClientId(userFcmToken.getRegisteredClientId()).get().getClientType().name())
                                                                                   .updatedAt(userFcmToken.getUpdatedAt())
                                                                                   .build()).collect(Collectors.groupingBy(UserFcmTokenResponseDto::getUserId));
    }

    @Transactional(readOnly = true)
    public List<AdmUserFcmTokenDto> getAdmUserFcmTokens() {
        List<AdmUserFcmTokenDto> tokens = userFcmTokenRepository.findAllForAdminList().stream()
                .map(token -> AdmUserFcmTokenDto.builder()
                        .userId(token.getUserId())
                        .userEmail(token.getUserEmail())
                        .userMobile(token.getUserMobile())
                        .registeredClientId(token.getRegisteredClientId())
                        .fcmToken(token.getFcmToken())
                        .firebaseInstallationId(token.getFirebaseInstallationId())
                        .updatedAt(token.getUpdatedAt())
                        .build())
                .toList();

        Map<String, List<String>> userRolesByUserId = userRepository.findAllById(
                        tokens.stream().map(AdmUserFcmTokenDto::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user.getUserRoles() == null ? List.of() :
                        user.getUserRoles().stream()
                                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                                .map(userRole -> userRole.getRole().getName())
                                .sorted(String::compareToIgnoreCase)
                                .toList()));

        tokens.forEach(token -> token.setUserRoles(userRolesByUserId.getOrDefault(token.getUserId(), List.of())));
        return tokens;
    }

    @Transactional
    public void deleteFcmTokens(@NotEmpty List<String> fcmTokens) {
        int deletedCount = userFcmTokenRepository.deleteByFcmTokenIn(fcmTokens);
        log.info("Deleted {} FCM tokens out of {} ", deletedCount, fcmTokens.size());
    }

    @Transactional(readOnly = true)
    public List<AdmUserFcmTokenDto> searchAdmUserFcmTokens(AdmUserFcmTokenSearchCriteria criteria) {
        List<AdmUserFcmTokenDto> tokens = userFcmTokenRepository.searchForAdminList(
                        normalizeSearchTerm(criteria.getUserId()),
                        normalizeSearchTerm(criteria.getUserEmail()),
                        normalizeSearchTerm(criteria.getUserMobile()),
                        normalizeSearchTerm(criteria.getUserRole()),
                        normalizeSearchTerm(criteria.getFirebaseInstallationId()),
                        normalizeSearchTerm(criteria.getFcmToken()))
                .stream()
                .map(token -> AdmUserFcmTokenDto.builder()
                        .userId(token.getUserId())
                        .userEmail(token.getUserEmail())
                        .userMobile(token.getUserMobile())
                        .registeredClientId(token.getRegisteredClientId())
                        .fcmToken(token.getFcmToken())
                        .firebaseInstallationId(token.getFirebaseInstallationId())
                        .updatedAt(token.getUpdatedAt())
                        .build())
                .toList();

        Map<String, List<String>> userRolesByUserId = userRepository.findAllById(
                        tokens.stream().map(AdmUserFcmTokenDto::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user.getUserRoles() == null ? List.of() :
                        user.getUserRoles().stream()
                                .filter(userRole -> userRole.getRole() != null && userRole.getRole().getName() != null)
                                .map(userRole -> userRole.getRole().getName())
                                .sorted(String::compareToIgnoreCase)
                                .toList()));

        tokens.forEach(token -> token.setUserRoles(userRolesByUserId.getOrDefault(token.getUserId(), List.of())));
        return tokens;
    }

    @Transactional
    public UserMetaChange validatePasswordResetLinkParam(String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validatePasswordResetUserMetaChange(userMetaChange, null);
        return userMetaChange;
    }

    private void validatePasswordResetUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.PASSWORD_RESET)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        serviceUtils.validateUserMetaChange(userMetaChange, code);
    }

    private String normalizeSearchTerm(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
    }

    private void saveNewPassword(@NotNull UserMetaChange userMetaChange, String newPassword, String repeatNewPassword) {
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (!user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (!newPassword.equals(repeatNewPassword)) {
            throw new BusinessLogicException(ErrorEnum.USER_PASSWORD_CHANGE_NEW_PASSWORD_MISMATCH);
        }

        user.setPassword(new BCryptPasswordEncoder().encode(newPassword));
        user.setAuthProvider(AuthProviderType.KURTUBA);
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(Instant.now());
        userMetaChangeService.create(userMetaChange);

        if (StringUtils.hasLength(user.getEmail())) {
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.PASSWORD_RESET,
                    user.getUserSetting().getLanguageCode(), userMetaChange.getId());
        }

        //todo uncomment after SMS integration
        /*if(StringUtils.hasLength(user.getMobile())){
            messageJobService.sendUserMetaChangeNotificationSMS(user.getEmail(), MetaOperationType.PASSWORD_RESET);
        }*/

    }

    @Transactional
    public User saveUser(User user) {
       user.setEmail(StringUtils.hasLength(user.getEmail()) ? user.getEmail() : null);
       user.setMobile(StringUtils.hasLength(user.getMobile()) ? user.getMobile() : null);
       user.setUsername(StringUtils.hasLength(user.getUsername()) ? user.getUsername() : null);
       return userRepository.save(user);
    }

    @Transactional
    public User updateAdminSecurityAndActivity(String userId,
                                               boolean activated,
                                               boolean locked,
                                               boolean blocked,
                                               boolean showCaptcha,
                                               int failedLoginCount) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        user.setActivated(activated);
        user.setLocked(locked);
        boolean wasBlocked = user.isBlocked();
        user.setBlocked(blocked);
        user.setShowCaptcha(showCaptcha);
        user.setFailedLoginCount(Math.max(failedLoginCount, 0));
        User savedUser = saveUser(user);
        if (blocked && !wasBlocked) {
            userTokenService.blockUsersTokens(userId);
        }
        return savedUser;
    }

    private Specification<User> buildUserAdminSpecification(UserAdminSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("userSetting", JoinType.LEFT);
                root.fetch("userRoles", JoinType.LEFT).fetch("role", JoinType.LEFT);
                query.distinct(true);
            }

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            addContainsPredicate(predicates, cb, root.get("id"), criteria.getId());
            addContainsPredicate(predicates, cb, root.get("username"), criteria.getUsername());
            addContainsPredicate(predicates, cb, root.get("email"), criteria.getEmail());
            addContainsPredicate(predicates, cb, root.get("mobile"), criteria.getMobile());
            addContainsPredicate(predicates, cb, root.get("name"), criteria.getName());
            addContainsPredicate(predicates, cb, root.get("surname"), criteria.getSurname());

            if (StringUtils.hasLength(criteria.getAuthProvider())) {
                predicates.add(cb.equal(root.get("authProvider"), AuthProviderType.valueOf(criteria.getAuthProvider())));
            }

            addBooleanFilterPredicate(predicates, cb, root.get("activated"), criteria.getActivated());
            addBooleanFilterPredicate(predicates, cb, root.get("locked"), criteria.getLocked());
            addBooleanFilterPredicate(predicates, cb, root.get("blocked"), criteria.getBlocked());
            addBooleanFilterPredicate(predicates, cb, root.get("showCaptcha"), criteria.getShowCaptcha());
            addBooleanFilterPredicate(predicates, cb, root.get("emailVerified"), criteria.getEmailVerified());
            addBooleanFilterPredicate(predicates, cb, root.get("mobileVerified"), criteria.getMobileVerified());

            if (StringUtils.hasLength(criteria.getLocale())) {
                var userSettingJoin = root.join("userSetting", JoinType.LEFT);
                String normalizedLocale = "%" + criteria.getLocale().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(userSettingJoin.get("languageCode")), normalizedLocale),
                        cb.like(cb.lower(userSettingJoin.get("countryCode")), normalizedLocale)
                ));
            }

            if (StringUtils.hasLength(criteria.getRole())) {
                var userRoleJoin = root.join("userRoles", JoinType.LEFT);
                var roleJoin = userRoleJoin.join("role", JoinType.LEFT);
                predicates.add(cb.like(cb.lower(roleJoin.get("name")), "%" + criteria.getRole().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void addContainsPredicate(List<jakarta.persistence.criteria.Predicate> predicates,
                                      jakarta.persistence.criteria.CriteriaBuilder cb,
                                      jakarta.persistence.criteria.Path<String> path,
                                      String value) {
        if (StringUtils.hasLength(value)) {
            predicates.add(cb.like(cb.lower(path), "%" + value.toLowerCase() + "%"));
        }
    }

    private void addBooleanFilterPredicate(List<jakarta.persistence.criteria.Predicate> predicates,
                                           jakarta.persistence.criteria.CriteriaBuilder cb,
                                           jakarta.persistence.criteria.Path<Boolean> path,
                                           String value) {
        if (!StringUtils.hasLength(value) || "all".equalsIgnoreCase(value)) {
            return;
        }
        predicates.add("yes".equalsIgnoreCase(value) ? cb.isTrue(path) : cb.isFalse(path));
    }


    /**
     * Verify email by rest request. User must enter the code mailed to them
     *
     * @param userId
     * @param code
     * @return
     */
    @Transactional
    public UserDto verifyEmailByCode(@NotBlank String userId, @NotBlank String code) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(user.getId(), MetaOperationType.EMAIL_CHANGE).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));

        validateEmailChangeUserMetaChange(userMetaChange, code);

        return saveNewEmail(userMetaChange, user);
    }

    @Transactional
    public UserMetaChange verifyEmailByLink(@NotBlank String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validateEmailChangeUserMetaChange(userMetaChange, null);
        User user = userRepository.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        saveNewEmail(userMetaChange, user);
        return userMetaChange;
    }

    private void validateEmailChangeUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.EMAIL_CHANGE)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        serviceUtils.validateUserMetaChange(userMetaChange, code);
    }

    private UserDto saveNewEmail(UserMetaChange userMetaChange, User user) {

        if (StringUtils.hasLength(user.getEmail())) {
            //send change notification mail to old e-mail
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.EMAIL_CHANGE,
                    user.getUserSetting().getLanguageCode(), userMetaChange.getId());
        }

        user.setEmail(userMetaChange.getMeta());
        user.setEmailVerified(true);
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(Instant.now());
        userMetaChangeService.update(userMetaChange);

        return userMapper.mapToUserDto(user);
    }

    /**
     * User registered and there is already a verified email(email change operation)
     *
     * @param userId
     * @param mobile
     * @return
     */
    @Transactional
    public UserMetaChange requestChangeMobile(String userId, String mobile) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked() || user.isLocked() || user.isShowCaptcha() || !user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        if (userRepository.getUserByMobile(mobile).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_MOBILE_ALREADY_EXISTS);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.MOBILE_CHANGE)
                .contactType(ContactType.MOBILE)
                .meta(mobile)
                .executed(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plus(Duration.ofMinutes(emailChangeCodeValidityMinutes)))
                .maxTryCount(metaChangeSmsMaxTryCount)
                .tryCount(0)
                .code(null)
                .linkParam(null)
                .build();

        userMetaChangeService.create(metaChange);
        messageJobService.sendVerificationCodeSMSViaTwilio(mobile, metaChange.getId());

        return metaChange;
    }

    /**
     * Verify mobile by rest request. User must enter the code sent to them
     *
     * @param userId
     * @param code
     * @return
     */
    @Transactional
    public UserDto verifyMobileByCode(@NotBlank String userId, @NotBlank String code) {
        User user = userRepository.getUserById(userId).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        UserMetaChange userMetaChange = userMetaChangeService
                .findActiveMetaChangeOperationForUser(user.getId(), MetaOperationType.MOBILE_CHANGE).orElseThrow(() ->
                        new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));

        validateMobileChangeUserMetaChange(userMetaChange, code);

        return saveNewMobile(userMetaChange, user);
    }

    private void validateMobileChangeUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.MOBILE_CHANGE)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        serviceUtils.validateUserMetaChange(userMetaChange, code);
    }
    private UserDto saveNewMobile(UserMetaChange userMetaChange, User user) {

        if (StringUtils.hasLength(user.getMobile())) {
            //send change notification mail to user
            messageJobService.sendUserMetaChangeNotificationMail(user.getEmail(), MetaOperationType.MOBILE_CHANGE,
                    user.getUserSetting().getLanguageCode(), userMetaChange.getId());
        }

        user.setMobile(userMetaChange.getMeta());
        user.setMobileVerified(true);
        userRepository.save(user);
        userMetaChange.setExecuted(true);
        userMetaChange.setUpdatedDate(Instant.now());
        userMetaChangeService.update(userMetaChange);

        return userMapper.mapToUserDto(user);
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
