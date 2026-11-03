package br.com.marketplacedoacoes.marketplacedoacoes.util;

public final class Telefones {

    private Telefones() {
    }

    // Devolve só os dígitos com DDD (10 ou 11 dígitos), ou null se o telefone for inválido.
    // Aceita "(51) 99999-8888", "51999998888" e "+55 51 99999-8888".
    public static String normalizar(String telefone) {
        if (telefone == null) {
            return null;
        }

        String digitos = telefone.replaceAll("\\D", "");

        // Remove o código do país (55) quando vier junto
        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }

        if (digitos.length() != 10 && digitos.length() != 11) {
            return null;
        }
        // DDD vai de 11 a 99, sem zero
        if (digitos.charAt(0) == '0' || digitos.charAt(1) == '0') {
            return null;
        }
        // Celular (11 dígitos) começa com 9; fixo (10 dígitos) começa com 2 a 9
        if (digitos.length() == 11 && digitos.charAt(2) != '9') {
            return null;
        }
        if (digitos.length() == 10 && (digitos.charAt(2) == '0' || digitos.charAt(2) == '1')) {
            return null;
        }
        return digitos;
    }
}