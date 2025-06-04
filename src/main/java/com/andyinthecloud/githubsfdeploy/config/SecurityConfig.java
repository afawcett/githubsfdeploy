package com.andyinthecloud.githubsfdeploy.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @SuppressWarnings("unused")
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.jsp", "/error", "/_auth", "/sfdcSetup.html", "/resources/**", "/css/**", "/js/**", "/images/**", "/fonts/**", "/assets/**").permitAll()
                .requestMatchers("/app/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/oauth2/authorization/salesforce")
                .successHandler(successHandler())
                .failureHandler(authenticationFailureHandler())
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository())
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/_auth")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService())
                )
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/_auth"),
                    new AntPathRequestMatcher("/app/githubdeploy/**")
                )
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .maximumSessions(-1)
            )
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            )
            .requestCache(cache -> cache
                .requestCache(requestCache())
            );
    
        return http.build();
    }

    @Bean
    RequestCache requestCache() {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setMatchingRequestParameterName("continue");
        return requestCache;
    }

    @Bean
    @SuppressWarnings("unused")
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    AuthenticationSuccessHandler successHandler() {
        SimpleUrlAuthenticationSuccessHandler handler = new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
                SavedRequest savedRequest = requestCache().getRequest(request, response);
                if (savedRequest != null) {
                    return savedRequest.getRedirectUrl();
                }
                return "/app/githubdeploy";
            }
        };
        handler.setDefaultTargetUrl("/app/githubdeploy");
        handler.setAlwaysUseDefaultTargetUrl(false);
        return handler;
    }

    @Bean
    AuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler("/error");
        return handler;
    }

    @Bean
    OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            Map<String, Object> attributes = new HashMap<>(user.getAttributes());
            
            // Log the full userinfo response
            System.out.println("Salesforce UserInfo Response:");
            System.out.println("----------------------------");
            attributes.forEach((key, value) -> System.out.println(key + ": " + value));
            System.out.println("----------------------------");
            
            // Add access token and instance URL to attributes
            attributes.put("access_token", request.getAccessToken().getTokenValue());
            attributes.put("instance_url", request.getAdditionalParameters().get("instance_url"));
            
            // Salesforce returns user_id as the ID field
            String userId = (String) attributes.get("user_id");
            if (userId == null) {
                // Fallback to sub if user_id is not present
                userId = (String) attributes.get("sub");
            }
            
            // Ensure we have an ID field
            if (userId == null) {
                throw new IllegalArgumentException("No user ID found in Salesforce userinfo response");
            }
            
            // Create a new user with the ID field
            return new DefaultOAuth2User(
                user.getAuthorities(),
                attributes,
                "user_id" // Use user_id as the name attribute key
            );
        };
    }
} 