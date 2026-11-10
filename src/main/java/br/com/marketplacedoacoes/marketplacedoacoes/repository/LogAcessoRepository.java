package br.com.marketplacedoacoes.marketplacedoacoes.repository;

import br.com.marketplacedoacoes.marketplacedoacoes.model.LogAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {
}