import { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const ImportSaleCSV = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [importedSales, setImportedSales] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append("csv", file);

    setLoading(true);
    setError("");

    try {
      const response = await fetch("/api/sales/csv", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });

      if (response.ok) {
        const data = await response.json();
        const salesToProcess = Array.isArray(data) ? data : [data];

        setImportedSales((prevSales) => {
          if (!prevSales || prevSales.length === 0) {
            return salesToProcess;
          }

          const updatedList = [...prevSales];

          salesToProcess.forEach((sale) => {
            if (!sale.saleDate || !sale.machineName) return;

            const existingIndex = updatedList.findIndex(
              (sale) =>
                sale.saleDate === sale.saleDate &&
                sale.machineName === sale.machineName,
            );

            if (existingIndex !== -1) {
              updatedList[existingIndex] = sale;
            } else {
              updatedList.push(sale);
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

  const saveEditedSale = (updatedSale) => {
    const newSales = [...importedSales];
    newSales[editingIndex] = updatedSale;
    setImportedSales(newSales);
    setIsModalOpen(false);
  };

  const getColumnLabel = (index) => {
    if (index < 0) return "";
    let current = index;
    let label = "";
    while (current >= 0) {
      label = String.fromCharCode((current % 26) + 65) + label;
      current = Math.floor(current / 26) - 1;
    }
    return label;
  };

  const handleFinalConfirm = async () => {
    setLoading(true);
    setError("");

    const currentSales = [...importedSales];
    const remainingSales = [];

    for (let i = 0; i < currentSales.length; i++) {
      const sale = currentSales[i];

      try {
        const response = await fetch("/api/sales", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(sale),
        });

        if (response.ok) {
          console.log(
            `Venta en ${sale.machineName} en la fecha ${sale.saleDate} creada.`,
          );
        } else if (response.status === 404) {
          let backendMessage = "";
          try {
            const errorBody = await response.json();
            backendMessage = (errorBody?.message || "").toString();
          } catch {
            backendMessage = (await response.text()) || "";
          }

          const normalized = backendMessage.toLowerCase();
          if (
            normalized.includes("product") ||
            normalized.includes("barcode")
          ) {
            sale.statusMessage =
              "No se encuentra el producto.";
          } else if (
            normalized.includes("vending machine") ||
            normalized.includes("machine")
          ) {
            sale.statusMessage = "No se encuentra la maquina expendedora.";
          } else if (
            normalized.includes("vending slot") ||
            normalized.includes("slot")
          ) {
            sale.statusMessage =
              "No se encuentra la ranura (fila/columna) en la maquina indicada.";
          } else {
            sale.statusMessage =
              backendMessage ||
              "No se encontro el recurso para crear la venta.";
          }

          remainingSales.push(sale);
        } else {
          sale.statusMessage = "Error al crear: " + (await response.text());
          remainingSales.push(sale);
        }
      } catch (err) {
        console.error(
          `Error al crear venta en ${sale.machineName} en la fecha ${sale.saleDate}:`,
          err,
        );
        sale.statusMessage = "Error de conexión.";
        remainingSales.push(sale);
      }
    }
    setImportedSales(remainingSales);
    setLoading(false);

    if (remainingSales.length === 0) {
      alert("¡Todas las ventas se crearon con éxito!");
      navigate("/sales");
    } else {
      setError(
        "Se procesaron las ventas. Revisa los mensajes de error en las que quedan en la lista.",
      );
    }
  };

  const deleteSale = (index) => {
    setImportedSales((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1100px" }}>
        <h1 className="section-title">Importar Ventas (CSV)</h1>
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
            ORDEN: saleDate, totalAmount, paymentMethod, status, barcode,
            machineName, rowNumber, columnNumber
          </p>
        </div>

        {importedSales.length > 0 && (
          <section
            style={{ marginTop: "40px" }}
            aria-labelledby="preview-heading"
          >
            <h2 id="preview-heading" className="section-label">
              Previsualización de Ventas
            </h2>
            <div className="table-responsive">
              <table
                className="dark-table"
                aria-label="Ventas importadas listas para procesar"
              >
                <thead>
                  <tr>
                    <th scope="col">Fecha</th>
                    <th scope="col">Importe total</th>
                    <th scope="col">Método de pago</th>
                    <th scope="col" style={{ textAlign: "center" }}>Estado</th>
                    <th scope="col">Código de barras</th>
                    <th scope="col">Nombre máquina</th>
                    <th scope="col">Ranura</th>
                    <th scope="col">Estado/Mensaje</th>
                    <th scope="col" style={{ textAlign: "center" }}>
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {importedSales.map((sale, idx) => (
                    <tr
                      key={idx}
                      style={
                        sale.statusMessage
                          ? { background: "rgba(220, 38, 38, 0.1)" }
                          : {}
                      }
                    >
                      <td>
                        <strong>{sale.saleDate}</strong>
                      </td>
                      <td>{sale.totalAmount}</td>
                      <td>{sale.paymentMethod}</td>
                      <td>
                        <span
                          className={`badge ${sale.status === "SUCCESS" ? "badge-green" : "badge-red"}`}
                          style={{
                            color:
                              sale.status === "SUCCESS"
                                ? "var(--primary-green)"
                                : "var(--primary-red)",
                            fontWeight: "bold",
                          }}
                        >
                          {sale.status === "SUCCESS" ? "EXITOSA" : "FALLIDA"}
                        </span>
                      </td>
                      <td>{sale.barcode}</td>
                      <td>{sale.machineName}</td>
                      <td style={{ textAlign: "center" }}>
                        {getColumnLabel(sale.rowNumber - 1)}
                        {sale.columnNumber}
                      </td>
                      <td
                        style={{
                          fontSize: "0.85rem",
                          color: sale.statusMessage?.includes("existe")
                            ? "var(--primary-orange)"
                            : "var(--primary-red)",
                        }}
                      >
                        {sale.statusMessage || "Pendiente de confirmación"}
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
                          onClick={() => deleteSale(idx)}
                          aria-label={`Borrar venta en ${sale.machineName} en la fecha ${sale.saleDate}`}
                        >
                          Borrar
                        </button>
                        <button
                          className="action-btn-blue"
                          onClick={() => {
                            setEditingIndex(idx);
                            setIsModalOpen(true);
                          }}
                          aria-label={`Editar venta en ${sale.machineName} en la fecha ${sale.saleDate}`}
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
                onClick={() => navigate("/sales")}
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
                  : `Confirmar Importación (${importedSales.length})`}
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
        <EditSaleModal
          saleData={importedSales[editingIndex]}
          onClose={() => setIsModalOpen(false)}
          onSave={saveEditedSale}
        />
      )}
    </main>
  );
};

const EditSaleModal = ({ saleData, onClose, onSave }) => {
  const [sale, setSale] = useState(() => {
    const barcode = saleData.barcode ?? "";
    return {
      ...saleData,
      barcode,
      saleDate: saleData.saleDate ? saleData.saleDate.slice(0, 16) : "",
    };
  });

  const handleSave = () => {
    onSave({
      ...sale,
      barcode: sale.barcode,
    });
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
          maxWidth: "800px",
          maxHeight: "90vh",
          overflowY: "auto",
        }}
      >
        <h2 className="section-title" id="edit-modal-title">
          Editando Venta
        </h2>

        <div className="form-grid">
          <div className="input-group">
            <label htmlFor="modal-sale-date" className="input-label">
              Fecha y Hora
            </label>
            <input
              id="modal-sale-date"
              type="datetime-local"
              className="dark-input"
              value={sale.saleDate || ""}
              onChange={(e) => setSale({ ...sale, saleDate: e.target.value })}
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-total-amount" className="input-label">
              Importe Total
            </label>
            <input
              id="modal-total-amount"
              type="number"
              step="0.01"
              min="0.01"
              className="dark-input"
              value={sale.totalAmount ?? ""}
              onChange={(e) =>
                setSale({ ...sale, totalAmount: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-payment-method" className="input-label">
              Método de Pago
            </label>
            <select
              id="modal-payment-method"
              className="dark-input"
              value={sale.paymentMethod || ""}
              onChange={(e) =>
                setSale({ ...sale, paymentMethod: e.target.value })
              }
            >
              <option value="">Seleccionar</option>
              <option value="CASH">CASH</option>
              <option value="CREDIT_CARD">CREDIT_CARD</option>
            </select>
          </div>
          <div className="input-group">
            <label htmlFor="modal-status" className="input-label">
              Estado
            </label>
            <select
              id="modal-status"
              className="dark-input"
              value={sale.status || ""}
              onChange={(e) => setSale({ ...sale, status: e.target.value })}
            >
              <option value="">Seleccionar</option>
              <option value="SUCCESS">Exitosa</option>
              <option value="FAILED">Fallida</option>
            </select>
          </div>
          <div className="input-group">
            <label htmlFor="modal-barcode" className="input-label">
              Código de Barras
            </label>
            <input
              id="modal-barcode"
              className="dark-input"
              value={sale.barcode || ""}
              onChange={(e) =>
                setSale({
                  ...sale,
                  barcode: e.target.value,
                })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-machine-name" className="input-label">
              Nombre de Máquina
            </label>
            <input
              id="modal-machine-name"
              className="dark-input"
              value={sale.machineName || ""}
              onChange={(e) =>
                setSale({ ...sale, machineName: e.target.value })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-row-number" className="input-label">
              Fila
            </label>
            <input
              id="modal-row-number"
              type="number"
              min="1"
              className="dark-input"
              value={sale.rowNumber ?? ""}
              onChange={(e) =>
                setSale({ ...sale, rowNumber: Number(e.target.value) })
              }
            />
          </div>
          <div className="input-group">
            <label htmlFor="modal-column-number" className="input-label">
              Columna
            </label>
            <input
              id="modal-column-number"
              type="number"
              min="1"
              className="dark-input"
              value={sale.columnNumber ?? ""}
              onChange={(e) =>
                setSale({ ...sale, columnNumber: Number(e.target.value) })
              }
            />
          </div>
        </div>

        <div className="form-footer">
          <button className="action-btn-red" onClick={onClose}>
            Descartar
          </button>
          <button
            className="action-btn-green"
            onClick={handleSave}
            aria-label="Guardar cambios de la venta"
          >
            Guardar Cambios
          </button>
        </div>
      </div>
    </div>
  );
};

export default ImportSaleCSV;
