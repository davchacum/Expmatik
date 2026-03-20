import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import "../global-form.css";

const EditProductInfo = () => {
  const { id } = useParams(); // Usando 'id' como confirmamos antes
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");

  const [product, setProduct] = useState(null);
  const [formData, setFormData] = useState({
    stockQuantity: 0,
    saleUnitPrice: 0,
    vatRate: 0,
  });

  // Estados calculados en tiempo real
  const [liveMetrics, setLiveMetrics] = useState({
    purchaseWithVat: 0,
    unitProfit: 0,
    totalStockValue: 0,
    totalProfit: 0,
  });

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [, setError] = useState("");

  const calculateMetrics = useCallback(
    (stock, salePrice, vat, purchasePrice) => {
      const purchase = parseFloat(purchasePrice) || 0;
      const sale = parseFloat(salePrice) || 0;
      const qty = parseInt(stock) || 0;
      const tax = parseFloat(vat) || 0;

      const purchaseWithVat = purchase * (1 + tax);
      const unitProfit = sale - purchaseWithVat;
      const totalStockValue = sale * qty;
      const totalProfit = unitProfit * qty;

      setLiveMetrics({
        purchaseWithVat,
        unitProfit,
        totalStockValue,
        totalProfit,
      });
    },
    [],
  );

  const fetchProductDetails = useCallback(async () => {
    if (!id || id === "undefined") return;
    try {
      const response = await fetch(
        `/api/product-info/get-or-create-product/${id}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (response.ok) {
        const data = await response.json();
        setProduct(data);
        setFormData({
          stockQuantity: data.stockQuantity || 0,
          saleUnitPrice: data.saleUnitPrice || 0,
          vatRate: data.vatRate || 0,
        });
        calculateMetrics(
          data.stockQuantity,
          data.saleUnitPrice,
          data.vatRate,
          data.lastPurchaseUnitPrice,
        );
      } else {
        setError("No se pudo cargar la información.");
      }
    } catch (err) {
      setError("Error de conexión." + err.message);
    } finally {
      setLoading(false);
    }
  }, [token, id, calculateMetrics]);

  useEffect(() => {
    if (!token) navigate("/login", { replace: true });
    else fetchProductDetails();
  }, [token, navigate, fetchProductDetails]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    const newFormData = { ...formData, [name]: value };
    setFormData(newFormData);

    // Actualizar cálculos en tiempo real
    calculateMetrics(
      newFormData.stockQuantity,
      newFormData.saleUnitPrice,
      newFormData.vatRate,
      product.lastPurchaseUnitPrice,
    );
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const response = await fetch(
        `/api/product-info/${product.productInfoId}`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            stockQuantity: parseInt(formData.stockQuantity),
            saleUnitPrice: parseFloat(formData.saleUnitPrice),
            vatRate: parseFloat(formData.vatRate),
          }),
        },
      );
      if (response.ok) navigate("/inventory");
      else setError("Error al guardar cambios.");
    } catch (err) {
      setError("Error de red." + err.message);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="form-container">Cargando...</div>;

  return (
    <main className="home-container" role="main">
      <div className="form-container" style={{ maxWidth: "1000px" }}>
        <h1 className="section-title">Modificar {product.productName}</h1>

        <form onSubmit={handleSubmit}>
          <div
            className="form-grid"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "20px",
            }}
          >
            <div className="input-group">
              <label htmlFor="stockQuantity" className="input-label">
                Stock Actual
              </label>
              <input
                id="stockQuantity"
                name="stockQuantity"
                type="number"
                className="dark-input"
                value={formData.stockQuantity}
                onChange={handleChange}
              />
            </div>

            <div className="input-group">
              <label htmlFor="purchasePriceBase" className="input-label">
                Precio Compra (Base)
              </label>
              <input
                id="purchasePriceBase"
                className="dark-input"
                value={
                  product.lastPurchaseUnitPrice
                    ? `${product.lastPurchaseUnitPrice.toFixed(2)}€`
                    : ""
                }
                readOnly
                aria-readonly="true"
                style={{ opacity: 0.6 , cursor: "default"}}
              />
            </div>

            {/* Compra + IVA - Solo lectura */}
            <div className="input-group">
              <label htmlFor="purchaseWithVat" className="input-label">
                Compra + IVA
              </label>
              <input
                id="purchaseWithVat"
                className="dark-input"
                value={
                  liveMetrics.purchaseWithVat
                    ? `${liveMetrics.purchaseWithVat.toFixed(2)}€`
                    : ""
                }
                readOnly
                aria-readonly="true"
                style={{ opacity: 0.6 , cursor: "default"}}
              />
            </div>

            {/* Precio Venta */}
            <div className="input-group">
              <label htmlFor="saleUnitPrice" className="input-label">
                Precio Venta (€)
              </label>
              <input
                id="saleUnitPrice"
                name="saleUnitPrice"
                type="number"
                step="0.01"
                className="dark-input"
                value={formData.saleUnitPrice}
                onChange={handleChange}
                style={{ borderColor: "var(--primary-blue)" }}
              />
            </div>

            {/* IVA Venta */}
            <div className="input-group">
              <label htmlFor="vatRate" className="input-label">
                IVA Venta
              </label>
              <input
                id="vatRate"
                name="vatRate"
                type="number"
                step="0.01"
                min="0"
                max="0.21"
                className="dark-input"
                value={formData.vatRate}
                onChange={handleChange}
              />
            </div>

            {/* Beneficio Unitario - Informativo dinámico */}
            <div className="input-group">
              <label htmlFor="unitProfit" className="input-label">
                Beneficio Unitario
              </label>
              <input
                id="unitProfit"
                className="dark-input"
                value={
                  liveMetrics.unitProfit
                    ? `${liveMetrics.unitProfit.toFixed(2)}€`
                    : ""
                }
                readOnly
                aria-readonly="true"
                style={{
                  color:
                    liveMetrics.unitProfit >= 0
                      ? "var(--primary-green)"
                      : "var(--primary-red)",
                  fontWeight: "700",
                  backgroundColor: "rgba(0,0,0,0.2)",
                }}
              />
            </div>

            {/* Valor Total Stock - Solo lectura */}
            <div className="input-group">
              <label htmlFor="totalStockValue" className="input-label">
                Valor Total Stock
              </label>
              <input
                id="totalStockValue"
                className="dark-input"
                value={
                  liveMetrics.totalStockValue
                    ? `${liveMetrics.totalStockValue.toFixed(2)}€`
                    : ""
                }
                readOnly
                aria-readonly="true"
                style={{ opacity: 0.6 , cursor: "default"}}
              />
            </div>

            <div className="input-group">
              <label htmlFor="totalProfit" className="input-label">
                Beneficio Total Stock
              </label>
              <input
                id="totalProfit"
                className="dark-input"
                value={
                  liveMetrics.totalProfit
                    ? `${liveMetrics.totalProfit.toFixed(2)}€`
                    : ""
                }
                readOnly
                aria-readonly="true"
                style={{
                  color:
                    liveMetrics.totalProfit >= 0
                      ? "var(--primary-green)"
                      : "var(--primary-red)",
                  fontWeight: "700",
                  backgroundColor: "rgba(0,0,0,0.2)",
                }}
              />
            </div>
          </div>

          {/* Footer del formulario */}
          <div className="form-footer">
            <button
              type="button"
              className="action-btn-red"
              onClick={() => navigate("/inventory")}
              aria-label="Cancelar cambios y volver al inventario"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="action-btn-green"
              disabled={saving}
              aria-busy={saving}
            >
              {saving ? "Guardando..." : "Guardar Cambios"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
};

export default EditProductInfo;
