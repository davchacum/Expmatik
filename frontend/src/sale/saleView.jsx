import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "../components/Modal";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";
import EyeIcon from "../components/EyeIcon";

const Sales = () => {
  const token = localStorage.getItem("accessToken");
  const navigate = useNavigate();
  useRequireTokenRedirect(token, navigate);
  const [sales, setSales] = useState([]);
  const [filters, setFilters] = useState({
    barcode: "",
    machineId: "",
    rowNumber: "",
    columnNumber: "",
    startDate: "",
    endDate: "",
    paymentMethod: "",
    status: "",
  });
  const [machines, setMachines] = useState([]);
  const [selectedMachine, setSelectedMachine] = useState(null);
  useEffect(() => {
    const fetchMachines = async () => {
      try {
        const params = new URLSearchParams({ size: 100 });
        const res = await fetch(`/api/vending-machines?${params}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setMachines(data.content || []);
        }
      } catch (err) {
        console.error(err);
      }
    };
    fetchMachines();
  }, [token]);

  useEffect(() => {
    if (!filters.machineId) {
      setSelectedMachine(null);
      setFilters((f) => ({ ...f, rowNumber: "", columnNumber: "" }));
      return;
    }
    const machine = machines.find((m) => m.id === filters.machineId);
    setSelectedMachine(machine || null);
    setFilters((f) => ({ ...f, rowNumber: "", columnNumber: "" }));
  }, [filters.machineId, machines]);
  const [search, setSearch] = useState({ ...filters });
  const [selectedSale, setSelectedSale] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });
  const [hasSearched, setHasSearched] = useState(false);
  const modalTitleRef = useRef(null);

  useEffect(() => {
    if (selectedSale && modalTitleRef.current) {
      modalTitleRef.current.focus();
    }
  }, [selectedSale]);

  const fetchSales = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", page);
      params.append("size", 8);

      if (search.barcode) params.append("barcode", search.barcode);

      if (search.machineId) {
        const selected = machines.find((m) => m.id === search.machineId);
        if (selected?.name) params.append("machineName", selected.name);
      }

      if (search.rowNumber !== "") {
        params.append("rowNumber", String(search.rowNumber));
      }

      if (search.columnNumber !== "") {
        params.append("columnNumber", String(search.columnNumber));
      }

      if (search.startDate) params.append("startDate", search.startDate);
      if (search.endDate) params.append("endDate", search.endDate);
      if (search.paymentMethod)
        params.append("paymentMethod", search.paymentMethod);
      if (search.status) params.append("status", search.status);

      const response = await fetch(`/api/sales?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setSales(data.content);
        console.log(data.content);
        setTotalPages(data.totalPages);
        if (hasSearched)
          setMessage({ text: "Búsqueda realizada con éxito", type: "success" });
      } else {
        let msg = "Error al buscar ventas.";
        try {
          const errData = await response.json();
          msg = errData.message || msg;
        } catch (err) {
          console.error(err);
        }
        setMessage({ text: msg, type: "error" });
        setSales([]);
        setTotalPages(0);
      }
    } catch (err) {
      setMessage({ text: "Error de conexión: " + err.message, type: "error" });
    } finally {
      setLoading(false);
    }
  }, [page, search, token, hasSearched, machines]);

  useEffect(() => {
    fetchSales();
  }, [fetchSales]);

  const handleApplySearch = () => {
    setPage(0);
    setSearch({ ...filters });
    setMessage({ text: "", type: "" });
    setHasSearched(true);
  };

  const getColumnLabel = (index) => {
    if (index < 0) return "";
    let current = index;
    let label = "";
    while (current >= 0) {
      label = String.fromCharCode((current % 26) + 65) + label;
      current = Math.floor(current / 26) - 1;
    }
    return label;
  };

  const handleExportSalesCsv = async () => {
    setExporting(true);
    setMessage({ text: "", type: "" });

    try {
      const params = new URLSearchParams();

      if (filters.barcode) params.append("barcode", filters.barcode);

      if (filters.machineId) {
        const selected = machines.find((m) => m.id === filters.machineId);
        if (selected?.name) params.append("machineName", selected.name);
      }

      if (filters.rowNumber !== "") {
        params.append("rowNumber", String(filters.rowNumber));
      }

      if (filters.columnNumber !== "") {
        params.append("columnNumber", String(filters.columnNumber));
      }

      if (filters.startDate) params.append("startDate", filters.startDate);
      if (filters.endDate) params.append("endDate", filters.endDate);
      if (filters.paymentMethod)
        params.append("paymentMethod", filters.paymentMethod);
      if (filters.status) params.append("status", filters.status);

      const response = await fetch(
        `/api/sales/csv-export?${params.toString()}`,
        {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (!response.ok) {
        let msg = "No se pudo exportar el CSV.";
        try {
          const errData = await response.json();
          msg = errData.message || msg;
        } catch {
          const text = await response.text();
          if (text) msg = text;
        }
        setMessage({ text: msg, type: "error" });
        return;
      }

      const blob = await response.blob();
      const fileName = "sales.csv";

      const url = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(url);

      setMessage({ text: "CSV exportado correctamente.", type: "success" });
    } catch (err) {
      setMessage({ text: `Error de conexión: ${err.message}`, type: "error" });
    } finally {
      setExporting(false);
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <header className="list-header-actions">
          <div className="input-group">
            <button
              className="action-btn-green"
              onClick={() => navigate("/sales/create/csv")}
              style={{ width: "100%", height: "44px", marginTop: "8px" }}
            >
              Importar ventas CSV
            </button>
          </div>
        </header>

        <div className="divider-dark" style={{ marginBottom: "20px" }} />
        <section aria-labelledby="filter-heading">
          <h2 id="filter-heading" className="section-label">
            Filtrar ventas
          </h2>
          <div
            className="search-row"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "16px",
              marginBottom: "20px",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label htmlFor="f-barcode" className="input-label">
                Código de barras
              </label>
              <input
                id="f-barcode"
                type="text"
                className="dark-input"
                placeholder="Código..."
                value={filters.barcode}
                onChange={(e) =>
                  setFilters({ ...filters, barcode: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="f-machine" className="input-label">
                Máquina
              </label>
              <select
                id="f-machine"
                className="dark-input"
                value={filters.machineId}
                onChange={(e) =>
                  setFilters({ ...filters, machineId: e.target.value })
                }
              >
                <option value="">Todas</option>
                {machines.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="f-row" className="input-label">
                Fila
              </label>
              <select
                id="f-row"
                className="dark-input"
                value={filters.rowNumber}
                onChange={(e) =>
                  setFilters({ ...filters, rowNumber: e.target.value })
                }
                disabled={!selectedMachine}
                aria-disabled={!selectedMachine}
              >
                <option value="">Todas</option>
                {selectedMachine &&
                  Array.from({ length: selectedMachine.rowCount }).map(
                    (_, idx) => (
                      <option key={idx} value={idx + 1}>
                        {getColumnLabel(idx)}
                      </option>
                    ),
                  )}
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="f-col" className="input-label">
                Columna
              </label>
              <select
                id="f-col"
                className="dark-input"
                value={filters.columnNumber}
                onChange={(e) =>
                  setFilters({ ...filters, columnNumber: e.target.value })
                }
                disabled={!selectedMachine}
                aria-disabled={!selectedMachine}
              >
                <option value="">Todas</option>
                {selectedMachine &&
                  Array.from({ length: selectedMachine.columnCount }).map(
                    (_, idx) => (
                      <option key={idx} value={idx + 1}>
                        {idx + 1}
                      </option>
                    ),
                  )}
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="f-start" className="input-label">
                Desde
              </label>
              <input
                id="f-start"
                type="datetime-local"
                className="dark-input"
                value={filters.startDate}
                onChange={(e) =>
                  setFilters({ ...filters, startDate: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="f-end" className="input-label">
                Hasta
              </label>
              <input
                id="f-end"
                type="datetime-local"
                className="dark-input"
                value={filters.endDate}
                onChange={(e) =>
                  setFilters({ ...filters, endDate: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="f-payment" className="input-label">
                Método de pago
              </label>
              <select
                id="f-payment"
                className="dark-input"
                value={filters.paymentMethod}
                onChange={(e) =>
                  setFilters({ ...filters, paymentMethod: e.target.value })
                }
              >
                <option value="">Todos</option>
                <option value="CASH">Efectivo</option>
                <option value="CREDIT_CARD">Tarjeta</option>
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="f-status" className="input-label">
                Estado
              </label>
              <select
                id="f-status"
                className="dark-input"
                value={filters.status}
                onChange={(e) =>
                  setFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="">Todos</option>
                <option value="SUCCESS">Correcta</option>
                <option value="FAILED">Fallida</option>
              </select>
            </div>

            <div style={{ display: "flex", gap: "8px" }}>
              <button
                className="btn-primary"
                onClick={handleApplySearch}
                style={{ height: "44px", flex: 1 }}
                aria-label="Ejecutar búsqueda con filtros aplicados"
              >
                Buscar
              </button>
              <button
                className="btn-secondary"
                type="button"
                style={{ height: "44px", flex: 1 }}
                onClick={() => {
                  setFilters({
                    barcode: "",
                    machineId: "",
                    rowNumber: "",
                    columnNumber: "",
                    startDate: "",
                    endDate: "",
                    paymentMethod: "",
                    status: "",
                  });
                  setMessage({ text: "", type: "" });
                }}
                aria-label="Limpiar todos los filtros de búsqueda"
              >
                Limpiar
              </button>
            </div>

            <div className="input-group">
              <button
                className="action-btn-green"
                onClick={handleExportSalesCsv}
                disabled={exporting}
                style={{ height: "44px", width: "100%" }}
                aria-label="Exportar ventas a CSV"
              >
                {exporting ? "Exportando..." : "Exportar Ventas"}
              </button>
            </div>
          </div>
        </section>
        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "16px" }}
          >
            {message.text}
          </div>
        )}
        <div className="table-responsive">
          <table className="dark-table" aria-label="Resultados de ventas">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Código de barras</th>
                <th scope="col">Producto</th>
                <th scope="col">Máquina</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Ranura
                </th>
                <th scope="col">Pago</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Estado
                </th>
                <th scope="col" style={{ textAlign: "right" }}>
                  Total
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Detalles
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {sales.map((sale) => (
                <tr key={sale.id}>
                  <td>
                    {sale.saleDate
                      ? new Date(sale.saleDate).toLocaleString()
                      : ""}
                  </td>
                  <td>{sale.barcode}</td>
                  <td>{sale.productName}</td>
                  <td>{sale.machineName || "-"}</td>
                  <td style={{ textAlign: "center" }}>
                    {getColumnLabel(sale.rowNumber - 1)}
                    {sale.columnNumber}
                  </td>
                  <td>{sale.paymentMethod}</td>
                  <td style={{ textAlign: "center" }}>
                    <span
                      className={`badge ${sale.status === "SUCCESS" ? "badge-green" : "badge-red"}`}
                      style={{
                        color:
                          sale.status === "SUCCESS"
                            ? "var(--primary-green)"
                            : "var(--primary-red)",
                        fontWeight: "bold",
                      }}
                    >
                      {sale.status === "SUCCESS" ? "EXITOSA" : "FALLIDA"}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {sale.totalAmount?.toFixed(2)}€
                  </td>
                  <td style={{ textAlign: "center" }}>
                    {sale.status === "FAILED" ? (
                      <button
                        className="view-desc-btn"
                        onClick={() => setSelectedSale(sale)}
                        aria-label={`Ver motivo de fallo de la venta ${sale.id}`}
                        title="Ver detalles del error"
                      >
                        <EyeIcon visible={false} />
                      </button>
                    ) : (
                      <span className="sr-only" aria-hidden="true">
                        -
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <nav
          className="list-footer"
          aria-label="Navegación de páginas de ventas"
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
      </div>
      {selectedSale && selectedSale.status === "FAILED" && (
        <Modal onClose={() => setSelectedSale(null)}>
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="modal-sale-error-title"
          >
            <h2
              id="modal-sale-error-title"
              ref={modalTitleRef}
              className="section-label"
              tabIndex="-1"
            >
              Detalles de la Venta Fallida
            </h2>
            <div className="divider-dark" />

            <article className="modal-content-group">
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "12px",
                  marginBottom: "16px",
                }}
              >
                <p>
                  <strong
                    className="input-label"
                    style={{ display: "inline-block", minWidth: "140px" }}
                  >
                    Fecha:
                  </strong>
                  <span style={{ color: "var(--text-main)" }}>
                    {selectedSale.saleDate
                      ? new Date(selectedSale.saleDate).toLocaleString()
                      : "N/A"}
                  </span>
                </p>
                <p>
                  <strong
                    className="input-label"
                    style={{ display: "inline-block", minWidth: "140px" }}
                  >
                    Producto:
                  </strong>
                  <span
                    style={{ color: "var(--text-main)", fontWeight: "600" }}
                  >
                    {selectedSale.productName}
                  </span>
                </p>
                <p>
                  <strong
                    className="input-label"
                    style={{ display: "inline-block", minWidth: "140px" }}
                  >
                    Código de Barras:
                  </strong>
                  <span style={{ color: "var(--text-main)" }}>
                    {selectedSale.barcode}
                  </span>
                </p>
                <p>
                  <strong
                    className="input-label"
                    style={{ display: "inline-block", minWidth: "140px" }}
                  >
                    Máquina:
                  </strong>
                  <span style={{ color: "var(--text-main)" }}>
                    {selectedSale.machineName || "No asignada"}
                  </span>
                </p>
                <p>
                  <strong
                    className="input-label"
                    style={{ display: "inline-block", minWidth: "140px" }}
                  >
                    Ranura:
                  </strong>
                  <span style={{ color: "var(--text-main)" }}>
                    {getColumnLabel(selectedSale.rowNumber - 1)}
                    {selectedSale.columnNumber}
                  </span>
                </p>

                {selectedSale.failureReason && (
                  <p style={{ display: "flex", gap: "12px" }}>
                    <strong
                      className="input-label"
                      style={{
                        minWidth: "160px",
                        lineHeight: "1.2",
                        alignSelf: "center",
                      }}
                    >
                      MOTIVO DEL FALLO:
                    </strong>
                    <span
                      style={{
                        color: "var(--primary-red)",
                        fontWeight: "bold",
                        lineHeight: "1.2",
                        alignSelf: "center",
                      }}
                    >
                      {selectedSale.failureReason}
                    </span>
                  </p>
                )}
              </div>
            </article>

            <div className="list-footer modal-footer">
              <button
                className="btn-primary"
                onClick={() => setSelectedSale(null)}
                autoFocus
                aria-label="Cerrar detalles de venta"
              >
                Cerrar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default Sales;
