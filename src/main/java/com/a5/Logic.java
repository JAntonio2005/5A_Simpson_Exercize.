package com.a5;
public final class Logic {
    private Logic() { }

    public static SimpsonIntegration.Resultado calcular(double x, int grados) {
        StudentDerivation distribucion = new StudentDerivation(grados);
        if (!Double.isFinite(x) || x < 0) {
            throw new IllegalArgumentException("x debe ser no negativo y finito");
        }
        return SimpsonIntegration.refinar(distribucion::densidad, 0, x, Constant.ERROR);
    }
}
