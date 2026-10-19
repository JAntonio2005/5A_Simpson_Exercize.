package com.a5;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import com.a5.reuse.OutFile;

class Testfile {

    // Pruebas de entrada, reportes y ejecucion de Main.

    @TempDir Path directorio;

    private record Ejecucion(int codigo, String salida, String errores, Path archivo) { }

    private Ejecucion ejecutar(String... args) {
        Path archivo = directorio.resolve("reporte.txt");
        List<String> entradas = new ArrayList<>(List.of(args));
        entradas.add("--output=" + archivo);
        var salida = new ByteArrayOutputStream();
        var errores = new ByteArrayOutputStream();
        int codigo = Main.ejecutar(entradas.toArray(String[]::new),
                new PrintStream(salida), new PrintStream(errores));
        return new Ejecucion(codigo, salida.toString(StandardCharsets.UTF_8),
                errores.toString(StandardCharsets.UTF_8), archivo);
    }

    @Test
    void casosPredeterminadosTerminanSinError() {
        assertEquals(0, ejecutar().codigo());
    }

    @ParameterizedTest(name = "Reporte incluye {0}")
    @ValueSource(strings = {"esperado=0.35006", "esperado=0.36757", "esperado=0.49500",
            "segmentos=20", "diferencia=", "p=0.35005864"})
    void reporteIncluyeResultados(String contenido) throws Exception {
        assertTrue(Files.readString(ejecutar().archivo()).contains(contenido));
    }

    @Test
    void imprimeResultadosEnConsola() {
        assertTrue(ejecutar().salida().contains("p=0.35005864"));
    }

    @Test
    void ejecucionCorrectaNoImprimeErrores() {
        assertEquals("", ejecutar().errores());
    }

    @Test
    void paresPersonalizadosTerminanSinError() {
        assertEquals(0, ejecutar("1", "1", "0", "9").codigo());
    }

    @ParameterizedTest(name = "Pares personalizados incluyen {0}")
    @ValueSource(strings = {"p=0.25000000", "p=0.00000000", "esperado=N/A"})
    void reporteAceptaParesPersonalizados(String contenido) throws Exception {
        assertTrue(Files.readString(ejecutar("1", "1", "0", "9").archivo()).contains(contenido));
    }

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(Arguments.of("par incompleto", List.of("1")),
                Arguments.of("x no numerico", List.of("abc", "9")),
                Arguments.of("dof no entero", List.of("1", "1.5")),
                Arguments.of("dof cero", List.of("1", "0")),
                Arguments.of("x negativo", List.of("-1", "9")),
                Arguments.of("x NaN", List.of("NaN", "9")));
    }

    @ParameterizedTest(name = "Codigo de error: {0}")
    @MethodSource("entradasInvalidas")
    void entradaInvalidaDevuelveUno(String nombre, List<String> args) {
        assertEquals(1, ejecutar(args.toArray(String[]::new)).codigo());
    }

    @ParameterizedTest(name = "Mensaje de error: {0}")
    @MethodSource("entradasInvalidas")
    void entradaInvalidaExplicaElError(String nombre, List<String> args) {
        assertTrue(ejecutar(args.toArray(String[]::new)).errores().startsWith("Error:"));
    }

    @ParameterizedTest(name = "No crea reporte: {0}")
    @MethodSource("entradasInvalidas")
    void entradaInvalidaNoCreaArchivo(String nombre, List<String> args) {
        assertFalse(Files.exists(ejecutar(args.toArray(String[]::new)).archivo()));
    }

    @Test
    void rechazaRutaVacia() {
        assertEquals(1, Main.ejecutar(new String[]{"--output="}, System.out, System.err));
    }

    @Test
    void rechazaEscribirSobreDirectorio() {
        assertEquals(1, Main.ejecutar(new String[]{"--output=" + directorio}, System.out, System.err));
    }

    @Test
    void rechazaCarpetaDeSalidaInexistente() {
        assertEquals(1, Main.ejecutar(new String[]{"--output=" + directorio.resolve("ausente/r.txt")},
                System.out, System.err));
    }

    @Test
    void ayudaTerminaSinError() {
        assertEquals(0, ejecutar("--help").codigo());
    }

    @Test
    void ayudaExplicaElUso() {
        assertTrue(ejecutar("--help").salida().contains("Instrucciones:"));
    }

    @Test
    void ayudaNoCreaReporte() {
        assertFalse(Files.exists(ejecutar("--help").archivo()));
    }

    @ParameterizedTest(name = "main({0}) termina con codigo {1}")
    @CsvSource({"--help,0", "1,1"})
    void mainDevuelveCodigoDelProceso(String argumento, int esperado) throws Exception {
        // Proceso separado para comprobar System.exit sin terminar JUnit.
        List<String> comando = new ArrayList<>();
        comando.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        comando.addAll(List.of("-cp", System.getProperty("surefire.test.class.path",
                System.getProperty("java.class.path")), Main.class.getName(), argumento));
        Process proceso = new ProcessBuilder(comando).directory(directorio.toFile())
                .redirectErrorStream(true).redirectOutput(directorio.resolve("proceso.txt").toFile()).start();
        try {
            if (!proceso.waitFor(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("El proceso de Main no termino en 20 segundos");
            }
            assertEquals(esperado, proceso.exitValue(), () -> leerSalidaProceso());
        } finally {
            proceso.destroyForcibly();
        }
    }

    @Test
    void mainPermiteConsultarAyudaSinCerrarLaJvm() {
        assertDoesNotThrow(() -> Main.main(new String[]{"--help"}));
    }

    @Test
    void inicioSolicitaTerminarConCodigoUnoAnteError() {
        var codigo = new AtomicInteger(-1);
        Main.iniciar(new String[]{"1"}, System.out, System.err, codigo::set);
        assertEquals(1, codigo.get());
    }

    @Test
    void inicioNoSolicitaCierreAnteExito() {
        var llamadas = new AtomicInteger(0);
        Main.iniciar(new String[]{"--help"}, System.out, System.err, codigo -> llamadas.incrementAndGet());
        assertEquals(0, llamadas.get());
    }

    private String leerSalidaProceso() {
        try {
            return Files.readString(directorio.resolve("proceso.txt"));
        } catch (java.io.IOException e) {
            return e.toString();
        }
    }

    // Pruebas de SimpsonIntegration.
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
        assertEquals(esperado, SimpsonIntegration.integrar(funcion, a, b, n), 1e-12);
    }

    @Test
    void intervaloSinAnchoTieneAreaCero() {
        assertEquals(0, SimpsonIntegration.integrar(x -> 5, 2, 2, 10));
    }

    @Test
    void refinamientoCalculaIntegralExponencial() {
        assertEquals(Math.E - 1, SimpsonIntegration.refinar(Math::exp, 0, 1, 1e-10).valor(), 1e-10);
    }

    @Test
    void refinamientoDuplicaMasDeUnaVezCuandoEsNecesario() {
        assertTrue(SimpsonIntegration.refinar(Math::exp, 0, 1, 1e-10).segmentos() > 20);
    }

    @Test
    void refinamientoRespetaLaTolerancia() {
        assertTrue(SimpsonIntegration.refinar(Math::exp, 0, 1, 1e-10).diferencia() < 1e-10);
    }

    @Test
    void rechazaFuncionNula() {
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegration.integrar(null, 0, 1, 10));
    }

    static Stream<Arguments> intervalosInvalidos() {
        return Stream.of(Arguments.of(Double.NaN, 1), Arguments.of(Double.NEGATIVE_INFINITY, 1),
                Arguments.of(0, Double.NaN), Arguments.of(0, Double.POSITIVE_INFINITY),
                Arguments.of(1, 0));
    }

    @ParameterizedTest(name = "Rechaza intervalo [{0}, {1}]")
    @MethodSource("intervalosInvalidos")
    void rechazaIntervaloInvalido(double a, double b) {
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegration.integrar(x -> x, a, b, 10));
    }

    @ParameterizedTest(name = "Rechaza {0} segmentos")
    @ValueSource(ints = {0, -2, 3, 2_000_000})
    void rechazaSegmentosInvalidos(int n) {
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegration.integrar(x -> x, 0, 1, n));
    }

    @ParameterizedTest(name = "Rechaza tolerancia {0}")
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY})
    void rechazaToleranciaInvalida(double error) {
        assertThrows(IllegalArgumentException.class, () -> SimpsonIntegration.refinar(x -> x, 0, 1, error));
    }

    @ParameterizedTest(name = "Rechaza altura {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaAlturaNoFinita(double altura) {
        assertThrows(ArithmeticException.class,
                () -> SimpsonIntegration.refinar(x -> altura, 0, 1, 1e-5));
    }

    @Test
    void rechazaDesbordamientoDeLaSuma() {
        assertThrows(ArithmeticException.class,
                () -> SimpsonIntegration.integrar(x -> Double.MAX_VALUE, 0, 1, 10));
    }

    @Test
    void detieneRefinamientoQueNoAlcanzaTolerancia() {
        // Funcion discontinua: SimpsonIntegration produce 1/(3*n). Con esta tolerancia
        // la diferencia sigue siendo demasiado grande al llegar al limite.
        assertThrows(IllegalStateException.class,
                () -> SimpsonIntegration.refinar(x -> x == 0 ? 1 : 0, 0, 1, 1e-20));
    }

    // Pruebas de Gamma, StudentDerivation y Logic.
    @ParameterizedTest(name = "PDF: x={0}, dof={1}, p={2}")
    @CsvSource({"1.1,9,0.35006", "1.1812,10,0.36757", "2.750,30,0.49500"})
    void coincideConTablaDelPdf(double x, int grados, double esperado) {
        assertEquals(esperado, Logic.calcular(x, grados).valor(), 0.00001);
    }

    @ParameterizedTest(name = "Gamma({0})")
    @MethodSource("valoresGammaConocidos")
    void calculaGamma(double z, double esperado) {
        assertEquals(esperado, Gamma.calcular(z), 1e-12);
    }

    static Stream<Arguments> valoresGammaConocidos() {
        return Stream.of(Arguments.of(1, 1), Arguments.of(0.5, Math.sqrt(Math.PI)),
                Arguments.of(5, 24), Arguments.of(4.5, 105 * Math.sqrt(Math.PI) / 16),
                Arguments.of(2.5, 0.75 * Math.sqrt(Math.PI)));
    }

    @ParameterizedTest(name = "Gamma rechaza {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, 0, -1, 0.25, 0.75})
    void rechazaArgumentoGammaInvalido(double z) {
        assertThrows(IllegalArgumentException.class, () -> Gamma.calcular(z));
    }

    @ParameterizedTest(name = "Gamma detecta desbordamiento para {0}")
    @ValueSource(doubles = {172, 172.5})
    void rechazaGammaQueDesborda(double numero) {
        assertThrows(ArithmeticException.class, () -> Gamma.calcular(numero));
    }

    @ParameterizedTest(name = "Gamma grande aun representable: {0}")
    @CsvSource({"171,7.257415615307998e306", "171.5,9.483367566824801e307"})
    void calculaGammaGrandeSinDesbordar(double numero, double esperado) {
        assertEquals(1, Gamma.calcular(numero) / esperado, 1e-14);
    }

    @ParameterizedTest(name = "Integral analitica Cauchy hasta {0}")
    @ValueSource(doubles = {0, 0.5, 1, 2, 5})
    void coincideConIntegralAnaliticaDeCauchy(double x) {
        assertEquals(Math.atan(x) / Math.PI, Logic.calcular(x, 1).valor(), 1e-5);
    }

    @Test
    void densidadEsSimetrica() {
        var t = new StudentDerivation(9);
        assertEquals(t.densidad(2), t.densidad(-2), 1e-15);
    }

    @Test
    void densidadCauchyEnCero() {
        assertEquals(1 / Math.PI, new StudentDerivation(1).densidad(0), 1e-12);
    }

    @Test
    void densidadConDosGradosTieneValorConocidoEnCero() {
        assertEquals(1 / (2 * Math.sqrt(2)), new StudentDerivation(2).densidad(0), 1e-12);
    }

    @Test
    void probabilidadConDosGradosCoincideConIntegralAnalitica() {
        // Para dof=2, la integral de 0 a x es x/(2*sqrt(x*x+2)).
        assertEquals(1 / (2 * Math.sqrt(3)), Logic.calcular(1, 2).valor(), 1e-5);
    }

    @Test
    void aceptaLimiteSuperiorDeGrados() {
        assertEquals(0.39865076290461415, new StudentDerivation(342).densidad(0), 1e-12);
    }

    @Test
    void aceptaGradosImparesCercaDelLimite() {
        assertTrue(Double.isFinite(new StudentDerivation(341).densidad(0)));
    }

    @Test
    void densidadEnExtremoFinitoTiendeACero() {
        assertEquals(0, new StudentDerivation(342).densidad(Double.MAX_VALUE));
    }

    @Test
    void integralHastaCeroEsCero() {
        assertEquals(0, Logic.calcular(0, 9).valor());
    }

    @ParameterizedTest(name = "Rechaza dof={0}")
    @ValueSource(ints = {0, -1, 343})
    void rechazaGradosFueraDelDominio(int grados) {
        assertThrows(IllegalArgumentException.class, () -> new StudentDerivation(grados));
    }

    @ParameterizedTest(name = "Probabilidad rechaza x={0}")
    @ValueSource(doubles = {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaLimiteInvalido(double x) {
        assertThrows(IllegalArgumentException.class, () -> Logic.calcular(x, 9));
    }

    @ParameterizedTest(name = "Densidad rechaza x={0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rechazaPuntoNoFinito(double x) {
        assertThrows(IllegalArgumentException.class, () -> new StudentDerivation(9).densidad(x));
    }

    // Pruebas del componente reutilizado de programa3a.
    @Test
    void outFileReutilizadoEscribeConRutaString() throws Exception {
        Path archivo = directorio.resolve("reuse-string.txt");
        new OutFile().escribir(archivo.toString(), "resultado");
        assertEquals("resultado", Files.readString(archivo));
    }

    @Test
    void outFileReutilizadoEscribeConPath() throws Exception {
        Path archivo = directorio.resolve("reuse-path.txt");
        new OutFile().escribir(archivo, "texto");
        assertEquals("texto", Files.readString(archivo));
    }
}
