package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.LoginDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.TokenDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.security.JwtService;
import br.com.marketplacedoacoes.marketplacedoacoes.security.RegistroAtividade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger atividade = LoggerFactory.getLogger("ATIVIDADE");

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RegistroAtividade registroAtividade;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          RegistroAtividade registroAtividade) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.registroAtividade = registroAtividade;
    }

    @PostMapping("/login")
    public TokenDTO login(@RequestBody @Valid LoginDTO dto, HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        Authentication autenticacao;
        try {
            // Confere email e senha (com BCrypt); se estiver errado, lança AuthenticationException
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
        } catch (AuthenticationException e) {
            atividade.warn("LOGIN_FALHOU email={} ip={}", limpar(dto.email()), limpar(ip));
            registroAtividade.registrar(dto.email(), "LOGIN_FALHOU", "POST /api/auth/login -> 401", ip);
            throw e; // o GlobalExceptionHandler continua devolvendo 401
        }

        atividade.info("LOGIN_OK email={} ip={}", limpar(autenticacao.getName()), limpar(ip));
        registroAtividade.registrar(autenticacao.getName(), "LOGIN_OK", "POST /api/auth/login -> 200", ip);

        String token = jwtService.gerarToken(autenticacao.getName());

        // "ROLE_ADMIN" vira "ADMIN"; o site usa isso só pra decidir o que mostrar no menu
        String role = autenticacao.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER")
                .replace("ROLE_", "");

        return new TokenDTO(token, "Bearer", jwtService.getExpiracaoSegundos(), role);
    }

    private static String limpar(String texto) {
        return texto == null ? "-" : texto.replaceAll("[\\r\\n\\t]", "_");
    }
}