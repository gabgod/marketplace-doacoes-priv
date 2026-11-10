package br.com.marketplacedoacoes.marketplacedoacoes.util;

public final class Telefones {
    private Telefones() {
    }

    public static String normalizar(String telefone) {
        if (telefone == null) {
            return null;
        }

        String digitos = telefone.replaceAll("\\D", "");

        if ((digitos.length() == 12 || digitos.length() == 13) && digitos.startsWith("55")) {
            digitos = digitos.substring(2);
        }

        if (digitos.length() != 10 && digitos.length() != 11) {
            return null;
        }
        if (digitos.charAt(0) == '0' || digitos.charAt(1) == '0') {
            return null;
        }
        if (digitos.length() == 11 && digitos.charAt(2) != '9') {
            return null;
        }
        if (digitos.length() == 10 && (digitos.charAt(2) == '0' || digitos.charAt(2) == '1')) {
            return null;
        }
        return digitos;
    }
}