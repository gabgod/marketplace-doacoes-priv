package br.com.marketplacedoacoes.marketplacedoacoes.repository;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Doacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoacaoRepository extends JpaRepository<Doacao, Long> {
    List<Doacao> findByUsuarioId(Long usuarioId);

    List<Doacao> findByInstituicaoId(Long instituicaoId);

    List<Doacao> findByStatus(Doacao.StatusDoacao status);

    boolean existsByNecessidadeId(Long necessidadeId);
}