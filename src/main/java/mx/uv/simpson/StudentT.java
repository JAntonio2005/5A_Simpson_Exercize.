package mx.uv.simpson;

/** Densidad t de Student para grados de libertad enteros de 1 a 10000. */
public final class StudentT {
    public static final double ERROR = 0.00001;
    private final int grados;
    private final double normalizacion;

    public StudentT(int grados) {
        if (grados < 1 || grados > 10_000) {
            throw new IllegalArgumentException("Los grados de libertad deben estar entre 1 y 10000");
        }
        this.grados = grados;
        // Usamos logaritmos para evitar el desbordamiento de gamma
        this.normalizacion = Math.exp(logGamma((grados + 1.0) / 2)
                - logGamma(grados / 2.0) - 0.5 * Math.log(grados * Math.PI));
    }

    public double densidad(double x) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("x debe ser finito");
        }
        // log(1 + x*x/v), evitando desbordamiento al elevar x al cuadrado.
        double escala = Math.abs(x) / Math.sqrt(grados);
        double logBase = escala > Math.sqrt(Double.MAX_VALUE)
                ? 2 * Math.log(escala) : Math.log1p(escala * escala);
        return normalizacion * Math.exp(-((grados + 1.0) / 2) * logBase);
    }

    public Simpson.Resultado probabilidad(double x) {
        if (!Double.isFinite(x) || x < 0) {
            throw new IllegalArgumentException("x debe ser no negativo y finito");
        }
        return Simpson.refinar(this::densidad, 0, x, ERROR);
    }

    /** Recurrencia Gamma(z)=(z-1)Gamma(z-1), solo enteros y semienteros. */
    static double logGamma(double z) {
        if (!Double.isFinite(z) || z < 0.5 || z > 5000.5 || z * 2 != Math.rint(z * 2)) {
            throw new IllegalArgumentException("Gamma requiere un entero o semientero entre 0.5 y 5000.5");
        }
        double suma = 0;
        while (z > 1) {
            z -= 1;
            suma += Math.log(z);
        }
        return suma + (z == 0.5 ? 0.5 * Math.log(Math.PI) : 0);
    }
}
