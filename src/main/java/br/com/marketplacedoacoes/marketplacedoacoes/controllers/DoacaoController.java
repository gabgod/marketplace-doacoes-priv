package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.DoacaoDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RecursoNaoEncontradoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RegraDeNegocioException;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Doacao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Necessidade;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.DoacaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.InstituicaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.NecessidadeRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.util.Telefones;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int PRAZO_PADRAO_DIAS = 90;
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DoacaoRepository doacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final NecessidadeRepository necessidadeRepository;

    public DoacaoController(DoacaoRepository doacaoRepository,
                            UsuarioRepository usuarioRepository,
                            InstituicaoRepository instituicaoRepository,
                            NecessidadeRepository necessidadeRepository) {
        this.doacaoRepository = doacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.necessidadeRepository = necessidadeRepository;
    }

    @GetMapping
    public List<DoacaoDTO> listar(@RequestParam(required = false) Long usuarioId,
                                  @RequestParam(required = false) Long instituicaoId,
                                  Authentication authentication) {
        List<Doacao> doacoes;
        if (isAdmin(authentication)) {
            if (usuarioId != null) {
                doacoes = doacaoRepository.findByUsuarioId(usuarioId);
            } else if (instituicaoId != null) {
                doacoes = doacaoRepository.findByInstituicaoId(instituicaoId);
            } else {
                doacoes = doacaoRepository.findAll();
            }
        } else if (isInstituicao(authentication)) {
            // instituição enxerga as doações destinadas a ela
            Instituicao instituicao = instituicaoRepository.findByResponsavelEmail(authentication.getName())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Nenhuma instituição vinculada a esta conta"));
            doacoes = doacaoRepository.findByInstituicaoId(instituicao.getId());
        } else {
            // usuário comum só enxerga as próprias doações, ignorando os filtros
            doacoes = doacaoRepository.findByUsuarioId(usuarioLogado(authentication).getId());
        }
        return doacoes.stream().map(this::toDTO).toList();
    }

    @GetMapping("/{id}")
    public DoacaoDTO buscarPorId(@PathVariable Long id, Authentication authentication) {
        Doacao doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada com id " + id));

        boolean doador = doacao.getUsuario().getEmail().equals(authentication.getName());
        if (!isAdmin(authentication) && !doador && !ehResponsavel(doacao, authentication)) {
            throw new AccessDeniedException("Acesso negado");
        }
        return toDTO(doacao);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoacaoDTO criar(@RequestBody @Valid DoacaoDTO dto, Authentication authentication) {
        Usuario usuario = usuarioLogado(authentication);
        Instituicao instituicao = instituicaoRepository.findById(dto.instituicaoId())
                .orElseThrow(() -> new RegraDeNegocioException("Instituição não encontrada com id " + dto.instituicaoId()));

        if (instituicao.getStatus() != Instituicao.StatusInstituicao.APROVADA) {
            throw new RegraDeNegocioException("Esta instituição ainda não foi aprovada para receber doações");
        }

        Doacao doacao = new Doacao();
        doacao.setUsuario(usuario);
        doacao.setInstituicao(instituicao);
        doacao.setTipo(dto.tipo());
        doacao.setDescricao(dto.descricao() == null || dto.descricao().isBlank() ? null : dto.descricao().trim());
        // status (PENDENTE) e dataDoacao (agora) usam os valores padrão da entidade

        if (dto.tipo() == Doacao.TipoDoacao.DINHEIRO) {
            if (dto.valor() == null || dto.valor() <= 0) {
                throw new RegraDeNegocioException("Doações em dinheiro precisam ter um valor maior que zero");
            }
            doacao.setValor(dto.valor());
        } else {
            preencherDadosDoItem(doacao, dto, instituicao);
        }

        return toDTO(doacaoRepository.save(doacao));
    }

    // Quem confirma o recebimento é a instituição dona da doação (ou o ADMIN)
    @PatchMapping("/{id}/status")
    public DoacaoDTO atualizarStatus(@PathVariable Long id,
                                     @RequestParam Doacao.StatusDoacao status,
                                     Authentication authentication) {
        Doacao doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Doação não encontrada com id " + id));

        if (!isAdmin(authentication) && !ehResponsavel(doacao, authentication)) {
            throw new AccessDeniedException("Acesso negado");
        }

        doacao.setStatus(status);
        return toDTO(doacaoRepository.save(doacao));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id) {
        if (!doacaoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Doação não encontrada com id " + id);
        }
        doacaoRepository.deleteById(id);
    }

    // ---------- Doação de item: quantidade, telefone, forma de entrega, endereço, data e necessidade ----------

    private void preencherDadosDoItem(Doacao doacao, DoacaoDTO dto, Instituicao instituicao) {
        if (doacao.getDescricao() == null) {
            throw new RegraDeNegocioException("Descreva o item que será doado");
        }

        if (dto.quantidade() == null) {
            throw new RegraDeNegocioException("Informe quantas unidades você vai doar");
        }

        String telefone = Telefones.normalizar(dto.telefoneDoador());
        if (telefone == null) {
            throw new RegraDeNegocioException("Telefone inválido. Informe o DDD e o número, ex.: (51) 99999-8888");
        }

        if (dto.formaEntrega() == null) {
            throw new RegraDeNegocioException("Informe se você vai levar o item ou se a instituição deve buscá-lo");
        }

        String endereco = null;
        if (dto.formaEntrega() == Doacao.FormaEntrega.INSTITUICAO_BUSCA) {
            if (dto.enderecoRetirada() == null || dto.enderecoRetirada().isBlank()) {
                throw new RegraDeNegocioException("Informe o endereço de retirada do item");
            }
            endereco = dto.enderecoRetirada().trim();
        }

        if (dto.dataPrevista() == null) {
            throw new RegraDeNegocioException("Informe a data prevista para a entrega");
        }

        LocalDate hoje = LocalDate.now(FUSO);
        Necessidade necessidade = buscarNecessidade(dto.necessidadeId(), instituicao, hoje);
        LocalDate limite = necessidade != null ? necessidade.getDataLimite() : hoje.plusDays(PRAZO_PADRAO_DIAS);

        if (dto.dataPrevista().isBefore(hoje)) {
            throw new RegraDeNegocioException("A data prevista não pode estar no passado");
        }
        if (dto.dataPrevista().isAfter(limite)) {
            throw new RegraDeNegocioException(necessidade != null
                    ? "A data prevista não pode passar do prazo desta necessidade (" + limite.format(FORMATO_DATA) + ")"
                    : "A data prevista deve ser de, no máximo, " + PRAZO_PADRAO_DIAS + " dias a partir de hoje");
        }

        doacao.setQuantidade(dto.quantidade());
        doacao.setTelefoneDoador(telefone);
        doacao.setFormaEntrega(dto.formaEntrega());
        doacao.setEnderecoRetirada(endereco);
        doacao.setDataPrevista(dto.dataPrevista());
        doacao.setNecessidade(necessidade);
    }

    private Necessidade buscarNecessidade(Long necessidadeId, Instituicao instituicao, LocalDate hoje) {
        if (necessidadeId == null) {
            return null;
        }
        Necessidade necessidade = necessidadeRepository.findById(necessidadeId)
                .orElseThrow(() -> new RegraDeNegocioException("Necessidade não encontrada com id " + necessidadeId));

        if (!necessidade.getInstituicao().getId().equals(instituicao.getId())) {
            throw new RegraDeNegocioException("Esta necessidade não pertence à instituição escolhida");
        }
        if (necessidade.getStatus() != Necessidade.StatusNecessidade.ABERTA || necessidade.getDataLimite().isBefore(hoje)) {
            throw new RegraDeNegocioException("Esta necessidade não está mais aberta");
        }
        return necessidade;
    }

    // ---------- Auxiliares ----------

    private Usuario usuarioLogado(Authentication authentication) {
        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não encontrado"));
    }

    private boolean isAdmin(Authentication authentication) {
        return temPerfil(authentication, "ROLE_ADMIN");
    }

    private boolean isInstituicao(Authentication authentication) {
        return temPerfil(authentication, "ROLE_INSTITUICAO");
    }

    private boolean temPerfil(Authentication authentication, String perfil) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(perfil));
    }

    // A conta logada é a responsável pela instituição que recebe esta doação?
    private boolean ehResponsavel(Doacao doacao, Authentication authentication) {
        Usuario responsavel = doacao.getInstituicao().getResponsavel();
        return responsavel != null && responsavel.getEmail().equals(authentication.getName());
    }

    private DoacaoDTO toDTO(Doacao d) {
        return new DoacaoDTO(
                d.getId(),
                d.getUsuario().getId(),
                d.getUsuario().getNome(),
                d.getInstituicao().getId(),
                d.getTipo(),
                d.getDescricao(),
                d.getValor(),
                d.getQuantidade(),
                d.getStatus(),
                d.getDataDoacao(),
                d.getTelefoneDoador(),
                d.getFormaEntrega(),
                d.getEnderecoRetirada(),
                d.getDataPrevista(),
                d.getNecessidade() != null ? d.getNecessidade().getId() : null,
                d.getNecessidade() != null ? d.getNecessidade().getItem() : null
        );
    }
}