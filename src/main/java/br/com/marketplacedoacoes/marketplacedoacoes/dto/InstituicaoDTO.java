package br.com.marketplacedoacoes.marketplacedoacoes.dto;

import br.com.marketplacedoacoes.marketplacedoacoes.model.AreaAtuacao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.TipoItem;

import java.util.List;

public record InstituicaoDTO(
        Long id,
        String nome,
        String cnpj,
        String endereco,
        String cidade,
        String descricao,
        String chavePix,
        List<AreaAtuacao> areasAtuacao,
        String outraArea,
        List<TipoItem> tiposAceitos,
        String outroTipoAceito
) {}