package mx.uv.simpson;

import java.util.function.DoubleUnaryOperator;


public final class Simpson {
    private static final int MAX_SEGMENTOS = 1_310_720;

    private Simpson() { }

    public record Resultado(double valor, int segmentos, double diferencia) { }

    public static double integrar(DoubleUnaryOperator funcion, double a, double b, int n) {
        if (funcion == null || !Double.isFinite(a) || !Double.isFinite(b) || b < a) {
            throw new IllegalArgumentException("Funcion o intervalo no valido");
        }
        if (n <= 0 || n % 2 != 0 || n > MAX_SEGMENTOS) {
            throw new IllegalArgumentException("Los segmentos deben ser pares entre 2 y " + MAX_SEGMENTOS);
        }
        if (a == b) {
            return 0;
        }
        double ancho = (b - a) / n;
        double suma = evaluar(funcion, a) + evaluar(funcion, b);
        for (int i = 1; i < n; i++) {
            suma += (i % 2 == 0 ? 2 : 4) * evaluar(funcion, a + i * ancho);
        }
        double valor = ancho * suma / 3;
        if (!Double.isFinite(valor)) {
            throw new ArithmeticException("La integral no es finita");
        }
        return valor;
    }

    public static Resultado refinar(DoubleUnaryOperator funcion, double a, double b, double error) {
        if (!Double.isFinite(error) || error <= 0) {
            throw new IllegalArgumentException("El error debe ser positivo y finito");
        }
        int n = 10;
        double anterior = integrar(funcion, a, b, n);
        while (n < MAX_SEGMENTOS) {
            n *= 2;
            double actual = integrar(funcion, a, b, n);
            double diferencia = Math.abs(actual - anterior);
            if (diferencia < error) {
                return new Resultado(actual, n, diferencia);
            }
            anterior = actual;
        }
        throw new IllegalStateException("No se alcanzo la tolerancia con " + n + " segmentos");
    }

    private static double evaluar(DoubleUnaryOperator funcion, double x) {
        double valor = funcion.applyAsDouble(x);
        if (!Double.isFinite(valor)) {
            throw new ArithmeticException("La funcion produjo un valor no finito");
        }
        return valor;
    }
}
