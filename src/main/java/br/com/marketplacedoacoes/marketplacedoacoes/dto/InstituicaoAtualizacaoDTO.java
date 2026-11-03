package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import br.com.marketplacedoacoes.marketplacedoacoes.model.AreaAtuacao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.TipoItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InstituicaoAtualizacaoDTO(
        @NotBlank(message = "O nome da instituição é obrigatório")
        String nome,

        @Size(max = 255, message = "O endereço deve ter no máximo 255 caracteres")
        String endereco,

        @NotBlank(message = "A cidade é obrigatória")
        String cidade,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @NotBlank(message = "A chave Pix é obrigatória")
        @Size(max = 77, message = "A chave Pix deve ter no máximo 77 caracteres")
        String chavePix,

        List<AreaAtuacao> areasAtuacao,

        @Size(max = 100, message = "O campo 'Outra área' deve ter no máximo 100 caracteres")
        String outraArea,

        List<TipoItem> tiposAceitos,

        @Size(max = 100, message = "O campo 'Outros itens' deve ter no máximo 100 caracteres")
        String outroTipoAceito
) {}