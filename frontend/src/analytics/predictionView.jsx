import { useCallback, useEffect, useState } from "react";
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

const r2Reliability = (r2) => {
  if (r2 >= 0.7) return { label: "Fiable", color: "#16a34a" };
  if (r2 >= 0.4) return { label: "Poco fiable", color: "#d97706" };
  return { label: "No fiable", color: "#dc2626" };
};

const PredictionView = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [products, setProducts] = useState([]);
  const [productSearch, setProductSearch] = useState("");
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [prediction, setPrediction] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const params = new URLSearchParams({ size: 50 });
    if (productSearch) params.set("name", productSearch);
    fetch(`/api/products?${params}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => (r.ok ? r.json() : { content: [] }))
      .then((d) => setProducts(d.content ?? []))
      .catch(console.error);
  }, [token, productSearch]);

  const handlePredict = useCallback(async () => {
    if (!selectedProduct) return;
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
        body: JSON.stringify({ barcode: selectedProduct.barcode }),
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
  }, [selectedProduct, token]);

  const chartData =
    prediction?.predictions?.map((p) => ({
      month: MONTH_LABELS[p.month] ?? p.month,
      ventas: Math.round(p.predictedSales * 10) / 10,
    })) ?? [];

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
            Seleccionar producto
          </h2>
          <div style={{ display: "flex", flexWrap: "wrap", gap: "12px", alignItems: "flex-end" }}>
            <div className="input-group" style={{ margin: 0, minWidth: "180px", flex: 1 }}>
              <label htmlFor="p-search" className="input-label">Buscar por nombre</label>
              <input
                id="p-search"
                type="text"
                className="dark-input"
                placeholder="Filtrar por nombre..."
                value={productSearch}
                onChange={(e) => {
                  setProductSearch(e.target.value);
                  setSelectedProduct(null);
                  setPrediction(null);
                  setError("");
                }}
              />
            </div>
            <div className="input-group" style={{ margin: 0, minWidth: "550px", flex: 4 }}>
              <label htmlFor="p-select" className="input-label">Producto</label>
              <select
                id="p-select"
                className="dark-input"
                value={selectedProduct?.id ?? ""}
                onChange={(e) => {
                  const product = products.find((p) => p.id === e.target.value);
                  setSelectedProduct(product ?? null);
                  setPrediction(null);
                  setError("");
                }}
              >
                <option value="">Selecciona un producto</option>
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}{p.isCustom ? " (Personalizado)" : ""} — {p.barcode}
                  </option>
                ))}
              </select>
            </div>
            <div className="input-group" style={{ margin: 0 }}>
              <label className="input-label" aria-hidden="true" style={{ visibility: "hidden" }}>
                &nbsp;
              </label>
              <button
                className="btn-primary"
                onClick={handlePredict}
                disabled={!selectedProduct || loading}
                aria-label="Generar predicción para el producto seleccionado"
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
                Predicción mensual — {selectedProduct.name}
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
                    {prediction.predictions.map((p) => (
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
