package mx.uv.simpson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SimpsonTest {
    @ParameterizedTest
    @CsvSource({"1.1,9,0.35006", "1.1812,10,0.36757", "2.750,30,0.49500"})
    void casosDelPdf(double x, int grados, double esperado) {
        var r = new StudentT(grados).probabilidad(x);
        assertEquals(esperado, r.valor(), 0.00001);
        assertTrue(r.segmentos() >= 20 && r.segmentos() % 2 == 0);
        assertTrue(r.diferencia() < StudentT.ERROR);
    }

    @Test
    void simpsonIntegraExactamentePolinomiosHastaGradoTres() {
        assertEquals(20, Simpson.integrar(x -> 5, 0, 4, 4), 1e-12);
        assertEquals(16, Simpson.integrar(x -> 2 * x, 0, 4, 4), 1e-12);
        assertEquals(0.25, Simpson.integrar(x -> x * x * x, 0, 1, 10), 1e-12);
    }

    @Test
    void refinamientoNecesitaMasDeUnaDuplicacion() {
        var r = Simpson.refinar(Math::exp, 0, 1, 1e-10);
        assertEquals(Math.E - 1, r.valor(), 1e-10);
        assertTrue(r.segmentos() > 20);
    }

    @Test
    void gammaEnteraYSemientera() {
        assertEquals(1, Math.exp(StudentT.logGamma(1)), 1e-12);
        assertEquals(Math.sqrt(Math.PI), Math.exp(StudentT.logGamma(0.5)), 1e-12);
        assertEquals(24, Math.exp(StudentT.logGamma(5)), 1e-12);
        assertEquals(105 * Math.sqrt(Math.PI) / 16, Math.exp(StudentT.logGamma(4.5)), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> StudentT.logGamma(0.75));
    }

    @Test
    void contrastaConIntegralAnaliticaDeCauchy() {
        // t con un grado de libertad: integral desde 0 hasta x = atan(x)/pi.
        for (double x : new double[]{0, 0.5, 1, 2, 5}) {
            assertEquals(Math.atan(x) / Math.PI, new StudentT(1).probabilidad(x).valor(), 1e-5);
        }
    }

    @Test
    void densidadSimetricaYGradosAltosSinDesbordamiento() {
        var t = new StudentT(9);
        assertEquals(t.densidad(2), t.densidad(-2), 1e-15);
        assertEquals(1 / Math.PI, new StudentT(1).densidad(0), 1e-12);
        var alta = new StudentT(1000);
        assertTrue(Double.isFinite(alta.densidad(0)));
        assertEquals(0.3988425573, alta.densidad(0), 1e-9);
        assertTrue(alta.probabilidad(1).valor() > 0.34);
        assertEquals(0, alta.densidad(Double.MAX_VALUE));
    }

    @Test
    void integralConAnchoCero() {
        assertEquals(0, new StudentT(9).probabilidad(0).valor());
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "10001"})
    void rechazaGradosFueraDelDominio(int grados) {
        assertThrows(IllegalArgumentException.class, () -> new StudentT(grados));
    }

    @Test
    void rechazaEntradasInvalidas() {
        for (double x : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new StudentT(9).probabilidad(x));
        }
        assertThrows(IllegalArgumentException.class, () -> new StudentT(9).densidad(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(null, 0, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(x -> x, 1, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(x -> x, 0, Double.NaN, 10));
        for (int n : new int[]{0, -2, 3, 2_000_000}) {
            assertThrows(IllegalArgumentException.class, () -> Simpson.integrar(x -> x, 0, 1, n));
        }
        for (double error : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> Simpson.refinar(x -> x, 0, 1, error));
        }
    }

    @Test
    void rechazaFuncionesNoFinitas() {
        assertThrows(ArithmeticException.class, () -> Simpson.refinar(x -> Double.NaN, 0, 1, 1e-5));
        assertThrows(ArithmeticException.class, () -> Simpson.integrar(x -> Double.MAX_VALUE, 0, 1, 10));
    }

    @Test
    void cliGuardaCasosObligatorios(@TempDir Path dir) throws Exception {
        var out = new ByteArrayOutputStream();
        Path archivo = dir.resolve("reporte.txt");
        assertEquals(0, App.ejecutar(new String[]{"--output=" + archivo},
                new PrintStream(out), System.err));
        String texto = Files.readString(archivo);
        assertTrue(texto.contains("esperado=0.35006"));
        assertTrue(texto.contains("esperado=0.36757"));
        assertTrue(texto.contains("esperado=0.49500"));
        assertTrue(texto.contains("segmentos="));
    }

    @Test
    void cliAceptaParesVariables(@TempDir Path dir) throws Exception {
        Path archivo = dir.resolve("reporte.txt");
        assertEquals(0, App.ejecutar(new String[]{"1", "1", "0", "9", "--output=" + archivo},
                System.out, System.err));
        String texto = Files.readString(archivo);
        assertTrue(texto.contains("p=0.25000000"));
        assertTrue(texto.contains("p=0.00000000"));
        assertTrue(texto.contains("esperado=N/A"));
    }

    @Test
    void cliIndicaErroresYNoCreaReporte(@TempDir Path dir) {
        var errores = new ByteArrayOutputStream();
        for (String[] args : new String[][]{{"1"}, {"abc", "9"}, {"1", "0"},
                {"--output="}, {"-1", "9"}, {"1", "9", "--output=" + dir}}) {
            assertEquals(1, App.ejecutar(args, System.out, new PrintStream(errores)));
        }
        assertTrue(errores.toString().contains("Error:"));
    }

    @Test
    void cliMuestraAyuda() {
        var out = new ByteArrayOutputStream();
        assertEquals(0, App.ejecutar(new String[]{"--help"}, new PrintStream(out), System.err));
        assertTrue(out.toString().contains("Uso:"));
    }
}
