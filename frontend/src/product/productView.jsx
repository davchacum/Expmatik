import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
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

const Products = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");

  // Referencia para el foco del modal
  const modalTitleRef = useRef(null);

  const [products, setProducts] = useState([]);
  const [filters, setFilters] = useState({ name: "", brand: "", barcode: "" });
  const [search, setSearch] = useState({ name: "", brand: "", barcode: "" });
  const [selectedProduct, setSelectedProduct] = useState(null);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [offBarcode, setOffBarcode] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });

  // FOCO DINÁMICO: Solo al abrir la descripción
  useEffect(() => {
    if (selectedProduct && modalTitleRef.current) {
      modalTitleRef.current.focus();
    }
  }, [selectedProduct]);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", page);
      params.append("size", 8);
      if (search.name) params.append("name", search.name);
      if (search.brand) params.append("brand", search.brand);
      if (search.barcode && search.barcode.trim() !== "") {
        params.append("barcode", search.barcode.trim());
      }

      const response = await fetch(`/api/products?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setProducts(data.content);
        setTotalPages(data.totalPages);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, search, token]);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const handleApplySearch = () => {
    setPage(0);
    setSearch({ ...filters });
  };

  const handleAddOFF = async (e) => {
    e.preventDefault();

    if (!offBarcode || (offBarcode.length !== 8 && offBarcode.length !== 13)) {
      setMessage({
        text: "El código de barras debe tener 8 o 13 dígitos",
        type: "error",
      });
      return;
    }

    try {
      const response = await fetch(
        `/api/products/non-custom?barcode=${offBarcode}`,
        {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.ok) {
        setMessage({ text: "Producto importado con éxito", type: "success" });
        setOffBarcode("");
        fetchProducts();
      } else if (response.status === 400) {
        setMessage({
          text: "Formato de código de barras inválido (8 o 13 dígitos)",
          type: "error",
        });
      }
    } catch (err) {
      setMessage({
        text: "Error de conexión",
        type: "error",
        err: err.toString(),
      });
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <div className="list-header-actions" style={{ alignItems: "end" }}>
          <div className="input-group">
            <label htmlFor="off-barcode" className="section-label">
              Importar desde Open Food Facts
            </label>
            <form onSubmit={handleAddOFF} className="inline-form">
              <input
                id="off-barcode"
                className="dark-input"
                placeholder="Código de barras..."
                value={offBarcode}
                onChange={(e) => setOffBarcode(e.target.value)}
                aria-required="true"
              />
              <button type="submit" className="btn-primary">
                Importar
              </button>
            </form>
          </div>

          <div className="input-group">
            <span className="section-label">Gestión Manual</span>
            <button
              className="btn-secondary"
              onClick={() => navigate("/products/create-custom")}
              style={{ width: "100%" }}
              aria-label="Ir a crear nuevo producto personalizado"
            >
              Nuevo Producto Personalizado
            </button>
          </div>
        </div>

        {message.text && (
          <div
            className={
              message.type === "error" ? "login-error" : "success-message"
            }
            role="alert"
            aria-live="assertive"
          >
            {message.text}
          </div>
        )}

        <div className="divider-dark" aria-hidden="true" />

        <section className="search-section" aria-labelledby="filter-heading">
          <span id="filter-heading" className="section-label">
            Filtrar productos
          </span>
          <div
            className="search-row"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "15px",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label htmlFor="filter-name" className="input-label">
                Nombre
              </label>
              <input
                id="filter-name"
                className="dark-input"
                placeholder="Buscar nombre..."
                value={filters.name}
                onChange={(e) =>
                  setFilters({ ...filters, name: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="filter-brand" className="input-label">
                Marca
              </label>
              <input
                id="filter-brand"
                className="dark-input"
                placeholder="Buscar marca..."
                value={filters.brand}
                onChange={(e) =>
                  setFilters({ ...filters, brand: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="filter-barcode" className="input-label">
                Código de Barras
              </label>
              <input
                id="filter-barcode"
                className="dark-input"
                placeholder="Referencia..."
                value={filters.barcode}
                onChange={(e) =>
                  setFilters({ ...filters, barcode: e.target.value })
                }
              />
            </div>
            <button className="btn-primary" onClick={handleApplySearch}>
              Buscar
            </button>
          </div>
        </section>

        <div className="table-responsive">
          <table className="dark-table">
            <thead>
              <tr>
                <th scope="col">Producto</th>
                <th scope="col">Marca</th>
                <th scope="col">Código de barras</th>
                <th scope="col">Origen</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Perecedero
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Info
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {products.map((p) => (
                <tr key={p.id}>
                  <td>
                    <strong>{p.name}</strong>
                  </td>
                  <td>{p.brand}</td>
                  <td className="barcode-text">{p.barcode}</td>
                  <td>
                    <span
                      className={`badge ${p.isCustom ? "badge-orange" : "badge-blue"}`}
                    >
                      {p.isCustom ? "Propio" : "Catálogo"}
                    </span>
                  </td>
                  <td style={{ textAlign: "center" }}>
                    <span
                      className={`badge-perishable ${p.isPerishable ? "yes" : "no"}`}
                    >
                      {p.isPerishable ? "Sí" : "No"}
                    </span>
                  </td>
                  <td style={{ textAlign: "center" }}>
                    {p.description && (
                      <button
                        type="button"
                        className="view-desc-btn"
                        onClick={() => setSelectedProduct(p)}
                        aria-label={`Ver descripción de ${p.name}`}
                      >
                        <EyeIcon visible={false} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <nav className="list-footer" aria-label="Paginación de productos">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            aria-label="Página anterior"
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
            aria-label="Página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>

      {selectedProduct && (
        <div
          className="modal-overlay"
          onClick={() => setSelectedProduct(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <h2
              ref={modalTitleRef}
              className="section-title"
              style={{ fontSize: "1.2rem" }}
              tabIndex="-1"
            >
              Detalles del Producto
            </h2>
            <div className="divider-dark" aria-hidden="true" />

            <div className="input-group" style={{ marginBottom: "15px" }}>
              <span className="section-label">Producto y Marca</span>
              <p style={{ color: "var(--text-main)" }}>
                {selectedProduct.name} — {selectedProduct.brand}
              </p>
            </div>

            <div className="input-group">
              <span className="section-label">Descripción Completa</span>
              <p className="description-popup-text">
                {selectedProduct.description}
              </p>
            </div>

            <div
              className="list-footer"
              style={{ justifyContent: "flex-end", marginTop: "20px" }}
            >
              <button
                className="btn-primary"
                onClick={() => setSelectedProduct(null)}
                aria-label="Cerrar detalles"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};

export default Products;
