package br.com.marketplacedoacoes.marketplacedoacoes.repository;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import br.com.marketplacedoacoes.marketplacedoacoes.model.Necessidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface NecessidadeRepository extends JpaRepository<Necessidade, Long> {
    List<Necessidade> findByInstituicaoIdOrderByDataLimiteAsc(Long instituicaoId);

    @Query("select n from Necessidade n "
            + "where n.status = :status and n.dataLimite >= :hoje "
            + "and n.instituicao.status = :statusInstituicao "
            + "order by n.dataLimite asc")
    List<Necessidade> buscarPublicas(@Param("status") Necessidade.StatusNecessidade status,
                                     @Param("hoje") LocalDate hoje,
                                     @Param("statusInstituicao") Instituicao.StatusInstituicao statusInstituicao);
}