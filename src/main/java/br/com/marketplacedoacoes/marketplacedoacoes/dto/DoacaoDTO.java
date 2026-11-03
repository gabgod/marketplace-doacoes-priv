package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Doacao;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DoacaoDTO(
        Long id,

        // Ignorados na criação: o doador é sempre o usuário logado
        Long usuarioId,
        String usuarioNome,

        @NotNull(message = "O id da instituição é obrigatório")
        Long instituicaoId,

        @NotNull(message = "O tipo da doação é obrigatório")
        Doacao.TipoDoacao tipo,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        Double valor,

        // Somente doação de item: quantas unidades serão doadas
        @Min(value = 1, message = "A quantidade deve ser de, no mínimo, 1 unidade")
        @Max(value = 10000, message = "A quantidade deve ser de, no máximo, 10000 unidades")
        Integer quantidade,

        // Ignorados na criação: status e data são definidos pelo servidor
        Doacao.StatusDoacao status,
        LocalDateTime dataDoacao,

        // Somente doação de item
        String telefoneDoador,
        Doacao.FormaEntrega formaEntrega,

        @Size(max = 255, message = "O endereço de retirada deve ter no máximo 255 caracteres")
        String enderecoRetirada,

        LocalDate dataPrevista,
        Long necessidadeId,

        // Somente na resposta
        String necessidadeItem
) {}