package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioCadastroDTO(
    @NotBlank (message = "O nome é obrigatório")
    String nome,
    
    @NotBlank (message = "O email é obrigatório")
    @Email (message = "Email invalido")
    String email,

    @NotBlank (message = "A senha é obrigatória")
    @Size (min = 6, message = "A senha deve ter no minimo 6 caracteres")
    String senha

) {} 
