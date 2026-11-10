package br.com.marketplacedoacoes.marketplacedoacoes.repository;

import br.com.marketplacedoacoes.marketplacedoacoes.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {
    Optional<Instituicao> findByCnpj(String cnpj);

    boolean existsByCnpj(String cnpj);

    List<Instituicao> findByStatus(Instituicao.StatusInstituicao status);

    Optional<Instituicao> findByResponsavelEmail(String email);
}