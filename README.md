# Ejercicio 5A: regla de Simpson y distribución t

Calcula **p = integral de 0 a x de f(t) dt**, usando la densidad t de Student.
No es la probabilidad acumulada desde menos infinito: para x >= 0, esa acumulada sería 0.5 + p.

## Ejecutar

```sh
mvn clean verify
java -jar target/simpson-5a-1.0.0.jar
```

Sin argumentos ejecuta los tres casos de la tabla 1 del PDF y guarda `resultados.txt`.
Para otros valores, proporciona pares `x dof`; `x` usa punto decimal:

```sh
java -jar target/simpson-5a-1.0.0.jar 1.1 9 1.1812 10 2.750 30 --output=resultados.txt
java -jar target/simpson-5a-1.0.0.jar 1 1
```

Los grados de libertad admitidos son enteros de 1 a 342; x debe ser finito y no negativo.
Con 343 grados, la formula necesita Gamma(172), que ya desborda un `double`.
La carpeta de salida debe existir. Un error de entrada, escritura o convergencia termina con código 1.

## Cómo funciona

1. `StudentDerivation` calcula la densidad:
   `f(t) = Gamma((v+1)/2) / (sqrt(v*pi)*Gamma(v/2)) * (1+t²/v)^(-(v+1)/2)`.
2. `Gamma.calcular` usa `Gamma(z)=(z-1)*Gamma(z-1)`, `Gamma(1)=1` y `Gamma(1/2)=sqrt(pi)`.
   Multiplica los factores con un ciclo y usa los dos casos base.
   Por ejemplo, Gamma(5) = 4 * 3 * 2 * 1 = 24 y Gamma(2.5) = 1.5 * 0.5 * sqrt(pi).
   Si una multiplicacion desborda, informa un error. Para normalizar, primero divide
   Gamma((v+1)/2) entre Gamma(v/2) y luego entre sqrt(v*pi), evitando desbordar
   el producto del denominador. El factor se calcula una sola vez.
3. `SimpsonIntegration.integrar` usa `h/3 * [f(a) + 4*f(a+h) + 2*f(a+2h) + ... + f(b)]`.
4. `SimpsonIntegration.refinar` empieza con 10 segmentos, duplica a 20, 40, etc. y devuelve el último resultado cuando
   `abs(p_actual-p_anterior) < 0.00001`. Como pide el ejercicio, este es un criterio entre aproximaciones,
   no una garantía matemática del error real para cualquier función o intervalo. El límite de seguridad es 1310720 segmentos.
5. `Logic.calcular` coordina la distribucion y Simpson. `Main` interpreta la entrada
   y crea el reporte. `reuse.OutFile` lo escribe en disco. `Constant` centraliza tolerancia y limites.

## Estructura

```text
src/
  main/
    java/com/a5/
      reuse/
        OutFile.java
      Constant.java
      Gamma.java
      Logic.java
      Main.java
      SimpsonIntegration.java
      StudentDerivation.java
    resources/
  test/java/com/a5/
    Testfile.java
```

Se utiliza el nombre `StudentDerivation` para coincidir con la estructura solicitada;
esta clase calcula la densidad t, no una derivada matematica.
`reuse.OutFile` se reutiliza de `helloworld/programa3a/src/main/java/centenoProgram3/OutFile.java`:
conserva ambos metodos `escribir`, con cambio de paquete y comentario de procedencia.
La escritura de resultados mantiene UTF-8, que es la codificacion predeterminada de `Files.writeString`.

## Pruebas y resultados esperados

| x | dof | p esperado (PDF, tabla 1) |
|---|---|---|
| 1.1 | 9 | 0.35006 |
| 1.1812 | 10 | 0.36757 |
| 2.750 | 30 | 0.49500 |

JUnit comprueba estos valores con tolerancia absoluta 0.00001, polinomios con integral conocida,
refinamiento repetido, la integral analítica para dof=1 (`atan(x)/pi`), simetría, grados altos,
validaciones y escritura del reporte.

Todas las pruebas se encuentran en `src/test/java/com/a5/Testfile.java`,
con **un assert por caso ejecutado**. Conservan los 110 casos anteriores y agregan
dos pruebas de las sobrecargas del archivo reutilizado:

| Grupo dentro de Testfile | Casos | Responsabilidad |
|---|---:|---|
| SimpsonIntegration | 27 | Integrales conocidas, segmentos, tolerancia, valores no finitos y limite de refinamiento |
| Gamma, StudentDerivation y Logic | 41 | Casos del PDF, Gamma directa y desbordamiento, integrales analiticas, simetria y limites de entrada |
| Main | 42 | Argumentos, reportes, ayuda, errores y codigos de salida del proceso |
| reuse.OutFile | 2 | Escritura con ruta String y Path |
| **Total** | **112** | Cada entrada de una prueba parametrizada cuenta como un caso independiente |

`mvn clean verify` exige **100 % de lineas, ramas e instrucciones** del codigo de produccion
medidas por JaCoCo, sin excluir clases. Si baja cualquiera de estas medidas, Maven falla y
el check obligatorio `verificar` del pull request bloquea el merge a `main`.
Esto mide ejecucion de codigo; no garantiza ausencia de errores ni todas las combinaciones de entradas.

`Main.iniciar` permite probar la decision de cerrar el programa sin cerrar JUnit. Ademas,
dos pruebas lanzan una JVM separada y verifican los codigos reales de salida de `main`.

Después de `mvn clean verify`:

- Pruebas: `target/surefire-reports/`.
- Cobertura: abrir `target/site/jacoco/index.html`.
- Programa: `target/simpson-5a-1.0.0.jar`.

## GitHub Actions 

El workflow `.github/workflows/ci.yml` se ejecuta en cada push, pull request o ejecución manual.
Prepara Java 17, ejecuta `mvn -B clean verify`, corre los casos del PDF y publica el JAR,
resultados, reportes JUnit y cobertura en el artefacto `evidencia-simpson-5a`.
En GitHub abre **Actions**, selecciona una ejecución y descarga su artefacto.

El manifiesto del JAR utiliza `com.a5.Main`. El comando `java -jar` es el mismo.

Los cambios se realizan en develop y se integran a main mediante un pull request con las pruebas aprobadas.
