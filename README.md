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

Los grados de libertad admitidos son enteros de 1 a 10000; x debe ser finito y no negativo.
La carpeta de salida debe existir. Un error de entrada, escritura o convergencia termina con código 1.

## Cómo funciona

1. `StudentT` calcula la densidad:
   `f(t) = Gamma((v+1)/2) / (sqrt(v*pi)*Gamma(v/2)) * (1+t²/v)^(-(v+1)/2)`.
2. Gamma usa `Gamma(z)=(z-1)*Gamma(z-1)`, `Gamma(1)=1` y `Gamma(1/2)=sqrt(pi)`.
   Se acumulan logaritmos para evitar desbordamiento de los factoriales; el factor de normalización se calcula una sola vez.
3. `Simpson.integrar` usa `h/3 * [f(a) + 4*f(a+h) + 2*f(a+2h) + ... + f(b)]`.
4. `Simpson.refinar` empieza con 10 segmentos, duplica a 20, 40, etc. y devuelve el último resultado cuando
   `abs(p_actual-p_anterior) < 0.00001`. Como pide el ejercicio, este es un criterio entre aproximaciones,
   no una garantía matemática del error real para cualquier función o intervalo. El límite de seguridad es 1310720 segmentos.
5. `App` interpreta la entrada y guarda valor, segmentos y diferencia final.

## Pruebas y resultados esperados

| x | dof | p esperado (PDF, tabla 1) |
|---|---|---|
| 1.1 | 9 | 0.35006 |
| 1.1812 | 10 | 0.36757 |
| 2.750 | 30 | 0.49500 |

JUnit comprueba estos valores con tolerancia absoluta 0.00001, polinomios con integral conocida,
refinamiento repetido, la integral analítica para dof=1 (`atan(x)/pi`), simetría, grados altos,
validaciones y escritura del reporte.

Las pruebas se organizan por clase, con **un assert por caso ejecutado**:

| Archivo | Casos | Responsabilidad |
|---|---:|---|
| `SimpsonTest.java` | 27 | Integrales conocidas, segmentos, tolerancia, valores no finitos y limite de refinamiento |
| `StudentTTest.java` | 34 | Casos del PDF, Gamma, integral de Cauchy, simetria y limites de entrada |
| `AppTest.java` | 42 | Argumentos, reportes, ayuda, errores y codigos de salida del proceso |
| **Total** | **103** | Cada entrada de una prueba parametrizada cuenta como un caso independiente |

`mvn clean verify` exige **100 % de lineas, ramas e instrucciones** del codigo de produccion
medidas por JaCoCo, sin excluir clases. Si baja cualquiera de estas medidas, Maven falla y
el check obligatorio `verificar` del pull request bloquea el merge a `main`.
Esto mide ejecucion de codigo; no garantiza ausencia de errores ni todas las combinaciones de entradas.

`App.iniciar` permite probar la decision de cerrar el programa sin cerrar JUnit. Ademas,
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

Los cambios se realizan en develop y se integran a main mediante un pull request con las pruebas aprobadas.
