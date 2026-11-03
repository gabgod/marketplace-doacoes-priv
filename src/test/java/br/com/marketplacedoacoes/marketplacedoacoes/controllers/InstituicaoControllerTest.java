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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InstituicaoControllerTest {

    private static final String CNPJ_FORMATADO = "11.222.333/0001-81";
    private static final String CNPJ_LIMPO = "11222333000181";
    private static final String CPF_COMO_PIX = "12345678909";

    @Mock
    private InstituicaoRepository instituicaoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private InstituicaoController controller;
    private Authentication comoInstituicao;

    @BeforeEach
    void preparar() {
        controller = new InstituicaoController(instituicaoRepository, usuarioRepository, passwordEncoder);
        comoInstituicao = new UsernamePasswordAuthenticationToken(
                "ong@teste.com", null, List.of(new SimpleGrantedAuthority("ROLE_INSTITUICAO")));

        when(passwordEncoder.encode(anyString())).thenReturn("hash-da-senha");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(instituicaoRepository.save(any(Instituicao.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    // ---------- Cadastro ----------

    @Test
    void cadastraInstituicaoPendenteComContaDeAcesso() {
        InstituicaoCadastroDTO dto = cadastro(CNPJ_FORMATADO, CPF_COMO_PIX,
                List.of(AreaAtuacao.CRIANCAS_E_ADOLESCENTES, AreaAtuacao.EDUCACAO), null);

        InstituicaoPainelDTO resposta = controller.cadastrar(dto);

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getRole()).isEqualTo(Role.INSTITUICAO);
        assertThat(usuario.getValue().getEmail()).isEqualTo("casa@teste.com");
        assertThat(usuario.getValue().getSenha()).isEqualTo("hash-da-senha");

        ArgumentCaptor<Instituicao> instituicao = ArgumentCaptor.forClass(Instituicao.class);
        verify(instituicaoRepository).save(instituicao.capture());
        assertThat(instituicao.getValue().getCnpj()).isEqualTo(CNPJ_LIMPO);
        assertThat(instituicao.getValue().getNome()).isEqualTo("Casa Esperança");
        assertThat(instituicao.getValue().getCidade()).isEqualTo("Porto Alegre");
        assertThat(instituicao.getValue().getStatus()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);
        assertThat(resposta.responsavelEmail()).isEqualTo("casa@teste.com");
        assertThat(resposta.areasAtuacao())
                .containsExactly(AreaAtuacao.CRIANCAS_E_ADOLESCENTES, AreaAtuacao.EDUCACAO);
    }

    @Test
    void rejeitaCnpjSemQuatorzeDigitos() {
        InstituicaoCadastroDTO dto = cadastro("123.456", CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null);

        assertThatThrownBy(() -> controller.cadastrar(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("14 dígitos");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void rejeitaCnpjJaCadastrado() {
        when(instituicaoRepository.existsByCnpj(CNPJ_LIMPO)).thenReturn(true);
        InstituicaoCadastroDTO dto = cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null);

        assertThatThrownBy(() -> controller.cadastrar(dto)).isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).save(any());
        verify(instituicaoRepository, never()).save(any());
    }

    @Test
    void rejeitaEmailJaCadastrado() {
        when(usuarioRepository.existsByEmail("casa@teste.com")).thenReturn(true);
        InstituicaoCadastroDTO dto = cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null);

        assertThatThrownBy(() -> controller.cadastrar(dto)).isInstanceOf(ConflitoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @ParameterizedTest(name = "chave \"{0}\" é guardada como {1}")
    @CsvSource(delimiter = '|', value = {
            "123.456.789-09|12345678909",
            "11.222.333/0001-81|11222333000181",
            "+55 51 99999-8888|+5551999998888",
            "'  ong@teste.com '|ong@teste.com",
            "123e4567-e89b-12d3-a456-426614174000|123e4567-e89b-12d3-a456-426614174000"
    })
    void limpaEAceitaChavesPixValidas(String digitada, String guardada) {
        InstituicaoPainelDTO resposta = controller.cadastrar(
                cadastro(CNPJ_FORMATADO, digitada, List.of(AreaAtuacao.IDOSOS), null));

        assertThat(resposta.chavePix()).isEqualTo(guardada);
    }

    @ParameterizedTest(name = "chave \"{0}\" é inválida")
    @ValueSource(strings = {"123", "abc", "+5551", "ong@", "12345678901234567890", "123.456"})
    void rejeitaChavesPixInvalidas(String chave) {
        InstituicaoCadastroDTO dto = cadastro(CNPJ_FORMATADO, chave, List.of(AreaAtuacao.IDOSOS), null);

        assertThatThrownBy(() -> controller.cadastrar(dto))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Chave Pix inválida");
        verify(instituicaoRepository, never()).save(any());
    }

    // ---------- Categorias ----------

    @Test
    void exigePeloMenosUmaAreaOuOutraArea() {
        InstituicaoCadastroDTO semNada = cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, List.of(), null);
        InstituicaoCadastroDTO semListaEOutraEmBranco = cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, null, "   ");

        assertThatThrownBy(() -> controller.cadastrar(semNada))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pelo menos uma área");
        assertThatThrownBy(() -> controller.cadastrar(semListaEOutraEmBranco))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pelo menos uma área");
    }

    @Test
    void aceitaApenasOutraAreaSemMarcarNenhuma() {
        InstituicaoPainelDTO resposta = controller.cadastrar(
                cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, List.of(), "  Cuidados paliativos "));

        assertThat(resposta.areasAtuacao()).isEmpty();
        assertThat(resposta.outraArea()).isEqualTo("Cuidados paliativos");
    }

    @Test
    void tiposAceitosSaoOpcionaisEOsTextosLivresSaoLimpos() {
        InstituicaoCadastroDTO dto = new InstituicaoCadastroDTO("Casa Esperança", CNPJ_FORMATADO, "Rua A, 10",
                "Porto Alegre", "Acolhimento", CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null,
                null, "   ", "casa@teste.com", "senha123");

        InstituicaoPainelDTO resposta = controller.cadastrar(dto);

        assertThat(resposta.tiposAceitos()).isEmpty();
        assertThat(resposta.outroTipoAceito()).isNull();
    }

    @Test
    void ignoraCategoriasRepetidasENulas() {
        List<AreaAtuacao> areas = Arrays.asList(AreaAtuacao.SAUDE, AreaAtuacao.SAUDE, null, AreaAtuacao.IDOSOS);

        InstituicaoPainelDTO resposta = controller.cadastrar(cadastro(CNPJ_FORMATADO, CPF_COMO_PIX, areas, null));

        // sem repetidos, sem nulos e na ordem em que o enum foi declarado
        assertThat(resposta.areasAtuacao()).containsExactly(AreaAtuacao.IDOSOS, AreaAtuacao.SAUDE);
    }

    // ---------- Edição pela própria instituição ----------

    @Test
    void alterarSoDescricaoECategoriasNaoTiraDoAr() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("Casa Esperança", CPF_COMO_PIX, List.of(AreaAtuacao.EDUCACAO, AreaAtuacao.SAUDE), null),
                comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.APROVADA);
        assertThat(resposta.descricao()).isEqualTo("Nova descrição");
        // as categorias antigas (IDOSOS) são substituídas, não somadas; a ordem é a do enum (SAUDE vem antes de EDUCACAO)
        assertThat(resposta.areasAtuacao()).containsExactly(AreaAtuacao.SAUDE, AreaAtuacao.EDUCACAO);
        assertThat(resposta.tiposAceitos()).isEmpty();
    }

    @Test
    void trocarONomeVoltaParaAnalise() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("Outro Nome", CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null), comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);
    }

    @Test
    void trocarAChavePixVoltaParaAnalise() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("Casa Esperança", "golpista@teste.com", List.of(AreaAtuacao.IDOSOS), null), comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);
        assertThat(resposta.chavePix()).isEqualTo("golpista@teste.com");
    }

    @Test
    void espacosSobrandoNoNomeENaChaveNaoContamComoMudanca() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("  Casa Esperança ", " 123.456.789-09 ", List.of(AreaAtuacao.IDOSOS), null), comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.APROVADA);
    }

    @Test
    void instituicaoRejeitadaQueCorrigeEEnviadaDeNovoParaAnalise() {
        Instituicao instituicao = aprovada();
        instituicao.setStatus(Instituicao.StatusInstituicao.REJEITADA);
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("Casa Esperança", CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null), comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);
    }

    @Test
    void instituicaoPendenteContinuaPendente() {
        Instituicao instituicao = aprovada();
        instituicao.setStatus(Instituicao.StatusInstituicao.PENDENTE);
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        InstituicaoPainelDTO resposta = controller.atualizarMinhaInstituicao(
                atualizacao("Outro Nome", CPF_COMO_PIX, List.of(AreaAtuacao.IDOSOS), null), comoInstituicao);

        assertThat(resposta.status()).isEqualTo(Instituicao.StatusInstituicao.PENDENTE);
    }

    @Test
    void edicaoSemAreaNaoMudaNada() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        assertThatThrownBy(() -> controller.atualizarMinhaInstituicao(
                atualizacao("Casa Esperança", CPF_COMO_PIX, List.of(), null), comoInstituicao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pelo menos uma área");
        verify(instituicaoRepository, never()).save(any());
    }

    @Test
    void edicaoComChavePixInvalidaEBarrada() {
        Instituicao instituicao = aprovada();
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.of(instituicao));

        assertThatThrownBy(() -> controller.atualizarMinhaInstituicao(
                atualizacao("Casa Esperança", "abc", List.of(AreaAtuacao.IDOSOS), null), comoInstituicao))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Chave Pix inválida");
    }

    @Test
    void contaSemInstituicaoVinculadaDa404() {
        when(instituicaoRepository.findByResponsavelEmail("ong@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.minhaInstituicao(comoInstituicao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- Consulta pública e administração ----------

    @Test
    void listaPublicaTrazSoAprovadasComCategoriasOrdenadas() {
        Instituicao instituicao = aprovada();
        // a instituição de teste já vem com uma área e um item; limpa para controlar exatamente o que entra
        instituicao.getAreasAtuacao().clear();
        instituicao.getAreasAtuacao().add(AreaAtuacao.SAUDE);
        instituicao.getAreasAtuacao().add(AreaAtuacao.CRIANCAS_E_ADOLESCENTES);
        instituicao.getTiposAceitos().clear();
        instituicao.getTiposAceitos().add(TipoItem.FRALDAS);
        instituicao.getTiposAceitos().add(TipoItem.ROUPAS_E_CALCADOS);
        when(instituicaoRepository.findByStatus(Instituicao.StatusInstituicao.APROVADA)).thenReturn(List.of(instituicao));

        List<InstituicaoDTO> lista = controller.listarAprovadas();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).areasAtuacao())
                .containsExactly(AreaAtuacao.CRIANCAS_E_ADOLESCENTES, AreaAtuacao.SAUDE);
        assertThat(lista.get(0).tiposAceitos())
                .containsExactly(TipoItem.ROUPAS_E_CALCADOS, TipoItem.FRALDAS);
    }

    @Test
    void detalhePublicoEscondeInstituicaoNaoAprovada() {
        Instituicao pendente = aprovada();
        pendente.setStatus(Instituicao.StatusInstituicao.PENDENTE);
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.of(pendente));

        assertThatThrownBy(() -> controller.buscarPorId(10L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void detalhePublicoMostraInstituicaoAprovada() {
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.of(aprovada()));

        assertThat(controller.buscarPorId(10L).nome()).isEqualTo("Casa Esperança");
    }

    @Test
    void adminAprovaERejeita() {
        Instituicao instituicao = aprovada();
        instituicao.setStatus(Instituicao.StatusInstituicao.PENDENTE);
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.of(instituicao));

        assertThat(controller.atualizarStatus(10L, Instituicao.StatusInstituicao.APROVADA).status())
                .isEqualTo(Instituicao.StatusInstituicao.APROVADA);
        assertThat(controller.atualizarStatus(10L, Instituicao.StatusInstituicao.REJEITADA).status())
                .isEqualTo(Instituicao.StatusInstituicao.REJEITADA);
    }

    @Test
    void adminNaoAtualizaStatusDeInstituicaoInexistente() {
        when(instituicaoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.atualizarStatus(10L, Instituicao.StatusInstituicao.APROVADA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- Montagem dos dados de teste ----------

    // Instituição aprovada que já vem com a área IDOSOS e o item ALIMENTOS
    private Instituicao aprovada() {
        Usuario responsavel = new Usuario();
        responsavel.setId(2L);
        responsavel.setEmail("ong@teste.com");
        responsavel.setRole(Role.INSTITUICAO);

        Instituicao instituicao = new Instituicao();
        instituicao.setId(10L);
        instituicao.setNome("Casa Esperança");
        instituicao.setCnpj(CNPJ_LIMPO);
        instituicao.setCidade("Porto Alegre");
        instituicao.setChavePix(CPF_COMO_PIX);
        instituicao.setStatus(Instituicao.StatusInstituicao.APROVADA);
        instituicao.setResponsavel(responsavel);
        instituicao.getAreasAtuacao().add(AreaAtuacao.IDOSOS);
        instituicao.getTiposAceitos().add(TipoItem.ALIMENTOS);
        return instituicao;
    }

    private InstituicaoCadastroDTO cadastro(String cnpj, String chavePix, List<AreaAtuacao> areas, String outraArea) {
        return new InstituicaoCadastroDTO("  Casa Esperança ", cnpj, "Rua A, 10", " Porto Alegre ", "Acolhimento",
                chavePix, areas, outraArea, List.of(), null, "casa@teste.com", "senha123");
    }

    private InstituicaoAtualizacaoDTO atualizacao(String nome, String chavePix, List<AreaAtuacao> areas, String outraArea) {
        return new InstituicaoAtualizacaoDTO(nome, "Rua B, 20", "Porto Alegre", "Nova descrição",
                chavePix, areas, outraArea, List.of(), null);
    }
}