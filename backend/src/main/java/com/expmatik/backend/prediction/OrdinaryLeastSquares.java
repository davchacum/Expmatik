package com.expmatik.backend.prediction;

/**
 * Regresión lineal ordinaria (OLS) mediante la ecuación normal: β = (X'X)⁻¹ X'y
 *
 * El sistema (X'X)β = X'y se resuelve con eliminación gaussiana y pivoteo parcial.
 * No requiere dependencias externas.
 */
public final class OrdinaryLeastSquares {

    private OrdinaryLeastSquares() {}

    /**
     * Ajusta el modelo OLS con regularización Ridge (λ=1e-6) y devuelve el vector β.
     * La regularización Ridge resuelve: (X'X + λI)β = X'y, mejorando la estabilidad numérica.
     * La primera columna de X debe ser todo 1.0 (término independiente).
     *
     * @param X matriz de features [n_muestras × n_features]
     * @param y vector objetivo [n_muestras]
     * @return coeficientes β [n_features]
     */
    public static double[] fit(double[][] X, double[] y) {
        return fit(X, y, 1e-6);
    }

    /**
     * Ajusta el modelo OLS con regularización Ridge y devuelve el vector β.
     * Resuelve: (X'X + λI)β = X'y
     *
     * @param X      matriz de features [n_muestras × n_features]
     * @param y      vector objetivo [n_muestras]
     * @param lambda parámetro de regularización (λ > 0)
     * @return coeficientes β [n_features]
     */
    public static double[] fit(double[][] X, double[] y, double lambda) {
        int n = X.length;
        int p = X[0].length;

        // Construir X'X (p×p) y X'y (p)
        double[][] XtX = new double[p][p];
        double[] Xty = new double[p];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < p; j++) {
                Xty[j] += X[i][j] * y[i];
                for (int k = 0; k < p; k++) {
                    XtX[j][k] += X[i][j] * X[i][k];
                }
            }
        }

        // Añadir regularización Ridge: (X'X + λI)
        for (int i = 0; i < p; i++) {
            XtX[i][i] += lambda;
        }

        return resolverSistemaLineal(XtX, Xty);
    }

    /** Devuelve la predicción para una muestra: x · β */
    public static double predict(double[] x, double[] beta) {
        double resultado = 0.0;
        for (int i = 0; i < beta.length; i++) {
            resultado += beta[i] * x[i];
        }
        return resultado;
    }

    /** Coeficiente de determinación R² ∈ [0, 1] — cuanto más alto, mejor ajuste. */
    public static double rSquared(double[][] X, double[] y, double[] beta) {
        int n = y.length;
        double media = 0.0;
        for (double yi : y) media += yi;
        media /= n;

        double ssTot = 0.0;
        double ssRes = 0.0;
        for (int i = 0; i < n; i++) {
            double pred = predict(X[i], beta);
            ssRes += (y[i] - pred) * (y[i] - pred);
            ssTot += (y[i] - media) * (y[i] - media);
        }
        return ssTot < 1e-12 ? 1.0 : 1.0 - ssRes / ssTot;
    }

    /**
     * Resuelve Ax = b mediante eliminación gaussiana con pivoteo parcial.
     * No modifica A ni b (trabaja sobre copias).
     */
    private static double[] resolverSistemaLineal(double[][] A, double[] b) {
        int n = b.length;

        // Construir la matriz aumentada [A | b]
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aug[i], 0, n);
            aug[i][n] = b[i];
        }

        // Eliminación hacia adelante con pivoteo parcial
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int fila = col + 1; fila < n; fila++) {
                if (Math.abs(aug[fila][col]) > Math.abs(aug[pivot][col])) pivot = fila;
            }
            double[] tmp = aug[col];
            aug[col] = aug[pivot];
            aug[pivot] = tmp;

            if (Math.abs(aug[col][col]) < 1e-12) {
                throw new IllegalStateException("Matriz singular — los features pueden ser colineales");
            }

            for (int fila = col + 1; fila < n; fila++) {
                double factor = aug[fila][col] / aug[col][col];
                for (int j = col; j <= n; j++) {
                    aug[fila][j] -= factor * aug[col][j];
                }
            }
        }

        // Sustitución hacia atrás
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            x[i] = aug[i][n];
            for (int j = i + 1; j < n; j++) x[i] -= aug[i][j] * x[j];
            x[i] /= aug[i][i];
        }
        return x;
    }
}
