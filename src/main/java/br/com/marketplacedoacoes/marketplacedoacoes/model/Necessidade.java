package br.com.marketplacedoacoes.marketplacedoacoes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "necessidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Necessidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "instituicao_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Instituicao instituicao;

    @NotBlank(message = "O item é obrigatório")
    @Column(length = 100)
    private String item;

    @Column(length = 500)
    private String descricao;

    private Integer quantidade;

    @NotNull(message = "A data limite é obrigatória")
    private LocalDate dataLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusNecessidade status = StatusNecessidade.ABERTA;

    public enum StatusNecessidade {
        ABERTA, ATENDIDA
    }
}