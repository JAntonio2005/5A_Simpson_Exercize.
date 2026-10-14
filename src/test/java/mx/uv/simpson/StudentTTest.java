package mx.uv.simpson;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class StudentTTest {
    @ParameterizedTest(name = "PDF: x={0}, dof={1}, p={2}")
    @CsvSource({"1.1,9,0.35006", "1.1812,10,0.36757", "2.750,30,0.49500"})
    void coincideConTablaDelPdf(double x, int grados, double esperado) {
        assertEquals(esperado, new StudentT(grados).probabilidad(x).valor(), 0.00001);
    }

    @ParameterizedTest(name = "Gamma({0})")
    @MethodSource("valoresGammaConocidos")
    void calculaGamma(double z, double esperado) {
        assertEquals(esperado, StudentT.gamma(z), 1e-12);
    }

    static Stream<Arguments> valoresGammaConocidos() {
        return Stream.of(Arguments.of(1, 1), Arguments.of(0.5, Math.sqrt(Math.PI)),
                Arguments.of(5, 24), Arguments.of(4.5, 105 * Math.sqrt(Math.PI) / 16),
                Arguments.of(2.5, 0.75 * Math.sqrt(Math.PI)));
    }

    @ParameterizedTest(name = "Gamma rechaza {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, 0, -1, 0.25, 0.75})
    void rechazaArgumentoGammaInvalido(double z) {
        assertThrows(IllegalArgumentException.class, () -> StudentT.gamma(z));
    }

    @ParameterizedTest(name = "Gamma detecta desbordamiento para {0}")
    @ValueSource(doubles = {172, 172.5})
    void rechazaGammaQueDesborda(double numero) {
        assertThrows(ArithmeticException.class, () -> StudentT.gamma(numero));
    }

    @ParameterizedTest(name = "Gamma grande aun representable: {0}")
    @CsvSource({"171,7.257415615307998e306", "171.5,9.483367566824801e307"})
    void calculaGammaGrandeSinDesbordar(double numero, double esperado) {
        assertEquals(1, StudentT.gamma(numero) / esperado, 1e-14);
    }

    @ParameterizedTest(name = "Integral analitica Cauchy hasta {0}")
    @ValueSource(doubles = {0, 0.5, 1, 2, 5})
    void coincideConIntegralAnaliticaDeCauchy(double x) {
        assertEquals(Math.atan(x) / Math.PI, new StudentT(1).probabilidad(x).valor(), 1e-5);
    }

    @Test
    void densidadEsSimetrica() {
        var t = new StudentT(9);
        assertEquals(t.densidad(2), t.densidad(-2), 1e-15);
    }

    @Test
    void densidadCauchyEnCero() {
        assertEquals(1 / Math.PI, new StudentT(1).densidad(0), 1e-12);
    }

    @Test
    void densidadConDosGradosTieneValorConocidoEnCero() {
        assertEquals(1 / (2 * Math.sqrt(2)), new StudentT(2).densidad(0), 1e-12);
    }

    @Test
    void probabilidadConDosGradosCoincideConIntegralAnalitica() {
        // Para dof=2, la integral de 0 a x es x/(2*sqrt(x*x+2)).
        assertEquals(1 / (2 * Math.sqrt(3)), new StudentT(2).probabilidad(1).valor(), 1e-5);
    }

    @Test
    void aceptaLimiteSuperiorDeGrados() {
        assertEquals(0.39865076290461415, new StudentT(342).densidad(0), 1e-12);
    }

    @Test
    void aceptaGradosImparesCercaDelLimite() {
        assertTrue(Double.isFinite(new StudentT(341).densidad(0)));
    }

    @Test
    void densidadEnExtremoFinitoTiendeACero() {
        assertEquals(0, new StudentT(342).densidad(Double.MAX_VALUE));
    }

    @Test
    void integralHastaCeroEsCero() {
        assertEquals(0, new StudentT(9).probabilidad(0).valor());
    }

    @ParameterizedTest(name = "Rechaza dof={0}")
    @ValueSource(ints = {0, -1, 343})
    void rechazaGradosFueraDelDominio(int grados) {
        assertThrows(IllegalArgumentException.class, () -> new StudentT(grados));
    }

    @ParameterizedTest(name = "Probabilidad rechaza x={0}")
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaLimiteInvalido(double x) {
        assertThrows(IllegalArgumentException.class, () -> new StudentT(9).probabilidad(x));
    }

    @ParameterizedTest(name = "Densidad rechaza x={0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaPuntoNoFinito(double x) {
        assertThrows(IllegalArgumentException.class, () -> new StudentT(9).densidad(x));
    }
}
