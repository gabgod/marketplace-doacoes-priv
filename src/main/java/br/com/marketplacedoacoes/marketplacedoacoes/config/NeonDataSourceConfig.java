package br.com.marketplacedoacoes.marketplacedoacoes.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.URI;

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
