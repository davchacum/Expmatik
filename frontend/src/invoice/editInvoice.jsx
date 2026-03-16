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
        setError("No se pudo cargar la factura seleccionada.");
      }
    } catch (err) {
      setError("Error de conexión al cargar los datos: " + err.message);
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
        setSuccess("Datos generales actualizados correctamente.");
      } else {
        setError("Error al actualizar los datos de la factura.");
      }
    } catch (err) {
      setError("Error de red al intentar actualizar: " + err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteInvoice = async () => {
    if (
      !window.confirm(
        "¿Estás seguro de que deseas eliminar esta factura y todos sus lotes?",
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
        setError("No se pudo eliminar la factura.");
      }
    } catch (err) {
      setError("Error de red al intentar eliminar: " + err.message);
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
        setError("Error al añadir el lote. Verifique el código de barras.");
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
      if (response.ok) fetchInvoiceData();
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
        <div className="list-container" aria-busy="true">
          Cargando datos de la factura...
        </div>
      </main>
    );

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <div aria-live="polite">
          {error && (
            <div
              className="message-error"
              role="alert"
              style={{ marginBottom: "20px" }}
            >
              {error}
            </div>
          )}
          {success && (
            <div
              className="message-success"
              role="status"
              style={{ marginBottom: "20px" }}
            >
              {success}
            </div>
          )}
        </div>

        <form onSubmit={handleUpdateInvoice}>
          <div className="form-grid">
            <div className="input-group">
              <label htmlFor="edit-inv-num" className="input-label">
                Nº Factura
              </label>
              <input
                id="edit-inv-num"
                className="dark-input"
                value={invoice.invoiceNumber}
                onChange={(e) =>
                  setInvoice({ ...invoice, invoiceNumber: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="edit-supplier" className="input-label">
                Proveedor
              </label>
              <input
                id="edit-supplier"
                className="dark-input"
                value={invoice.supplierName}
                onChange={(e) =>
                  setInvoice({ ...invoice, supplierName: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="edit-date" className="input-label">
                Fecha
              </label>
              <input
                id="edit-date"
                type="date"
                className="dark-input"
                value={invoice.invoiceDate}
                onChange={(e) =>
                  setInvoice({ ...invoice, invoiceDate: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="edit-status" className="input-label">
                Estado
              </label>
              <select
                id="edit-status"
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
              Eliminar Factura
            </button>
          </div>
        </form>

        <div className="divider-dark" />

        <section aria-labelledby="add-batch-section">
          <h2
            id="add-batch-section"
            className="section-label"
            style={{ marginBottom: "15px" }}
          >
            Añadir Nuevo Lote
          </h2>
          <div
            className="form-grid"
            style={{
              gridTemplateColumns: "2fr 1fr 1fr 1fr auto",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label htmlFor="new-batch-barcode" className="input-label">
                Código de Barras
              </label>
              <input
                id="new-batch-barcode"
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
              <label htmlFor="new-batch-qty" className="input-label">
                Unidades
              </label>
              <input
                id="new-batch-qty"
                type="number"
                className="dark-input"
                value={currentBatch.quantity}
                onChange={(e) =>
                  setCurrentBatch({ ...currentBatch, quantity: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="new-batch-price" className="input-label">
                Precio
              </label>
              <input
                id="new-batch-price"
                type="number"
                step="0.01"
                className="dark-input"
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
              <label htmlFor="new-batch-exp" className="input-label">
                Caducidad
              </label>
              <input
                id="new-batch-exp"
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
        </section>

        <div className="table-responsive" style={{ marginTop: "20px" }}>
          <table className="dark-table" aria-label="Lotes de esta factura">
            <thead>
              <tr>
                <th scope="col">Código de Barras</th>
                <th scope="col">Cant.</th>
                <th scope="col">P. Unitario</th>
                <th scope="col">Subtotal</th>
                <th scope="col">Caducidad</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody>
              {batches.map((b, i) => (
                <tr key={b.id || i}>
                  {editingIndex === i ? (
                    <>
                      <td>
                        <input
                          aria-label="Editar código"
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
                          aria-label="Editar cantidad"
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
                          aria-label="Editar precio"
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
                          aria-label="Editar caducidad"
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
                        <div
                          style={{
                            display: "flex",
                            gap: "5px",
                            justifyContent: "center",
                          }}
                        >
                          <button
                            type="button"
                            className="action-btn-red"
                            onClick={() => setEditingIndex(null)}
                          >
                            Cancelar
                          </button>
                          <button
                            type="button"
                            className="action-btn-blue"
                            onClick={() => handleUpdateBatch(i)}
                            disabled={actionLoading}
                          >
                            Confirmar
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
                            aria-label="Eliminar lote"
                          >
                            Borrar
                          </button>
                          <button
                            type="button"
                            className="action-btn-blue"
                            onClick={() => setEditingIndex(i)}
                            aria-label="Editar lote"
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
            aria-label="Volver al listado"
          >
            Volver
          </button>
        </div>
      </div>
    </main>
  );
};

export default EditInvoice;
