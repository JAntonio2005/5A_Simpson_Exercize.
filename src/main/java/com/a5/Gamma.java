package com.a5;

/** Funcion Gamma para enteros y semienteros positivos. */
public final class Gamma {
    private Gamma() { }

    /** Recurrencia Gamma(z)=(z-1)Gamma(z-1), solo enteros y semienteros. */
    public static double calcular(double numero) {
        if (!Double.isFinite(numero) || numero <= 0
                || numero * 2 != Math.floor(numero * 2)) {
            throw new IllegalArgumentException("Se requiere un entero o semientero positivo");
        }

        double resultado = 1;
        while (numero > 1) {
            numero = numero - 1;
            resultado = resultado * numero;

            if (!Double.isFinite(resultado)) {
                throw new ArithmeticException("Gamma es demasiado grande");
            }
        }

        if (numero == 0.5) {
            resultado = resultado * Math.sqrt(Math.PI);
        }

        return resultado;
    }
}
