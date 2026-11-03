package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record NecessidadeEntradaDTO(
        @NotBlank(message = "O item é obrigatório")
        @Size(max = 100, message = "O item deve ter no máximo 100 caracteres")
        String item,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Min(value = 1, message = "A quantidade deve ser pelo menos 1")
        Integer quantidade,

        @NotNull(message = "A data limite é obrigatória")
        LocalDate dataLimite
) {}