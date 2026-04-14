import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { TransformComponent, TransformWrapper } from "react-zoom-pan-pinch";
import EyeIcon from "../components/EyeIcon";
import Modal from "../components/Modal";
import "../global-form.css";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const VendingMachineDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [machine, setMachine] = useState(null);
  const [slots, setSlots] = useState([]);
  const [expirations, setExpirations] = useState({});
  const [isEditingLocation, setIsEditingLocation] = useState(false);
  const [locationDraft, setLocationDraft] = useState("");
  const [isSavingLocation, setIsSavingLocation] = useState(false);
  const [showExpirationModal, setShowExpirationModal] = useState(false);
  const [selectedSlotForModal, setSelectedSlotForModal] = useState(null);
  const [selectedSlotBatches, setSelectedSlotBatches] = useState([]);
  const [isLoadingModalBatches, setIsLoadingModalBatches] = useState(false);
  const [expirationBatchesBySlot, setExpirationBatchesBySlot] = useState({});
  const [loading, setLoading] = useState(true);

  const [showAssignModal, setShowAssignModal] = useState(false);
  const [assignSlot, setAssignSlot] = useState(null);
  const [searchName, setSearchName] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [isAssigning, setIsAssigning] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [message, setMessage] = useState({ text: "", type: "" });

  const formatDate = (dateString) => {
    if (!dateString) return "N/A";
    const date = new Date(dateString);
    return date.toLocaleDateString("es-ES", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const fetchAllExpirations = useCallback(async (slotsData, headers) => {
    const expirationMap = {};
    const promises = slotsData
      .filter((slot) => slot.product)
      .map(async (slot) => {
        try {
          const res = await fetch(
            `/api/vending-slots/${slot.id}/expiration-batches`,
            { headers },
          );
          if (res.ok) {
            const batches = await res.json();
            if (Array.isArray(batches) && batches.length > 0) {
              const sorted = batches.sort(
                (a, b) =>
                  new Date(a.expirationDate) - new Date(b.expirationDate),
              );
              expirationMap[slot.id] = sorted[0].expirationDate;
            }
          }
        } catch (e) {
          console.error(`Error buscando expiración para slot ${slot.id}`, e);
        }
      });
    await Promise.all(promises);
    setExpirations((prev) => ({ ...prev, ...expirationMap }));
  }, []);

  const fetchData = useCallback(async () => {
    try {
      const headers = { Authorization: `Bearer ${token}` };
      const [mRes, sRes] = await Promise.all([
        fetch(`/api/vending-machines/${id}`, { headers }),
        fetch(`/api/vending-slots/vending-machines/${id}`, { headers }),
      ]);
      if (mRes.ok && sRes.ok) {
        const machineData = await mRes.json();
        const slotsData = await sRes.json();
        setMachine(machineData);
        setLocationDraft(machineData.location ?? "");
        setSlots(slotsData);
        fetchAllExpirations(slotsData, headers);
      }
    } catch (err) {
      console.error("Error cargando máquina", err);
    } finally {
      setLoading(false);
    }
  }, [id, token, fetchAllExpirations]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    if (!showAssignModal) return;
    const timer = setTimeout(async () => {
      try {
        const params = new URLSearchParams({ name: searchName, size: 50 });
        const res = await fetch(`/api/products?${params}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setSearchResults(data.content || []);
        }
      } catch (err) {
        console.error(err);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchName, showAssignModal, token]);

  if (loading || !machine)
    return <div className="form-container">Cargando...</div>;

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
  const maxSlotColumn = slots.length
    ? Math.max(...slots.map((s) => s.columnNumber))
    : machine.columnCount - 1;
  const maxSlotRow = slots.length
    ? Math.max(...slots.map((s) => s.rowNumber))
    : machine.rowCount - 1;
  const isOneBasedColumns = maxSlotColumn >= machine.columnCount;
  const isOneBasedRows = maxSlotRow >= machine.rowCount;
  const normalizeColumn = (value) => (isOneBasedColumns ? value - 1 : value);
  const normalizeRow = (value) => (isOneBasedRows ? value - 1 : value);
  const getSlotLabel = (slot) =>
    `${getColumnLabel(normalizeColumn(slot.columnNumber))}${normalizeRow(slot.rowNumber) + 1}`;

  const startLocationEdit = () => {
    setLocationDraft(machine.location ?? "");
    setIsEditingLocation(true);
  };
  const cancelLocationEdit = () => {
    setLocationDraft(machine.location ?? "");
    setIsEditingLocation(false);
  };

  const confirmLocationUpdate = async () => {
    const trimmedLocation = locationDraft.trim();
    if (!trimmedLocation) {
      alert("La ubicación no puede estar vacía.");
      return;
    }

    try {
      setIsSavingLocation(true);
      const response = await fetch(`/api/vending-machines/${id}`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ location: trimmedLocation }),
      });

      if (!response.ok) {
        throw new Error("No se pudo actualizar la ubicación de la máquina.");
      }

      const updatedMachine = await response.json();
      setMachine(updatedMachine);
      setLocationDraft(updatedMachine.location ?? trimmedLocation);
      setIsEditingLocation(false);
    } catch (error) {
      console.error("Error actualizando ubicación", error);
      alert("No se pudo actualizar la ubicación. Inténtalo de nuevo.");
    } finally {
      setIsSavingLocation(false);
    }
  };

  const openAssignModal = (slot) => {
    setAssignSlot(slot);
    setSelectedProduct(slot.product || null);
    setSearchName("");
    setErrorMsg("");
    setShowAssignModal(true);
  };

  const handleAssignConfirm = async () => {
    setErrorMsg("");
    try {
      setIsAssigning(true);
      const barcodeParam = selectedProduct
        ? `?barcode=${selectedProduct.barcode}`
        : "";
      const response = await fetch(
        `/api/vending-slots/${assignSlot.id}/assign-or-unassign-product${barcodeParam}`,
        {
          method: "PATCH",
          headers: { Authorization: `Bearer ${token}` },
        },
      );
      if (response.ok) {
        setShowAssignModal(false);
        fetchData();
      } else {
        const errorData = await response.json();
        setErrorMsg(
          errorData.message ||
            "Error al asignar el producto. Inténtalo de nuevo.",
        );
      }
    } catch (error) {
      setErrorMsg("Error de red al asignar el producto. Inténtalo de nuevo.");
      console.error(error);
    } finally {
      setIsAssigning(false);
    }
  };

  const handleRealTimeSale = async (slotId) => {
    setErrorMsg("");
    setIsAssigning(true);
    try {
      const response = await fetch("/api/sales/real-time", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          vendingSlotId: slotId,
          paymentMethod: "CREDIT_CARD",
        }),
      });

      let saleData = null;
      try {
        saleData = await response.json();
      } catch {
        saleData = null;
      }

      if (response.ok) {
        const saleStatus =
          typeof saleData?.status === "string"
            ? saleData.status.toUpperCase()
            : "";

        if (saleStatus === "SUCCESS") {
          setShowAssignModal(false);
          setMessage({
            text: `Venta realizada con éxito de la ranura ${getSlotLabel(assignSlot)}: ${saleData.productName}`,
            type: "success",
          });
          fetchData();
          setTimeout(() => setMessage({ text: "", type: "" }), 5000);
        } else if (saleStatus === "FAILED") {
          setShowAssignModal(false);
          const reason =
            saleData?.failureReason || "La operación no pudo completarse.";
          setMessage({
            text: `Se ha registrado una venta que ha fallado por: ${reason}`,
            type: "error",
          });
          fetchData();
          setTimeout(() => setMessage({ text: "", type: "" }), 5000);
        } else {
          setErrorMsg("Respuesta de venta no reconocida.");
        }
      } else {
        setErrorMsg(saleData?.message || "Error al procesar la venta");
      }
    } catch (error) {
      setErrorMsg("Error de red al procesar la venta");
      console.error(error);
    } finally {
      setIsAssigning(false);
    }
  };

  const handleToggleBlock = async () => {
    if (!assignSlot) return;

    setErrorMsg("");
    setIsAssigning(true);
    const shouldBlock = !assignSlot.isBlocked;

    try {
      const response = await fetch(
        `/api/vending-slots/${assignSlot.id}/block-or-unblock?blocked=${shouldBlock}`,
        {
          method: "PATCH",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      let responseData = null;
      try {
        responseData = await response.json();
      } catch {
        responseData = null;
      }

      if (response.ok) {
        setAssignSlot(responseData);
        setMessage({
          text: shouldBlock
            ? `Ranura ${getSlotLabel(assignSlot)} bloqueada correctamente.`
            : `Ranura ${getSlotLabel(assignSlot)} desbloqueada correctamente.`,
          type: "success",
        });
        fetchData();
        setTimeout(() => setMessage({ text: "", type: "" }), 5000);
      } else {
        setErrorMsg(
          responseData?.message ||
            "No se pudo actualizar el estado de bloqueo de la ranura.",
        );
      }
    } catch (error) {
      setErrorMsg("Error de red al actualizar el bloqueo de la ranura.");
      console.error(error);
    } finally {
      setIsAssigning(false);
    }
  };

  const openExpirationModal = async (slot) => {
    setSelectedSlotForModal(slot);
    setShowExpirationModal(true);
    if (expirationBatchesBySlot[slot.id]) {
      setSelectedSlotBatches(expirationBatchesBySlot[slot.id]);
      return;
    }
    try {
      setIsLoadingModalBatches(true);
      const response = await fetch(
        `/api/vending-slots/${slot.id}/expiration-batches`,
        {
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (!response.ok) {
        throw new Error("No se pudieron cargar los lotes de caducidad.");
      }

      const data = await response.json();
      const batches = Array.isArray(data) ? data : [];
      setExpirationBatchesBySlot((prev) => ({ ...prev, [slot.id]: batches }));
      setSelectedSlotBatches(batches);
    } catch (error) {
      console.error("Error cargando lotes de caducidad", error);
      setSelectedSlotBatches([]);
    } finally {
      setIsLoadingModalBatches(false);
    }
  };

  return (
    <main className="home-container" style={{ maxWidth: "1600px" }} role="main">
      <div style={{ width: "100%", maxWidth: "1400px", margin: "0 auto" }}>
        <div
          role={message.type === "error" ? "alert" : "status"}
          aria-live={message.type === "error" ? "assertive" : "polite"}
          id="system-message"
          className={
            message.text
              ? message.type === "error"
                ? "message-error"
                : "message-success"
              : ""
          }
          style={{
            marginBottom: "20px",
            width: "100%",
            fontWeight: "600",
            fontSize: "0.95rem",
            textAlign: "center",
            whiteSpace: "normal",
            overflowWrap: "anywhere",
            display: message.text ? "block" : "none",
            padding: message.text ? "12px" : "0",
            minHeight: message.text ? "auto" : "0",
          }}
        >
          {message.text}
        </div>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1.2fr 0.8fr",
            gap: "25px",
            alignItems: "start",
          }}
        >
          <section
            className="form-container"
            aria-label="Representación visual de la máquina"
            style={{
              height: "700px",
              padding: 0,
              overflow: "hidden",
              position: "relative",
              backgroundColor: "var(--text-muted)",
            }}
          >
            <div
              style={{
                position: "absolute",
                top: "10px",
                left: "10px",
                zIndex: 10,
                fontSize: "0.7rem",
                color: "var(--bg-card)",
                fontWeight: "800",
              }}
            >
              Usa el ratón para moverte y la rueda para Zoom
            </div>
            <TransformWrapper
              centerOnInit
              initialScale={2}
              minScale={0.5}
              maxScale={3}
            >
              <TransformComponent
                wrapperStyle={{ width: "100%", height: "100%" }}
              >
                <div
                  role="grid"
                  aria-label={`Esquema de la máquina con ${machine.rowCount} filas y ${machine.columnCount} columnas`}
                  style={{
                    padding: "60px",
                    background: "var(--bg-card)",
                    borderRadius: "20px",
                    boxShadow: "0 20px 50px rgba(0,0,0,0.5)",
                    display: "grid",
                    gridTemplateColumns: `repeat(${machine.columnCount}, 120px)`,
                    gap: "15px",
                    border: "15px solid var(--bg-dark)",
                  }}
                >
                  {Array.from({ length: machine.rowCount }).map((_, r) =>
                    Array.from({ length: machine.columnCount }).map((_, c) => {
                      const slot = slots.find(
                        (s) =>
                          normalizeRow(s.rowNumber) === r &&
                          normalizeColumn(s.columnNumber) === c,
                      );
                      const slotStatusColor = slot?.isBlocked
                        ? "#b91c1c"
                        : slot?.product && slot.currentStock > 0
                          ? "#166534"
                          : slot?.product
                            ? "#bb4e00"
                            : "#1d4ed8";
                      const slotStatusLabel = slot?.isBlocked
                        ? "Bloqueada"
                        : slot?.product && slot.currentStock > 0
                          ? "Disponible"
                          : slot?.product
                            ? "Sin stock"
                            : "Sin producto asignado";

                      const label = `${getColumnLabel(c)}${r + 1}`;
                      return (
                        <button
                          key={`${r}-${c}`}
                          role="gridcell"
                          aria-label={`Ranura ${label}, estado: ${slotStatusLabel}. ${slot?.product ? `Producto: ${slot.product.name}, Stock: ${slot.currentStock}` : ""}`}
                          onClick={() => slot && openAssignModal(slot)}
                          style={{
                            width: "120px",
                            height: "85px",
                            background: "var(--text-muted)",
                            borderRadius: "8px",
                            display: "flex",
                            flexDirection: "column",
                            padding: "8px",
                            boxShadow: "inset 0 2px 4px rgba(0,0,0,0.2)",
                            cursor: "pointer",
                            border: "none",
                            textAlign: "left",
                          }}
                        >
                          <span
                            style={{
                              fontSize: "0.7rem",
                              fontWeight: "800",
                              color: "var(--bg-card)",
                            }}
                          >
                            {label}
                          </span>
                          {slot?.product && (
                            <div
                              style={{
                                marginTop: "5px",
                                fontSize: "0.65rem",
                                fontWeight: "700",
                                color: "var(--bg-dark)",
                                textAlign: "center",
                              }}
                            >
                              {slot.product.name.substring(0, 15)}
                            </div>
                          )}
                          <div style={{ marginTop: "auto" }}>
                            <div
                              style={{
                                fontSize: "0.55rem",
                                color: slotStatusColor,
                                fontWeight: "700",
                                textAlign: "center",
                                marginBottom: "3px",
                              }}
                            >
                              {slotStatusLabel}
                            </div>
                            <div
                              style={{
                                height: "4px",
                                background: "rgba(0,0,0,0.2)",
                                borderRadius: "2px",
                                marginBottom: "2px",
                              }}
                            >
                              <div
                                style={{
                                  height: "100%",
                                  background: slotStatusColor,
                                  width: "100%",
                                }}
                              />
                            </div>
                          </div>
                        </button>
                      );
                    }),
                  )}
                </div>
              </TransformComponent>
            </TransformWrapper>
          </section>
          <section
            className="form-container"
            style={{ padding: "25px" }}
            aria-labelledby="info-title"
          >
            <h2
              id="info-title"
              className="checkbox-label-text"
              style={{ marginBottom: "0px", fontSize: "1.2rem" }}
            >
              Información de la Máquina:
            </h2>
            <h3
              className="checkbox-label-text"
              style={{ marginBottom: "20px" }}
            >
              <span style={{ color: "var(--text-muted)", fontWeight: "700" }}>
                {machine.name}
              </span>
            </h3>
            <div className="input-group" style={{ marginBottom: "15px" }}>
              <label htmlFor="machine-loc" className="input-label">
                Ubicación
              </label>
              <input
                id="machine-loc"
                className="dark-input"
                readOnly={!isEditingLocation}
                value={isEditingLocation ? locationDraft : machine.location}
                onChange={(e) => setLocationDraft(e.target.value)}
              />
              <div style={{ marginTop: "10px", display: "flex", gap: "10px" }}>
                {!isEditingLocation ? (
                  <button
                    type="button"
                    className="btn-primary"
                    onClick={startLocationEdit}
                    style={{ width: "100%" }}
                  >
                    Actualizar ubicación
                  </button>
                ) : (
                  <>
                    <button
                      type="button"
                      className="action-btn-orange"
                      onClick={cancelLocationEdit}
                      disabled={isSavingLocation}
                      style={{ flex: 1 }}
                    >
                      Cancelar
                    </button>
                    <button
                      type="button"
                      className="action-btn-green"
                      onClick={confirmLocationUpdate}
                      disabled={isSavingLocation}
                      style={{ flex: 1 }}
                    >
                      {isSavingLocation ? "Guardando..." : "Confirmar"}
                    </button>
                  </>
                )}
              </div>
            </div>
            <div
              className="form-grid"
              style={{ gridTemplateColumns: "1fr 1fr", gap: "10px" }}
            >
              <div className="input-group">
                <label htmlFor="machine-cols" className="input-label">
                  Columnas
                </label>
                <input
                  id="machine-cols"
                  className="dark-input"
                  readOnly
                  value={machine.columnCount}
                />
              </div>
              <div className="input-group">
                <label htmlFor="machine-rows" className="input-label">
                  Filas
                </label>
                <input
                  id="machine-rows"
                  className="dark-input"
                  readOnly
                  value={machine.rowCount}
                />
              </div>
            </div>
            <h3
              className="checkbox-label-text"
              style={{ margin: "25px 0 10px 0" }}
            >
              Stock y Productos Asignados
            </h3>
            <div className="table-responsive" style={{ maxHeight: "300px" }}>
              <table
                className="dark-table"
                style={{ fontSize: "0.8rem" }}
                aria-label="Productos asignados a ranuras"
              >
                <thead>
                  <tr>
                    <th scope="col">RANURA</th>
                    <th scope="col">PRODUCTO</th>
                    <th scope="col">STOCK</th>
                    <th scope="col" style={{ textAlign: "center" }}>
                      CADUCIDAD
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {slots
                    .filter((s) => s.product)
                    .map((slot) => (
                      <tr key={slot.id}>
                        <td>
                          <strong>
                            {getColumnLabel(normalizeColumn(slot.columnNumber))}
                            {normalizeRow(slot.rowNumber) + 1}
                          </strong>
                        </td>
                        <td>{slot.product.name}</td>
                        <td>
                          {slot.currentStock} / {slot.maxCapacity}
                        </td>
                        <td style={{ textAlign: "center" }}>
                          {expirations[slot.id] ? (
                            <button
                              type="button"
                              className="view-desc-btn"
                              onClick={() => openExpirationModal(slot)}
                              aria-label={`Ver lotes de caducidad para ranura ${getColumnLabel(normalizeColumn(slot.columnNumber))}${normalizeRow(slot.rowNumber) + 1}`}
                            >
                              <EyeIcon visible={false} />
                            </button>
                          ) : (
                            <span
                              style={{
                                color: "var(--text-muted)",
                                fontWeight: "700",
                              }}
                            >
                              N/A
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>

      {showAssignModal && (
        <Modal onClose={() => setShowAssignModal(false)}>
          <div
            style={{ padding: "10px", minWidth: "400px" }}
            role="dialog"
            aria-labelledby="modal-assign-title"
          >
            <h2
              id="modal-assign-title"
              className="section-label"
              style={{ marginBottom: "15px" }}
            >
              Asignar Producto a Ranura {getSlotLabel(assignSlot)}
            </h2>
            <div className="input-group" style={{ marginBottom: "15px" }}>
              <label
                htmlFor="product-search"
                className="sr-only"
                style={{ display: "none" }}
              >
                Buscar producto
              </label>
              <input
                id="product-search"
                className="dark-input"
                placeholder="Buscar producto por nombre o código..."
                value={searchName}
                onChange={(e) => setSearchName(e.target.value)}
                aria-controls="search-results-list"
                aria-haspopup="listbox"
              />
            </div>
            <div
              id="search-results-list"
              role="listbox"
              aria-label="Resultados de búsqueda de productos"
              style={{
                maxHeight: "200px",
                overflowY: "auto",
                border: "1px solid var(--border-color)",
                borderRadius: "8px",
                marginBottom: "15px",
              }}
            >
              {searchResults.length > 0 ? (
                searchResults.map((p) => (
                  <div
                    key={p.id}
                    role="option"
                    aria-selected={selectedProduct?.id === p.id}
                    tabIndex="0"
                    onClick={() => {
                      setSelectedProduct(p);
                      setErrorMsg("");
                    }}
                    onKeyDown={(e) => {
                      if (e.key === "Enter" || e.key === " ") {
                        e.preventDefault();
                        setSelectedProduct(p);
                      }
                    }}
                    style={{
                      padding: "10px",
                      cursor: "pointer",
                      background:
                        selectedProduct?.id === p.id
                          ? "rgba(56, 189, 248, 0.2)"
                          : "transparent",
                      borderBottom: "1px solid var(--border-color)",
                      outline: "none",
                    }}
                  >
                    <div
                      style={{ color: "var(--text-main)", fontWeight: "600" }}
                    >
                      {p.name}
                    </div>
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: "var(--text-muted)",
                      }}
                    >
                      {p.brand} - {p.barcode}
                    </div>
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: "var(--text-muted)",
                      }}
                    >
                      {p.isCustom ? "Propio" : "Catálogo"}
                    </div>
                  </div>
                ))
              ) : (
                <div
                  style={{
                    padding: "10px",
                    color: "var(--text-muted)",
                    textAlign: "center",
                  }}
                >
                  No hay resultados
                </div>
              )}
            </div>
            <div
              aria-live="polite"
              style={{
                textAlign: "center",
                marginBottom: "15px",
                padding: "10px",
                background: "var(--bg-dark)",
                borderRadius: "8px",
              }}
            >
              <span
                className="input-label"
                style={{ fontSize: "0.7rem", display: "block" }}
              >
                Producto Seleccionado:
              </span>
              <div
                style={{
                  color: "var(--primary-blue)",
                  fontWeight: "700",
                  margin: "4px 0",
                }}
              >
                {selectedProduct ? selectedProduct.name : "Ninguno"}
              </div>
              {selectedProduct && (
                <button
                  className="action-btn-orange"
                  style={{
                    height: "28px",
                    fontSize: "0.85rem",
                    margin: "8px auto 0",
                    display: "flex",
                    alignItems: "center",
                  }}
                  onClick={() => setSelectedProduct(null)}
                  aria-label="Desmarcar producto seleccionado"
                >
                  Desmarcar
                </button>
              )}
            </div>
            {errorMsg && (
              <div
                role="alert"
                className="message-error"
                style={{ marginBottom: "15px", fontSize: "0.85rem" }}
              >
                {errorMsg}
              </div>
            )}

            <div
              style={{ display: "flex", flexDirection: "column", gap: "10px" }}
            >
              <button
                className="action-btn-green"
                style={{ width: "100%" }}
                onClick={handleAssignConfirm}
                disabled={
                  isAssigning || (!selectedProduct && !assignSlot?.product)
                }
                aria-busy={isAssigning}
              >
                {isAssigning ? "Asignando..." : "Confirmar Asignación"}
              </button>

              {assignSlot?.product && (
                <button
                  className="action-btn-blue"
                  style={{ width: "100%" }}
                  onClick={() => handleRealTimeSale(assignSlot.id)}
                  disabled={isAssigning || assignSlot?.isBlocked}
                >
                  Efectuar Venta Manual
                </button>
              )}
              <button
                type="button"
                className={
                  assignSlot?.isBlocked
                    ? "action-btn-green"
                    : "action-btn-red"
                }
                style={{ width: "100%" }}
                onClick={handleToggleBlock}
                disabled={isAssigning}
              >
                {isAssigning
                  ? "Actualizando..."
                  : assignSlot?.isBlocked
                    ? "Desbloquear Ranura"
                    : "Bloquear Ranura"}
              </button>
              <button
                type="button"
                className="btn-secondary"
                style={{ width: "100%" }}
                onClick={() => setShowAssignModal(false)}
              >
                Cancelar
              </button>
            </div>
            <div
              style={{
                textAlign: "center",
                marginTop: "15px",
                fontSize: "0.8rem",
                color: "var(--text-muted)",
                fontWeight: "700",
                textTransform: "uppercase",
              }}
            >
              Ubicación: RANURA {getSlotLabel(assignSlot)}
            </div>
          </div>
        </Modal>
      )}

      {showExpirationModal && selectedSlotForModal && (
        <Modal
          onClose={() => {
            setShowExpirationModal(false);
            setSelectedSlotForModal(null);
            setSelectedSlotBatches([]);
          }}
        >
          <h2
            id="expiration-modal-title"
            className="section-title"
            style={{ marginBottom: "14px" }}
          >
            Lotes de Caducidad - Ranura {getSlotLabel(selectedSlotForModal)}
          </h2>

          {isLoadingModalBatches ? (
            <p role="status" className="message-success">
              Cargando lotes...
            </p>
          ) : selectedSlotBatches.length === 0 ? (
            <p role="status" className="message-error">
              No hay lotes para esta ranura.
            </p>
          ) : (
            <div className="table-responsive modal-table-wrapper">
              <table
                className="dark-table modal-table"
                aria-labelledby="expiration-modal-title"
              >
                <thead>
                  <tr>
                    <th scope="col">CANTIDAD</th>
                    <th scope="col">FECHA DE CADUCIDAD</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedSlotBatches.map((batch) => (
                    <tr key={batch.id}>
                      <td>{batch.quantity}</td>
                      <td>{formatDate(batch.expirationDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <div className="list-footer modal-footer">
            <button
              type="button"
              className="btn-secondary"
              onClick={() => {
                setShowExpirationModal(false);
                setSelectedSlotForModal(null);
                setSelectedSlotBatches([]);
              }}
            >
              Cerrar
            </button>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default VendingMachineDetail;
