package com.kurtuba.auth.config.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomWebClientOAuth2AccessTokenResponseAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final Log logger = LogFactory.getLog(getClass());

	private int tokenCookieMaxAgeSeconds;

	public CustomWebClientOAuth2AccessTokenResponseAuthenticationSuccessHandler(int tokenCookieMaxAgeSeconds){
		this.tokenCookieMaxAgeSeconds = tokenCookieMaxAgeSeconds;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
										Authentication authentication) throws IOException {
		if (!(authentication instanceof OAuth2AccessTokenAuthenticationToken accessTokenAuthentication)) {
			if (this.logger.isErrorEnabled()) {
				this.logger.error(Authentication.class.getSimpleName() + " must be of type "
						+ OAuth2AccessTokenAuthenticationToken.class.getName() + " but was "
						+ authentication.getClass().getName());
			}
			OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
					"Unable to process the access token response.", null);
			throw new OAuth2AuthenticationException(error);
		}

		OAuth2AccessToken accessToken = accessTokenAuthentication.getAccessToken();
		ResponseCookie cookie = ResponseCookie.from("jwt", accessToken.getTokenValue())
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(tokenCookieMaxAgeSeconds)
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
		new GsonHttpMessageConverter().write("",null, httpResponse);
	}
}