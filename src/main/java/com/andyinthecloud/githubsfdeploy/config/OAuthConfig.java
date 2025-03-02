package com.andyinthecloud.githubsfdeploy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuthConfig {

    @Value("${spring.security.oauth2.client.registration.salesforce.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.salesforce.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.salesforce.scope}")
    private String scope;

    @Value("${spring.security.oauth2.client.provider.salesforce.authorization-uri}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.salesforce.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.salesforce.user-info-uri}")
    private String userInfoUri;

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration salesforceRegistration = ClientRegistration.withRegistrationId("salesforce")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/_auth")
            .scope(scope.split(","))
            .authorizationUri(authorizationUri)
            .tokenUri(tokenUri)
            .userInfoUri(userInfoUri)
            .userNameAttributeName("user_id")
            .clientName("Salesforce")
            .build();

        return new InMemoryClientRegistrationRepository(salesforceRegistration);
    }
} 