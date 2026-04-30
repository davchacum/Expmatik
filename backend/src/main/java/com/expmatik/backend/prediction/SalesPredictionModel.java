package com.expmatik.backend.prediction;

import java.util.List;

/**
 * Envuelve OrdinaryLeastSquares con la ingeniería de features del dominio.
 *
 * Vector de features por muestra (4 dimensiones):
 *   [1.0, sin(2π·mes/12), cos(2π·mes/12), precioUnitario]
 *
 * El par sin/cos codifica la estacionalidad mensual de forma cíclica para que
 * diciembre (12) y enero (1) queden numéricamente próximos.
 */
public class SalesPredictionModel {

    private double[] beta; // coeficientes: [intercepto, sinMes, cosMes, precio]
    private double r2;

    /**
     * Entrena el modelo a partir de vectores de features y sus objetivos.
     *
     * @param samples lista de vectores de features, construidos con buildFeatureVector()
     * @param targets ventas correspondientes a cada muestra
     */
    public void train(List<double[]> samples, List<Double> targets) {
        if (samples.size() < 4) {
            throw new IllegalArgumentException("Se necesitan al menos 4 muestras mensuales para ajustar 4 coeficientes");
        }
        int n = samples.size();
        double[][] X = new double[n][];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            X[i] = samples.get(i);
            y[i] = targets.get(i);
        }

        this.beta = OrdinaryLeastSquares.fit(X, y);
        this.r2 = OrdinaryLeastSquares.rSquared(X, y, beta);
    }

    /**
     * Predice el número de ventas para el mes y precio unitario indicados.
     * Fija en 0 las predicciones negativas (las ventas no pueden ser negativas).
     *
     * @param month     1–12
     * @param unitPrice precio de venta medio para el período
     * @return unidades vendidas estimadas
     */
    public double predict(int month) {
        if (beta == null) throw new IllegalStateException("El modelo aún no ha sido entrenado");
        return Math.max(0.0, OrdinaryLeastSquares.predict(buildFeatureVector(month), beta));
    }

    /**
     * Construye el vector de features [1, sin, cos, precio] para un mes y precio dados.
     * Es estático para que el servicio pueda reutilizarlo durante el entrenamiento.
     */
    public static double[] buildFeatureVector(int month) {
        double angle = 2.0 * Math.PI * month / 12.0;
        return new double[]{1.0, Math.sin(angle), Math.cos(angle)};
    }

    public boolean isTrained() {
        return beta != null;
    }

    /** Coeficiente de determinación (R²) medido sobre el conjunto de entrenamiento. */
    public double getR2() {
        return r2;
    }

    /** Coeficientes OLS brutos [intercepto, β_sin, β_cos, β_precio]. */
    public double[] getBeta() {
        return beta;
    }
}
