import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const MONTH_LABELS = {
  JANUARY: "Enero",
  FEBRUARY: "Febrero",
  MARCH: "Marzo",
  APRIL: "Abril",
  MAY: "Mayo",
  JUNE: "Junio",
  JULY: "Julio",
  AUGUST: "Agosto",
  SEPTEMBER: "Septiembre",
  OCTOBER: "Octubre",
  NOVEMBER: "Noviembre",
  DECEMBER: "Diciembre",
};

const MONTH_ORDER = [
  "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
  "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER",
];

const sortFromCurrentMonth = (predictions) => {
  const currentMonthIndex = new Date().getMonth();
  const ordered = [
    ...MONTH_ORDER.slice(currentMonthIndex),
    ...MONTH_ORDER.slice(0, currentMonthIndex),
  ];
  return [...predictions].sort(
    (a, b) => ordered.indexOf(a.month) - ordered.indexOf(b.month)
  );
};

const r2Reliability = (r2) => {
  if (r2 >= 0.7) return { label: "Fiable", color: "#16a34a" };
  if (r2 >= 0.4) return { label: "Poco fiable", color: "#d97706" };
  return { label: "No fiable", color: "#dc2626" };
};

const PredictionView = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [barcode, setBarcode] = useState("");
  const [prediction, setPrediction] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handlePredict = useCallback(async () => {
    const trimmed = barcode.trim();
    if (!trimmed) return;
    if (!/^\d{8,13}$/.test(trimmed)) {
      setError("El código de barras debe tener entre 8 y 13 dígitos numéricos.");
      return;
    }
    setLoading(true);
    setError("");
    setPrediction(null);
    try {
      const res = await fetch("/api/analytics/predict", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ barcode: trimmed }),
      });
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || "Error al obtener la predicción");
      }
      setPrediction(await res.json());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [barcode, token]);

  const sortedPredictions = prediction ? sortFromCurrentMonth(prediction.predictions) : [];

  const chartData = sortedPredictions.map((p) => ({
    month: MONTH_LABELS[p.month] ?? p.month,
    ventas: Math.round(p.predictedSales * 10) / 10,
  }));

  const reliability = prediction ? r2Reliability(prediction.modelR2) : null;

  return (
    <main className="home-container" role="main">
      <div className="list-container">

        <section
          aria-labelledby="product-heading"
          className="form-container"
          style={{ padding: "16px", marginBottom: "20px" }}
        >
          <h2 id="product-heading" className="section-label" style={{ marginBottom: "14px" }}>
            Introducir código de barras
          </h2>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "12px", alignItems: "flex-end" }}>
            <div className="input-group" style={{ margin: 0, flex: 1 }}>
              <label htmlFor="barcode-input" className="input-label">Código de barras</label>
              <input
                id="barcode-input"
                type="text"
                className="dark-input"
                placeholder="Ej: 8410188020100"
                value={barcode}
                onChange={(e) => {
                  setBarcode(e.target.value);
                  setPrediction(null);
                  setError("");
                }}
                onKeyDown={(e) => e.key === "Enter" && handlePredict()}
              />
            </div>
            <div className="input-group" style={{ margin: 0 }}>
              <label className="input-label" aria-hidden="true" style={{ visibility: "hidden" }}>
                &nbsp;
              </label>
              <button
                className="btn-primary"
                onClick={handlePredict}
                disabled={!barcode.trim() || loading}
                aria-label="Generar predicción para el código de barras introducido"
                style={{ height: "44px", padding: "0 24px", fontSize: "0.85rem", whiteSpace: "nowrap" }}
              >
                {loading ? "Calculando..." : "Predecir"}
              </button>
            </div>
          </div>
        </section>

        {error && (
          <div role="alert" className="message-error" style={{ marginBottom: "16px" }}>
            {error}
          </div>
        )}

        {prediction && (
          <>
            <div
              className="form-container"
              style={{
                padding: "20px 24px",
                marginBottom: "20px",
                display: "flex",
                alignItems: "center",
                gap: "20px",
              }}
            >
              <div>
                <div className="input-label" style={{ marginBottom: "4px" }}>
                  Precisión del modelo (R²)
                </div>
                <div
                  style={{
                    fontSize: "2rem",
                    fontWeight: "800",
                    color: reliability.color,
                    letterSpacing: "-0.5px",
                  }}
                >
                  {prediction.modelR2.toFixed(4)}
                </div>
              </div>
              <div
                style={{
                  padding: "6px 16px",
                  borderRadius: "20px",
                  background: reliability.color + "22",
                  border: `1px solid ${reliability.color}`,
                  color: reliability.color,
                  fontWeight: "700",
                  fontSize: "0.9rem",
                }}
              >
                {reliability.label}
              </div>
            </div>

            <div className="form-container" style={{ padding: "20px", marginBottom: "20px" }}>
              <h3 className="section-label" style={{ marginBottom: "16px", fontSize: "0.85rem" }}>
                Predicción mensual — {barcode}
              </h3>
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chartData} margin={{ top: 8, right: 24, left: 8, bottom: 24 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" />
                  <XAxis
                    dataKey="month"
                    tick={{ fontSize: 11, fill: "var(--text-muted)", angle: -35, textAnchor: "end" }}
                    axisLine={false}
                    tickLine={false}
                    interval={0}
                  />
                  <YAxis
                    tick={{ fontSize: 11, fill: "var(--text-muted)" }}
                    axisLine={false}
                    tickLine={false}
                    tickFormatter={(v) => v.toLocaleString("es-ES")}
                    width={55}
                  />
                  <Tooltip
                    contentStyle={{
                      background: "var(--bg-card)",
                      border: "1px solid var(--border-color)",
                      borderRadius: "6px",
                      fontSize: "0.85rem",
                      color: "var(--text-main)",
                    }}
                    itemStyle={{ color: "var(--text-main)" }}
                    formatter={(value) => [
                      `${value.toLocaleString("es-ES", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} uds.`,
                      "Ventas estimadas",
                    ]}
                  />
                  <Line
                    type="monotone"
                    dataKey="ventas"
                    stroke="var(--primary-blue)"
                    strokeWidth={2}
                    dot={{ r: 4, fill: "var(--primary-blue)", strokeWidth: 0 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>

            <div className="form-container" style={{ padding: "0", overflow: "hidden" }}>
              <div className="table-responsive">
                <table className="dark-table" aria-label="Predicción mensual de ventas">
                  <thead>
                    <tr>
                      <th scope="col">Mes</th>
                      <th scope="col" style={{ textAlign: "right" }}>Ventas estimadas (uds.)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {sortedPredictions.map((p) => (
                      <tr key={p.month}>
                        <td>{MONTH_LABELS[p.month] ?? p.month}</td>
                        <td style={{ textAlign: "right", fontWeight: "600" }}>
                          {p.predictedSales.toLocaleString("es-ES", {
                            minimumFractionDigits: 1,
                            maximumFractionDigits: 1,
                          })}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}

      </div>
    </main>
  );
};

export default PredictionView;
