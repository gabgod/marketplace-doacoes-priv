package br.com.marketplacedoacoes.marketplacedoacoes.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;

/**
 * Conecta a aplicação ao Neon usando a variável gerada pelo "neon deploy".
 *
 * O Neon entrega postgresql://usuario:senha@host/banco?params, mas o driver JDBC
 * espera jdbc:postgresql://host/banco?params, com usuário e senha separados.
 *
 * Usa a conexão direta (sem -pooler): o ddl-auto=update altera o schema no boot,
 * e o HikariCP já faz o pool de conexões da aplicação.
 */
@Configuration
@Profile("neon")
public class NeonDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(@Value("${DATABASE_URL_UNPOOLED}") String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String[] credenciais = uri.getUserInfo().split(":", 2);
        String porta = uri.getPort() == -1 ? "" : ":" + uri.getPort();

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://" + uri.getHost() + porta + uri.getRawPath() + "?" + uri.getRawQuery());
        dataSource.setUsername(credenciais[0]);
        dataSource.setPassword(credenciais[1]);
        return dataSource;
    }
}
