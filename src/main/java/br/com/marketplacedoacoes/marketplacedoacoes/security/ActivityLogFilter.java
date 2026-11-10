package br.com.marketplacedoacoes.marketplacedoacoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ActivityLogFilter extends OncePerRequestFilter {

    private static final Logger atividade = LoggerFactory.getLogger("ATIVIDADE");

    private final RegistroAtividade registroAtividade;

    public ActivityLogFilter(RegistroAtividade registroAtividade) {
        this.registroAtividade = registroAtividade;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        String email = emailAtual(); // null = visitante sem login
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = System.currentTimeMillis() - inicio;
            String ip = limpar(request.getRemoteAddr());
            String acao = request.getMethod() + " " + limpar(request.getRequestURI())
                    + " -> " + response.getStatus();

            atividade.info("usuario={} ip={} {} ({}ms)",
                    email == null ? "anonimo" : limpar(email), ip, acao, ms);

            if (email != null) {
                registroAtividade.registrar(email, "REQUISICAO", acao, ip);
            }
        }
    }

    private String emailAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getName();
    }

    static String limpar(String texto) {
        return texto == null ? "-" : texto.replaceAll("[\\r\\n\\t]", "_");
    }
}