import { useState,useEffect} from "react";
import { useNavigate } from "react-router-dom";
import "../global-form.css";
import "../global-list.css";

const CreateInvoice = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");

  const [invoice, setInvoice] = useState({
    invoiceNumber: "",
    supplierName: "",
    status: "PENDING",
    invoiceDate: new Date().toISOString().split("T")[0],
  });

  const [batches, setBatches] = useState([]);
  const [editingIndex, setEditingIndex] = useState(null); // Índice del lote en edición
  const [currentBatch, setCurrentBatch] = useState({
    productBarcode: "",
    quantity: "",
    unitPrice: "",
    expirationDate: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!token) {
      navigate("/login", { replace: true });
      return;
    }
  }, [token, navigate]);

  const addBatch = () => {
    if (
      !currentBatch.productBarcode ||
      !currentBatch.quantity ||
      !currentBatch.unitPrice
    ) {
      setError("El código de barras, cantidad y precio son obligatorios.");
      return;
    }
    setBatches([...batches, currentBatch]);
    setCurrentBatch({
      productBarcode: "",
      quantity: "",
      unitPrice: "",
      expirationDate: "",
    });
    setError("");
  };

  const removeBatch = (index) => {
    setBatches(batches.filter((_, i) => i !== index));
    if (editingIndex === index) setEditingIndex(null);
  };

  // Manejar cambios en un lote que ya está en la tabla
  const handleEditBatchChange = (index, field, value) => {
    const updatedBatches = [...batches];
    updatedBatches[index] = { ...updatedBatches[index], [field]: value };
    setBatches(updatedBatches);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (batches.length === 0) {
      setError("No se puede crear una factura sin productos.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await fetch("/api/invoices", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ ...invoice, batches }),
      });

      if (response.ok) {
        navigate("/invoices");
      } else {
        const errData = await response.json().catch(() => ({}));
        setError(errData.message || "Error al crear la factura");
      }
    } catch (err) {
      setError("No se pudo conectar con el servidor: " + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="home-container">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <span className="section-label">Creación Manual de Factura</span>

        {error && (
          <div className="message-error" style={{ marginBottom: "20px" }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div className="input-group">
              <label className="input-label">Número de Factura</label>
              <input
                className="dark-input"
                placeholder="Ej: FAC-2024-001"
                required
                onChange={(e) =>
                  setInvoice({ ...invoice, invoiceNumber: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Proveedor</label>
              <input
                className="dark-input"
                placeholder="Nombre del proveedor..."
                required
                onChange={(e) =>
                  setInvoice({ ...invoice, supplierName: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Fecha de Emisión</label>
              <input
                type="date"
                className="dark-input"
                value={invoice.invoiceDate}
                onChange={(e) =>
                  setInvoice({ ...invoice, invoiceDate: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Estado</label>
              <select
                className="dark-input"
                value={invoice.status}
                required
                onChange={(e) =>
                  setInvoice({ ...invoice, status: e.target.value })
                }
              >
                <option value="PENDING">Pendiente</option>
                <option value="RECEIVED">Recibida</option>
                <option value="CANCELED">Cancelada</option>
              </select>
            </div>
          </div>

          <div className="divider-dark" />

          <span className="section-label">Añadir Nuevo Lote</span>
          <div
            className="form-grid"
            style={{
              gridTemplateColumns: "2fr 1fr 1fr 1fr auto",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label className="input-label">Código de Barras</label>
              <input
                className="dark-input"
                placeholder="Barcode..."
                value={currentBatch.productBarcode}
                onChange={(e) =>
                  setCurrentBatch({
                    ...currentBatch,
                    productBarcode: e.target.value,
                  })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Unidades</label>
              <input
                type="number"
                className="dark-input"
                placeholder="Cant."
                value={currentBatch.quantity}
                onChange={(e) =>
                  setCurrentBatch({ ...currentBatch, quantity: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Precio Unit.</label>
              <input
                type="number"
                step="0.01"
                className="dark-input"
                placeholder="0.00€"
                value={currentBatch.unitPrice}
                onChange={(e) =>
                  setCurrentBatch({
                    ...currentBatch,
                    unitPrice: e.target.value,
                  })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Caducidad</label>
              <input
                type="date"
                className="dark-input"
                value={currentBatch.expirationDate}
                onChange={(e) =>
                  setCurrentBatch({
                    ...currentBatch,
                    expirationDate: e.target.value,
                  })
                }
              />
            </div>
            <button
              type="button"
              className="action-btn-blue"
              onClick={addBatch}
            >
              +
            </button>
          </div>

          <div className="table-responsive" style={{ marginTop: "20px" }}>
            <table className="dark-table">
              <thead>
                <tr>
                  <th>Código de Barras</th>
                  <th>Cant.</th>
                  <th>P. Unitario</th>
                  <th>Subtotal</th>
                  <th>Caducidad</th>
                  <th style={{ textAlign: "center" }}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {batches.map((b, i) => (
                  <tr key={i}>
                    {editingIndex === i ? (
                      <>
                        <td>
                          <input
                            className="dark-input"
                            value={b.productBarcode}
                            onChange={(e) =>
                              handleEditBatchChange(
                                i,
                                "productBarcode",
                                e.target.value,
                              )
                            }
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            className="dark-input"
                            value={b.quantity}
                            onChange={(e) =>
                              handleEditBatchChange(
                                i,
                                "quantity",
                                e.target.value,
                              )
                            }
                          />
                        </td>
                        <td>
                          <input
                            type="number"
                            step="0.01"
                            className="dark-input"
                            value={b.unitPrice}
                            onChange={(e) =>
                              handleEditBatchChange(
                                i,
                                "unitPrice",
                                e.target.value,
                              )
                            }
                          />
                        </td>
                        <td>{(b.quantity * b.unitPrice).toFixed(2)}€</td>
                        <td>
                          <input
                            type="date"
                            className="dark-input"
                            value={b.expirationDate}
                            onChange={(e) =>
                              handleEditBatchChange(
                                i,
                                "expirationDate",
                                e.target.value,
                              )
                            }
                          />
                        </td>
                        <td style={{ textAlign: "center" }}>
                          <button
                            type="button"
                            className="action-btn-blue"
                            onClick={() => setEditingIndex(null)}
                          >
                            Confirmar
                          </button>
                        </td>
                      </>
                    ) : (
                      <>
                        <td>
                          <strong>{b.productBarcode}</strong>
                        </td>
                        <td>{b.quantity}</td>
                        <td>{b.unitPrice}€</td>
                        <td>{(b.quantity * b.unitPrice).toFixed(2)}€</td>
                        <td>{b.expirationDate || "N/A"}</td>
                        <td style={{ textAlign: "center" }}>
                          <div
                            style={{
                              display: "flex",
                              gap: "8px",
                              justifyContent: "center",
                            }}
                          >
                            <button
                              type="button"
                              className="action-btn-blue"
                              onClick={() => setEditingIndex(i)}
                            >
                              Editar
                            </button>
                            <button
                              type="button"
                              className="action-btn-red"
                              onClick={() => removeBatch(i)}
                            >
                              Borrar
                            </button>
                          </div>
                        </td>
                      </>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="form-footer">
            <button
              type="button"
              className="action-btn-red"
              onClick={() => navigate("/invoices")}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="action-btn-green"
              disabled={loading}
            >
              {loading ? "Procesando..." : "Guardar Factura"}
            </button>
          </div>
        </form>
      </div>
    </main>
  );
};

export default CreateInvoice;
