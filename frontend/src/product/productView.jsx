import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "./products.css";

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

  const [products, setProducts] = useState([]);
  const [filters, setFilters] = useState({ name: "", brand: "", barcode: "" });
  const [search, setSearch] = useState({ name: "", brand: "", barcode: "" });
  const [selectedProduct, setSelectedProduct] = useState(null);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [offBarcode, setOffBarcode] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });

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

  return (
    <main className="home-container" role="main">
      <div className="profile-card products-wide">
        {message.text && (
          <div
            className={
              message.type === "error" ? "login-error" : "success-message"
            }
            role="alert"
          >
            {message.text}
          </div>
        )}

        <div className="divider-dark" />

        <div className="search-container-dark">
          <div className="profile-info-group">
            <label>Filtrar productos</label>
            <div className="search-row">
              <input
                className="dark-input"
                placeholder="Nombre..."
                value={filters.name}
                onChange={(e) =>
                  setFilters({ ...filters, name: e.target.value })
                }
                aria-label="Filtrar por nombre"
              />
              <input
                className="dark-input"
                placeholder="Marca..."
                value={filters.brand}
                onChange={(e) =>
                  setFilters({ ...filters, brand: e.target.value })
                }
                aria-label="Filtrar por marca"
              />
              <input
                className="dark-input"
                placeholder="Código de barras..."
                value={filters.barcode}
                onChange={(e) =>
                  setFilters({ ...filters, barcode: e.target.value })
                }
                aria-label="Filtrar por código de barras"
              />
              <button className="action-btn-blue" onClick={handleApplySearch}>
                Buscar
              </button>
            </div>
          </div>
        </div>

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
                <th scope="col">Info</th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {products.map((p) => (
                <tr key={p.id}>
                  <td className="product-cell">
                    <div className="product-name-stack">
                      <span>{p.name}</span>
                    </div>
                  </td>
                  <td>{p.brand}</td>
                  <td className="barcode-text">{p.barcode}</td>
                  <td>
                    <span
                      className={`profile-role-badge ${p.isCustom ? "role-maintainer" : "role-admin"}`}
                    >
                      {p.isCustom ? "Propio" : "Catálogo"}
                    </span>
                  </td>
                  <td style={{ textAlign: "center" }}>
                    {p.isPerishable ? (
                      <span className="badge-perishable yes">Sí</span>
                    ) : (
                      <span className="badge-perishable no">No</span>
                    )}
                  </td>
                  <td>
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

        <div className="profile-actions pagination-gap">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Anterior
          </button>
          <span className="page-info" aria-live="polite">
            Página {page + 1} de {totalPages}
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

      {selectedProduct && (
        <div
          className="modal-overlay"
          onClick={() => setSelectedProduct(null)}
          role="dialog"
          aria-modal="true"
        >
          <div
            className="modal-content profile-card"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="profile-card-title">Descripción del Producto</h2>

            <div className="profile-info-group">
              <label>Nombre y Marca</label>
              <p>
                {selectedProduct.name} - {selectedProduct.brand}
              </p>
            </div>

            <div className="profile-info-group">
              <label>Detalles</label>
              <p className="description-popup-text">
                {selectedProduct.description}
              </p>
            </div>

            <div className="profile-actions">
              <button
                className="logout-danger-btn"
                onClick={() => setSelectedProduct(null)}
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
