package mx.uv.simpson;

/** Densidad t de Student para grados de libertad enteros de 1 a 342. */
public final class StudentT {
    public static final double ERROR = 0.00001;
    private final int grados;
    private final double normalizacion;

    public StudentT(int grados) {
        if (grados < 1 || grados > 342) {
            throw new IllegalArgumentException("Los grados de libertad deben estar entre 1 y 342");
        }
        this.grados = grados;
        double gammaNumerador = gamma((grados + 1.0) / 2);
        double gammaDenominador = gamma(grados / 2.0);
        // Dividimos primero las Gamma para no desbordar el denominador.
        this.normalizacion = (gammaNumerador / gammaDenominador)
                / Math.sqrt(grados * Math.PI);
    }

    public double densidad(double x) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("x debe ser finito");
        }
        // log(1 + x*x/v), evitando desbordamiento al elevar x al cuadrado.
        double escala = Math.abs(x) / Math.sqrt(grados);
        double logBase = escala > Math.sqrt(Double.MAX_VALUE)
                ? 2 * Math.log(escala) : Math.log1p(escala * escala);
        return normalizacion * Math.exp(-((grados + 1.0) / 2) * logBase);
    }

    public Simpson.Resultado probabilidad(double x) {
        if (!Double.isFinite(x) || x < 0) {
            throw new IllegalArgumentException("x debe ser no negativo y finito");
        }
        return Simpson.refinar(this::densidad, 0, x, ERROR);
    }

    /** Recurrencia Gamma(z)=(z-1)Gamma(z-1), solo enteros y semienteros. */
    static double gamma(double numero) {
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
