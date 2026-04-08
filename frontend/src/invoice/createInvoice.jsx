import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const CreateInvoice = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [invoice, setInvoice] = useState({
    invoiceNumber: "",
    supplierName: "",
    status: "PENDING",
    invoiceDate: new Date().toISOString().split("T")[0],
  });

  const [batches, setBatches] = useState([]);
  const [editingIndex, setEditingIndex] = useState(null);
  const [currentBatch, setCurrentBatch] = useState({
    productBarcode: "",
    quantity: "",
    unitPrice: "",
    expirationDate: "",
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const validateBarcode = async (barcode) => {
    try {
      const response = await fetch(
        `/api/products/validate-barcode/${barcode}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (!response.ok) return false;
      return await response.json();
    } catch (err) {
      console.error("Error validando barcode:", err);
      return false;
    }
  };

  const addBatch = async () => {
    setError("");
    if (
      !currentBatch.productBarcode ||
      !currentBatch.quantity ||
      !currentBatch.unitPrice
    ) {
      setError("El código de barras, cantidad y precio son obligatorios.");
      return;
    }

    setLoading(true);
    const exists = await validateBarcode(currentBatch.productBarcode);
    setLoading(false);

    if (!exists) {
      setError(
        `El producto con código ${currentBatch.productBarcode} no existe en tu catálogo, considere crear un nuevo producto personalizado.`,
      );
      return;
    }

    setBatches([...batches, currentBatch]);
    setCurrentBatch({
      productBarcode: "",
      quantity: "",
      unitPrice: "",
      expirationDate: "",
    });
  };

  const confirmEdit = async (index) => {
    setError("");
    const batchToValidate = batches[index];
    setLoading(true);
    const exists = await validateBarcode(batchToValidate.productBarcode);
    setLoading(false);

    if (!exists) {
      setError(
        `El producto con código ${batchToValidate.productBarcode} no existe en tu catálogo, considere crear un nuevo producto personalizado.`,
      );
      return;
    }
    setEditingIndex(null);
  };

  const removeBatch = (index) => {
    setBatches(batches.filter((_, i) => i !== index));
    if (editingIndex === index) setEditingIndex(null);
  };

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
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <h1 className="section-title">Creación Manual de Factura</h1>
        {error && (
          <div
            className="message-error"
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "20px" }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <section aria-labelledby="invoice-data-title">
            <h2
              id="invoice-data-title"
              className="section-label"
              style={{ marginBottom: "15px" }}
            >
              Datos Generales
            </h2>
            <div className="form-grid">
              <div className="input-group">
                <label htmlFor="invoice-num" className="input-label">
                  Número de Factura
                </label>
                <input
                  id="invoice-num"
                  className="dark-input"
                  placeholder="Ej: FAC-2024-001"
                  required
                  onChange={(e) =>
                    setInvoice({ ...invoice, invoiceNumber: e.target.value })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="supplier-name" className="input-label">
                  Proveedor
                </label>
                <input
                  id="supplier-name"
                  className="dark-input"
                  placeholder="Nombre del proveedor..."
                  required
                  onChange={(e) =>
                    setInvoice({ ...invoice, supplierName: e.target.value })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="invoice-date" className="input-label">
                  Fecha de Emisión
                </label>
                <input
                  id="invoice-date"
                  type="date"
                  className="dark-input"
                  value={invoice.invoiceDate}
                  onChange={(e) =>
                    setInvoice({ ...invoice, invoiceDate: e.target.value })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="invoice-status" className="input-label">
                  Estado
                </label>
                <select
                  id="invoice-status"
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
          </section>

          <div className="divider-dark" />

          <section aria-labelledby="add-batch-title">
            <h2
              id="add-batch-title"
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
                <label htmlFor="batch-barcode" className="input-label">
                  Código de Barras
                </label>
                <input
                  id="batch-barcode"
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
                <label htmlFor="batch-qty" className="input-label">
                  Unidades
                </label>
                <input
                  id="batch-qty"
                  type="number"
                  className="dark-input"
                  placeholder="Cant."
                  value={currentBatch.quantity}
                  onChange={(e) =>
                    setCurrentBatch({
                      ...currentBatch,
                      quantity: e.target.value,
                    })
                  }
                />
              </div>
              <div className="input-group">
                <label htmlFor="batch-price" className="input-label">
                  Precio Unit.
                </label>
                <input
                  id="batch-price"
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
                <label htmlFor="batch-exp" className="input-label">
                  Caducidad
                </label>
                <input
                  id="batch-exp"
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
                disabled={loading}
                aria-label="Añadir lote a la lista"
              >
                {loading ? "Validando..." : "Añadir Lote"}
              </button>
            </div>
          </section>
          <div className="table-responsive" style={{ marginTop: "20px" }}>
            <table
              className="dark-table"
              aria-label="Lotes añadidos a la factura"
            >
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
                  <tr key={i}>
                    {editingIndex === i ? (
                      <>
                        <td>
                          <input
                            aria-label="Editar código de barras"
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
                            aria-label="Editar precio unitario"
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
                            aria-label="Editar fecha de caducidad"
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
                              gap: "8px",
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
                              onClick={() => confirmEdit(i)}
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
                              onClick={() => removeBatch(i)}
                              aria-label={`Borrar lote ${b.productBarcode}`}
                            >
                              Borrar
                            </button>
                            <button
                              type="button"
                              className="action-btn-blue"
                              onClick={() => setEditingIndex(i)}
                              aria-label={`Editar lote ${b.productBarcode}`}
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
