package com.kurtuba.auth.data.model;


import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.utils.StringSetConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "registered_client")
public class RegisteredClient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;// good old unique id. can be different in any environment

    @NotBlank
    @Column(unique=true)
    private String clientId;//this is what client(mobile-web app etc.) receives

    @NotBlank
    @Column(unique=true)
    private String clientName;//this is human-readable name of the client

    private String clientSecret;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RegisteredClientType clientType;

    @Convert(converter = StringSetConverter.class)
    private Set<String> auds; // comma seperated strings

    private boolean scopeEnabled;

    @Convert(converter = StringSetConverter.class)
    private Set<String> scopes; // comma seperated strings

    @Convert(converter = StringSetConverter.class)
    private Set<String> redirectUrls; // comma seperated strings

    @Convert(converter = StringSetConverter.class)
    private Set<String> postLogoutRedirectUrls; // comma seperated strings

    private int accessTokenTtlMinutes;

    private boolean refreshTokenEnabled;

    private int refreshTokenTtlMinutes;

    private boolean sendTokenInCookie;

    private boolean cookieHttpOnly;

    private boolean cookieSecure;

    private int cookieMaxAgeSeconds;

    private Instant createdDate;

}
