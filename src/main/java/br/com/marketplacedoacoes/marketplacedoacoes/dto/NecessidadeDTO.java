package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Necessidade;

import java.time.LocalDate;

public record NecessidadeDTO(
        Long id,
        Long instituicaoId,
        String instituicaoNome,
        String item,
        String descricao,
        Integer quantidade,
        LocalDate dataLimite,
        Necessidade.StatusNecessidade status,
        boolean expirada
) {}