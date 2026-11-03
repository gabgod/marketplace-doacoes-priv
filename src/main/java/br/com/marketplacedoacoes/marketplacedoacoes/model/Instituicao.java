package br.com.marketplacedoacoes.marketplacedoacoes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "instituicao")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instituicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da instituição é obrigatório")
    private String nome;

    @NotBlank(message = "O CNPJ é obrigatório")
    @Column(unique = true)
    private String cnpj;

    private String endereco;

    private String cidade;

    @Column(length = 1000)
    private String descricao;

    @Column(length = 77)
    private String chavePix;

    // ---------- Como a instituição atua (usado na busca do doador) ----------

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "instituicao_area_atuacao", joinColumns = @JoinColumn(name = "instituicao_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "area", length = 40, nullable = false)
    private Set<AreaAtuacao> areasAtuacao = new HashSet<>();

    // Texto livre para o que não está na lista ("Outros")
    @Column(length = 100)
    private String outraArea;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "instituicao_tipo_aceito", joinColumns = @JoinColumn(name = "instituicao_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 40, nullable = false)
    private Set<TipoItem> tiposAceitos = new HashSet<>();

    @Column(length = 100)
    private String outroTipoAceito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusInstituicao status = StatusInstituicao.PENDENTE;

    // Conta que administra esta instituição (uma conta para uma instituição)
    @OneToOne
    @JoinColumn(name = "responsavel_id", unique = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuario responsavel;

    @OneToMany(mappedBy = "instituicao", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Doacao> doacoes;

    public enum StatusInstituicao {
        PENDENTE, APROVADA, REJEITADA
    }
}