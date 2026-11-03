package br.com.marketplacedoacoes.marketplacedoacoes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Registra no logger "ATIVIDADE" quem fez o quê: usuário, IP, método, caminho, status e duração.
 * Só registra chamadas à API (/api/**); páginas e arquivos estáticos ficam de fora.
 * Nunca registra corpo da requisição nem query string (LGPD: sem senha, telefone ou chave Pix).
 */
public class ActivityLogFilter extends OncePerRequestFilter {

    private static final Logger atividade = LoggerFactory.getLogger("ATIVIDADE");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long inicio = System.currentTimeMillis();
        String usuario = usuarioAtual();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = System.currentTimeMillis() - inicio;
            atividade.info("usuario={} ip={} {} {} -> {} ({}ms)",
                    limpar(usuario), limpar(request.getRemoteAddr()), request.getMethod(),
                    limpar(request.getRequestURI()), response.getStatus(), ms);
        }
    }

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return "anonimo";
        }
        return auth.getName();
    }

    /** Evita "log injection": tira quebras de linha de qualquer texto vindo do cliente. */
    static String limpar(String texto) {
        return texto == null ? "-" : texto.replaceAll("[\\r\\n\\t]", "_");
    }
}