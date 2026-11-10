package br.com.marketplacedoacoes.marketplacedoacoes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "A doação precisa de um doador")
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull(message = "A doação precisa de uma instituição destinatária")
    @ManyToOne
    @JoinColumn(name = "instituicao_id", nullable = false)
    private Instituicao instituicao;

    @Enumerated(EnumType.STRING)
    private TipoDoacao tipo;

    @Column(length = 500)
    private String descricao;

    private Double valor;

    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    private StatusDoacao status = StatusDoacao.PENDENTE;

    private LocalDateTime dataDoacao = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "necessidade_id")
    private Necessidade necessidade;

    @Column(length = 11)
    private String telefoneDoador;

    @Enumerated(EnumType.STRING)
    private FormaEntrega formaEntrega;

    @Column(length = 255)
    private String enderecoRetirada;

    private LocalDate dataPrevista;

    public enum TipoDoacao {
        DINHEIRO, ITEM
    }

    public enum StatusDoacao {
        PENDENTE, CONFIRMADA, CANCELADA
    }

    public enum FormaEntrega {
        DOADOR_LEVA, INSTITUICAO_BUSCA
    }
}