package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.InstituicaoAtualizacaoDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.InstituicaoCadastroDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.InstituicaoDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.InstituicaoPainelDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.ConflitoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RecursoNaoEncontradoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RegraDeNegocioException;
import br.com.marketplacedoacoes.marketplacedoacoes.model.AreaAtuacao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Role;
import br.com.marketplacedoacoes.marketplacedoacoes.model.TipoItem;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.InstituicaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instituicoes")
public class InstituicaoController {
    private final InstituicaoRepository instituicaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public InstituicaoController(InstituicaoRepository instituicaoRepository,
                                 UsuarioRepository usuarioRepository,
                                 PasswordEncoder passwordEncoder) {
        this.instituicaoRepository = instituicaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<InstituicaoDTO> listarAprovadas() {
        return instituicaoRepository.findByStatus(Instituicao.StatusInstituicao.APROVADA)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public InstituicaoDTO buscarPorId(@PathVariable Long id) {
        return instituicaoRepository.findById(id)
                .filter(i -> i.getStatus() == Instituicao.StatusInstituicao.APROVADA)
                .map(this::toDTO)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Instituição não encontrada com id " + id));
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public InstituicaoPainelDTO cadastrar(@RequestBody @Valid InstituicaoCadastroDTO dto) {
        String cnpj = dto.cnpj().replaceAll("\\D", "");
        if (cnpj.length() != 14) {
            throw new RegraDeNegocioException("O CNPJ deve ter 14 dígitos");
        }

        String chavePix = limparChave(dto.chavePix());
        validarChavePix(chavePix);

        Set<AreaAtuacao> areas = comoConjunto(dto.areasAtuacao());
        Set<TipoItem> tipos = comoConjunto(dto.tiposAceitos());
        String outraArea = limparTexto(dto.outraArea());
        String outroTipo = limparTexto(dto.outroTipoAceito());
        validarAreas(areas, outraArea);

        if (instituicaoRepository.existsByCnpj(cnpj)) {
            throw new ConflitoException("Já existe uma instituição com o CNPJ " + dto.cnpj());
        }
        if (usuarioRepository.existsByEmail(dto.email())) {
            throw new ConflitoException("Já existe um usuário com o email " + dto.email());
        }

        Usuario responsavel = new Usuario();
        responsavel.setNome(dto.nome().trim());
        responsavel.setEmail(dto.email());
        responsavel.setSenha(passwordEncoder.encode(dto.senha()));
        responsavel.setRole(Role.INSTITUICAO);
        usuarioRepository.save(responsavel);

        Instituicao instituicao = new Instituicao();
        instituicao.setNome(dto.nome().trim());
        instituicao.setCnpj(cnpj);
        instituicao.setEndereco(dto.endereco());
        instituicao.setCidade(dto.cidade().trim());
        instituicao.setDescricao(dto.descricao());
        instituicao.setChavePix(chavePix);
        instituicao.setResponsavel(responsavel);
        aplicarCategorias(instituicao, areas, outraArea, tipos, outroTipo);

        return toPainelDTO(instituicaoRepository.save(instituicao));
    }

    @GetMapping("/minha")
    public InstituicaoPainelDTO minhaInstituicao(Authentication authentication) {
        return toPainelDTO(instituicaoDoResponsavel(authentication));
    }

    @PutMapping("/minha")
    public InstituicaoPainelDTO atualizarMinhaInstituicao(@RequestBody @Valid InstituicaoAtualizacaoDTO dto,
                                                          Authentication authentication) {
        Instituicao instituicao = instituicaoDoResponsavel(authentication);

        String nome = dto.nome().trim();
        String chavePix = limparChave(dto.chavePix());
        validarChavePix(chavePix);

        Set<AreaAtuacao> areas = comoConjunto(dto.areasAtuacao());
        Set<TipoItem> tipos = comoConjunto(dto.tiposAceitos());
        String outraArea = limparTexto(dto.outraArea());
        String outroTipo = limparTexto(dto.outroTipoAceito());
        validarAreas(areas, outraArea);

        boolean mudouDadoSensivel = !nome.equals(instituicao.getNome())
                || !chavePix.equals(instituicao.getChavePix());

        instituicao.setNome(nome);
        instituicao.setEndereco(dto.endereco());
        instituicao.setCidade(dto.cidade().trim());
        instituicao.setDescricao(dto.descricao());
        instituicao.setChavePix(chavePix);
        aplicarCategorias(instituicao, areas, outraArea, tipos, outroTipo);

        boolean voltaParaAnalise =
                instituicao.getStatus() == Instituicao.StatusInstituicao.REJEITADA
                || (instituicao.getStatus() == Instituicao.StatusInstituicao.APROVADA && mudouDadoSensivel);
        if (voltaParaAnalise) {
            instituicao.setStatus(Instituicao.StatusInstituicao.PENDENTE);
        }

        return toPainelDTO(instituicaoRepository.save(instituicao));
    }

    @GetMapping("/admin")
    public List<InstituicaoPainelDTO> listarTodas() {
        return instituicaoRepository.findAll()
                .stream()
                .map(this::toPainelDTO)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public InstituicaoPainelDTO atualizarStatus(@PathVariable Long id,
                                                @RequestParam Instituicao.StatusInstituicao status) {
        Instituicao instituicao = instituicaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Instituição não encontrada com id " + id));
        instituicao.setStatus(status);
        return toPainelDTO(instituicaoRepository.save(instituicao));
    }

    private Instituicao instituicaoDoResponsavel(Authentication authentication) {
        return instituicaoRepository.findByResponsavelEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma instituição vinculada a esta conta"));
    }

    private String limparChave(String chave) {
        String limpa = chave.trim().replaceAll("\\s", "");
        if (limpa.startsWith("+")) {
            return "+" + limpa.substring(1).replaceAll("\\D", "");
        }
        if (limpa.matches("[\\d.\\-/()]+")) {
            return limpa.replaceAll("\\D", "");
        }
        return limpa;
    }

    private void validarChavePix(String chave) {
        boolean cpfOuCnpj = chave.matches("\\d{11}") || chave.matches("\\d{14}");
        boolean telefone = chave.matches("\\+55\\d{10,11}");
        boolean email = chave.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
        boolean aleatoria = chave.matches(
                "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

        if (!(cpfOuCnpj || telefone || email || aleatoria)) {
            throw new RegraDeNegocioException(
                    "Chave Pix inválida. Use CPF/CNPJ (só números), telefone com +55, email ou chave aleatória");
        }
    }

    private <E> Set<E> comoConjunto(List<E> lista) {
        if (lista == null) {
            return new HashSet<>();
        }
        return lista.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private String limparTexto(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    private void validarAreas(Set<AreaAtuacao> areas, String outraArea) {
        if (areas.isEmpty() && outraArea == null) {
            throw new RegraDeNegocioException("Escolha pelo menos uma área de atuação (ou preencha 'Outra área')");
        }
    }

    private void aplicarCategorias(Instituicao instituicao,
                                   Set<AreaAtuacao> areas, String outraArea,
                                   Set<TipoItem> tipos, String outroTipo) {
        instituicao.getAreasAtuacao().clear();
        instituicao.getAreasAtuacao().addAll(areas);
        instituicao.setOutraArea(outraArea);

        instituicao.getTiposAceitos().clear();
        instituicao.getTiposAceitos().addAll(tipos);
        instituicao.setOutroTipoAceito(outroTipo);
    }

    private <E extends Enum<E>> List<E> ordenar(Set<E> conjunto) {
        return conjunto.stream().sorted().toList();
    }

    private InstituicaoDTO toDTO(Instituicao i) {
        return new InstituicaoDTO(
                i.getId(), i.getNome(), i.getCnpj(), i.getEndereco(),
                i.getCidade(), i.getDescricao(), i.getChavePix(),
                ordenar(i.getAreasAtuacao()), i.getOutraArea(),
                ordenar(i.getTiposAceitos()), i.getOutroTipoAceito());
    }

    private InstituicaoPainelDTO toPainelDTO(Instituicao i) {
        return new InstituicaoPainelDTO(
                i.getId(), i.getNome(), i.getCnpj(), i.getEndereco(),
                i.getCidade(), i.getDescricao(), i.getChavePix(),
                ordenar(i.getAreasAtuacao()), i.getOutraArea(),
                ordenar(i.getTiposAceitos()), i.getOutroTipoAceito(),
                i.getStatus(),
                i.getResponsavel() != null ? i.getResponsavel().getEmail() : null);
    }
}