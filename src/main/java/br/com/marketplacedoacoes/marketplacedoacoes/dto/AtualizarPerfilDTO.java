package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import jakarta.validation.constraints.NotBlank;

public record AtualizarPerfilDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome
) {}