package completo.projeto.completo.Autenticacao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        // público geral
                        .requestMatchers(
                                "/auth/**",
                                "/h2-console/**",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/swagger-resources/**", "/webjars/**"
                        ).permitAll()

                        // libera WebSocket e STOMP para teste
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/topic/**").permitAll()
                        .requestMatchers("/app/**").permitAll()

                        // libera estes GETs globais para teste
                        .requestMatchers(HttpMethod.GET, "/api/v1/vital-signs/latest").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/vital-signs/history").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/vital-signs").permitAll()

                        // o resto exige autenticação
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
