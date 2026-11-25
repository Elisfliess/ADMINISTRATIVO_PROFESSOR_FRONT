package br.com.compasso.application.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.compasso.usuario.security.LegalAcceptanceFilter;
import br.com.compasso.usuario.service.LegalDocumentService;
import tools.jackson.databind.ObjectMapper;

/**
 * Segurança stateful com cookie de sessão (D-014/SDA-006): CSRF habilitado,
 * CORS restrito a origens explícitas e sessão em memória.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] ENDPOINTS_PUBLICOS = {
            "/api/v1/auth/register",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/verification/resend",
            "/api/v1/auth/login",
            "/api/v1/auth/password-recovery",
            "/api/v1/auth/password-recovery/confirm"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CookieCsrfTokenRepository csrfTokenRepository,
            LegalDocumentService legalDocumentService,
            ObjectMapper objectMapper,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        LegalAcceptanceFilter legalAcceptanceFilter = new LegalAcceptanceFilter(legalDocumentService, objectMapper);

        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfHandler))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(contexto -> contexto
                        .securityContextRepository(securityContextRepository))
                .exceptionHandling(excecoes -> excecoes
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(HttpMethod.POST, ENDPOINTS_PUBLICOS).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterAfter(legalAcceptanceFilter, AuthorizationFilter.class)
                .build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookieName("XSRF-TOKEN");
        repositorio.setHeaderName("X-XSRF-TOKEN");
        repositorio.setCookiePath("/");
        return repositorio;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${compasso.security.cors.allowed-origins}") String origensPermitidas) {

        List<String> origens = Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();

        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        configuracao.setAllowedHeaders(List.of(
                HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, "X-XSRF-TOKEN"));
        configuracao.setExposedHeaders(List.of("X-Request-Id"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
