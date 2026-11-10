package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.AlterarSenhaDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.AtualizarPerfilDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.UsuarioCadastroDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.UsuarioDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.ConflitoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RecursoNaoEncontradoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RegraDeNegocioException;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public UsuarioDTO meuPerfil(Authentication authentication) {
        return toDTO(usuarioLogado(authentication));
    }

    @PutMapping("/me")
    public UsuarioDTO atualizarMeuPerfil(@RequestBody @Valid AtualizarPerfilDTO dto,
                                         Authentication authentication) {
        Usuario usuario = usuarioLogado(authentication);
        usuario.setNome(dto.nome());
        return toDTO(usuarioRepository.save(usuario));
    }

    @PutMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarMinhaSenha(@RequestBody @Valid AlterarSenhaDTO dto,
                                  Authentication authentication) {
        Usuario usuario = usuarioLogado(authentication);

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDTO criar(@RequestBody @Valid UsuarioCadastroDTO dto) {
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new ConflitoException("Já existe um usuário com o email " + dto.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));

        return toDTO(usuarioRepository.save(usuario));
    }

    @GetMapping
    public List<UsuarioDTO> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public UsuarioDTO buscarPorId(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com id " + id));
    }

    @PutMapping("/{id}")
    public UsuarioDTO atualizar(@PathVariable Long id, @RequestBody @Valid UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado com id " + id));

        usuarioRepository.findByEmail(dto.email())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflitoException("Já existe um usuário com o email " + dto.email());
                });

        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        return toDTO(usuarioRepository.save(usuario));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado com id " + id);
        }
        usuarioRepository.deleteById(id);
    }

    private Usuario usuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não encontrado"));
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}