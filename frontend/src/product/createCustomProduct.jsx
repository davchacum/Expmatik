import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../global-form.css";

const CreateCustomProduct = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");

  const [formData, setFormData] = useState({
    name: "",
    brand: "",
    description: "",
    barcode: "",
    isPerishable: false,
  });
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    const data = new FormData();
    data.append("name", formData.name);
    data.append("brand", formData.brand);
    data.append("description", formData.description);
    data.append("barcode", formData.barcode);
    data.append("isPerishable", formData.isPerishable);
    if (file) data.append("file", file);

    try {
      const response = await fetch("/api/products/custom", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: data,
      });

      if (response.ok) {
        navigate("/products");
      } else {
        const errData = await response.json().catch(() => ({}));
        setError(errData.message || "Error al crear el producto");
      }
    } catch (err) {
      console.error(err);
      setError("No se pudo conectar con el servidor.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="profile-card">
        {error && (
          <div
            className="login-error"
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "20px" }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="profile-details">
          <div className="profile-info-group">
            <label htmlFor="name">Nombre del Producto</label>
            <input
              id="name"
              name="name"
              className="dark-input"
              required
              aria-required="true"
              value={formData.name}
              onChange={handleChange}
            />
          </div>

          <div className="profile-info-group">
            <label htmlFor="brand">Marca</label>
            <input
              id="brand"
              name="brand"
              className="dark-input"
              required
              aria-required="true"
              value={formData.brand}
              onChange={handleChange}
            />
          </div>

          <div className="profile-info-group">
            <label htmlFor="barcode">Código de Barras</label>
            <input
              id="barcode"
              name="barcode"
              className="dark-input"
              required
              aria-required="true"
              value={formData.barcode}
              onChange={handleChange}
            />
          </div>

          <div className="profile-info-group">
            <label htmlFor="description">Descripción (Opcional)</label>
            <textarea
              id="description"
              name="description"
              className="dark-input"
              style={{ minHeight: "80px", resize: "none" }}
              value={formData.description}
              onChange={handleChange}
            />
          </div>

          <div className="profile-info-group">
            <label className="checkbox-container" htmlFor="isPerishable">
              <input
                type="checkbox"
                id="isPerishable"
                name="isPerishable"
                checked={formData.isPerishable}
                onChange={handleChange}
              />
              <span className="checkbox-label-text">¿Es perecedero?</span>
              <span className="custom-checkmark" aria-hidden="true"></span>
            </label>
          </div>

          <div className="profile-info-group">
            <label htmlFor="product-file">Imagen del Producto</label>
            <input
              id="product-file"
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              className="dark-input"
              style={{ fontSize: "0.8rem" }}
            />
          </div>

          <div
            className="profile-actions"
            style={{ flexDirection: "column", gap: "10px" }}
          >
            <button
              type="submit"
              className="action-btn-blue"
              disabled={loading}
              style={{ padding: "12px", width: "100%" }}
              aria-busy={loading}
            >
              {loading ? "Guardando..." : "Crear Producto"}
            </button>
            <button
              type="button"
              className="page-btn"
              onClick={() => navigate("/products")}
              style={{ width: "100%" }}
              aria-label="Cancelar y volver a la lista de productos"
            >
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </main>
  );
};

export default CreateCustomProduct;
