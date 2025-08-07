package greencity.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import greencity.security.filters.AccessTokenAuthenticationFilter;
import greencity.security.jwt.JwtTool;
import greencity.security.providers.JwtAuthenticationProvider;
import greencity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${CLIENT_APP_URL}")
    private String clientAppUrl;

    private final JwtTool jwtTool;
    private final UserService userService;

    @Autowired
    public SecurityConfig(JwtTool jwtTool, UserService userService) {
        this.jwtTool = jwtTool;
        this.userService = userService;
    }

    /**
     * Настройка CORS — разрешает запросы с фронтенда
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("✅ CORS: Setting for origin: " + clientAppUrl);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Collections.singletonList(clientAppUrl));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "OPTIONS", "DELETE", "PUT", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Основная цепочка безопасности
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("✅ SecurityFilterChain: Initialization. CLIENT_APP_URL = " + clientAppUrl);

        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new AccessTokenAuthenticationFilter(jwtTool, authenticationManager(), userService),
                UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((req, resp, exc) -> {
                    System.out.println("❌ 401: " + req.getRequestURI());
                    resp.sendError(org.springframework.http.HttpStatus.UNAUTHORIZED.value(), "Authorize first.");
                })
                .accessDeniedHandler((req, resp, exc) -> {
                    System.out.println("🚫 403: " + req.getRequestURI());
                    resp.sendError(org.springframework.http.HttpStatus.FORBIDDEN.value(),
                        "You don't have authorities.");
                }));

        return http.build();
    }

    /**
     * Менеджер аутентификации
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(jwtAuthenticationProvider());
    }

    /**
     * Провайдер JWT-аутентификации
     */
    @Bean
    public JwtAuthenticationProvider jwtAuthenticationProvider() {
        return new JwtAuthenticationProvider(jwtTool);
    }

    /**
     * Кодировщик паролей
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Google ID Token Verifier
     */
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier() {
        return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList("your-google-client-id"))
            .build();
    }
}
