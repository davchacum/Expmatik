import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "../components/Modal";
import "../global-list.css";

const EyeIcon = ({ visible }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {visible ? (
      <>
        <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19C7 19 2.73 15.11 1 12c.74-1.32 1.81-2.87 3.11-4.19M9.53 9.53A3.5 3.5 0 0 1 12 8.5c1.93 0 3.5 1.57 3.5 3.5 0 .47-.09.92-.26 1.33M14.47 14.47A3.5 3.5 0 0 1 12 15.5c-1.93 0-3.5-1.57-3.5-3.5 0-.47.09-.92.26-1.33" />
        <line x1="1" y1="1" x2="23" y2="23" />
      </>
    ) : (
      <>
        <path d="M1 12S5 5 12 5s11 7 11 7-4 7-11 7S1 12 1 12z" />
        <circle cx="12" cy="12" r="3" />
      </>
    )}
  </svg>
);

const Invoices = () => {
  const token = localStorage.getItem("accessToken");
  const navigate = useNavigate();
  const [invoices, setInvoices] = useState([]);
  const [filters, setFilters] = useState({
    status: "",
    startDate: "",
    endDate: "",
    supplierName: "",
    invoiceNumber: "",
    minPrice: "",
    maxPrice: "",
  });
  const [search, setSearch] = useState({ ...filters });
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });
  const [hasSearched, setHasSearched] = useState(false);
  const modalTitleRef = useRef(null);

  useEffect(() => {
    if (selectedInvoice && modalTitleRef.current) {
      modalTitleRef.current.focus();
    }
  }, [selectedInvoice]);

  useEffect(() => {
    if (!token) {
      navigate("/login", { replace: true });
    }
  }, [token, navigate]);

  const fetchInvoices = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", page);
      params.append("size", 8);
      Object.keys(search).forEach((key) => {
        if (search[key]) params.append(key, search[key]);
      });

      const response = await fetch(
        `/api/invoices/search?${params.toString()}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.ok) {
        const data = await response.json();
        setInvoices(data.content);
        setTotalPages(data.totalPages);
        if (hasSearched)
          setMessage({ text: "Búsqueda realizada con éxito", type: "success" });
      } else {
        let msg = "Error al buscar facturas.";
        try {
          const errData = await response.json();
          msg = errData.message || msg;
        } catch (err) {
          console.error(err);
        }
        setMessage({ text: msg, type: "error" });
        setInvoices([]);
        setTotalPages(0);
      }
    } catch (err) {
      setMessage({ text: "Error de conexión: " + err.message, type: "error" });
    } finally {
      setLoading(false);
    }
  }, [page, search, token, hasSearched]);

  useEffect(() => {
    fetchInvoices();
  }, [fetchInvoices]);

  const handleApplySearch = () => {
    setPage(0);
    setSearch({ ...filters });
    setMessage({ text: "", type: "" });
    setHasSearched(true);
  };

  const handleStatusChange = async (invoiceId, newStatus) => {
    if (newStatus === "PENDING") return;
    if (
      !window.confirm(`¿Seguro que quieres cambiar el estado a ${newStatus}?`)
    )
      return;

    try {
      const response = await fetch(
        `/api/invoices/${invoiceId}/status?status=${newStatus}`,
        {
          method: "PATCH",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.ok) {
        setMessage({
          text: "Estado actualizado correctamente",
          type: "success",
        });
        fetchInvoices();
      } else {
        setMessage({ text: "Error al actualizar estado", type: "error" });
      }
    } catch (err) {
      setMessage({ text: "Error de red: " + err.message, type: "error" });
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <div className="list-header-actions">
          <div className="input-group">
            <h2 className="section-label">Crear factura manualmente</h2>
            <button
              className="action-btn-orange"
              onClick={() => navigate("/invoices/create")}
              style={{ width: "100%", height: "44px" }}
            >
              Crear Factura Manual
            </button>
          </div>
          <div className="input-group">
            <h2 className="section-label">Importar facturas CSV</h2>
            <button
              className="action-btn-green"
              onClick={() => navigate("/invoices/create/csv")}
              style={{ width: "100%", height: "44px" }}
            >
              Importar CSV
            </button>
          </div>
        </div>

        <div className="divider-dark" style={{ marginBottom: "20px" }} />

        <section aria-labelledby="filter-heading">
          <h2 id="filter-heading" className="section-label">
            Filtrar facturas
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
                <option value="PENDING">Pendiente</option>
                <option value="RECEIVED">Recibida</option>
                <option value="CANCELED">Cancelada</option>
              </select>
            </div>
            <div className="input-group">
              <label htmlFor="f-start" className="input-label">
                Desde
              </label>
              <input
                id="f-start"
                type="date"
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
                type="date"
                className="dark-input"
                value={filters.endDate}
                onChange={(e) =>
                  setFilters({ ...filters, endDate: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="f-supplier" className="input-label">
                Proveedor
              </label>
              <input
                id="f-supplier"
                type="text"
                className="dark-input"
                placeholder="Nombre..."
                value={filters.supplierName}
                onChange={(e) =>
                  setFilters({ ...filters, supplierName: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="f-num" className="input-label">
                Nº Factura
              </label>
              <input
                id="f-num"
                type="text"
                className="dark-input"
                placeholder="Ref..."
                value={filters.invoiceNumber}
                onChange={(e) =>
                  setFilters({ ...filters, invoiceNumber: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="f-min" className="input-label">
                Mínimo (€)
              </label>
              <input
                id="f-min"
                type="number"
                step="0.01"
                className="dark-input"
                value={filters.minPrice}
                onChange={(e) =>
                  setFilters({ ...filters, minPrice: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="f-max" className="input-label">
                Máximo (€)
              </label>
              <input
                id="f-max"
                type="number"
                step="0.01"
                className="dark-input"
                value={filters.maxPrice}
                onChange={(e) =>
                  setFilters({ ...filters, maxPrice: e.target.value })
                }
              />
            </div>
            <button
              className="btn-primary"
              onClick={handleApplySearch}
              style={{ height: "44px" }}
              aria-label="Buscar facturas"
            >
              Buscar
            </button>
          </div>
        </section>
        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="polite"
            style={{ marginBottom: "16px" }}
          >
            {message.text}
          </div>
        )}
        <div className="table-responsive">
          <table className="dark-table" aria-label="Listado de facturas">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Nº Factura</th>
                <th scope="col">Proveedor</th>
                <th scope="col">Estado</th>
                <th scope="col" style={{ textAlign: "right" }}>
                  Total sin IVA
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Detalles
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {invoices.map((inv) => (
                <tr key={inv.id}>
                  <td>{new Date(inv.invoiceDate).toLocaleDateString()}</td>
                  <td>
                    <strong>{inv.invoiceNumber}</strong>
                  </td>
                  <td>{inv.supplierName}</td>
                  <td>
                    {inv.status === "PENDING" ? (
                      <select
                        aria-label={`Cambiar estado factura ${inv.invoiceNumber}`}
                        className="badge badge-yellow"
                        value={inv.status}
                        onChange={(e) =>
                          handleStatusChange(inv.id, e.target.value)
                        }
                      >
                        <option value="PENDING" className="badge">
                          PENDIENTE
                        </option>
                        <option value="RECEIVED" className="badge badge-green">
                          RECIBIDA
                        </option>
                        <option value="CANCELED" className="badge badge-red">
                          CANCELADA
                        </option>
                      </select>
                    ) : (
                      <span
                        className={`badge ${inv.status === "RECEIVED" ? "badge-green" : inv.status === "CANCELED" ? "badge-red" : "badge-blue"}`}
                      >
                        {inv.status === "PENDING"
                          ? "PENDIENTE"
                          : inv.status === "RECEIVED"
                            ? "RECIBIDA"
                            : inv.status === "CANCELED"
                              ? "CANCELADA"
                              : inv.status.toLowerCase()}
                      </span>
                    )}
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {inv.totalAmount.toFixed(2)}€
                  </td>
                  <td style={{ textAlign: "center" }}>
                    <button
                      className="view-desc-btn"
                      onClick={() => setSelectedInvoice(inv)}
                      aria-label={`Ver detalles factura ${inv.invoiceNumber}`}
                      title="Ver detalles"
                    >
                      <EyeIcon visible={false} />
                    </button>
                  </td>
                  <td style={{ textAlign: "center" }}>
                    {inv.status === "PENDING" && (
                      <button
                        className="action-btn-blue"
                        onClick={() => navigate(`/invoices/${inv.id}/edit`)}
                        aria-label={`Editar factura ${inv.invoiceNumber}`}
                      >
                        Editar
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <nav className="list-footer" aria-label="Navegación de resultados">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            aria-label="Página anterior"
          >
            Anterior
          </button>
          <span className="page-info">
            Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong>
          </span>
          <button
            className="page-btn"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>
      {selectedInvoice && (
        <Modal onClose={() => setSelectedInvoice(null)}>
          <h2 ref={modalTitleRef} className="section-label" tabIndex="-1">
            Detalles Factura: {selectedInvoice.invoiceNumber}
          </h2>
          <div className="divider-dark" />
          <article className="modal-content-group">
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "8px",
                marginBottom: "16px",
              }}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "12px" }}
              >
                <span className="input-label" style={{ minWidth: "120px" }}>
                  Proveedor:
                </span>
                <span>{selectedInvoice.supplierName}</span>
              </div>
              <div
                style={{ display: "flex", alignItems: "center", gap: "12px" }}
              >
                <span className="input-label" style={{ minWidth: "120px" }}>
                  Fecha:
                </span>
                <span>{selectedInvoice.invoiceDate}</span>
              </div>
              <div
                style={{ display: "flex", alignItems: "center", gap: "12px" }}
              >
                <span className="input-label" style={{ minWidth: "120px" }}>
                  Estado:
                </span>
                <span>
                  <span
                    className={`badge ${selectedInvoice.status === "PENDING" ? "badge-yellow" : selectedInvoice.status === "RECEIVED" ? "badge-green" : selectedInvoice.status === "CANCELED" ? "badge-red" : "badge-blue"}`}
                  >
                    {selectedInvoice.status === "PENDING"
                      ? "PENDIENTE"
                      : selectedInvoice.status === "RECEIVED"
                        ? "RECIBIDA"
                        : selectedInvoice.status === "CANCELED"
                          ? "CANCELADA"
                          : selectedInvoice.status.toLowerCase()}
                  </span>
                </span>
              </div>
              <div
                style={{ display: "flex", alignItems: "center", gap: "12px" }}
              >
                <span className="input-label" style={{ minWidth: "120px" }}>
                  Total sin IVA:
                </span>
                <span>
                  <strong>{selectedInvoice.totalAmount.toFixed(2)}€</strong>
                </span>
              </div>
            </div>
          </article>

          <h3 className="section-label" id="batches-title">
            Lotes incluidos
          </h3>
          <div className="table-responsive modal-table-wrapper">
            <table
              className="dark-table modal-table"
              aria-labelledby="batches-title"
            >
              <thead>
                <tr>
                  <th scope="col">Producto</th>
                  <th scope="col">Marca</th>
                  <th scope="col">Código</th>
                  <th scope="col">Caducidad</th>
                  <th scope="col">Unidades</th>
                  <th scope="col">P. Unitario</th>
                  <th scope="col">Subtotal</th>
                </tr>
              </thead>
              <tbody>
                {selectedInvoice.batches.map((b, idx) => (
                  <tr key={idx}>
                    <td>
                      <strong>{b.productName}</strong>
                    </td>
                    <td>{b.brand}</td>
                    <td className="barcode-text">{b.barcode}</td>
                    <td>{b.expirationDate || "N/A"}</td>
                    <td>{b.quantity}</td>
                    <td>{b.unitPrice.toFixed(2)}€</td>
                    <td>{b.totalPrice.toFixed(2)}€</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="list-footer modal-footer">
            <button
              className="btn-primary"
              onClick={() => setSelectedInvoice(null)}
              autoFocus
            >
              Cerrar Detalles
            </button>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default Invoices;
