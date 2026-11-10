package br.com.marketplacedoacoes.marketplacedoacoes.security;

import br.com.marketplacedoacoes.marketplacedoacoes.model.LogAcesso;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.LogAcessoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class RegistroAtividade {

    private static final Logger log = LoggerFactory.getLogger(RegistroAtividade.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final LogAcessoRepository logAcessoRepository;
    private final UsuarioRepository usuarioRepository;

    public RegistroAtividade(LogAcessoRepository logAcessoRepository,
                             UsuarioRepository usuarioRepository) {
        this.logAcessoRepository = logAcessoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrar(String email, String evento, String acao, String ip) {
        try {
            String nome = null;
            if (email != null) {
                nome = usuarioRepository.findByEmail(email).map(Usuario::getNome).orElse(null);
            }

            LogAcesso registro = new LogAcesso();
            registro.setUsuarioNome(cortar(nome != null ? nome : "(desconhecido)", 150));
            registro.setUsuarioEmail(cortar(email, 150));
            registro.setEvento(cortar(evento, 30));
            registro.setAcao(cortar(acao, 255));
            registro.setIp(cortar(ip, 64));
            registro.setDataHora(LocalDateTime.now(FUSO));
            logAcessoRepository.save(registro);
        } catch (Exception e) {
            log.warn("Não foi possível gravar o log no banco: {}", e.getMessage());
        }
    }

    private static String cortar(String texto, int max) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.replaceAll("[\\r\\n\\t]", "_");
        return limpo.length() > max ? limpo.substring(0, max) : limpo;
    }
}