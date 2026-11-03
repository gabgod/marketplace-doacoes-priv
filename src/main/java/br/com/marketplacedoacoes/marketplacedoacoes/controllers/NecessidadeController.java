package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.NecessidadeDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.dto.NecessidadeEntradaDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.ConflitoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RecursoNaoEncontradoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RegraDeNegocioException;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Necessidade;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.DoacaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.InstituicaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.NecessidadeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/necessidades")
public class NecessidadeController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int PRAZO_MAXIMO_DIAS = 365;

    private final NecessidadeRepository necessidadeRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final DoacaoRepository doacaoRepository;

    public NecessidadeController(NecessidadeRepository necessidadeRepository,
                                 InstituicaoRepository instituicaoRepository,
                                 DoacaoRepository doacaoRepository) {
        this.necessidadeRepository = necessidadeRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.doacaoRepository = doacaoRepository;
    }

    // ---------- Público: só necessidades abertas, no prazo e de instituições aprovadas ----------

    @GetMapping
    public List<NecessidadeDTO> listarAbertas(@RequestParam(required = false) Long instituicaoId) {
        LocalDate hoje = LocalDate.now(FUSO);
        return necessidadeRepository
                .buscarPublicas(Necessidade.StatusNecessidade.ABERTA, hoje, Instituicao.StatusInstituicao.APROVADA)
                .stream()
                .filter(n -> instituicaoId == null || n.getInstituicao().getId().equals(instituicaoId))
                .map(n -> toDTO(n, hoje))
                .toList();
    }

    // ---------- A própria instituição (perfil INSTITUICAO) ----------

    @GetMapping("/minhas")
    public List<NecessidadeDTO> listarMinhas(Authentication authentication) {
        Instituicao instituicao = instituicaoDoResponsavel(authentication);
        LocalDate hoje = LocalDate.now(FUSO);
        return necessidadeRepository.findByInstituicaoIdOrderByDataLimiteAsc(instituicao.getId())
                .stream()
                .map(n -> toDTO(n, hoje))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NecessidadeDTO criar(@RequestBody @Valid NecessidadeEntradaDTO dto, Authentication authentication) {
        Instituicao instituicao = instituicaoDoResponsavel(authentication);
        if (instituicao.getStatus() != Instituicao.StatusInstituicao.APROVADA) {
            throw new RegraDeNegocioException("Só instituições aprovadas podem publicar necessidades");
        }
        LocalDate hoje = LocalDate.now(FUSO);
        validarDataLimite(dto.dataLimite(), hoje);

        Necessidade necessidade = new Necessidade();
        necessidade.setInstituicao(instituicao);
        aplicar(necessidade, dto);

        return toDTO(necessidadeRepository.save(necessidade), hoje);
    }

    @PutMapping("/{id}")
    public NecessidadeDTO atualizar(@PathVariable Long id,
                                    @RequestBody @Valid NecessidadeEntradaDTO dto,
                                    Authentication authentication) {
        Necessidade necessidade = necessidadeDaInstituicao(id, authentication);
        LocalDate hoje = LocalDate.now(FUSO);
        validarDataLimite(dto.dataLimite(), hoje);

        aplicar(necessidade, dto);
        return toDTO(necessidadeRepository.save(necessidade), hoje);
    }

    @PatchMapping("/{id}/status")
    public NecessidadeDTO atualizarStatus(@PathVariable Long id,
                                          @RequestParam Necessidade.StatusNecessidade status,
                                          Authentication authentication) {
        Necessidade necessidade = necessidadeDaInstituicao(id, authentication);
        necessidade.setStatus(status);
        return toDTO(necessidadeRepository.save(necessidade), LocalDate.now(FUSO));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Necessidade necessidade = necessidadeDaInstituicao(id, authentication);
        if (doacaoRepository.existsByNecessidadeId(id)) {
            throw new ConflitoException(
                    "Já existem doações ligadas a esta necessidade. Marque-a como atendida em vez de removê-la");
        }
        necessidadeRepository.delete(necessidade);
    }

    // ---------- Auxiliares ----------

    private Instituicao instituicaoDoResponsavel(Authentication authentication) {
        return instituicaoRepository.findByResponsavelEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma instituição vinculada a esta conta"));
    }

    // Busca a necessidade e confere se ela pertence à instituição logada
    private Necessidade necessidadeDaInstituicao(Long id, Authentication authentication) {
        Instituicao instituicao = instituicaoDoResponsavel(authentication);
        Necessidade necessidade = necessidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Necessidade não encontrada com id " + id));
        if (!necessidade.getInstituicao().getId().equals(instituicao.getId())) {
            throw new AccessDeniedException("Acesso negado");
        }
        return necessidade;
    }

    private void validarDataLimite(LocalDate dataLimite, LocalDate hoje) {
        if (dataLimite.isBefore(hoje)) {
            throw new RegraDeNegocioException("A data limite não pode estar no passado");
        }
        if (dataLimite.isAfter(hoje.plusDays(PRAZO_MAXIMO_DIAS))) {
            throw new RegraDeNegocioException("A data limite pode ser de, no máximo, " + PRAZO_MAXIMO_DIAS + " dias a partir de hoje");
        }
    }

    private void aplicar(Necessidade necessidade, NecessidadeEntradaDTO dto) {
        necessidade.setItem(dto.item().trim());
        necessidade.setDescricao(dto.descricao() == null || dto.descricao().isBlank() ? null : dto.descricao().trim());
        necessidade.setQuantidade(dto.quantidade());
        necessidade.setDataLimite(dto.dataLimite());
    }

    private NecessidadeDTO toDTO(Necessidade n, LocalDate hoje) {
        boolean expirada = n.getStatus() == Necessidade.StatusNecessidade.ABERTA && n.getDataLimite().isBefore(hoje);
        return new NecessidadeDTO(
                n.getId(),
                n.getInstituicao().getId(),
                n.getInstituicao().getNome(),
                n.getItem(),
                n.getDescricao(),
                n.getQuantidade(),
                n.getDataLimite(),
                n.getStatus(),
                expirada);
    }
}