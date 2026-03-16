import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import "../global-form.css";
import "../global-list.css";

const EditInvoice = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");

  const [invoice, setInvoice] = useState({
    invoiceNumber: "",
    supplierName: "",
    status: "PENDING",
    invoiceDate: "",
  });

  const [batches, setBatches] = useState([]);
  const [editingIndex, setEditingIndex] = useState(null);
  const [currentBatch, setCurrentBatch] = useState({
    productBarcode: "",
    quantity: "",
    unitPrice: "",
    expirationDate: "",
  });

  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const fetchInvoiceData = useCallback(async () => {
    try {
      const response = await fetch(`/api/invoices/${id}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setInvoice({
          invoiceNumber: data.invoiceNumber,
          supplierName: data.supplierName,
          status: data.status,
          invoiceDate: data.invoiceDate,
        });
        setBatches(data.batches);
      } else {
        setError("No se pudo cargar la factura.");
      }
    } catch (err) {
      setError("Error de conexión al cargar:" + err.message);
    } finally {
      setLoading(false);
    }
  }, [id, token]);

  useEffect(() => {
    if (!token) {
      navigate("/login");
      return;
    }
    fetchInvoiceData();
  }, [fetchInvoiceData, token, navigate]);

  const handleUpdateInvoice = async (e) => {
    e.preventDefault();
    setActionLoading(true);
    setError("");
    setSuccess("");

    try {
      const response = await fetch(`/api/invoices/${id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(invoice),
      });

      if (response.ok) {
        setSuccess("Datos de factura actualizados correctamente.");
      } else {
        let errorMsg = "Error al actualizar la factura.";
        try {
          const data = await response.json();
          if (data.message) {
            errorMsg = data.message;
          }
          let details = [];
          if (data.errors && Array.isArray(data.errors)) {
            details = data.errors;
          } else if (data.errors && typeof data.errors === "object") {
            details = Object.values(data.errors);
          }
          if (data.detail) {
            details.push(data.detail);
          }
          if (data.fieldErrors) {
            details = details.concat(Object.values(data.fieldErrors));
          }

          if (details.length > 0) {
            errorMsg += ": " + details.join("; ");
          }
        } catch (err) {
          console.error("Error validando factura:", err);
        }
        setError(errorMsg);
      }
    } catch (err) {
      setError("Error de red: " + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteInvoice = async () => {
    if (
      !window.confirm(
        "¿Estás seguro de que deseas eliminar esta factura y todos sus lotes permanentemente?",
      )
    )
      return;

    setActionLoading(true);
    setError("");
    try {
      const response = await fetch(`/api/invoices/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.ok) {
        navigate("/invoices");
      } else {
        const data = await response.json().catch(() => ({}));
        setError(data.message || "No se pudo eliminar la factura.");
      }
    } catch (err) {
      setError("Error de red: " + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleAddBatch = async () => {
    setActionLoading(true);
    setError("");
    try {
      const response = await fetch(`/api/batches?invoiceId=${id}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(currentBatch),
      });

      if (response.ok) {
        setCurrentBatch({
          productBarcode: "",
          quantity: "",
          unitPrice: "",
          expirationDate: "",
        });
        fetchInvoiceData();
      } else {
        const data = await response.json().catch(() => ({}));
        setError(
          data.message ||
            "Error al crear el lote (verifique el código de barras)",
        );
      }
    } catch (err) {
      setError("Error de red: " + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateBatch = async (index) => {
    const batch = batches[index];
    setActionLoading(true);
    try {
      const response = await fetch(`/api/batches/${batch.id}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          productBarcode: batch.productBarcode,
          quantity: batch.quantity,
          unitPrice: batch.unitPrice,
          expirationDate: batch.expirationDate,
        }),
      });

      if (response.ok) {
        setEditingIndex(null);
        fetchInvoiceData();
      } else {
        setError("Error al actualizar lote.");
      }
    } catch (err) {
      setError("Error de red: " + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteBatch = async (batchId) => {
    if (!window.confirm("¿Estás seguro de borrar este lote?")) return;
    try {
      const response = await fetch(`/api/batches/${batchId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        fetchInvoiceData();
      } else {
        let errorMsg = "Error al eliminar lote.";
        try {
          const data = await response.json();
          if (data.message) {
            errorMsg = data.message;
          } else if (data.detail) {
            errorMsg = data.detail;
          } else if (data.errors) {
            errorMsg = Array.isArray(data.errors)
              ? data.errors.join("; ")
              : data.errors;
          }
        } catch (err) {
          console.error("Error validando eliminación de lote:", err);
        }
        setError(errorMsg);
      }
    } catch (err) {
      setError("Error de red: " + err.message);
    }
  };

  const handleEditBatchChange = (index, field, value) => {
    const updated = [...batches];
    updated[index] = { ...updated[index], [field]: value };
    setBatches(updated);
  };

  if (loading)
    return (
      <main className="home-container">
        <div className="list-container">Cargando...</div>
      </main>
    );

  return (
    <main className="home-container">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <span className="section-label">
          Editando Factura: {invoice.invoiceNumber}
        </span>

        {error && (
          <div className="message-error" style={{ marginBottom: "20px" }}>
            {error}
          </div>
        )}
        {success && (
          <div className="message-success" style={{ marginBottom: "20px" }}>
            {success}
          </div>
        )}

        <form onSubmit={handleUpdateInvoice}>
          <div className="form-grid">
            <div className="input-group">
              <label className="input-label">Nº Factura</label>
              <input
                className="dark-input"
                value={invoice.invoiceNumber}
                onChange={(e) =>
                  setInvoice({ ...invoice, invoiceNumber: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Proveedor</label>
              <input
                className="dark-input"
                value={invoice.supplierName}
                onChange={(e) =>
                  setInvoice({ ...invoice, supplierName: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label className="input-label">Fecha</label>
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
          <div style={{ display: "flex", gap: "10px", marginTop: "15px" }}>
            <button
              type="submit"
              className="action-btn-blue"
              disabled={actionLoading}
            >
              {actionLoading ? "Guardando..." : "Actualizar Datos Generales"}
            </button>
            <button
              type="button"
              className="action-btn-red"
              onClick={handleDeleteInvoice}
              disabled={actionLoading}
            >
              {actionLoading ? "Procesando..." : "Eliminar Factura"}
            </button>
          </div>
        </form>

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
            <label className="input-label">Barcode</label>
            <input
              className="dark-input"
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
            <label className="input-label">Cant.</label>
            <input
              type="number"
              className="dark-input"
              value={currentBatch.quantity}
              onChange={(e) =>
                setCurrentBatch({ ...currentBatch, quantity: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label className="input-label">Precio</label>
            <input
              type="number"
              step="0.01"
              className="dark-input"
              value={currentBatch.unitPrice}
              onChange={(e) =>
                setCurrentBatch({ ...currentBatch, unitPrice: e.target.value })
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
            onClick={handleAddBatch}
            disabled={actionLoading}
          >
            Añadir Lote
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
                <tr key={b.id || i}>
                  {editingIndex === i ? (
                    <>
                      <td>
                        <input
                          className="dark-input"
                          value={b.barcode}
                          onChange={(e) =>
                            handleEditBatchChange(i, "barcode", e.target.value)
                          }
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          className="dark-input"
                          value={b.quantity}
                          onChange={(e) =>
                            handleEditBatchChange(i, "quantity", e.target.value)
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
                        <div style={{ display: "flex", gap: "5px" }}>
                          <button
                            type="button"
                            className="action-btn-red"
                            onClick={() => {
                              setEditingIndex(null);
                              setError("");
                            }}
                            style={{ height: "44px", padding: "0 15px" }}
                          >
                            Cancelar
                          </button>
                          <button
                            type="button"
                            className="action-btn-blue"
                            onClick={() => handleUpdateBatch(i)}
                            disabled={loading}
                          >
                            {loading ? "..." : "Confirmar"}
                          </button>
                        </div>
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
                            className="action-btn-red"
                            onClick={() => handleDeleteBatch(b.id)}
                          >
                            Borrar
                          </button>
                          <button
                            type="button"
                            className="action-btn-blue"
                            onClick={() => setEditingIndex(i)}
                          >
                            Editar
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
            className="action-btn-red"
            onClick={() => navigate("/invoices")}
          >
            Volver
          </button>
        </div>
      </div>
    </main>
  );
};

export default EditInvoice;
