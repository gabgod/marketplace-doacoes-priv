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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NecessidadeControllerTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private NecessidadeRepository necessidadeRepository;
    @Mock
    private InstituicaoRepository instituicaoRepository;
    @Mock
    private DoacaoRepository doacaoRepository;

    private NecessidadeController controller;

    private LocalDate hoje;
    private Instituicao instituicao;
    private Authentication comoInstituicao;

    @BeforeEach
    void preparar() {
        controller = new NecessidadeController(necessidadeRepository, instituicaoRepository, doacaoRepository);
        hoje = LocalDate.now(FUSO);

        instituicao = new Instituicao();
        instituicao.setId(10L);
        instituicao.setNome("Casa Esperança");
        instituicao.setStatus(Instituicao.StatusInstituicao.APROVADA);

        comoInstituicao = new UsernamePasswordAuthenticationToken(
                "ong@teste.com", null, List.of(new SimpleGrantedAuthority("ROLE_INSTITUICAO")));

        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));
        when(necessidadeRepository.save(any(Necessidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    // ---------- Publicação ----------

    @Test
    void publicaNecessidadeAbertaLimpandoOsTextos() {
        NecessidadeEntradaDTO dto = new NecessidadeEntradaDTO("  Cobertores  ", "   ", 20, hoje.plusDays(10));

        NecessidadeDTO resposta = controller.criar(dto, comoInstituicao);

        assertThat(resposta.item()).isEqualTo("Cobertores");
        assertThat(resposta.descricao()).isNull();
        assertThat(resposta.quantidade()).isEqualTo(20);
        assertThat(resposta.dataLimite()).isEqualTo(hoje.plusDays(10));
        assertThat(resposta.status()).isEqualTo(Necessidade.StatusNecessidade.ABERTA);
        assertThat(resposta.expirada()).isFalse();
        assertThat(resposta.instituicaoId()).isEqualTo(10L);
    }

    @Test
    void soInstituicaoAprovadaPublica() {
        instituicao.setStatus(Instituicao.StatusInstituicao.PENDENTE);

        assertThatThrownBy(() -> controller.criar(entrada(hoje.plusDays(5)), comoInstituicao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("aprovadas");
        verify(necessidadeRepository, never()).save(any());
    }

    @Test
    void rejeitaDataLimiteNoPassado() {
        assertThatThrownBy(() -> controller.criar(entrada(hoje.minusDays(1)), comoInstituicao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("passado");
    }

    @Test
    void aceitaDataLimiteDeHojeEDeAte365Dias() {
        assertThat(controller.criar(entrada(hoje), comoInstituicao).dataLimite()).isEqualTo(hoje);
        assertThat(controller.criar(entrada(hoje.plusDays(365)), comoInstituicao).dataLimite()).isEqualTo(hoje.plusDays(365));
    }

    @Test
    void rejeitaDataLimiteDepoisDe365Dias() {
        assertThatThrownBy(() -> controller.criar(entrada(hoje.plusDays(366)), comoInstituicao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("365 dias");
    }

    @Test
    void contaSemInstituicaoNaoPublica() {
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.criar(entrada(hoje.plusDays(5)), comoInstituicao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- Edição, prorrogação e status ----------

    @Test
    void prorrogaUmaNecessidadeExpirada() {
        Necessidade vencida = necessidade(7L, instituicao, hoje.minusDays(3), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.of(vencida));

        NecessidadeDTO resposta = controller.atualizar(7L, entrada(hoje.plusDays(5)), comoInstituicao);

        assertThat(resposta.dataLimite()).isEqualTo(hoje.plusDays(5));
        assertThat(resposta.expirada()).isFalse();
    }

    @Test
    void naoEditaNecessidadeDeOutraInstituicao() {
        Instituicao outra = new Instituicao();
        outra.setId(99L);
        Necessidade alheia = necessidade(7L, outra, hoje.plusDays(5), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.of(alheia));

        assertThatThrownBy(() -> controller.atualizar(7L, entrada(hoje.plusDays(5)), comoInstituicao))
                .isInstanceOf(AccessDeniedException.class);
        verify(necessidadeRepository, never()).save(any());
    }

    @Test
    void editarNecessidadeInexistenteDa404() {
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.atualizar(7L, entrada(hoje.plusDays(5)), comoInstituicao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void marcaComoAtendidaEReabre() {
        Necessidade n = necessidade(7L, instituicao, hoje.plusDays(5), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.of(n));

        assertThat(controller.atualizarStatus(7L, Necessidade.StatusNecessidade.ATENDIDA, comoInstituicao).status())
                .isEqualTo(Necessidade.StatusNecessidade.ATENDIDA);
        assertThat(controller.atualizarStatus(7L, Necessidade.StatusNecessidade.ABERTA, comoInstituicao).status())
                .isEqualTo(Necessidade.StatusNecessidade.ABERTA);
    }

    // ---------- Remoção ----------

    @Test
    void removeNecessidadeSemDoacoes() {
        Necessidade n = necessidade(7L, instituicao, hoje.plusDays(5), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.of(n));
        when(doacaoRepository.existsByNecessidadeId(7L)).thenReturn(false);

        controller.deletar(7L, comoInstituicao);

        verify(necessidadeRepository).delete(n);
    }

    @Test
    void naoRemoveNecessidadeQueJaTemDoacoes() {
        Necessidade n = necessidade(7L, instituicao, hoje.plusDays(5), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findById(7L)).thenReturn(Optional.of(n));
        when(doacaoRepository.existsByNecessidadeId(7L)).thenReturn(true);

        assertThatThrownBy(() -> controller.deletar(7L, comoInstituicao))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("atendida");
        verify(necessidadeRepository, never()).delete(any(Necessidade.class));
    }

    // ---------- Listagens ----------

    @Test
    void listaDaInstituicaoMarcaComoExpiradaSoAsAbertasVencidas() {
        Necessidade vencidaAberta = necessidade(1L, instituicao, hoje.minusDays(2), Necessidade.StatusNecessidade.ABERTA);
        Necessidade vencidaAtendida = necessidade(2L, instituicao, hoje.minusDays(2), Necessidade.StatusNecessidade.ATENDIDA);
        Necessidade noPrazo = necessidade(3L, instituicao, hoje.plusDays(2), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.findByInstituicaoIdOrderByDataLimiteAsc(10L))
                .thenReturn(List.of(vencidaAberta, vencidaAtendida, noPrazo));

        List<NecessidadeDTO> lista = controller.listarMinhas(comoInstituicao);

        assertThat(lista).extracting(NecessidadeDTO::expirada).containsExactly(true, false, false);
    }

    @Test
    void listaPublicaPodeSerFiltradaPorInstituicao() {
        Instituicao outra = new Instituicao();
        outra.setId(99L);
        outra.setNome("Abrigo");
        Necessidade daCasa = necessidade(1L, instituicao, hoje.plusDays(2), Necessidade.StatusNecessidade.ABERTA);
        Necessidade doAbrigo = necessidade(2L, outra, hoje.plusDays(2), Necessidade.StatusNecessidade.ABERTA);
        when(necessidadeRepository.buscarPublicas(
                eq(Necessidade.StatusNecessidade.ABERTA), any(LocalDate.class), eq(Instituicao.StatusInstituicao.APROVADA)))
                .thenReturn(List.of(daCasa, doAbrigo));

        assertThat(controller.listarAbertas(null)).hasSize(2);
        assertThat(controller.listarAbertas(99L)).extracting(NecessidadeDTO::id).containsExactly(2L);
    }

    // ---------- Montagem dos dados de teste ----------

    private NecessidadeEntradaDTO entrada(LocalDate dataLimite) {
        return new NecessidadeEntradaDTO("Cobertores", "Em bom estado", 20, dataLimite);
    }

    private Necessidade necessidade(Long id, Instituicao dona, LocalDate limite, Necessidade.StatusNecessidade status) {
        Necessidade n = new Necessidade();
        n.setId(id);
        n.setInstituicao(dona);
        n.setItem("Cobertores");
        n.setDataLimite(limite);
        n.setStatus(status);
        return n;
    }
}