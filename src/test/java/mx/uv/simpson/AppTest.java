package mx.uv.simpson;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @TempDir Path directorio;

    private record Ejecucion(int codigo, String salida, String errores, Path archivo) { }

    private Ejecucion ejecutar(String... args) {
        Path archivo = directorio.resolve("reporte.txt");
        List<String> entradas = new ArrayList<>(List.of(args));
        entradas.add("--output=" + archivo);
        var salida = new ByteArrayOutputStream();
        var errores = new ByteArrayOutputStream();
        int codigo = App.ejecutar(entradas.toArray(String[]::new),
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
        assertEquals(1, App.ejecutar(new String[]{"--output="}, System.out, System.err));
    }

    @Test
    void rechazaEscribirSobreDirectorio() {
        assertEquals(1, App.ejecutar(new String[]{"--output=" + directorio}, System.out, System.err));
    }

    @Test
    void rechazaCarpetaDeSalidaInexistente() {
        assertEquals(1, App.ejecutar(new String[]{"--output=" + directorio.resolve("ausente/r.txt")},
                System.out, System.err));
    }

    @Test
    void ayudaTerminaSinError() {
        assertEquals(0, ejecutar("--help").codigo());
    }

    @Test
    void ayudaExplicaElUso() {
        assertTrue(ejecutar("--help").salida().contains("Uso:"));
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
                System.getProperty("java.class.path")), App.class.getName(), argumento));
        Process proceso = new ProcessBuilder(comando).directory(directorio.toFile())
                .redirectErrorStream(true).redirectOutput(directorio.resolve("proceso.txt").toFile()).start();
        try {
            if (!proceso.waitFor(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("El proceso de App no termino en 20 segundos");
            }
            assertEquals(esperado, proceso.exitValue(), () -> leerSalidaProceso());
        } finally {
            proceso.destroyForcibly();
        }
    }

    @Test
    void mainPermiteConsultarAyudaSinCerrarLaJvm() {
        assertDoesNotThrow(() -> App.main(new String[]{"--help"}));
    }

    @Test
    void inicioSolicitaTerminarConCodigoUnoAnteError() {
        var codigo = new AtomicInteger(-1);
        App.iniciar(new String[]{"1"}, System.out, System.err, codigo::set);
        assertEquals(1, codigo.get());
    }

    @Test
    void inicioNoSolicitaCierreAnteExito() {
        var llamadas = new AtomicInteger(0);
        App.iniciar(new String[]{"--help"}, System.out, System.err, codigo -> llamadas.incrementAndGet());
        assertEquals(0, llamadas.get());
    }

    private String leerSalidaProceso() {
        try {
            return Files.readString(directorio.resolve("proceso.txt"));
        } catch (java.io.IOException e) {
            return e.toString();
        }
    }
}
