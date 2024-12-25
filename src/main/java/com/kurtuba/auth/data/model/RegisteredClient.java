package com.kurtuba.auth.data.model;


import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.utils.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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

    @NotEmpty
    @Column(unique=true)
    private String clientId;//this is what client(mobile-web app) receives. must be same in dev/test/stage/prod

    @NotEmpty
    @Column(unique=true)
    private String clientName;//this is human-readable name of the client

    private String clientSecret;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RegisteredClientType clientType;

    private boolean scopeEnabled;

    @Convert(converter = StringListConverter.class)
    private List<String> scopes;

    @Convert(converter = StringListConverter.class)
    private List<String> redirectUrls;

    @Convert(converter = StringListConverter.class)
    private List<String> postLogoutRedirectUrls;

    private int accessTokenTtlMinutes;

    private boolean refreshTokenEnabled;

    private int refreshTokenTtlMinutes;

    private boolean sendTokenInCookie;

    private int cookieMaxAgeSeconds;

    private LocalDateTime createdDate;

}
