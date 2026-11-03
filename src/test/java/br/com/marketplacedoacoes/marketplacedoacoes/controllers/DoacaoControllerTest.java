package br.com.marketplacedoacoes.marketplacedoacoes.controllers;

import br.com.marketplacedoacoes.marketplacedoacoes.dto.DoacaoDTO;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RecursoNaoEncontradoException;
import br.com.marketplacedoacoes.marketplacedoacoes.exception.RegraDeNegocioException;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Doacao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Necessidade;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Role;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Usuario;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.DoacaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.InstituicaoRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.NecessidadeRepository;
import br.com.marketplacedoacoes.marketplacedoacoes.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DoacaoControllerTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private DoacaoRepository doacaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InstituicaoRepository instituicaoRepository;
    @Mock
    private NecessidadeRepository necessidadeRepository;

    private DoacaoController controller;

    private LocalDate hoje;
    private Usuario doador;
    private Usuario responsavel;
    private Instituicao instituicao;
    private Authentication comoDoador;
    private Authentication comoInstituicao;
    private Authentication comoAdmin;

    @BeforeEach
    void preparar() {
        controller = new DoacaoController(doacaoRepository, usuarioRepository, instituicaoRepository, necessidadeRepository);
        hoje = LocalDate.now(FUSO);

        doador = usuario(1L, "Maria", "doador@teste.com");
        responsavel = usuario(2L, "Casa Esperança", "ong@teste.com");

        instituicao = new Instituicao();
        instituicao.setId(10L);
        instituicao.setNome("Casa Esperança");
        instituicao.setStatus(Instituicao.StatusInstituicao.APROVADA);
        instituicao.setResponsavel(responsavel);

        comoDoador = autenticado("doador@teste.com", "ROLE_USER");
        comoInstituicao = autenticado("ong@teste.com", "ROLE_INSTITUICAO");
        comoAdmin = autenticado("admin@teste.com", "ROLE_ADMIN");

        when(usuarioRepository.findByEmail("doador@teste.com")).thenReturn(Optional.of(doador));
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.of(instituicao));
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));
        when(doacaoRepository.save(any(Doacao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    // ---------- Doação em dinheiro ----------

    @Test
    void registraDoacaoEmDinheiroComoPendente() {
        DoacaoDTO resposta = controller.criar(dinheiro(25.0), comoDoador);

        assertThat(resposta.tipo()).isEqualTo(Doacao.TipoDoacao.DINHEIRO);
        assertThat(resposta.valor()).isEqualTo(25.0);
        assertThat(resposta.status()).isEqualTo(Doacao.StatusDoacao.PENDENTE);
        assertThat(resposta.usuarioId()).isEqualTo(1L);
        assertThat(resposta.instituicaoId()).isEqualTo(10L);
        assertThat(resposta.quantidade()).isNull();
    }

    @Test
    void rejeitaDoacaoEmDinheiroComValorZero() {
        assertThatThrownBy(() -> controller.criar(dinheiro(0.0), comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("maior que zero");
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void rejeitaDoacaoEmDinheiroSemValor() {
        assertThatThrownBy(() -> controller.criar(dinheiro(null), comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void rejeitaDoacaoParaInstituicaoNaoAprovada() {
        instituicao.setStatus(Instituicao.StatusInstituicao.PENDENTE);

        assertThatThrownBy(() -> controller.criar(dinheiro(10.0), comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não foi aprovada");
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void rejeitaDoacaoParaInstituicaoInexistente() {
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.criar(dinheiro(10.0), comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Instituição não encontrada");
    }

    // ---------- Doação de item ----------

    @Test
    void registraDoacaoDeItemComDadosDeEntrega() {
        DoacaoDTO resposta = controller.criar(itemValido(), comoDoador);

        assertThat(resposta.tipo()).isEqualTo(Doacao.TipoDoacao.ITEM);
        assertThat(resposta.descricao()).isEqualTo("Cobertores");
        assertThat(resposta.quantidade()).isEqualTo(3);
        assertThat(resposta.telefoneDoador()).isEqualTo("51999998888");
        assertThat(resposta.formaEntrega()).isEqualTo(Doacao.FormaEntrega.DOADOR_LEVA);
        assertThat(resposta.enderecoRetirada()).isNull();
        assertThat(resposta.dataPrevista()).isEqualTo(hoje.plusDays(5));
        assertThat(resposta.status()).isEqualTo(Doacao.StatusDoacao.PENDENTE);
        assertThat(resposta.necessidadeId()).isNull();
    }

    @Test
    void rejeitaItemSemQuantidade() {
        DoacaoDTO dto = item("Cobertores", null, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("unidades");
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void rejeitaItemSemDescricao() {
        DoacaoDTO dto = item("   ", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Descreva o item");
    }

    @Test
    void rejeitaItemComTelefoneInvalido() {
        DoacaoDTO dto = item("Cobertores", 1, "123", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Telefone inválido");
    }

    @Test
    void rejeitaItemSemFormaDeEntrega() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", null, null, hoje.plusDays(5), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("levar o item");
    }

    @Test
    void exigeEnderecoQuandoAInstituicaoVaiBuscar() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.INSTITUICAO_BUSCA, "  ", hoje.plusDays(5), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("endereço de retirada");
    }

    @Test
    void guardaOEnderecoDeRetiradaSemEspacosSobrando() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.INSTITUICAO_BUSCA,
                "  Rua A, 10, Centro  ", hoje.plusDays(5), null);

        DoacaoDTO resposta = controller.criar(dto, comoDoador);

        assertThat(resposta.formaEntrega()).isEqualTo(Doacao.FormaEntrega.INSTITUICAO_BUSCA);
        assertThat(resposta.enderecoRetirada()).isEqualTo("Rua A, 10, Centro");
    }

    @Test
    void ignoraOEnderecoQuandoOProprioDoadorLeva() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA,
                "Rua que não importa", hoje.plusDays(5), null);

        assertThat(controller.criar(dto, comoDoador).enderecoRetirada()).isNull();
    }

    @Test
    void exigeDataPrevistaNaDoacaoDeItem() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, null, null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("data prevista");
    }

    @Test
    void rejeitaDataPrevistaNoPassado() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.minusDays(1), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("passado");
    }

    @Test
    void aceitaDataPrevistaDeHoje() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje, null);

        assertThat(controller.criar(dto, comoDoador).dataPrevista()).isEqualTo(hoje);
    }

    @Test
    void aceitaDataPrevistaNoLimiteDe90Dias() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(90), null);

        assertThat(controller.criar(dto, comoDoador).dataPrevista()).isEqualTo(hoje.plusDays(90));
    }

    @Test
    void rejeitaDataPrevistaDepoisDe90Dias() {
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(91), null);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("90 dias");
    }

    // ---------- Doação de item ligada a uma necessidade ----------

    @Test
    void ligaADoacaoAUmaNecessidadeAberta() {
        necessidade(7L, instituicao, hoje.plusDays(10), Necessidade.StatusNecessidade.ABERTA);
        DoacaoDTO dto = item("Cobertores", 5, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), 7L);

        DoacaoDTO resposta = controller.criar(dto, comoDoador);

        assertThat(resposta.necessidadeId()).isEqualTo(7L);
        assertThat(resposta.necessidadeItem()).isEqualTo("Cobertores");
        assertThat(resposta.quantidade()).isEqualTo(5);
    }

    @Test
    void limitaADataPrevistaAoPrazoDaNecessidade() {
        necessidade(7L, instituicao, hoje.plusDays(10), Necessidade.StatusNecessidade.ABERTA);
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(11), 7L);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("prazo desta necessidade");
    }

    @Test
    void aceitaNecessidadeQueVenceHojeComEntregaHoje() {
        necessidade(7L, instituicao, hoje, Necessidade.StatusNecessidade.ABERTA);
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje, 7L);

        assertThat(controller.criar(dto, comoDoador).necessidadeId()).isEqualTo(7L);
    }

    @Test
    void rejeitaNecessidadeDeOutraInstituicao() {
        Instituicao outra = new Instituicao();
        outra.setId(99L);
        necessidade(7L, outra, hoje.plusDays(10), Necessidade.StatusNecessidade.ABERTA);
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), 7L);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    void rejeitaNecessidadeJaAtendida() {
        necessidade(7L, instituicao, hoje.plusDays(10), Necessidade.StatusNecessidade.ATENDIDA);
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), 7L);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não está mais aberta");
    }

    @Test
    void rejeitaNecessidadeComPrazoVencido() {
        necessidade(7L, instituicao, hoje.minusDays(1), Necessidade.StatusNecessidade.ABERTA);
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje, 7L);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não está mais aberta");
    }

    @Test
    void rejeitaNecessidadeInexistente() {
        when(necessidadeRepository.findById(404L)).thenReturn(Optional.empty());
        DoacaoDTO dto = item("Cobertores", 1, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), 404L);

        assertThatThrownBy(() -> controller.criar(dto, comoDoador))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Necessidade não encontrada");
    }

    // ---------- Quem enxerga o quê ----------

    @Test
    void doadorComumEnxergaSoAsPropriasDoacoesEIgnoraOsFiltros() {
        when(doacaoRepository.findByUsuarioId(1L)).thenReturn(List.of(doacao(5L, doador)));

        List<DoacaoDTO> lista = controller.listar(99L, 77L, comoDoador);

        assertThat(lista).hasSize(1);
        verify(doacaoRepository).findByUsuarioId(1L);
        verify(doacaoRepository, never()).findByUsuarioId(99L);
        verify(doacaoRepository, never()).findByInstituicaoId(77L);
        verify(doacaoRepository, never()).findAll();
    }

    @Test
    void instituicaoEnxergaAsDoacoesDestinadasAela() {
        when(doacaoRepository.findByInstituicaoId(10L)).thenReturn(List.of(doacao(5L, doador), doacao(6L, doador)));

        assertThat(controller.listar(null, null, comoInstituicao)).hasSize(2);
        verify(doacaoRepository).findByInstituicaoId(10L);
    }

    @Test
    void contaDeInstituicaoSemVinculoNaoListaNada() {
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.listar(null, null, comoInstituicao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void adminEnxergaTudoOuFiltraPorUsuario() {
        when(doacaoRepository.findAll()).thenReturn(List.of(doacao(5L, doador)));
        when(doacaoRepository.findByUsuarioId(1L)).thenReturn(List.of(doacao(5L, doador)));

        assertThat(controller.listar(null, null, comoAdmin)).hasSize(1);
        verify(doacaoRepository).findAll();

        assertThat(controller.listar(1L, null, comoAdmin)).hasSize(1);
        verify(doacaoRepository).findByUsuarioId(1L);
    }

    @Test
    void detalheDaDoacaoSoParaDoadorInstituicaoEAdmin() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.of(doacao(5L, doador)));
        Authentication estranho = autenticado("outro@teste.com", "ROLE_USER");

        assertThat(controller.buscarPorId(5L, comoDoador).id()).isEqualTo(5L);
        assertThat(controller.buscarPorId(5L, comoInstituicao).id()).isEqualTo(5L);
        assertThat(controller.buscarPorId(5L, comoAdmin).id()).isEqualTo(5L);
        assertThatThrownBy(() -> controller.buscarPorId(5L, estranho)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void detalheDeDoacaoInexistenteDa404() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.buscarPorId(5L, comoDoador))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- Confirmação do recebimento ----------

    @Test
    void instituicaoDestinatariaConfirmaADoacao() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.of(doacao(5L, doador)));

        DoacaoDTO resposta = controller.atualizarStatus(5L, Doacao.StatusDoacao.CONFIRMADA, comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Doacao.StatusDoacao.CONFIRMADA);
    }

    @Test
    void adminTambemPodeCancelarUmaDoacao() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.of(doacao(5L, doador)));

        assertThat(controller.atualizarStatus(5L, Doacao.StatusDoacao.CANCELADA, comoAdmin).status())
                .isEqualTo(Doacao.StatusDoacao.CANCELADA);
    }

    @Test
    void doadorNaoConfirmaAPropriaDoacao() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.of(doacao(5L, doador)));

        assertThatThrownBy(() -> controller.atualizarStatus(5L, Doacao.StatusDoacao.CONFIRMADA, comoDoador))
                .isInstanceOf(AccessDeniedException.class);
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void outraInstituicaoNaoConfirmaDoacaoDeOutra() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.of(doacao(5L, doador)));
        Authentication outraOng = autenticado("outra-ong@teste.com", "ROLE_INSTITUICAO");

        assertThatThrownBy(() -> controller.atualizarStatus(5L, Doacao.StatusDoacao.CONFIRMADA, outraOng))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmarDoacaoInexistenteDa404() {
        when(doacaoRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.atualizarStatus(5L, Doacao.StatusDoacao.CONFIRMADA, comoAdmin))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- Montagem dos dados de teste ----------

    private Usuario usuario(Long id, String nome, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setRole(Role.USER);
        return u;
    }

    private Authentication autenticado(String email, String perfil) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(perfil)));
    }

    private Necessidade necessidade(Long id, Instituicao dona, LocalDate limite, Necessidade.StatusNecessidade status) {
        Necessidade n = new Necessidade();
        n.setId(id);
        n.setInstituicao(dona);
        n.setItem("Cobertores");
        n.setDataLimite(limite);
        n.setStatus(status);
        when(necessidadeRepository.findById(id)).thenReturn(Optional.of(n));
        return n;
    }

    private Doacao doacao(Long id, Usuario dono) {
        Doacao d = new Doacao();
        d.setId(id);
        d.setUsuario(dono);
        d.setInstituicao(instituicao);
        d.setTipo(Doacao.TipoDoacao.DINHEIRO);
        d.setValor(10.0);
        return d;
    }

    private DoacaoDTO dinheiro(Double valor) {
        return new DoacaoDTO(null, null, null, 10L, Doacao.TipoDoacao.DINHEIRO, "Boa sorte", valor,
                null, null, null, null, null, null, null, null, null);
    }

    private DoacaoDTO itemValido() {
        return item("Cobertores", 3, "(51) 99999-8888", Doacao.FormaEntrega.DOADOR_LEVA, null, hoje.plusDays(5), null);
    }

    private DoacaoDTO item(String descricao, Integer quantidade, String telefone, Doacao.FormaEntrega forma,
                           String endereco, LocalDate dataPrevista, Long necessidadeId) {
        return new DoacaoDTO(null, null, null, 10L, Doacao.TipoDoacao.ITEM, descricao, null,
                quantidade, null, null, telefone, forma, endereco, dataPrevista, necessidadeId, null);
    }
}