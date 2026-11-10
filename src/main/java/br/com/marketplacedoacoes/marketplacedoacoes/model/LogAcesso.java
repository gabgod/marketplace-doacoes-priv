package br.com.marketplacedoacoes.marketplacedoacoes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter
@Setter
@NoArgsConstructor
public class LogAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_nome", length = 150)
    private String usuarioNome;

    @Column(name = "usuario_email", length = 150)
    private String usuarioEmail;

    /** LOGIN_OK, LOGIN_FALHOU ou REQUISICAO */
    @Column(nullable = false, length = 30)
    private String evento;

    /** Ex.: "POST /api/doacoes -> 201" */
    @Column(length = 255)
    private String acao;

    @Column(length = 64)
    private String ip;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
}