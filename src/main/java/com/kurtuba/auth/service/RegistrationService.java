package com.kurtuba.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kurtuba.auth.data.dto.AvailableLocalizationOptionsDto;
import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.RegistrationOtherProviderDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.mapper.UserMapper;
import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import com.kurtuba.auth.data.model.LocalizationSupportedLang;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.repository.LocalizationSupportedCountryRepository;
import com.kurtuba.auth.data.repository.LocalizationSupportedLangRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.ServiceUtils;
import com.kurtuba.auth.utils.TokenUtils;
import com.kurtuba.auth.utils.annotation.EmailMobile;
import com.kurtuba.auth.utils.annotation.MobileNumber;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.kurtuba.auth.utils.Utils.generateVerificationCode;

@Service
public class RegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    final
    UserService userService;

    final
    UserMetaChangeService userMetaChangeService;

    final
    AuthenticationService authenticationService;

    final
    UserTokenService userTokenService;

    final
    MessageJobService messageJobService;

    final
    UserRoleService userRoleService;

    final
    LocalizationSupportedCountryRepository localizationSupportedCountryRepository;

    final
    LocalizationSupportedLangRepository localizationSupportedLangRepository;

    final
    ServiceUtils serviceUtils;
    private final UserMapper userMapper;

    @Value("${kurtuba.meta-change.email-max-try-count}")
    private int metaChangeEmailMaxTryCount;
    @Value("${kurtuba.meta-change.sms-max-try-count}")
    private int metaChangeSMSMaxTryCount;
    @Value("${kurtuba.meta-change.validity.email.activation-code.minutes}")
    private int activationEmailCodeValidityMinutes;
    @Value("${kurtuba.meta-change.validity.sms.activation-code.minutes}")
    private int activationSmsCodeValidityMinutes;
    @Value("${kurtuba.auth-provider.google.client-id:}")
    private String googleClientId;
    @Value("${kurtuba.auth-provider.google.client-secret:}")
    private String googleClientSecret;
    @Value("${kurtuba.auth-provider.facebook.client-id:}")
    private String facebookClientId;
    @Value("${kurtuba.auth-provider.facebook.client-secret:}")
    private String facebookClientSecret;

    public RegistrationService(UserService userService, UserMetaChangeService userMetaChangeService, AuthenticationService authenticationService, UserTokenService userTokenService, MessageJobService messageJobService, UserRoleService userRoleService, LocalizationSupportedCountryRepository localizationSupportedCountryRepository, LocalizationSupportedLangRepository localizationSupportedLangRepository, ServiceUtils serviceUtils,
                               UserMapper userMapper) {
        this.userService = userService;
        this.userMetaChangeService = userMetaChangeService;
        this.authenticationService = authenticationService;
        this.userTokenService = userTokenService;
        this.messageJobService = messageJobService;
        this.userRoleService = userRoleService;
        this.localizationSupportedCountryRepository = localizationSupportedCountryRepository;
        this.localizationSupportedLangRepository = localizationSupportedLangRepository;
        this.serviceUtils = serviceUtils;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserMetaChange register(@Valid RegistrationDto registrationDto) {

        validateRegistrationRequirements(registrationDto);

        String pass = registrationDto.getPassword();
        registrationDto.setPassword(new BCryptPasswordEncoder().encode(pass));

        User user = userMapper.maptoUser(registrationDto);
        normalizeUserLocalization(user);
        user.getUserSetting().setCanChangeUsername(!StringUtils.hasLength(user.getUsername()));
        user.getUserSetting().setCreatedDate(Instant.now());
        user.getUserSetting().setUser(user);

        //first save user and userSettings
        User savedUser = userService.saveUser(user);

        // and then save user roles
        user.setUserRoles(List.of(
                userRoleService.create(UserRole.builder()
                        .user(user)
                        .role(Role.builder().name(AuthoritiesType.USER.name()).build())
                        .createdDate(Instant.now())
                        .build())
        ));

        if (registrationDto.getPreferredVerificationContact().equals(ContactType.EMAIL)) {
            return sendAccountActivationMail(savedUser.getEmail(), registrationDto.isVerificationByCode());
        } else {
            return sendAccountActivationSMS(savedUser.getMobile());
        }
    }

    private void validateRegistrationRequirements(RegistrationDto newUser) {
        if (StringUtils.hasLength(newUser.getEmail()) && userService.getUserByEmail(newUser.getEmail()).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_EMAIL_ALREADY_EXISTS);
        }

        if (StringUtils.hasLength(newUser.getMobile()) && userService.getUserByMobile(newUser.getMobile()).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_MOBILE_ALREADY_EXISTS);
        }

        if (StringUtils.hasLength(newUser.getUsername()) && userService.getUserByUsername(newUser.getUsername()).isPresent()) {
            throw new BusinessLogicException(ErrorEnum.USER_USERNAME_ALREADY_EXISTS);
        }

        if (!StringUtils.hasLength(newUser.getEmail()) && !StringUtils.hasLength(newUser.getMobile())) {
            throw new BusinessLogicException(ErrorEnum.USER_CONTACT_REQUIRED);
        }

        if (newUser.getPreferredVerificationContact().equals(ContactType.EMAIL) && !StringUtils.hasLength(newUser.getEmail())) {
            throw new BusinessLogicException(ErrorEnum.USER_CONTACT_REQUIRED);
        }

        if (newUser.getPreferredVerificationContact().equals(ContactType.MOBILE) && !StringUtils.hasLength(newUser.getMobile())) {
            throw new BusinessLogicException(ErrorEnum.USER_CONTACT_REQUIRED);
        }
    }


    @Transactional
    public User registerByAnotherProvider(@Valid RegistrationOtherProviderDto newUserByOtherProvider) {

        RegistrationDto decodedUser = null;
        if (newUserByOtherProvider.getProvider().equals(AuthProviderType.GOOGLE)) {
            try {
                decodedUser = decodeGoogleRegistration(newUserByOtherProvider);
            } catch (Exception e) {
                LOGGER.warn("Google token registration failed", e);
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN, e);
            }
        }
        if (newUserByOtherProvider.getProvider().equals(AuthProviderType.FACEBOOK)) {
            try {
                decodedUser = decodeFacebookRegistration(newUserByOtherProvider);
            } catch (Exception e) {
                LOGGER.warn("Facebook token registration failed", e);
                throw new BusinessLogicException(ErrorEnum.USER_OTHER_PROVIDER_INVALID_TOKEN, e);
            }
        }

        //else check twitter etc. then...

        //check if user already exists
        User existingUser = userService.getUserByEmail(decodedUser.getEmail()).orElse(null);

        if (existingUser == null) {
            //this user never existed, let make one and return a token
            User user = userMapper.maptoUser(decodedUser);
            user.getUserSetting().setLanguageCode(newUserByOtherProvider.getLanguageCode());
            user.getUserSetting().setCountryCode(newUserByOtherProvider.getCountryCode());
            normalizeUserLocalization(user);
            user.setEmailVerified(true);
            user.setPassword(new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()));
            /*String provisionalUsername = user.getEmail().split("@")[0];
            if (provisionalUsername.length() > 25) {
                provisionalUsername = provisionalUsername.substring(0, 25);
            }
            user.setUsername(provisionalUsername + "." + generateRandomAlphanumericString(6));*/
            user.getUserSetting().setCanChangeUsername(true);
            user.getUserSetting().setCreatedDate(Instant.now());
            user.getUserSetting().setUser(user);
            user.setActivated(true);
            // save user and userSettings
            userService.saveUser(user);
            user.setUserRoles(List.of(
                    UserRole.builder()
                            .user(user)
                            .role(Role.builder().name(AuthoritiesType.USER.name()).build())
                            .createdDate(Instant.now())
                            .build()));
            // save roles
            userRoleService.create(user.getUserRoles().get(0));
            return user;
        }

        // todo: activated, locked and showCaptcha can be handled here but must use the same implementation as in
        //  register/login
        if (existingUser.isBlocked() || !existingUser.isActivated() || existingUser.isLocked() || existingUser.isShowCaptcha()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

       // todo: uncomment this to control user-provider-relationship
       /* if (existingUser.getAuthProvider().equals(newUserByOtherProvider.getProvider())) {
            return existingUser;
        }*/

        // same email is treated as the same account across manual and social providers
        return existingUser;

    }

    public AvailableLocalizationOptionsDto getAvailableLocales() {
        return AvailableLocalizationOptionsDto.builder()
                .languages(localizationSupportedLangRepository.findAllByOrderByLanguageCodeAsc().stream()
                        .map(LocalizationSupportedLang::getLanguageCode)
                        .toList())
                .countries(localizationSupportedCountryRepository.findAllByOrderByCountryCodeAsc().stream()
                        .map(LocalizationSupportedCountry::getCountryCode)
                        .toList())
                .build();
    }

    RegistrationDto decodeGoogleRegistration(RegistrationOtherProviderDto request) {
        String token = request.getToken();
        if (!StringUtils.hasLength(token)) {
            token = exchangeGoogleAuthorizationCodeForIdToken(request);
        }

        return decodeGoogleRegistrationFromIdToken(token, request.getProviderClientId());
    }

    RegistrationDto decodeGoogleRegistrationFromIdToken(String token, String clientId) {
        try {
            return TokenUtils.decodeGoogleToken(token, clientId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    RegistrationDto decodeFacebookRegistration(RegistrationOtherProviderDto request) {
        String token = request.getToken();
        if (!StringUtils.hasLength(token)) {
            token = exchangeFacebookAuthorizationCodeForAccessToken(request);
        }

        Map<String, Object> jsonUser = fetchFacebookUserData(token);
        RegistrationDto decodedUser = new RegistrationDto();
        decodedUser.setEmail((String) jsonUser.get("email"));
        decodedUser.setName((String) jsonUser.get("first_name"));
        decodedUser.setSurname((String) jsonUser.get("last_name"));
        decodedUser.setAuthProvider(AuthProviderType.FACEBOOK);

        if (!StringUtils.hasLength(decodedUser.getEmail())) {
            throw new IllegalArgumentException("Facebook user info is missing email");
        }

        if (!StringUtils.hasLength(decodedUser.getName()) && jsonUser.get("name") instanceof String fullName) {
            decodedUser.setName(fullName);
        }

        return decodedUser;
    }

    String exchangeGoogleAuthorizationCodeForIdToken(RegistrationOtherProviderDto request) {
        validateAuthorizationCodeRequest(request, googleClientId, googleClientSecret, "Google");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add(OAuth2ParameterNames.CLIENT_ID, googleClientId);
        formData.add(OAuth2ParameterNames.CLIENT_SECRET, googleClientSecret);
        formData.add(OAuth2ParameterNames.CODE, request.getAuthorizationCode());
        formData.add(OAuth2ParameterNames.REDIRECT_URI, request.getRedirectUri());
        formData.add(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");

        Map<String, Object> tokenResponse = readMapResponse(RestClient.create("https://oauth2.googleapis.com")
                .post()
                .uri("/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(String.class), "Google token response");

        Object idToken = tokenResponse.get("id_token");
        if (!(idToken instanceof String idTokenString) || !StringUtils.hasLength(idTokenString)) {
            throw new IllegalArgumentException("Google token response is missing id_token");
        }
        return idTokenString;
    }

    String exchangeFacebookAuthorizationCodeForAccessToken(RegistrationOtherProviderDto request) {
        validateAuthorizationCodeRequest(request, facebookClientId, facebookClientSecret, "Facebook");

        Map<String, Object> tokenResponse = readMapResponse(RestClient.create("https://graph.facebook.com")
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v25.0/oauth/access_token")
                        .queryParam("client_id", facebookClientId)
                        .queryParam("client_secret", facebookClientSecret)
                        .queryParam("redirect_uri", request.getRedirectUri())
                        .queryParam("code", request.getAuthorizationCode())
                        .build())
                .retrieve()
                .body(String.class), "Facebook token response");

        Object accessToken = tokenResponse.get("access_token");
        if (!(accessToken instanceof String accessTokenString) || !StringUtils.hasLength(accessTokenString)) {
            throw new IllegalArgumentException("Facebook token response is missing access_token");
        }
        return accessTokenString;
    }

    Map<String, Object> fetchFacebookUserData(String token) {
        String responseBody = RestClient.create("https://graph.facebook.com")
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me")
                        .queryParam("fields", "email,first_name,last_name,name")
                        .queryParam("access_token", token)
                        .build())
                .retrieve()
                .body(String.class);

        Map<String, Object> response;
        try {
            response = OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Facebook user info response is invalid", e);
        }

        if (response == null || response.isEmpty()) {
            throw new IllegalArgumentException("Facebook user info response is empty");
        }

        return response;
    }

    private void validateAuthorizationCodeRequest(RegistrationOtherProviderDto request,
                                                  String configuredClientId,
                                                  String configuredClientSecret,
                                                  String providerName) {
        if (!StringUtils.hasLength(request.getAuthorizationCode())) {
            throw new IllegalArgumentException(providerName + " authorization code is missing");
        }
        if (!StringUtils.hasLength(request.getRedirectUri())) {
            throw new IllegalArgumentException(providerName + " redirect URI is missing");
        }
        if (!StringUtils.hasLength(configuredClientId) || !StringUtils.hasLength(configuredClientSecret)) {
            throw new IllegalStateException(providerName + " OAuth client is not configured on the server");
        }
        if (!configuredClientId.equals(request.getProviderClientId())) {
            throw new IllegalArgumentException(providerName + " client ID does not match server configuration");
        }
    }

    private Map<String, Object> readMapResponse(String responseBody, String responseName) {
        try {
            return OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(responseName + " is invalid", e);
        }
    }

    @Transactional
    public TokensResponseDto activateAccountByCode(String emailMobile, String code, String clientId, String clientSecret) {
        User user = userService.getUserByEmailOrMobile(emailMobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        UserMetaChange userMetaChange = userMetaChangeService.findActiveMetaChangeOperationForUser(user.getId(),
                MetaOperationType.ACCOUNT_ACTIVATION).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validateAccountActivationUserMetaChange(userMetaChange, code);
        return activateAccount(user, userMetaChange, clientId, clientSecret);
    }

    @Transactional
    public UserMetaChange activateAccountByLink(String linkParam) {
        UserMetaChange userMetaChange = userMetaChangeService.findByLinkParam(linkParam).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION));
        validateAccountActivationUserMetaChange(userMetaChange, null);
        User user = userService.getUserById(userMetaChange.getUserId()).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));
        activateAccount(user, userMetaChange, null, null);
        return userMetaChange;
    }

    private void validateAccountActivationUserMetaChange(UserMetaChange userMetaChange, String code) {

        if (!userMetaChange.getMetaOperationType().equals(MetaOperationType.ACCOUNT_ACTIVATION)) {
            throw new BusinessLogicException(ErrorEnum.USER_META_CHANGE_INVALID_OPERATION);
        }

        serviceUtils.validateUserMetaChange(userMetaChange, code);
    }

    private TokensResponseDto activateAccount(User user, UserMetaChange userMetaChange, String clientId, String clientSecret) {

        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        user.setActivated(true);
        if (userMetaChange.getContactType().equals(ContactType.EMAIL)) {
            user.setEmailVerified(true);
        } else {
            user.setMobileVerified(true);
        }
        userService.saveUser(user);
        userMetaChange.setUpdatedDate(Instant.now());
        userMetaChange.setExecuted(true);
        userMetaChangeService.update(userMetaChange);

        return userTokenService.validateRegisteredClientAndGetTokens(user, clientId, clientSecret);

    }

    @Transactional
    public UserMetaChange sendAccountActivationMessage(@EmailMobile String emailMobile, boolean byCode) {
        if (emailMobile.contains("@")) {
            //email
            return sendAccountActivationMail(emailMobile, byCode);
        } else {
            return sendAccountActivationSMS(emailMobile);
        }

    }

    /**
     * Send activation MAIL to the user's email address
     * May contain code or link
     *
     * @return
     */
    @Transactional
    public UserMetaChange sendAccountActivationMail(@NotBlank String email, boolean byCode) {

        User user = userService.getUserByEmail(email).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));


        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (user.isActivated()) {
            // already activated
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(ContactType.EMAIL)
                .meta(user.getEmail())
                .executed(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plus(Duration.ofMinutes(activationEmailCodeValidityMinutes)))
                .maxTryCount(byCode ? metaChangeEmailMaxTryCount : null)
                .tryCount(byCode ? 0 : null)
                .code(byCode ? generateVerificationCode() : null)
                .linkParam(!byCode ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();
        metaChange = userMetaChangeService.create(metaChange);
        if (byCode) {
            messageJobService.sendAccountActivationCodeMail(user.getEmail(), metaChange.getCode(),
                    user.getUserSetting().getLanguageCode(), metaChange.getId());
        } else {
            messageJobService.sendAccountActivationLinkMail(user.getEmail(), metaChange.getLinkParam(),
                    user.getUserSetting().getLanguageCode(), metaChange.getId());
        }

        return metaChange;


    }

    /**
     * Send activation SMS to the user's mobile number
     * May contain code
     *
     * @return
     */
    @Transactional
    public UserMetaChange sendAccountActivationSMS(@MobileNumber String mobile) {

        if(!mobile.startsWith("+")){
            throw new BusinessLogicException(ErrorEnum.INVALID_MOBILE_NUMBER_FORMAT);
        }

        User user = userService.getUserByEmailOrMobile(mobile).orElseThrow(() ->
                new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST));

        if (user.isBlocked()) {
            throw new BusinessLogicException(ErrorEnum.USER_BLOCKED);
        }

        if (user.isActivated()) {
            // already activated
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(user.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(ContactType.MOBILE)
                .meta(user.getMobile())
                .executed(false)
                .createdDate(Instant.now())
                .expirationDate(Instant.now().plus(Duration.ofMinutes(activationSmsCodeValidityMinutes)))
                .maxTryCount(metaChangeSMSMaxTryCount)
                .tryCount(0)
                .code(null)
                .linkParam(null)
                .build();

        metaChange = userMetaChangeService.create(metaChange);
        messageJobService.sendVerificationCodeSMSViaTwilio(user.getMobile(), metaChange.getId());
        return metaChange;
    }

    private void normalizeUserLocalization(User user) {
        String normalizedLanguageCode = normalizeCode(user.getUserSetting().getLanguageCode());
        String normalizedCountryCode = normalizeCode(user.getUserSetting().getCountryCode());

        localizationSupportedLangRepository.findByLanguageCode(normalizedLanguageCode)
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_UNSUPPORTED_LANGUAGE));
        localizationSupportedCountryRepository.findByCountryCode(normalizedCountryCode)
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_UNSUPPORTED_REGION));

        user.getUserSetting().setLanguageCode(normalizedLanguageCode);
        user.getUserSetting().setCountryCode(normalizedCountryCode);
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

}
