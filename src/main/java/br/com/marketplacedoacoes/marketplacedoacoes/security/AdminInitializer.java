package br.com.marketplacedoacoes.marketplacedoacoes.security;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Role;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);
    private static final String ADMIN_ANTIGO = "admin@marketplace.com";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String adminEmail;
    private final String adminSenha;

    public AdminInitializer(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            Environment environment,
                            @Value("${app.admin.email}") String adminEmail,
                            @Value("${app.admin.senha}") String adminSenha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.adminEmail = adminEmail.trim();
        this.adminSenha = adminSenha;
    }

    @Override
    public void run(String... args) {
        boolean producao = environment.acceptsProfiles(Profiles.of("prod"));

        // Em produção, recusa credenciais fracas ou as antigas de teste
        if (producao) {
            if (adminSenha.length() < 12 || adminSenha.equals("admin123")) {
                throw new IllegalStateException(
                        "ADMIN_SENHA fraca: use pelo menos 12 caracteres e nada de 'admin123'.");
            }
            if (adminEmail.equalsIgnoreCase(ADMIN_ANTIGO)) {
                throw new IllegalStateException(
                        "ADMIN_EMAIL não pode ser o email padrão de teste (" + ADMIN_ANTIGO + ").");
            }
        }

        Usuario admin = usuarioRepository.findByEmail(adminEmail).orElse(null);

        if (admin == null) {
            Usuario antigo = usuarioRepository.findByEmail(ADMIN_ANTIGO).orElse(null);
            if (antigo != null) {
                // Já existia o admin de teste no banco: reaproveita a conta (mantém o histórico)
                antigo.setEmail(adminEmail);
                antigo.setSenha(passwordEncoder.encode(adminSenha));
                antigo.setRole(Role.ADMIN);
                usuarioRepository.save(antigo);
                log.info("Admin padrão migrado para o novo email configurado.");
                return;
            }
            admin = new Usuario();
            admin.setNome("Administrador");
            admin.setEmail(adminEmail);
            admin.setRole(Role.ADMIN);
            admin.setSenha(passwordEncoder.encode(adminSenha));
            usuarioRepository.save(admin);
            log.info("Admin criado: {}", adminEmail);
        } else {
            // ADMIN_SENHA é a fonte da verdade: se você trocar a variável, a senha troca no próximo start
            boolean mudou = false;
            if (!passwordEncoder.matches(adminSenha, admin.getSenha())) {
                admin.setSenha(passwordEncoder.encode(adminSenha));
                mudou = true;
            }
            if (admin.getRole() != Role.ADMIN) {
                admin.setRole(Role.ADMIN);
                mudou = true;
            }
            if (mudou) {
                usuarioRepository.save(admin);
                log.info("Credenciais do admin atualizadas a partir da configuração.");
            }
        }

        // Se o admin de teste ainda existe ao lado do novo, ele perde o poder e a senha vira aleatória
        if (!adminEmail.equalsIgnoreCase(ADMIN_ANTIGO)) {
            usuarioRepository.findByEmail(ADMIN_ANTIGO).ifPresent(antigo -> {
                antigo.setRole(Role.USER);
                antigo.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
                usuarioRepository.save(antigo);
                log.warn("Conta admin de teste ({}) desativada.", ADMIN_ANTIGO);
            });
        }
    }
}