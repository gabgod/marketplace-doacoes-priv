package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioDTO(
    Long id,

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank (message = "O email é obrigatório")
    @Email (message = "Email inválido")
    String email

) {}
