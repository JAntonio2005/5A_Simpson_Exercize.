package mx.uv.simpson;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class App {
    private App() { }

    public static void main(String[] args) {
        int codigo = ejecutar(args, System.out, System.err);
        if (codigo != 0) {
            System.exit(codigo);
        }
    }

    private record Caso(double x, int grados, Double esperado) { }

    static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
        try {
            Path archivo = Path.of("resultados.txt");
            List<String> entradas = new ArrayList<>();
            for (String arg : args) {
                if (arg.equals("--help")) {
                    salida.println("Uso: java -jar target/simpson-5a-1.0.0.jar [x dof ...] [--output=ruta]");
                    return 0;
                }
                if (arg.startsWith("--output=")) {
                    String ruta = arg.substring(9);
                    if (ruta.isBlank()) {
                        throw new IllegalArgumentException("La ruta de salida esta vacia");
                    }
                    archivo = Path.of(ruta);
                } else {
                    entradas.add(arg);
                }
            }
            if (entradas.size() % 2 != 0) {
                throw new IllegalArgumentException("Proporciona pares: x gradosDeLibertad");
            }
            List<Caso> casos = new ArrayList<>();
            if (entradas.isEmpty()) {
                casos.addAll(List.of(new Caso(1.1, 9, 0.35006),
                        new Caso(1.1812, 10, 0.36757), new Caso(2.750, 30, 0.49500)));
            } else {
                for (int i = 0; i < entradas.size(); i += 2) {
                    casos.add(new Caso(Double.parseDouble(entradas.get(i)),
                            Integer.parseInt(entradas.get(i + 1)), null));
                }
            }
            StringBuilder reporte = new StringBuilder("Ejercicio 5A: integral t de Student de 0 a x\n");
            for (Caso caso : casos) {
                Simpson.Resultado r = new StudentT(caso.grados()).probabilidad(caso.x());
                String esperado = caso.esperado() == null ? "N/A"
                        : String.format(Locale.ROOT, "%.5f", caso.esperado());
                reporte.append(String.format(Locale.ROOT,
                        "x=%.4f dof=%d esperado=%s p=%.8f segmentos=%d diferencia=%.3e%n",
                        caso.x(), caso.grados(), esperado, r.valor(), r.segmentos(), r.diferencia()));
            }
            Files.writeString(archivo, reporte, StandardCharsets.UTF_8);
            salida.print(reporte);
            salida.println("Reporte guardado en: " + archivo);
            return 0;
        } catch (IllegalArgumentException | IOException | ArithmeticException | IllegalStateException e) {
            errores.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
