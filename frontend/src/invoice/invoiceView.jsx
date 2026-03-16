import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "../components/Modal";
import "../global-list.css";

const EyeIcon = ({ visible }) =>
  visible ? (
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
      <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19C7 19 2.73 15.11 1 12c.74-1.32 1.81-2.87 3.11-4.19M9.53 9.53A3.5 3.5 0 0 1 12 8.5c1.93 0 3.5 1.57 3.5 3.5 0 .47-.09.92-.26 1.33M14.47 14.47A3.5 3.5 0 0 1 12 15.5c-1.93 0-3.5-1.57-3.5-3.5 0-.47.09-.92.26-1.33" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
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
      <path d="M1 12S5 5 12 5s11 7 11 7-4 7-11 7S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
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
  const [search, setSearch] = useState({
    status: "",
    startDate: "",
    endDate: "",
    supplierName: "",
    invoiceNumber: "",
    minPrice: "",
    maxPrice: "",
  });
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
      return;
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
        if (hasSearched) {
          setMessage({ text: "Búsqueda realizada con éxito", type: "success" });
        } else {
          setMessage({ text: "", type: "" });
        }
      } else if (response.status === 401) {
        setMessage({
          text: "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.",
          type: "error",
        });
        setInvoices([]);
        setTotalPages(0);
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
      setMessage({ text: "Error de conexión o inesperado.", type: "error" });
      setInvoices([]);
      setTotalPages(0);
      console.error("Error fetching invoices:", err);
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

  return (
    <main className="home-container">
      <div className="list-container">
        <div className="list-header-actions" style={{ marginBottom: "end" }}>
          <div className="input-group">
            <span className="section-label">Crear factura manualmente</span>
            <button
              className="action-btn-orange"
              onClick={() => navigate("/invoices/create")}
              style={{ width: "100%", height: "44px" }}
            >
              Crear Factura Manual
            </button>
          </div>
          <div className="input-group">
            <label className="section-label">Importar facturas CSV</label>
            <div className="inline-form">
              <button
                className="action-btn-green"
                onClick={() => {}}
                style={{ width: "100%", height: "44px" }}
              >
                Importar CSV
              </button>
            </div>
          </div>
        </div>

        <div className="divider-dark" style={{ marginBottom: "20px" }} />

        <span className="section-label">Filtrar facturas</span>
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
            <span className="input-label">Estado</span>
            <select
              className="dark-input"
              value={filters.status}
              onChange={(e) =>
                setFilters({ ...filters, status: e.target.value })
              }
            >
              <option value="">Todos los estados</option>
              <option value="PENDING">Pendiente</option>
              <option value="RECEIVED">Recibida</option>
              <option value="CANCELED">Cancelada</option>
            </select>
          </div>
          <div className="input-group">
            <span className="input-label">Desde</span>
            <input
              type="date"
              className="dark-input"
              value={filters.startDate}
              onChange={(e) =>
                setFilters({ ...filters, startDate: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <span className="input-label">Hasta</span>
            <input
              type="date"
              className="dark-input"
              value={filters.endDate}
              onChange={(e) =>
                setFilters({ ...filters, endDate: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <span className="input-label">Proveedor</span>
            <input
              type="text"
              className="dark-input"
              placeholder="Nombre del proveedor..."
              value={filters.supplierName}
              onChange={(e) =>
                setFilters({ ...filters, supplierName: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <span className="input-label">Nº Factura</span>
            <input
              type="text"
              className="dark-input"
              placeholder="Referencia..."
              value={filters.invoiceNumber}
              onChange={(e) =>
                setFilters({ ...filters, invoiceNumber: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <span className="input-label">Precio Mínimo</span>
            <input
              type="number"
              className="dark-input"
              step="0.01"
              placeholder="0.00 €"
              min="0"
              value={filters.minPrice}
              onChange={(e) =>
                setFilters({ ...filters, minPrice: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <span className="input-label">Precio Máximo</span>
            <input
              type="number"
              className="dark-input"
              step="0.01"
              placeholder="0.00 €"
              min="0"
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
          >
            Buscar
          </button>
        </div>

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
          <table className="dark-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Nº Factura</th>
                <th>Proveedor</th>
                <th>Estado</th>
                <th style={{ textAlign: "right" }}>Total sin IVA</th>
                <th style={{ textAlign: "center" }}>Detalles</th>
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
                    <span
                      className={`badge ${
                        inv.status === "PENDING"
                          ? "badge-yellow"
                          : inv.status === "RECEIVED"
                            ? "badge-green"
                            : inv.status === "CANCELED"
                              ? "badge-red"
                              : "badge-blue"
                      }`}
                    >
                      {inv.status === "PENDING"
                        ? "pendiente"
                        : inv.status === "RECEIVED"
                          ? "recibido"
                          : inv.status === "CANCELED"
                            ? "cancelado"
                            : inv.status}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }}>
                    {inv.totalAmount.toFixed(2)}€
                  </td>
                  <td style={{ textAlign: "center" }}>
                    <button
                      className="view-desc-btn"
                      onClick={() => setSelectedInvoice(inv)}
                    >
                      <EyeIcon visible={false} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="list-footer">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
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
          >
            Siguiente
          </button>
        </div>
      </div>

      {selectedInvoice && (
        <Modal onClose={() => setSelectedInvoice(null)}>
          <h2 ref={modalTitleRef} className="section-label" tabIndex="-1">
            Detalles Factura: {selectedInvoice.invoiceNumber}
          </h2>
          <div className="divider-dark" />
          <div className="modal-content-group">
            <p>
              <strong>Proveedor:</strong> {selectedInvoice.supplierName}
            </p>
            <p>
              <strong>Fecha:</strong> {selectedInvoice.invoiceDate}
            </p>
            <p>
              <strong>Estado:</strong>{" "}
              <span
                className={`badge ${
                  selectedInvoice.status === "PENDING"
                    ? "badge-yellow"
                    : selectedInvoice.status === "RECEIVED"
                      ? "badge-green"
                      : selectedInvoice.status === "CANCELED"
                        ? "badge-red"
                        : "badge-blue"
                }`}
              >
                {selectedInvoice.status === "PENDING"
                  ? "pendiente"
                  : selectedInvoice.status === "RECEIVED"
                    ? "recibido"
                    : selectedInvoice.status === "CANCELED"
                      ? "cancelado"
                      : selectedInvoice.status}
              </span>
            </p>
            <p>
              <strong>Total sin IVA:</strong> {selectedInvoice.totalAmount}€
            </p>
          </div>

          <span className="section-label">Lotes incluidos</span>

          <div className="table-responsive modal-table-wrapper">
            <table className="dark-table modal-table">
              <thead>
                <tr>
                  <th scope="col">Producto</th>
                  <th scope="col">Marca</th>
                  <th scope="col">Código de Barras</th>
                  <th scope="col">Fecha de Caducidad</th>
                  <th scope="col">Unidades</th>
                  <th scope="col">Precio Unitario</th>
                  <th scope="col">Precio Total sin IVA</th>
                </tr>
              </thead>

              <tbody>
                {selectedInvoice.batches.map((b, idx) => (
                  <tr key={idx}>
                    <td>{b.productName}</td>
                    <td>{b.brand}</td>
                    <td>{b.barcode}</td>
                    <td>{b.expirationDate ? b.expirationDate : "N/A"}</td>
                    <td>{b.quantity}</td>
                    <td>{b.unitPrice}€</td>
                    <td>{b.totalPrice}€</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="list-footer modal-footer">
            <button
              className="btn-primary"
              onClick={() => setSelectedInvoice(null)}
            >
              Cerrar
            </button>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default Invoices;
