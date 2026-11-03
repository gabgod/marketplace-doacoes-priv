package br.com.marketplacedoacoes.marketplacedoacoes.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TelefonesTest {

    @ParameterizedTest(name = "\"{0}\" vira {1}")
    @CsvSource(delimiter = '|', value = {
            "(51) 99999-8888|51999998888",
            "51999998888|51999998888",
            "+55 51 99999-8888|51999998888",
            "55 51 99999-8888|51999998888",
            "(51) 3333-4444|5133334444",
            "+55 (51) 3333-4444|5133334444",
            // DDD 55 (interior do RS) não pode ser confundido com o código do país
            "(55) 99999-8888|55999998888",
            "55 55 99999-8888|55999998888"
    })
    void aceitaTelefonesValidosEDevolveSoOsDigitos(String entrada, String esperado) {
        assertThat(Telefones.normalizar(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "\"{0}\" é inválido")
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "123",
            "abcdef",
            "999999999",          // sem DDD
            "(01) 99999-8888",    // DDD não começa com zero
            "(10) 99999-8888",    // DDD não termina em zero
            "(51) 89999-8888",    // celular precisa começar com 9
            "(51) 1333-4444",     // fixo não começa com 1
            "(51) 0333-4444",     // fixo não começa com 0
            "(51) 99999-88889"    // dígitos demais
    })
    void rejeitaTelefonesInvalidos(String entrada) {
        assertThat(Telefones.normalizar(entrada)).isNull();
    }
}