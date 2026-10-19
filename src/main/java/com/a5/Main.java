package com.a5;

import com.a5.reuse.OutFile;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        iniciar(args, System.out, System.err, System::exit);
    }

    // Separa la decision de terminar del cierre real de la JVM para probarla.
    static void iniciar(String[] args, PrintStream salida, PrintStream errores, IntConsumer terminar) {
        int codigo = ejecutar(args, salida, errores);
        if (codigo != 0) {
            terminar.accept(codigo);
        }
    }

    private record Caso(double x, int grados, Double esperado) { }

    static int ejecutar(String[] args, PrintStream salida, PrintStream errores) {
        try {
            Path archivo = Path.of("resultados.txt");
            List<String> entradas = new ArrayList<>();
            for (String arg : args) {
                if (arg.equals("--help")) {
                    salida.println("Instrucciones: java -jar target/simpson-5a-1.0.0.jar [x dof ...] [--output=ruta]");
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
                SimpsonIntegration.Resultado r = Logic.calcular(caso.x(), caso.grados());
                String esperado = caso.esperado() == null ? "N/A"
                        : String.format(Locale.ROOT, "%.5f", caso.esperado());
                reporte.append(String.format(Locale.ROOT,
                        "x=%.4f dof=%d esperado=%s p=%.8f segmentos=%d diferencia=%.3e%n",
                        caso.x(), caso.grados(), esperado, r.valor(), r.segmentos(), r.diferencia()));
            }
            new OutFile().escribir(archivo, reporte.toString());
            salida.print(reporte);
            salida.println("Reporte guardado en: " + archivo);
            return 0;
        } catch (IllegalArgumentException | IOException | ArithmeticException | IllegalStateException e) {
            errores.println("Error: " + e.getMessage());
            return 1;
        }
    }
}
