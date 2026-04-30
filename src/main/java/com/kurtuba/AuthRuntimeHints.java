package com.kurtuba;

import com.kurtuba.adm.data.dto.AdmUserDto;
import com.kurtuba.adm.data.dto.AdmUserFcmTokenDto;
import com.kurtuba.adm.data.dto.RegisteredClientListRowDto;
import com.kurtuba.auth.data.dto.ResponseErrorDto;
import com.kurtuba.auth.data.dto.RoleDto;
import com.kurtuba.auth.data.dto.RegistrationResponseDto;
import com.kurtuba.auth.data.dto.TokensResponseDto;
import com.kurtuba.auth.data.dto.UserDto;
import com.kurtuba.auth.data.dto.UserMetaChangeDto;
import com.kurtuba.auth.data.dto.UserRoleDto;
import com.kurtuba.auth.data.dto.UserSettingDto;
import com.kurtuba.auth.data.dto.UserSettingLocaleDto;
import com.kurtuba.auth.utils.annotation.EmailAddressValidator;
import com.kurtuba.auth.utils.annotation.EmailMobileValidator;
import com.kurtuba.auth.utils.annotation.MobileNumberValidator;
import com.kurtuba.auth.utils.annotation.UsernameValidator;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.servlet.FlashMap;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArrayList;

class AuthRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.resources().registerPattern("jwk.json");
		hints.resources().registerPattern("com/google/i18n/phonenumbers/data/*");
		hints.reflection().registerType(EmailAddressValidator.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		hints.reflection().registerType(EmailMobileValidator.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		hints.reflection().registerType(MobileNumberValidator.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		hints.reflection().registerType(UsernameValidator.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
		registerDto(hints, ResponseErrorDto.class);
		registerDto(hints, RegistrationResponseDto.class);
		registerDto(hints, RoleDto.class);
		registerDto(hints, AdmUserFcmTokenDto.class);
		registerDto(hints, AdmUserDto.class);
		registerDto(hints, RegisteredClientListRowDto.class);
		registerDto(hints, TokensResponseDto.class);
		registerDto(hints, UserDto.class);
		registerDto(hints, UserMetaChangeDto.class);
		registerDto(hints, UserRoleDto.class);
		registerDto(hints, UserSettingDto.class);
		registerDto(hints, UserSettingLocaleDto.class);
		registerConstructor(hints, "io.jsonwebtoken.impl.DefaultJwtBuilder");
		registerConstructor(hints, "io.jsonwebtoken.impl.DefaultJwtParserBuilder");
		registerConstructor(hints, "io.jsonwebtoken.impl.DefaultClaimsBuilder");
		registerConstructor(hints, "io.jsonwebtoken.impl.DefaultClaims");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardKeyAlgorithms");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardHashAlgorithms");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardCurves");
		registerConstructor(hints, "io.jsonwebtoken.impl.security.StandardKeyOperations");
		registerConstructor(hints, "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms");
		registerJacksonType(hints, "com.twilio.exception.RestException");
		registerJacksonType(hints, "com.twilio.rest.api.v2010.account.Message");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.Verification");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.Verification$Channel");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.Verification$RiskCheck");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.Verification$Status");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.VerificationCheck");
		registerJacksonType(hints, "com.twilio.rest.verify.v2.service.VerificationCheck$Channel");
		registerGoogleJsonType(hints, "com.google.api.client.json.webtoken.JsonWebToken$Header");
		registerGoogleJsonType(hints, "com.google.api.client.json.webtoken.JsonWebSignature$Header");
		registerGoogleJsonType(hints, "com.google.api.client.json.webtoken.JsonWebToken$Payload");
		registerGoogleJsonType(hints, "com.google.api.client.auth.openidconnect.IdToken$Payload");
		registerGoogleJsonType(hints, "com.google.api.client.googleapis.auth.oauth2.GoogleIdToken$Payload");
		registerSpringdocConversionServiceType(hints,
				"org.springframework.core.convert.support.GenericConversionService");
		registerSpringdocConversionServiceType(hints,
				"org.springframework.core.convert.support.GenericConversionService$Converters");
		registerSpringdocConversionServiceType(hints,
				"org.springframework.core.convert.support.GenericConversionService$ConvertersForPair");
		hints.serialization().registerType(CopyOnWriteArrayList.class);
		hints.serialization().registerType(FlashMap.class);
		hints.serialization().registerType(LinkedHashMap.class);
		hints.serialization().registerType(LinkedHashSet.class);
		hints.serialization().registerType(SecurityContextImpl.class);
		hints.serialization().registerType(UsernamePasswordAuthenticationToken.class);
		hints.serialization().registerType(SimpleGrantedAuthority.class);
		hints.serialization().registerType(TypeReference.of("com.kurtuba.auth.data.dto.KurtubaUserDetailsDto"));
	}

	private void registerConstructor(RuntimeHints hints, String typeName) {
		hints.reflection().registerType(TypeReference.of(typeName),
				hint -> hint.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
	}

	private void registerDto(RuntimeHints hints, Class<?> type) {
		hints.reflection().registerType(type,
				hint -> hint.withMembers(
						MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INTROSPECT_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.DECLARED_FIELDS
				));
	}

	private void registerJacksonType(RuntimeHints hints, String typeName) {
		hints.reflection().registerType(TypeReference.of(typeName),
				hint -> hint.withMembers(
						MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INTROSPECT_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.DECLARED_FIELDS
				));
	}

	private void registerGoogleJsonType(RuntimeHints hints, String typeName) {
		hints.reflection().registerType(TypeReference.of(typeName),
				hint -> hint.withMembers(
						MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INTROSPECT_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS,
						MemberCategory.DECLARED_FIELDS
				));
	}

	private void registerSpringdocConversionServiceType(RuntimeHints hints, String typeName) {
		hints.reflection().registerType(TypeReference.of(typeName),
				hint -> hint.withMembers(
						MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.DECLARED_FIELDS
				));
	}
}
