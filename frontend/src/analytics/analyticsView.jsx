import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const TYPES = [
  { key: "EXPENSES", label: "Gastos" },
  { key: "INCOME", label: "Ingresos por Máquina" },
  { key: "INCOME_PRODUCT", label: "Ingresos por Producto" },
  { key: "PROFIT", label: "Beneficio" },
];

const TOTAL_LABEL = {
  EXPENSES: "Total Gastos",
  INCOME: "Total Ingresos",
  INCOME_PRODUCT: "Total Ingresos",
  PROFIT: "Beneficio Neto",
};

const LIST_HEADER = {
  EXPENSES: ["Producto", "Total Gastado", "Unidades"],
  INCOME: ["Máquina", "Total Ingresado", "Ventas"],
  INCOME_PRODUCT: ["Producto", "Total Ingresado", "Ventas"],
  PROFIT: ["Producto", "Beneficio"],
};

const CHART_TITLE = {
  EXPENSES: "Top 5 productos por gasto",
  INCOME: "Top 5 máquinas por ingreso",
  INCOME_PRODUCT: "Top 5 productos por ingreso",
  PROFIT: "Top 5 productos por beneficio",
};

const HAS_COUNT = { EXPENSES: true, INCOME: true, INCOME_PRODUCT: true, PROFIT: false };
const HAS_MACHINE = { EXPENSES: false, INCOME: true, INCOME_PRODUCT: true, PROFIT: true };
const HAS_PRODUCT_FILTERS = { EXPENSES: true, INCOME: false, INCOME_PRODUCT: true, PROFIT: true };
const EMPTY_FILTERS = { startDate: "", endDate: "", machineId: "", productName: "", brand: "" };

const AnalyticsView = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [selectedType, setSelectedType] = useState("EXPENSES");
  const [machines, setMachines] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [appliedFilters, setAppliedFilters] = useState(EMPTY_FILTERS);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [totalPages, setTotalPages] = useState(0);
  const [chartData, setChartData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetch("/api/vending-machines?size=200", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => (r.ok ? r.json() : { content: [] }))
      .then((d) => setMachines(d.content ?? d))
      .catch(console.error);
  }, [token]);

  const fetchAnalytics = useCallback(async () => {
    if (!selectedType) return;
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ type: selectedType, page, size: 10 });
      if (appliedFilters.startDate) params.set("startDate", appliedFilters.startDate);
      if (appliedFilters.endDate) params.set("endDate", appliedFilters.endDate);
      if (HAS_MACHINE[selectedType] && appliedFilters.machineId) params.set("machineId", appliedFilters.machineId);
      if (HAS_PRODUCT_FILTERS[selectedType] && appliedFilters.productName) params.set("productName", appliedFilters.productName);
      if (HAS_PRODUCT_FILTERS[selectedType] && appliedFilters.brand) params.set("brand", appliedFilters.brand);

      const res = await fetch(`/api/analytics?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error("Error cargando analíticas");
      const json = await res.json();
      setData(json);
      setTotalPages(json.analyticsItem?.totalPages ?? 0);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [selectedType, appliedFilters, page, token]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  const fetchChartData = useCallback(async () => {
    if (!selectedType) return;
    try {
      const params = new URLSearchParams({ type: selectedType, page: 0, size: 5 });
      if (appliedFilters.startDate) params.set("startDate", appliedFilters.startDate);
      if (appliedFilters.endDate) params.set("endDate", appliedFilters.endDate);
      if (HAS_MACHINE[selectedType] && appliedFilters.machineId) params.set("machineId", appliedFilters.machineId);
      if (HAS_PRODUCT_FILTERS[selectedType] && appliedFilters.productName) params.set("productName", appliedFilters.productName);
      if (HAS_PRODUCT_FILTERS[selectedType] && appliedFilters.brand) params.set("brand", appliedFilters.brand);
      const res = await fetch(`/api/analytics?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!res.ok) return;
      const json = await res.json();
      setChartData(
        (json.analyticsItem?.content ?? []).map((item) => ({
          label: item.label?.length > 14 ? item.label.substring(0, 14) + "…" : item.label,
          fullLabel: item.label,
          amount: parseFloat(item.amount) || 0,
        })),
      );
    } catch (err) {
      console.error(err);
    }
  }, [selectedType, appliedFilters, token]);

  useEffect(() => {
    fetchChartData();
  }, [fetchChartData]);

  const handleTypeSelect = (type) => {
    setSelectedType(type);
    setPage(0);
    setData(null);
    setChartData([]);
    setFilters(EMPTY_FILTERS);
    setAppliedFilters(EMPTY_FILTERS);
  };

  const handleFilterChange = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleSearch = () => {
    setPage(0);
    setAppliedFilters(filters);
  };

  const handleClearFilters = () => {
    setFilters(EMPTY_FILTERS);
    setAppliedFilters(EMPTY_FILTERS);
    setPage(0);
  };

  const formatAmount = (amount) => {
    if (amount == null) return "—";
    const num = parseFloat(amount);
    const formatted = Math.abs(num).toLocaleString("es-ES", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return num < 0 ? `-${formatted} €` : `${formatted} €`;
  };

  const amountColor = (amount) => {
    if (selectedType !== "PROFIT") return "inherit";
    const num = parseFloat(amount);
    if (num > 0) return "#16a34a";
    if (num < 0) return "#dc2626";
    return "inherit";
  };

  const totalColor = (amount) => {
    if (selectedType !== "PROFIT") return "var(--primary-blue)";
    const num = parseFloat(amount);
    if (num > 0) return "#16a34a";
    if (num < 0) return "#dc2626";
    return "inherit";
  };

  const headers = LIST_HEADER[selectedType] ?? [];
  const items = data?.analyticsItem?.content ?? [];

  return (
    <main className="home-container" role="main">
      <div className="list-container">

        <header className="form-container" style={{ padding: "16px", marginBottom: "20px" }}>
          <h2 className="section-label" style={{ marginBottom: "14px" }}>Analíticas</h2>
          <div style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
            {TYPES.map((t) => (
              <button
                key={t.key}
                onClick={() => handleTypeSelect(t.key)}
                className={selectedType === t.key ? "action-btn-green" : "action-btn-blue"}
                disabled={selectedType === t.key}
                aria-pressed={selectedType === t.key}
                style={{ padding: "10px 20px", fontSize: "0.9rem", fontWeight: "600" }}
              >
                {t.label}
              </button>
            ))}
          </div>
        </header>

        <section
          aria-labelledby="filter-heading"
          className="form-container"
          style={{
            padding: "16px",
            marginBottom: "20px",
            display: "flex",
            flexWrap: "wrap",
            gap: "16px",
            alignItems: "flex-end",
          }}
        >
          <h2 id="filter-heading" className="sr-only">Filtros</h2>
          <div className="input-group" style={{ margin: 0, minWidth: "140px" }}>
            <label htmlFor="f-start" className="input-label">Fecha inicio</label>
            <input
              id="f-start"
              type="date"
              className="dark-input"
              value={filters.startDate}
              onChange={(e) => handleFilterChange("startDate", e.target.value)}
            />
          </div>
          <div className="input-group" style={{ margin: 0, minWidth: "140px" }}>
            <label htmlFor="f-end" className="input-label">Fecha fin</label>
            <input
              id="f-end"
              type="date"
              className="dark-input"
              value={filters.endDate}
              onChange={(e) => handleFilterChange("endDate", e.target.value)}
            />
          </div>
          {HAS_MACHINE[selectedType] && (
            <div className="input-group" style={{ margin: 0, minWidth: "180px" }}>
              <label htmlFor="f-machine" className="input-label">Máquina</label>
              <select
                id="f-machine"
                className="dark-input"
                value={filters.machineId}
                onChange={(e) => handleFilterChange("machineId", e.target.value)}
              >
                <option value="">Todas</option>
                {machines.map((m) => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </div>
          )}
          {HAS_PRODUCT_FILTERS[selectedType] && (
            <>
              <div className="input-group" style={{ margin: 0, minWidth: "150px" }}>
                <label htmlFor="f-product" className="input-label">Producto</label>
                <input
                  id="f-product"
                  type="text"
                  className="dark-input"
                  placeholder="Nombre del producto"
                  value={filters.productName}
                  onChange={(e) => handleFilterChange("productName", e.target.value)}
                />
              </div>
              <div className="input-group" style={{ margin: 0, minWidth: "130px" }}>
                <label htmlFor="f-brand" className="input-label">Marca</label>
                <input
                  id="f-brand"
                  type="text"
                  className="dark-input"
                  placeholder="Marca"
                  value={filters.brand}
                  onChange={(e) => handleFilterChange("brand", e.target.value)}
                />
              </div>
            </>
          )}
          <div style={{ display: "flex", gap: "8px", alignItems: "flex-end", marginLeft: "auto" }}>
            <button
              className="btn-primary"
              onClick={handleSearch}
              aria-label="Ejecutar búsqueda con filtros aplicados"
              style={{ padding: "8px 20px", fontSize: "0.85rem", whiteSpace: "nowrap" }}
            >
              Buscar
            </button>
            <button
              className="btn-secondary"
              onClick={handleClearFilters}
              aria-label="Limpiar todos los filtros"
              style={{ padding: "8px 16px", fontSize: "0.85rem", whiteSpace: "nowrap" }}
            >
              Limpiar filtros
            </button>
          </div>
        </section>

        {data && (
          <div
            className="form-container"
            style={{
              padding: "20px 24px",
              marginBottom: "20px",
              display: "flex",
              alignItems: "center",
              gap: "12px",
            }}
          >
            <div>
              <div className="input-label" style={{ marginBottom: "4px" }}>
                {TOTAL_LABEL[selectedType]}
              </div>
              <div
                style={{
                  fontSize: "2rem",
                  fontWeight: "800",
                  color: totalColor(data.total),
                  letterSpacing: "-0.5px",
                }}
              >
                {formatAmount(data.total)}
              </div>
            </div>
          </div>
        )}

        {chartData.length > 0 && (
          <div className="form-container" style={{ padding: "20px", marginBottom: "20px" }}>
            <h3 className="section-label" style={{ marginBottom: "16px", fontSize: "0.85rem" }}>
              {CHART_TITLE[selectedType]}
            </h3>
            <ResponsiveContainer width="100%" height={220}>
              <BarChart data={chartData} margin={{ top: 4, right: 16, left: 16, bottom: 4 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border-color)" vertical={false} />
                <XAxis
                  dataKey="label"
                  tick={{ fontSize: 12, fill: "var(--text-muted)" }}
                  axisLine={false}
                  tickLine={false}
                />
                <YAxis
                  tick={{ fontSize: 11, fill: "var(--text-muted)" }}
                  axisLine={false}
                  tickLine={false}
                  tickFormatter={(v) => `${v.toLocaleString("es-ES")} €`}
                  width={90}
                />
                <Tooltip
                  cursor={{ fill: "rgba(255,255,255,0.05)" }}
                  contentStyle={{
                    background: "var(--bg-card)",
                    border: "1px solid var(--border-color)",
                    borderRadius: "6px",
                    fontSize: "0.85rem",
                    color: "var(--text-main)",
                  }}
                  itemStyle={{ color: "var(--text-main)" }}
                  formatter={(value, _name, props) => [
                    `${value.toLocaleString("es-ES", { minimumFractionDigits: 2 })} €`,
                    props.payload.fullLabel,
                  ]}
                  labelFormatter={() => ""}
                />
                <Bar dataKey="amount" radius={[4, 4, 0, 0]}>
                  {chartData.map((entry, i) => (
                    <Cell
                      key={i}
                      fill={
                        selectedType === "PROFIT"
                          ? entry.amount >= 0
                            ? "#16a34a"
                            : "#dc2626"
                          : "var(--primary-blue)"
                      }
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}

        {error && (
          <div role="alert" className="message-error" style={{ marginBottom: "16px" }}>
            {error}
          </div>
        )}

        <div className="form-container" style={{ padding: "0", overflow: "hidden" }}>
          {loading ? (
            <div style={{ padding: "24px", textAlign: "center", color: "var(--text-muted)" }}>
              Cargando...
            </div>
          ) : !data || items.length === 0 ? (
            <div style={{ padding: "24px", textAlign: "center", color: "var(--text-muted)" }}>
              Sin resultados
            </div>
          ) : (
            <>
              <div className="table-responsive">
                <table className="dark-table" aria-label="Resultados de analíticas">
                  <thead>
                    <tr>
                      {headers.map((h) => (
                        <th key={h} scope="col" style={{ textAlign: h === headers[0] ? "left" : "right" }}>
                          {h}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody style={{ opacity: loading ? 0.5 : 1 }}>
                    {items.map((item, i) => (
                      <tr key={i}>
                        <td>{item.label ?? "—"}</td>
                        <td style={{ textAlign: "right", fontWeight: "700", color: amountColor(item.amount) }}>
                          {formatAmount(item.amount)}
                        </td>
                        {HAS_COUNT[selectedType] && (
                          <td style={{ textAlign: "right", color: "var(--text-muted)" }}>
                            {item.count ?? 0}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {totalPages > 1 && (
                <nav
                  className="list-footer"
                  aria-label="Navegación de páginas de analíticas"
                >
                  <button
                    className="page-btn"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                    aria-label="Ir a la página anterior"
                  >
                    Anterior
                  </button>
                  <span className="page-info" aria-live="polite">
                    Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong>
                  </span>
                  <button
                    className="page-btn"
                    disabled={page >= totalPages - 1}
                    onClick={() => setPage((p) => p + 1)}
                    aria-label="Ir a la página siguiente"
                  >
                    Siguiente
                  </button>
                </nav>
              )}
            </>
          )}
        </div>

      </div>
    </main>
  );
};

export default AnalyticsView;
