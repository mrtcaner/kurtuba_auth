package com.kurtuba.auth.service;

import com.kurtuba.auth.data.dto.RegistrationDto;
import com.kurtuba.auth.data.dto.RegistrationOtherProviderDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.enums.AuthProviderType;
import com.kurtuba.auth.data.enums.AuthoritiesType;
import com.kurtuba.auth.data.enums.ContactType;
import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.Role;
import com.kurtuba.auth.data.model.User;
import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.model.UserRole;
import com.kurtuba.auth.data.repository.LocalizationAvailableLocaleRepository;
import com.kurtuba.auth.error.enums.ErrorEnum;
import com.kurtuba.auth.error.exception.BusinessLogicException;
import com.kurtuba.auth.utils.TokenUtils;
import com.nimbusds.jose.shaded.gson.JsonObject;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.kurtuba.auth.utils.ServiceUtils.validateUserMetaChange;
import static com.kurtuba.auth.utils.Utils.generateVerificationCode;

@Service
public class RegistrationService {

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
    LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository;

    @Value("${kurtuba.meta-change.max-try-count}")
    private int metaChangeMaxTryCount;
    @Value("${kurtuba.meta-change.validity.email.activation-code.minutes}")
    private int activationCodeValidityMinutes;

    public RegistrationService(UserService userService, UserMetaChangeService userMetaChangeService, AuthenticationService authenticationService, UserTokenService userTokenService, MessageJobService messageJobService, UserRoleService userRoleService, LocalizationAvailableLocaleRepository localizationAvailableLocaleRepository) {
        this.userService = userService;
        this.userMetaChangeService = userMetaChangeService;
        this.authenticationService = authenticationService;
        this.userTokenService = userTokenService;
        this.messageJobService = messageJobService;
        this.userRoleService = userRoleService;
        this.localizationAvailableLocaleRepository = localizationAvailableLocaleRepository;
    }

    @Transactional
    public String register(@Valid RegistrationDto newUser) {

        validateRegistrationRequirements(newUser);

        String pass = newUser.getPassword();
        newUser.setPassword(new BCryptPasswordEncoder().encode(pass));

        User user = newUser.toUser();
        if (StringUtils.hasLength(user.getUsername())) {
            user.getUserSetting().setCanChangeUsername(false);
        } else {
            user.getUserSetting().setCanChangeUsername(true);
        }

        user.getUserSetting().setLocale(localizationAvailableLocaleRepository
                .findByLanguageCodeAndCountryCode(user.getUserSetting().getLocale().getLanguageCode(),
                        user.getUserSetting().getLocale().getCountryCode())
                .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_UNSUPPORTED_REGION)));
        user.getUserSetting().setCreatedDate(LocalDateTime.now());
        user.getUserSetting().setUser(user);

        //first save user and userSettings
        User savedUser = userService.saveUser(user);

        // and then save user roles
        user.setUserRoles(List.of(
                userRoleService.create(UserRole.builder()
                        .user(user)
                        .role(Role.builder().name(AuthoritiesType.USER.name()).build())
                        .createdDate(LocalDateTime.now())
                        .build())
        ));

        return createMetaChangeAndSendActivationMail(savedUser, newUser).getId();
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
    }

    private UserMetaChange createMetaChangeAndSendActivationMail(User savedUser, RegistrationDto newUser) {
        UserMetaChange metaChange = UserMetaChange.builder()
                .userId(savedUser.getId())
                .metaOperationType(MetaOperationType.ACCOUNT_ACTIVATION)
                .contactType(newUser.getPreferredVerificationContact())
                .meta(newUser.getPreferredVerificationContact().equals(ContactType.EMAIL) ? savedUser.getEmail() : savedUser.getMobile())
                .executed(false)
                .createdDate(LocalDateTime.now())
                .expirationDate(LocalDateTime.now().plusMinutes(activationCodeValidityMinutes))
                .maxTryCount(newUser.isVerificationByCode() ? metaChangeMaxTryCount : null)
                .tryCount(newUser.isVerificationByCode() ? 0 : null)
                .code(newUser.isVerificationByCode() ? generateVerificationCode() : null)
                .linkParam(!newUser.isVerificationByCode() ?
                        Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()) : null)
                .build();

        UserMetaChange savedMetaChange = userMetaChangeService.create(metaChange);

        // in case there are both email and mobile contacts, only one can be used to activate account.
        if (newUser.getPreferredVerificationContact().equals(ContactType.EMAIL)) {
            if (newUser.isVerificationByCode()) {
                messageJobService.sendAccountActivationCodeMail(savedUser.getEmail(), savedMetaChange.getCode(), savedUser.getUserSetting().getLocale().getLanguageCode());
            } else {
                messageJobService.sendAccountActivationLinkMail(savedUser.getEmail(), savedMetaChange.getLinkParam(), savedUser.getUserSetting().getLocale().getLanguageCode());
            }
        } else {
            // todo implement send sms
            throw new UnsupportedOperationException("Feature incomplete. Contact assistance.");
        }

        return savedMetaChange;
    }

    @Transactional
    public RegistrationDto registerByAnotherProvider(@Valid RegistrationOtherProviderDto newUserByOtherProvider) {

        RegistrationDto decodedUser = null;
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
                decodedUser = new RegistrationDto();
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
        User existingUser = userService.getUserByEmail(decodedUser.getEmail()).orElse(null);

        if (existingUser == null) {
            //this user never existed, let make one and return a token
            User user = decodedUser.toUser();
            user.setEmailVerified(true);
            String pass = UUID.randomUUID().toString();
            user.setPassword(new BCryptPasswordEncoder().encode(pass));
            /*String provisionalUsername = user.getEmail().split("@")[0];
            if (provisionalUsername.length() > 25) {
                provisionalUsername = provisionalUsername.substring(0, 25);
            }
            user.setUsername(provisionalUsername + "." + generateRandomAlphanumericString(6));*/
            user.getUserSetting().setCanChangeUsername(true);
            user.getUserSetting().setLocale(localizationAvailableLocaleRepository
                    .findByLanguageCodeAndCountryCode(user.getUserSetting().getLocale().getLanguageCode(),
                            user.getUserSetting().getLocale().getCountryCode())
                    .orElseThrow(() -> new BusinessLogicException(ErrorEnum.LOCALIZATION_UNSUPPORTED_REGION)));
            user.getUserSetting().setCreatedDate(LocalDateTime.now());
            user.getUserSetting().setUser(user);
            // save user and userSettings
            userService.saveUser(user);
            user.setUserRoles(List.of(
                    UserRole.builder()
                            .user(user)
                            .role(Role.builder().name(AuthoritiesType.USER.name()).build())
                            .build()));
            // save roles
            userRoleService.create(user.getUserRoles().get(0));
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
                userService.saveUser(existingUser);
                return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);
            }

        }

        //This email with different provider exists. TODO Check active, lock etc fields and return a token
        //That also means as long as user uses other providers with same email, same user will be logged in
        String pass = UUID.randomUUID().toString();
        existingUser.setPassword(new BCryptPasswordEncoder().encode(pass));
        existingUser.setAuthProvider(decodedUser.getAuthProvider());
        decodedUser.setPassword(pass);
        userService.saveUser(existingUser);
        return decodedUser;//accessTokenUtil.getAccessToken(existingUser.getEmail(), pass);

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

        validateUserMetaChange(userMetaChange, code);
    }

    private TokensResponseDto activateAccount(User user, UserMetaChange userMetaChange, String clientId, String clientSecret) {

        if (user == null) {
            throw new BusinessLogicException(ErrorEnum.USER_DOESNT_EXIST);
        }

        if (user.isActivated()) {
            throw new BusinessLogicException(ErrorEnum.USER_INVALID_STATE);
        }

        validateAccountActivationUserMetaChange(userMetaChange, null);

        user.setActivated(true);
        if (userMetaChange.getContactType().equals(ContactType.EMAIL)) {
            user.setEmailVerified(true);
        } else {
            user.setMobileVerified(true);
        }
        userService.saveUser(user);
        userMetaChange.setUpdatedDate(LocalDateTime.now());
        userMetaChange.setExecuted(true);
        userMetaChangeService.update(userMetaChange);

        return userTokenService.validateRegisteredClientAndGetTokens(user, clientId, clientSecret);

    }

}
