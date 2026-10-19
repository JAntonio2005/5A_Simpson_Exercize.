package com.a5;
public final class StudentDerivation {
    private final int grados;
    private final double normalizacion;
    public StudentDerivation(int grados) {
        if (grados < 1 || grados > Constant.MAX_GRADOS) {
            throw new IllegalArgumentException("Los grados de libertad deben estar entre 1 y 342");
        }
        this.grados = grados;
        double gammaNumerador = Gamma.calcular((grados + 1.0) / 2);
        double gammaDenominador = Gamma.calcular(grados / 2.0);
        this.normalizacion = (gammaNumerador / gammaDenominador)
                / Math.sqrt(grados * Math.PI);
    }
    public double densidad(double x) {
        if (!Double.isFinite(x)) {
            throw new IllegalArgumentException("x debe ser finito");
        }
        double escala = Math.abs(x) / Math.sqrt(grados);
        double logBase = escala > Math.sqrt(Double.MAX_VALUE)
                ? 2 * Math.log(escala) : Math.log1p(escala * escala);
        return normalizacion * Math.exp(-((grados + 1.0) / 2) * logBase);
    }
}
