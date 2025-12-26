package com.game.contraband.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.contraband.application.auth.GenerateTokenService;
import com.game.contraband.application.auth.LoginService;
import com.game.contraband.domain.auth.TokenDecoder;
import com.game.contraband.global.auth.TokenProperties;
import com.game.contraband.global.security.core.OAuth2UserDetailsService;
import com.game.contraband.global.security.filter.OAuth2AuthenticationFilter;
import com.game.contraband.global.security.filter.OAuth2RegistrationValidateFilter;
import com.game.contraband.global.security.handler.OAuth2AccessDeniedHandler;
import com.game.contraband.global.security.handler.OAuth2AuthenticationEntryPoint;
import com.game.contraband.global.security.handler.OAuth2AuthenticationFailureHandler;
import com.game.contraband.global.security.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Profile("!dev")
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final TokenProperties tokenProperties;
    private final TokenDecoder tokenDecoder;
    private final LoginService loginService;
    private final GenerateTokenService generateTokenService;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.*").permitAll()
                    .pathMatchers(HttpMethod.GET, "/*.html").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/groups/**").permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/tabs/groups/{groupId}/tree").permitAll()
                    .anyExchange().authenticated()
            )
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(oAuth2AuthenticationEntryPoint())
                    .accessDeniedHandler(oAuth2AccessDeniedHandler())
            )
            .oauth2Login(oauth2 -> oauth2
                    .authenticationSuccessHandler(oAuth2SuccessHandler())
                    .authenticationFailureHandler(oAuth2AuthenticationFailureHandler())
            )
            .addFilterAt(oAuth2AuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(oAuth2RegistrationValidateFilter(), SecurityWebFiltersOrder.HTTP_BASIC);

        return http.build();
    }

    @Bean
    public OAuth2AuthenticationEntryPoint oAuth2AuthenticationEntryPoint() {
        return new OAuth2AuthenticationEntryPoint(objectMapper);
    }

    @Bean
    public OAuth2AccessDeniedHandler oAuth2AccessDeniedHandler() {
        return new OAuth2AccessDeniedHandler(objectMapper);
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProperties, loginService, generateTokenService);
    }

    @Bean
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler(objectMapper);
    }

    @Bean
    public OAuth2AuthenticationFilter oAuth2AuthenticationFilter() {
        return new OAuth2AuthenticationFilter(oAuth2UserDetailsService());
    }

    @Bean
    public OAuth2UserDetailsService oAuth2UserDetailsService() {
        return new OAuth2UserDetailsService(tokenDecoder);
    }

    @Bean
    public OAuth2RegistrationValidateFilter oAuth2RegistrationValidateFilter() {
        return new OAuth2RegistrationValidateFilter(objectMapper);
    }
}
