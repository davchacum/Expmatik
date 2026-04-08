import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "../components/Modal";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const InventoryList = () => {
  const token = localStorage.getItem("accessToken");
  const navigate = useNavigate();
  useRequireTokenRedirect(token, navigate);

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 8;

  const [showAddModal, setShowAddModal] = useState(false);
  const [catalogProducts, setCatalogProducts] = useState([]);
  const [catalogLoading, setCatalogLoading] = useState(false);

  const fetchInventory = useCallback(async () => {
    setLoading(true);
    setMessage({ text: "", type: "" });
    try {
      const params = new URLSearchParams({ page, size: pageSize });
      const response = await fetch(`/api/product-info?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setProducts(data.content || []);
        setTotalPages(data.totalPages || 0);
      }
    } catch (err) {
      setMessage({
        text: "Error al conectar con el servidor",
        type: "error",
        error: err,
      });
    } finally {
      setLoading(false);
    }
  }, [token, page]);

  useEffect(() => {
    fetchInventory();
  }, [fetchInventory]);

  const [catalogFilters, setCatalogFilters] = useState({
    name: "",
    brand: "",
    barcode: "",
  });
  const fetchCatalog = useCallback(async () => {
    setCatalogLoading(true);
    try {
      const params = new URLSearchParams();
      if (catalogFilters.name) params.append("name", catalogFilters.name);
      if (catalogFilters.brand) params.append("brand", catalogFilters.brand);
      if (catalogFilters.barcode)
        params.append("barcode", catalogFilters.barcode);
      params.append("size", 5);

      const response = await fetch(
        `/api/products/without-info?${params.toString()}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (response.ok) {
        const data = await response.json();
        setCatalogProducts(data.content || []);
      }
    } catch (err) {
      console.error("Error al buscar en catálogo", err);
    } finally {
      setCatalogLoading(false);
    }
  }, [token, catalogFilters]);

  useEffect(() => {
    if (showAddModal) {
      fetchCatalog();
    }
  }, [showAddModal, fetchCatalog]);

  const handleAddProduct = async (productId) => {
    try {
      const response = await fetch(
        `/api/product-info/get-or-create-product/${productId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (response.ok) {
        setShowAddModal(false);
        setCatalogFilters({ name: "", brand: "", barcode: "" });
        fetchInventory();
        setMessage({ text: "Producto añadido correctamente", type: "success" });
      }
    } catch (err) {
      setMessage({
        text: "Error al añadir producto",
        type: "error",
        error: err,
      });
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1450px" }}>
        <header
          className="list-header-actions"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div style={{ maxWidth: "75%" }}>
            <h2
              style={{
                margin: "0 0 8px 0",
                fontSize: "1.4rem",
                color: "var(--text-main)",
                fontWeight: "700",
              }}
            >
              ¿No encuentras un producto?
            </h2>
            <p
              style={{
                margin: 0,
                color: "var(--text-muted)",
                fontSize: "0.95rem",
                lineHeight: "1.4",
              }}
            >
              Añádelo a tu inventario para empezar a gestionar su stock y ventas
              de forma eficiente.
            </p>
          </div>
          <button
            className="btn-primary"
            onClick={() => setShowAddModal(true)}
            aria-haspopup="dialog"
          >
            Añadir Producto Existente
          </button>
        </header>

        <div className="divider-dark" />

        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="polite"
            style={{ marginBottom: "20px" }}
          >
            {message.text}
          </div>
        )}

        <div className="table-responsive">
          <table
            className="dark-table"
            aria-label="Tabla de inventario de productos"
          >
            <thead>
              <tr>
                <th scope="col">PRODUCTO</th>
                <th scope="col">PERECEDERO</th>
                <th scope="col">STOCK</th>
                <th scope="col">ÚLT. COMPRA</th>
                <th scope="col">IVA</th>
                <th scope="col">COMPRA + IVA</th>
                <th scope="col">P. VENTA</th>
                <th scope="col">MARGEN U.</th>
                <th scope="col">VALOR STOCK</th>
                <th scope="col">BENEFICIO T.</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  ACCIONES
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {products.map((item) => (
                <tr
                  key={item.productInfoId}
                  style={{
                    backgroundColor: item.needUpdate
                      ? "rgba(243, 34, 34, 0.05)"
                      : "transparent",
                    borderLeft: item.needUpdate
                      ? "8px solid var(--primary-red)"
                      : "none",
                  }}
                >
                  <td>
                    <strong>{item.productName}</strong>
                    <div
                      style={{ fontSize: "0.7rem", color: "var(--text-muted)" }}
                    >
                      {item.productBrand} | {item.productBarcode}
                    </div>
                  </td>
                  <td>
                    <span
                      className={`badge ${item.isPerishable ? "badge-orange" : "badge-blue"}`}
                    >
                      {item.isPerishable ? "Sí" : "No"}
                    </span>
                  </td>
                  <td>{item.stockQuantity}</td>
                  <td>
                    {item.lastPurchaseUnitPrice
                      ? `${item.lastPurchaseUnitPrice.toFixed(2)}€`
                      : ""}
                  </td>
                  <td>{item.vatRate}%</td>
                  <td>
                    {item.lastPurchaseUnitPriceWithVat
                      ? `${item.lastPurchaseUnitPriceWithVat.toFixed(2)}€`
                      : ""}
                  </td>
                  <td style={{ fontWeight: "700" }}>
                    {item.saleUnitPrice.toFixed(2)}€
                  </td>
                  <td
                    style={{
                      color:
                        (item.unitProfit || 0) < 0
                          ? "var(--primary-red)"
                          : "var(--primary-green)",
                    }}
                  >
                    {item.unitProfit ? `${item.unitProfit.toFixed(2)}€` : ""}
                  </td>
                  <td>{item.totalStockValue.toFixed(2)}€</td>
                  <td
                    style={{
                      color:
                        (item.totalProfit || 0) < 0
                          ? "var(--primary-red)"
                          : "var(--primary-green)",
                    }}
                  >
                    {item.totalProfit ? `${item.totalProfit.toFixed(2)}€` : ""}
                  </td>
                  <td style={{ textAlign: "center" }}>
                    <button
                      className="btn-primary"
                      onClick={() =>
                        navigate(`/inventory/${item.productId}/edit`)
                      }
                      aria-label={`Editar información de ${item.productName}`}
                    >
                      {item.needUpdate ? "Configurar" : "Editar"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <nav className="list-footer" aria-label="Navegación de inventario">
          <button
            className="page-btn"
            disabled={page === 0 || loading}
            onClick={() => setPage((p) => p - 1)}
            aria-label="Ir a la página anterior"
          >
            Anterior
          </button>
          <span aria-current="page">
            Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong>
          </span>
          <button
            className="page-btn"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Ir a la página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>

      {showAddModal && (
        <Modal onClose={() => setShowAddModal(false)}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "20px",
            }}
          >
            <h2
              id="modal-title"
              className="section-title"
              style={{ margin: 0 }}
            >
              Catálogo Global de Productos
            </h2>
            <button
              className="action-btn-red"
              onClick={() => {
                setCatalogFilters({ name: "", brand: "", barcode: "" });
                setCatalogProducts([]);
                setShowAddModal(false);
              }}
              aria-label="Cerrar catálogo"
            >
              Cerrar
            </button>
          </div>

          <section aria-labelledby="catalog-filters-label">
            <h3
              id="catalog-filters-label"
              className="sr-only"
              style={{ display: "none" }}
            >
              Filtros del catálogo
            </h3>
            <div
              className="search-row"
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
                gap: "12px",
                marginBottom: "20px",
                alignItems: "end",
              }}
            >
              <div className="input-group">
                <label htmlFor="cat-name" className="input-label">
                  Nombre
                </label>
                <input
                  id="cat-name"
                  type="text"
                  className="dark-input"
                  placeholder="Buscar nombre..."
                  value={catalogFilters.name}
                  onChange={(e) =>
                    setCatalogFilters({
                      ...catalogFilters,
                      name: e.target.value,
                    })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="cat-brand" className="input-label">
                  Marca
                </label>
                <input
                  id="cat-brand"
                  type="text"
                  className="dark-input"
                  placeholder="Filtrar marca..."
                  value={catalogFilters.brand}
                  onChange={(e) =>
                    setCatalogFilters({
                      ...catalogFilters,
                      brand: e.target.value,
                    })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="cat-barcode" className="input-label">
                  Código de Barras
                </label>
                <input
                  id="cat-barcode"
                  type="text"
                  className="dark-input"
                  placeholder="EAN/UPC..."
                  value={catalogFilters.barcode}
                  onChange={(e) =>
                    setCatalogFilters({
                      ...catalogFilters,
                      barcode: e.target.value,
                    })
                  }
                />
              </div>
              <button
                className="btn-primary"
                onClick={fetchCatalog}
                disabled={catalogLoading}
                style={{ height: "44px" }}
                aria-label="Buscar en el catálogo"
              >
                {catalogLoading ? "..." : "Buscar"}
              </button>
            </div>
          </section>

          <div
            className="table-responsive modal-table-wrapper"
            style={{ minWidth: "650px", maxHeight: "450px" }}
          >
            <table
              className="dark-table modal-table"
              aria-label="Resultados del catálogo global"
            >
              <thead>
                <tr>
                  <th scope="col">PRODUCTO</th>
                  <th scope="col">MARCA</th>
                  <th scope="col">CÓDIGO DE BARRAS</th>
                  <th scope="col" style={{ textAlign: "center" }}>
                    ACCIÓN
                  </th>
                </tr>
              </thead>
              <tbody style={{ opacity: catalogLoading ? 0.5 : 1 }}>
                {catalogProducts.length > 0 ? (
                  catalogProducts.map((p) => (
                    <tr key={p.id}>
                      <td>
                        <strong>{p.name}</strong>
                      </td>
                      <td>{p.brand}</td>
                      <td>{p.barcode}</td>
                      <td style={{ textAlign: "center" }}>
                        <button
                          className="action-btn-green"
                          onClick={() => handleAddProduct(p.id)}
                          aria-label={`Añadir ${p.name} al inventario`}
                        >
                          Añadir
                        </button>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td
                      colSpan="4"
                      style={{
                        textAlign: "center",
                        padding: "30px",
                        color: "var(--text-muted)",
                      }}
                    >
                      {catalogLoading
                        ? "Cargando catálogo..."
                        : "No se han encontrado productos que coincidan."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default InventoryList;
