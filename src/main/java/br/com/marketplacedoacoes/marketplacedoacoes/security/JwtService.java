package br.com.marketplacedoacoes.marketplacedoacoes.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtService {
    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtService(@Value("${app.jwt.secret:}") String segredo,
                      @Value("${app.jwt.expiracao-minutos:60}") long expiracaoMinutos) {
        this.expiracaoMinutos = expiracaoMinutos;

        if (segredo.isBlank()) {
            this.chave = Jwts.SIG.HS256.key().build();
            log.warn("app.jwt.secret não definido: usando chave aleatória (tokens deixam de valer ao reiniciar)");
        } else {
            this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String gerarToken(String email) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiracaoMinutos, ChronoUnit.MINUTES)))
                .signWith(chave)
                .compact();
    }

    public String extrairEmail(String token) {
        return Jwts.parser()
                .verifyWith(chave)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public long getExpiracaoSegundos() {
        return expiracaoMinutos * 60;
    }
}