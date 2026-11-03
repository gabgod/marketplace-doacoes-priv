package br.com.marketplacedoacoes.marketplacedoacoes.security;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.ErroResposta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   @Value("${spring.h2.console.enabled:false}") boolean h2Console) throws Exception {

        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService);
        ActivityLogFilter activityLogFilter = new ActivityLogFilter();

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // Console do H2: só existe em desenvolvimento (em produção a propriedade é false)
                    if (h2Console) {
                        auth.requestMatchers(PathRequest.toH2Console()).permitAll();
                    }

                    auth
                            // Públicos: login e cadastros
                            .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                            .requestMatchers(antMatcher(HttpMethod.POST, "/api/usuarios")).permitAll()
                            .requestMatchers(antMatcher(HttpMethod.POST, "/api/instituicoes/cadastro")).permitAll()

                            // A instituição logada gerencia a própria instituição
                            .requestMatchers(antMatcher("/api/instituicoes/minha")).hasRole("INSTITUICAO")

                            // ADMIN: lista completa e aprovação de instituições
                            .requestMatchers(antMatcher(HttpMethod.GET, "/api/instituicoes/admin")).hasRole("ADMIN")
                            .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/instituicoes/*/status")).hasRole("ADMIN")

                            // Consulta pública (a API só devolve as aprovadas)
                            .requestMatchers(antMatcher(HttpMethod.GET, "/api/instituicoes/**")).permitAll()
                            // Qualquer outra operação em instituições: só ADMIN
                            .requestMatchers(antMatcher("/api/instituicoes/**")).hasRole("ADMIN")

                            // Necessidades: leitura pública (só abertas e no prazo); gestão só da própria instituição
                            .requestMatchers(antMatcher("/api/necessidades/minhas")).hasRole("INSTITUICAO")
                            .requestMatchers(antMatcher(HttpMethod.GET, "/api/necessidades")).permitAll()
                            .requestMatchers(antMatcher("/api/necessidades/**")).hasRole("INSTITUICAO")

                            // Qualquer usuário logado acessa o próprio perfil (precisa vir ANTES da regra de ADMIN)
                            .requestMatchers(antMatcher("/api/usuarios/me/**")).authenticated()
                            .requestMatchers(antMatcher("/api/usuarios/**")).hasRole("ADMIN")

                            // Doações: só USER e ADMIN doam; instituição dona (ou ADMIN) confirma; só ADMIN remove
                            .requestMatchers(antMatcher(HttpMethod.POST, "/api/doacoes")).hasAnyRole("USER", "ADMIN")
                            .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/doacoes/*/status")).hasAnyRole("ADMIN", "INSTITUICAO")
                            .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/doacoes/**")).hasRole("ADMIN")

                            // O resto da API exige estar logado
                            .requestMatchers(antMatcher("/api/**")).authenticated()
                            // Páginas do site (HTML, CSS, JS): públicas. Os dados continuam protegidos pela API.
                            .anyRequest().permitAll();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Logo depois do JWT: já sabe quem é o usuário e ainda enxerga 401/403 no status final
                .addFilterAfter(activityLogFilter, JwtAuthFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            escreverErro(response, objectMapper, HttpStatus.UNAUTHORIZED,
                                    "Autenticação necessária ou token inválido/expirado");
                        })
                        .accessDeniedHandler((request, response, e) ->
                                escreverErro(response, objectMapper, HttpStatus.FORBIDDEN,
                                        "Você não tem permissão para acessar este recurso")));

        // O console do H2 usa frames; só relaxa a proteção contra clickjacking em desenvolvimento
        if (h2Console) {
            http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        }

        return http.build();
    }

    private void escreverErro(HttpServletResponse response, ObjectMapper mapper,
                              HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErroResposta corpo = new ErroResposta(
                LocalDateTime.now(), status.value(), status.getReasonPhrase(), mensagem, null);
        mapper.writeValue(response.getWriter(), corpo);
    }
}