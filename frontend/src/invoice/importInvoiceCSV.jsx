import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const ImportInvoiceCSV = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [importedInvoices, setImportedInvoices] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);
    setError("");

    try {
      const response = await fetch("/api/invoices/csv", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        const invoicesToProcess = Array.isArray(data) ? data : [data];

        setImportedInvoices((prevInvoices) => {
          if (!prevInvoices || prevInvoices.length === 0) {
            return invoicesToProcess;
          }

          const updatedList = [...prevInvoices];

          invoicesToProcess.forEach((newInv) => {
            if (!newInv.invoiceNumber) return;

            const existingIndex = updatedList.findIndex(
              (inv) => inv.invoiceNumber === newInv.invoiceNumber,
            );

            if (existingIndex !== -1) {
              updatedList[existingIndex] = newInv;
            } else {
              updatedList.push(newInv);
            }
          });

          return updatedList;
        });

        e.target.value = null;
      } else {
        setError("Error al procesar el CSV. Revisa el formato del archivo.");
      }
    } catch (err) {
      console.error("Upload error:", err);
      setError("No se pudo conectar con el servidor.");
    } finally {
      setLoading(false);
    }
  };

  const saveEditedInvoice = (updatedInvoice) => {
    const newInvoices = [...importedInvoices];
    newInvoices[editingIndex] = updatedInvoice;
    setImportedInvoices(newInvoices);
    setIsModalOpen(false);
  };

  const handleFinalConfirm = async () => {
    setLoading(true);
    setError("");

    const currentInvoices = [...importedInvoices];
    const remainingInvoices = [];

    for (let i = 0; i < currentInvoices.length; i++) {
      const inv = currentInvoices[i];

      try {
        const response = await fetch("/api/invoices", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(inv),
        });

        if (response.ok) {
          console.log(`Factura ${inv.invoiceNumber} creada.`);
        } else if (response.status === 409) {
          inv.statusMessage = "Esta factura ya existe error.";
          remainingInvoices.push(inv);
        } else {
          inv.statusMessage = "Error al crear: " + (await response.text());
          remainingInvoices.push(inv);
        }
      } catch (err) {
        console.error(`Error al crear factura ${inv.invoiceNumber}:`, err);
        inv.statusMessage = "Error de conexión.";
        remainingInvoices.push(inv);
      }
    }
    setImportedInvoices(remainingInvoices);
    setLoading(false);

    if (remainingInvoices.length === 0) {
      alert("¡Todas las facturas se crearon con éxito!");
      navigate("/invoices");
    } else {
      setError(
        "Se procesaron las facturas. Revisa los mensajes de error en las que quedan en la lista.",
      );
    }
  };

  const deleteInvoice = (index) => {
    setImportedInvoices((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <h1 className="section-title">Importar Factura (CSV)</h1>
        <div
          className="upload-zone"
          role="button"
          tabIndex="0"
          aria-label="Zona de carga de archivo CSV. Haz clic o presiona Enter para seleccionar."
          style={{
            border: "2px dashed var(--border-color)",
            borderRadius: "16px",
            padding: "50px",
            textAlign: "center",
            cursor: "pointer",
            background: "rgba(30, 41, 59, 0.3)",
            transition: "all 0.3s",
          }}
          onClick={() => fileInputRef.current.click()}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              fileInputRef.current.click();
            }
          }}
        >
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: "none" }}
            accept=".csv"
            onChange={handleFileUpload}
            aria-hidden="true"
          />
          <p className="section-label">Haz click para seleccionar</p>
          <p className="input-label" style={{ fontSize: "0.7rem" }}>
            ORDEN: invoiceNumber, supplierName, status, invoiceDate,
            productBarcode, quantity, unitPrice, expirationDate
          </p>
        </div>

        {importedInvoices.length > 0 && (
          <section
            style={{ marginTop: "40px" }}
            aria-labelledby="preview-heading"
          >
            <h2 id="preview-heading" className="section-label">
              Previsualización de Facturas
            </h2>
            <div className="table-responsive">
              <table
                className="dark-table"
                aria-label="Facturas importadas listas para procesar"
              >
                <thead>
                  <tr>
                    <th scope="col">Número</th>
                    <th scope="col">Proveedor</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Fecha</th>
                    <th scope="col">Lotes</th>
                    <th scope="col">Estado/Mensaje</th>
                    <th scope="col" style={{ textAlign: "center" }}>
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {importedInvoices.map((inv, idx) => (
                    <tr
                      key={idx}
                      style={
                        inv.statusMessage
                          ? { background: "rgba(220, 38, 38, 0.1)" }
                          : {}
                      }
                    >
                      <td>
                        <strong>{inv.invoiceNumber}</strong>
                      </td>
                      <td>{inv.supplierName}</td>
                      <td>
                        <span
                          className={`badge ${inv.status === "PENDING" ? "badge-yellow" : inv.status === "RECEIVED" ? "badge-green" : inv.status === "CANCELED" ? "badge-red" : "badge-blue"}`}
                        >
                          {inv.status === "PENDING"
                            ? "PENDIENTE"
                            : inv.status === "RECEIVED"
                              ? "RECIBIDA"
                              : inv.status === "CANCELED"
                                ? "CANCELADA"
                                : inv.status.toLowerCase()}
                        </span>
                      </td>
                      <td>{inv.invoiceDate}</td>
                      <td>
                        <span className="badge-blue">
                          {inv.batches?.length}
                        </span>
                      </td>
                      <td
                        style={{
                          fontSize: "0.85rem",
                          color: inv.statusMessage?.includes("existe")
                            ? "var(--primary-orange)"
                            : "var(--primary-red)",
                        }}
                      >
                        {inv.statusMessage || "Pendiente de confirmación"}
                      </td>

                      <td
                        style={{
                          textAlign: "center",
                          display: "flex",
                          gap: "8px",
                          justifyContent: "center",
                        }}
                      >
                        <button
                          className="action-btn-red"
                          onClick={() => deleteInvoice(idx)}
                          aria-label={`Borrar factura ${inv.invoiceNumber}`}
                        >
                          Borrar
                        </button>
                        <button
                          className="action-btn-blue"
                          onClick={() => {
                            setEditingIndex(idx);
                            setIsModalOpen(true);
                          }}
                          aria-label={`Editar factura ${inv.invoiceNumber}`}
                        >
                          Editar
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="form-footer">
              <button
                className="action-btn-red"
                onClick={() => navigate("/invoices")}
                disabled={loading}
              >
                {loading ? "Procesando..." : `Cancelar`}
              </button>
              <button
                className="action-btn-green"
                onClick={handleFinalConfirm}
                disabled={loading}
              >
                {loading
                  ? "Procesando..."
                  : `Confirmar Importación (${importedInvoices.length})`}
              </button>
            </div>
          </section>
        )}

        {error && (
          <div
            className="message-error"
            role="alert"
            aria-live="assertive"
            style={{ marginTop: "20px" }}
          >
            {error}
          </div>
        )}
      </div>

      {isModalOpen && (
        <EditInvoiceModal
          invoiceData={importedInvoices[editingIndex]}
          onClose={() => setIsModalOpen(false)}
          onSave={saveEditedInvoice}
          token={token}
        />
      )}
    </main>
  );
};

const EditInvoiceModal = ({ invoiceData, onClose, onSave, token }) => {
  const [invoice, setInvoice] = useState({ ...invoiceData });
  const [batches, setBatches] = useState(invoiceData.batches || []);
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
      return response.ok ? await response.json() : false;
    } catch (err) {
      console.error("Validation error:", err);
      return false;
    }
  };

  const handleAddBatch = async () => {
    setError("");
    if (!currentBatch.productBarcode || !currentBatch.quantity) {
      setError("Código y cantidad son obligatorios.");
      return;
    }

    setLoading(true);
    const exists = await validateBarcode(currentBatch.productBarcode);
    setLoading(false);

    if (!exists) {
      setError(
        `El código ${currentBatch.productBarcode} no existe en el catálogo.`,
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

  const editBatch = (index) => {
    const b = batches[index];
    setCurrentBatch(b);
    setBatches(batches.filter((_, idx) => idx !== index));
  };

  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="edit-modal-title"
    >
      <div
        className="modal-box"
        style={{
          width: "95%",
          maxWidth: "1100px",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <h2 className="section-title" id="edit-modal-title">
          Editando Factura {invoice.invoiceNumber}
        </h2>

        {error && (
          <div
            className="message-error"
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "15px" }}
          >
            {error}
          </div>
        )}

        <div className="form-grid">
          <div className="input-group">
            <label htmlFor="modal-invoice-num" className="input-label">
              Número de Factura
            </label>
            <input
              id="modal-invoice-num"
              className="dark-input"
              value={invoice.invoiceNumber}
              onChange={(e) =>
                setInvoice({ ...invoice, invoiceNumber: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-supplier" className="input-label">
              Nombre del Proveedor
            </label>
            <input
              id="modal-supplier"
              className="dark-input"
              value={invoice.supplierName}
              onChange={(e) =>
                setInvoice({ ...invoice, supplierName: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-date" className="input-label">
              Fecha Emisión
            </label>
            <input
              id="modal-date"
              type="date"
              className="dark-input"
              value={invoice.invoiceDate}
              onChange={(e) =>
                setInvoice({ ...invoice, invoiceDate: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-status" className="input-label">
              Estado
            </label>
            <select
              id="modal-status"
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

        <div className="divider-dark" />

        <h3 className="section-label" id="add-batch-heading">
          Añadir o Modificar Lotes
        </h3>
        <div
          className="form-grid"
          style={{
            gridTemplateColumns: "2fr 1fr 1fr 1fr auto",
            alignItems: "end",
          }}
          aria-labelledby="add-batch-heading"
        >
          <div className="input-group">
            <label htmlFor="batch-barcode" className="input-label">
              Código de Barras
            </label>
            <input
              id="batch-barcode"
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
            <label htmlFor="batch-quantity" className="input-label">
              Cantidad
            </label>
            <input
              id="batch-quantity"
              type="number"
              className="dark-input"
              value={currentBatch.quantity}
              onChange={(e) =>
                setCurrentBatch({ ...currentBatch, quantity: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="batch-price" className="input-label">
              Precio
            </label>
            <input
              id="batch-price"
              type="number"
              className="dark-input"
              value={currentBatch.unitPrice}
              onChange={(e) =>
                setCurrentBatch({ ...currentBatch, unitPrice: e.target.value })
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
            onClick={handleAddBatch}
            disabled={loading}
          >
            {loading ? "..." : "Añadir"}
          </button>
        </div>

        <div className="table-responsive" style={{ marginTop: "20px" }}>
          <table
            className="dark-table"
            aria-label="Lotes de la factura en edición"
          >
            <thead>
              <tr>
                <th scope="col">Código</th>
                <th scope="col">Cant.</th>
                <th scope="col">Precio</th>
                <th scope="col">Subtotal</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody>
              {batches.map((b, i) => (
                <tr key={i}>
                  <td>
                    <strong>{b.productBarcode}</strong>
                  </td>
                  <td>{b.quantity}</td>
                  <td>{b.unitPrice}€</td>
                  <td>{(b.quantity * b.unitPrice).toFixed(2)}€</td>
                  <td
                    style={{
                      textAlign: "center",
                      display: "flex",
                      gap: "5px",
                      justifyContent: "center",
                    }}
                  >
                    <button
                      className="action-btn-red"
                      onClick={() =>
                        setBatches(batches.filter((_, idx) => idx !== i))
                      }
                      aria-label={`Borrar lote ${b.productBarcode}`}
                    >
                      Borrar
                    </button>
                    <button
                      className="action-btn-blue"
                      onClick={() => editBatch(i)}
                      aria-label={`Editar lote ${b.productBarcode}`}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="form-footer">
          <button className="action-btn-red" onClick={onClose}>
            Descartar
          </button>
          <button
            className="action-btn-green"
            onClick={() => onSave({ ...invoice, batches })}
            aria-label="Guardar cambios de la factura"
          >
            Guardar Cambios
          </button>
        </div>
      </div>
    </div>
  );
};

export default ImportInvoiceCSV;
