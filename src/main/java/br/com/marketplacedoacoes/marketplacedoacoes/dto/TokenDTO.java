package br.com.marketplacedoacoes.marketplacedoacoes.dto;

public record TokenDTO(
        String token,
        String tipo,
        long expiraEmSegundos,
        String role
) {}