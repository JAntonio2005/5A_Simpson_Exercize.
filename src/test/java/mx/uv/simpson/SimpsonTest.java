package mx.uv.simpson;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class SimpsonTest {
    static Stream<Arguments> funcionesConAreaConocida() {
        return Stream.of(
                Arguments.of("constante", (DoubleUnaryOperator) x -> 5, 0, 4, 4, 20),
                Arguments.of("lineal", (DoubleUnaryOperator) x -> 2 * x, 0, 4, 4, 16),
                Arguments.of("cuadratica", (DoubleUnaryOperator) x -> x * x, 0, 3, 6, 9),
                Arguments.of("cubica", (DoubleUnaryOperator) x -> x * x * x, 0, 1, 10, 0.25));
    }

    @ParameterizedTest(name = "Area de funcion {0}")
    @MethodSource("funcionesConAreaConocida")
    void integraPolinomio(String nombre, DoubleUnaryOperator funcion,
                         double a, double b, int n, double esperado) {
        assertEquals(esperado, Simpson.integrar(funcion, a, b, n), 1e-12);
    }

    @Test
    void intervaloSinAnchoTieneAreaCero() {
        assertEquals(0, Simpson.integrar(x -> 5, 2, 2, 10));
    }

    @Test
    void refinamientoCalculaIntegralExponencial() {
        assertEquals(Math.E - 1, Simpson.refinar(Math::exp, 0, 1, 1e-10).valor(), 1e-10);
    }

    @Test
    void refinamientoDuplicaMasDeUnaVezCuandoEsNecesario() {
        assertTrue(Simpson.refinar(Math::exp, 0, 1, 1e-10).segmentos() > 20);
    }

    @Test
    void refinamientoRespetaLaTolerancia() {
        assertTrue(Simpson.refinar(Math::exp, 0, 1, 1e-10).diferencia() < 1e-10);
    }

    @Test
    void rechazaFuncionNula() {
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(null, 0, 1, 10));
    }

    static Stream<Arguments> intervalosInvalidos() {
        return Stream.of(Arguments.of(Double.NaN, 1), Arguments.of(Double.NEGATIVE_INFINITY, 1),
                Arguments.of(0, Double.NaN), Arguments.of(0, Double.POSITIVE_INFINITY),
                Arguments.of(1, 0));
    }

    @ParameterizedTest(name = "Rechaza intervalo [{0}, {1}]")
    @MethodSource("intervalosInvalidos")
    void rechazaIntervaloInvalido(double a, double b) {
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(x -> x, a, b, 10));
    }

    @ParameterizedTest(name = "Rechaza {0} segmentos")
    @ValueSource(ints = {0, -2, 3, 2_000_000})
    void rechazaSegmentosInvalidos(int n) {
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(x -> x, 0, 1, n));
    }

    @ParameterizedTest(name = "Rechaza tolerancia {0}")
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void rechazaToleranciaInvalida(double error) {
        assertThrows(IllegalArgumentException.class, () -> Simpson.refinar(x -> x, 0, 1, error));
    }

    @ParameterizedTest(name = "Rechaza altura {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaAlturaNoFinita(double altura) {
        assertThrows(ArithmeticException.class,
                () -> Simpson.refinar(x -> altura, 0, 1, 1e-5));
    }

    @Test
    void rechazaDesbordamientoDeLaSuma() {
        assertThrows(ArithmeticException.class,
                () -> Simpson.integrar(x -> Double.MAX_VALUE, 0, 1, 10));
    }

    @Test
    void detieneRefinamientoQueNoAlcanzaTolerancia() {
        // Funcion discontinua: Simpson produce 1/(3*n). Con esta tolerancia
        // la diferencia sigue siendo demasiado grande al llegar al limite.
        assertThrows(IllegalStateException.class,
                () -> Simpson.refinar(x -> x == 0 ? 1 : 0, 0, 1, 1e-20));
    }
}
