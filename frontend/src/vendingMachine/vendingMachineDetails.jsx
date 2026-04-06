import { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { TransformComponent, TransformWrapper } from "react-zoom-pan-pinch";
import Modal from "../components/Modal";
import "../global-form.css";
import "../global-list.css";

const EyeIcon = ({ visible }) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width="20"
    height="20"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {visible ? (
      <>
        <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19C7 19 2.73 15.11 1 12c.74-1.32 1.81-2.87 3.11-4.19M9.53 9.53A3.5 3.5 0 0 1 12 8.5c1.93 0 3.5 1.57 3.5 3.5 0 .47-.09.92-.26 1.33M14.47 14.47A3.5 3.5 0 0 1 12 15.5c-1.93 0-3.5-1.57-3.5-3.5 0-.47.09-.92.26-1.33" />
        <line x1="1" y1="1" x2="23" y2="23" />
      </>
    ) : (
      <>
        <path d="M1 12S5 5 12 5s11 7 11 7-4 7-11 7S1 12 1 12z" />
        <circle cx="12" cy="12" r="3" />
      </>
    )}
  </svg>
);

const VendingMachineDetail = () => {
  const { id } = useParams();
  const token = localStorage.getItem("accessToken");

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
          <h3 className="checkbox-label-text" style={{ marginBottom: "20px" }}>
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
