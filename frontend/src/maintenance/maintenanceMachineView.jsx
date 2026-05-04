import { useCallback, useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { TransformComponent, TransformWrapper } from "react-zoom-pan-pinch";
import Modal from "../components/Modal";
import "../global-form.css";
import "../global-list.css";
import { useAuthenticatedUser } from "../hooks/useAuthenticatedUser";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const MaintenanceMachineView = () => {
  const { id } = useParams();
  const [searchParams] = useSearchParams();
  const maintenanceId = searchParams.get("maintenanceId");
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);
  const { user } = useAuthenticatedUser();

  const [maintenance, setMaintenance] = useState(null);
  const [machine, setMachine] = useState(null);
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedSlotDetail, setSelectedSlotDetail] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [statusError, setStatusError] = useState("");
  const [statusLoading, setStatusLoading] = useState(false);
  const [showCreateDetailForm, setShowCreateDetailForm] = useState(false);
  const [createDetailForm, setCreateDetailForm] = useState({
    quantityToRestock: "",
    expirationDate: "",
  });
  const [creatingDetail, setCreatingDetail] = useState(false);
  const [detailError, setDetailError] = useState("");

  const fetchData = useCallback(async () => {
    try {
      const headers = { Authorization: `Bearer ${token}` };
      const [mtRes, mRes, sRes] = await Promise.all([
        fetch(`/api/maintenances/${maintenanceId}`, { headers }),
        fetch(`/api/vending-machines/${id}`, { headers }),
        fetch(`/api/vending-slots/vending-machines/${id}`, { headers }),
      ]);
      if (!mtRes.ok || !mRes.ok || !sRes.ok) throw new Error("Error cargando datos");
      setMaintenance(await mtRes.json());
      setMachine(await mRes.json());
      setSlots(await sRes.json());
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [id, maintenanceId, token]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleChangeStatus = async (newStatus) => {
    setStatusLoading(true);
    setStatusError("");
    try {
      const isDelete = newStatus === "DELETED";
      const url = isDelete
        ? `/api/maintenances/${maintenanceId}`
        : `/api/maintenances/${maintenanceId}/${newStatus.toLowerCase()}`;
      const response = await fetch(url, {
        method: isDelete ? "DELETE" : "PATCH",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "No se pudo cambiar el estado de la tarea");
      }
      if (isDelete) {
        navigate(-1);
      } else {
        setMaintenance(await response.json());
      }
    } catch (err) {
      setStatusError(err.message);
    } finally {
      setStatusLoading(false);
    }
  };

  const getStatusButtons = () => {
    if (!maintenance || !user) return [];
    const buttons = [];
    const isAdmin = user.role === "ADMINISTRATOR";
    const isMaintainer = user.role === "MAINTAINER";
    if (maintenance.status === "DRAFT") {
      buttons.push({ label: "Marcar como Pendiente", status: "PENDING", className: "btn-primary" });
      buttons.push({ label: "Eliminar", status: "DELETE", className: "action-btn-red" });
    } else if (maintenance.status === "PENDING" || maintenance.status === "DELAYED") {
      if (isMaintainer) {
        buttons.push({ label: "Marcar como Completada", status: "COMPLETED", className: "btn-primary" });
      }
      if (isAdmin) {
        buttons.push({ label: "Marcar como Cancelada", status: "CANCELED", className: "btn-secondary" });
      }
    }
    return buttons;
  };

  const handleCreateMaintenanceDetail = async (e) => {
    e.preventDefault();
    setDetailError("");
    setCreatingDetail(true);
    try {
      const payload = {
        rowNumber: selectedSlotDetail.slot.rowNumber,
        columnNumber: selectedSlotDetail.slot.columnNumber,
        barcode: selectedSlotDetail.slot.product.barcode,
        quantityToRestock: parseInt(createDetailForm.quantityToRestock, 10),
        expirationDate: createDetailForm.expirationDate,
      };
      const response = await fetch(`/api/maintenances/${maintenanceId}/details`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "No se pudo crear el detalle de mantenimiento");
      }
      const updatedMaintenance = await response.json();
      setMaintenance(updatedMaintenance);
      setSelectedSlotDetail({
        ...selectedSlotDetail,
        details: updatedMaintenance.maintenanceDetails.filter(
          (d) =>
            d.rowNumber === selectedSlotDetail.slot.rowNumber &&
            d.columnNumber === selectedSlotDetail.slot.columnNumber,
        ),
      });
      setCreateDetailForm({ quantityToRestock: "", expirationDate: "" });
      setShowCreateDetailForm(false);
    } catch (err) {
      setDetailError(err.message);
    } finally {
      setCreatingDetail(false);
    }
  };

  const handleDeleteMaintenanceDetail = async (detailId) => {
    if (!window.confirm("¿Estás seguro de que deseas eliminar este detalle de mantenimiento?")) return;
    setCreatingDetail(true);
    setDetailError("");
    try {
      const response = await fetch(`/api/maintenances/${maintenanceId}/details/${detailId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "No se pudo eliminar el detalle de mantenimiento");
      }
      const updatedMaintenance = await response.json();
      setMaintenance(updatedMaintenance);
      setSelectedSlotDetail({
        ...selectedSlotDetail,
        details: updatedMaintenance.maintenanceDetails.filter(
          (d) =>
            d.rowNumber === selectedSlotDetail.slot.rowNumber &&
            d.columnNumber === selectedSlotDetail.slot.columnNumber,
        ),
      });
    } catch (err) {
      setDetailError(err.message);
    } finally {
      setCreatingDetail(false);
    }
  };

  if (loading || !machine || !maintenance)
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

  const maxSlotColumn = slots.length ? Math.max(...slots.map((s) => s.columnNumber)) : machine.columnCount - 1;
  const maxSlotRow = slots.length ? Math.max(...slots.map((s) => s.rowNumber)) : machine.rowCount - 1;
  const isOneBasedColumns = maxSlotColumn >= machine.columnCount;
  const isOneBasedRows = maxSlotRow >= machine.rowCount;
  const normalizeColumn = (value) => (isOneBasedColumns ? value - 1 : value);
  const normalizeRow = (value) => (isOneBasedRows ? value - 1 : value);
  const getSlotLabel = (slot) =>
    `${getColumnLabel(normalizeColumn(slot.columnNumber))}${normalizeRow(slot.rowNumber) + 1}`;

  const detailsMap = {};
  (maintenance.maintenanceDetails || []).forEach((d) => {
    const key = `${d.rowNumber}-${d.columnNumber}`;
    if (!detailsMap[key]) detailsMap[key] = [];
    detailsMap[key].push(d);
  });

  const getSlotColor = (slot) => {
    if (!slot?.product) return "#b91c1c";
    const key = `${slot.rowNumber}-${slot.columnNumber}`;
    const details = detailsMap[key] || [];
    if (details.length === 0) return "#166534";
    const totalRequested = details.reduce((sum, d) => sum + (d.quantityToRestock || 0), 0);
    const freeSpace = Math.max(slot.maxCapacity - slot.currentStock, 0);
    if (totalRequested >= freeSpace) return "#1d4ed8";
    return "#bb4e00";
  };

  const handleSlotClick = (slot) => {
    const key = `${slot.rowNumber}-${slot.columnNumber}`;
    const hasDetail = detailsMap[key]?.length > 0;
    const canAddDetails = maintenance.status === "DRAFT";
    if (hasDetail || canAddDetails) {
      setSelectedSlotDetail({ slot, details: detailsMap[key] || [] });
      setShowDetailModal(true);
      setShowCreateDetailForm(false);
      setDetailError("");
      setCreateDetailForm({ quantityToRestock: "", expirationDate: "" });
    }
  };

  const selectedSlotCurrentStock = selectedSlotDetail?.slot?.currentStock ?? 0;
  const selectedSlotMaxCapacity = selectedSlotDetail?.slot?.maxCapacity ?? 0;
  const selectedSlotFreeSpace = Math.max(selectedSlotMaxCapacity - selectedSlotCurrentStock, 0);
  const selectedSlotRequestedStock = (selectedSlotDetail?.details || []).reduce(
    (total, detail) => total + (detail.quantityToRestock || 0),
    0,
  );
  const selectedSlotRestockedTotal = (selectedSlotDetail?.details || []).reduce(
    (total, detail) => total + (detail.quantityRestocked || 0),
    0,
  );
  const selectedSlotReturnedTotal = (selectedSlotDetail?.details || []).reduce(
    (total, detail) => total + (detail.quantityReturned || 0),
    0,
  );
  const exceedsCapacity = selectedSlotRequestedStock > selectedSlotFreeSpace;
  const isDraft = maintenance.status === "DRAFT";
  const detailGridCols = isDraft ? "1fr 1fr 1fr 1fr 80px" : "1fr 1fr 1fr 1fr";

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "16px",
            marginBottom: "18px",
            padding: "16px",
            background: "var(--bg-card)",
            borderRadius: "8px",
            border: "1px solid var(--border-color)",
          }}
        >
          <div style={{ flex: 1, minWidth: 0 }}>
            <h2 className="section-label" style={{ marginBottom: "8px" }}>
              {machine.name}
            </h2>
            <div className="input-group" style={{ marginBottom: "8px" }}>
              <label className="input-label">Tarea</label>
              <p style={{ fontSize: "0.9rem", fontWeight: "500", margin: "2px 0 0" }}>
                {maintenance.description}
              </p>
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "16px", alignItems: "center" }}>
              <div className="input-group" style={{ margin: 0 }}>
                <label className="input-label">Fecha</label>
                <p style={{ fontSize: "0.85rem", fontWeight: "600", margin: "2px 0 0" }}>
                  {maintenance.maintenanceDate}
                </p>
              </div>
              <div style={{ margin: 0 }}>
                <label className="input-label">Estado</label>
                <div style={{ marginTop: "4px" }}>
                  <span
                    className={`badge ${
                      maintenance.status === "PENDING"
                        ? "badge-yellow"
                        : maintenance.status === "DELAYED"
                          ? "badge-orange"
                          : maintenance.status === "REJECTED_EXPIRED"
                            ? "badge-red"
                            : "badge-blue"
                    }`}
                  >
                    {maintenance.status}
                  </span>
                </div>
              </div>
            </div>
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", justifyContent: "flex-end" }}>
            {getStatusButtons().map((button) => (
              <button
                key={button.status}
                onClick={() => {
                  if (button.status === "DELETE") {
                    if (window.confirm("¿Estás seguro de que deseas eliminar esta tarea de mantenimiento?")) {
                      handleChangeStatus("DELETED");
                    }
                  } else {
                    handleChangeStatus(button.status);
                  }
                }}
                disabled={statusLoading}
                className={button.className}
                style={{ padding: "8px 14px", fontSize: "0.85rem", whiteSpace: "nowrap" }}
              >
                {statusLoading ? "Actualizando..." : button.label}
              </button>
            ))}
          </div>
        </div>

        {statusError && (
          <div
            role="alert"
            className="message-error"
            style={{ marginBottom: "16px", padding: "10px", borderRadius: "4px" }}
          >
            {statusError}
          </div>
        )}

        <div style={{ display: "grid", gridTemplateColumns: "1fr 0.35fr", gap: "24px" }}>
          <section
            className="form-container"
            aria-label="Representación visual de la máquina"
            style={{ height: "600px", padding: 0, overflow: "hidden", position: "relative", backgroundColor: "var(--text-muted)" }}
          >
            <TransformWrapper centerOnInit initialScale={2} minScale={0.5} maxScale={3}>
              <TransformComponent wrapperStyle={{ width: "100%", height: "100%" }}>
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
                        (s) => normalizeRow(s.rowNumber) === r && normalizeColumn(s.columnNumber) === c,
                      );
                      const slotKey = slot ? `${slot.rowNumber}-${slot.columnNumber}` : `${r}-${c}`;
                      const hasDetail = slot && detailsMap[slotKey]?.length > 0;
                      const canAddDetails = isDraft;
                      const isClickable = hasDetail || canAddDetails;
                      const color = slot ? getSlotColor(slot) : "#1d4ed8";
                      const label = `${getColumnLabel(c)}${r + 1}`;
                      const slotStatusLabel = hasDetail
                        ? "Reabastecer"
                        : slot
                          ? slot.currentStock > 0
                            ? "Disponible"
                            : slot.product
                              ? "Sin stock"
                              : "Sin producto"
                          : "—";

                      return (
                        <button
                          key={slotKey}
                          role="gridcell"
                          aria-label={`Ranura ${label}, estado: ${slotStatusLabel}`}
                          onClick={() => slot && handleSlotClick(slot)}
                          style={{
                            width: "120px",
                            height: "85px",
                            background: "var(--text-muted)",
                            borderRadius: "8px",
                            display: "flex",
                            flexDirection: "column",
                            padding: "8px",
                            boxShadow: "inset 0 2px 4px rgba(0,0,0,0.2)",
                            cursor: isClickable ? "pointer" : "default",
                            border: `2px solid ${color}`,
                            textAlign: "left",
                            opacity: isClickable ? 1 : 0.5,
                          }}
                        >
                          <span
                            style={{ fontSize: "0.7rem", fontWeight: "800", color: "var(--bg-card)" }}
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
                                color,
                                fontWeight: "700",
                                textAlign: "center",
                                marginBottom: "3px",
                              }}
                            >
                              {slotStatusLabel}
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
            style={{ padding: "16px", height: "600px", display: "flex", flexDirection: "column" }}
          >
            <div style={{ paddingBottom: "12px", borderBottom: "1px solid var(--border-color)" }}>
              <h3 className="section-label" style={{ marginBottom: "10px" }}>Responsables</h3>
              <div style={{ fontSize: "0.8rem" }}>
                <div className="input-group" style={{ marginBottom: "6px" }}>
                  <label className="input-label">Mantenedor</label>
                  <div>{maintenance.maintainer?.email ?? "—"}</div>
                </div>
                <div className="input-group" style={{ margin: 0 }}>
                  <label className="input-label">Administrador</label>
                  <div>{maintenance.administrator?.email ?? "—"}</div>
                </div>
              </div>
            </div>

            <div style={{ flex: 1, marginTop: "12px", overflow: "hidden", display: "flex", flexDirection: "column" }}>
              <h3 className="section-label" style={{ marginBottom: "8px" }}>
                Tareas ({(maintenance.maintenanceDetails || []).length})
              </h3>
              <div style={{ flex: 1, overflowY: "auto" }}>
                {(maintenance.maintenanceDetails || []).length === 0 ? (
                  <div style={{ fontSize: "0.8rem", color: "var(--text-muted)" }}>Sin detalles</div>
                ) : (
                  (maintenance.maintenanceDetails || []).map((d) => (
                    <button
                      key={d.id}
                      onClick={() => {
                        const key = `${d.rowNumber}-${d.columnNumber}`;
                        setSelectedSlotDetail({
                          slot: slots.find((s) => s.rowNumber === d.rowNumber && s.columnNumber === d.columnNumber),
                          details: detailsMap[key] || [d],
                        });
                        setShowDetailModal(true);
                      }}
                      style={{
                        width: "100%",
                        textAlign: "left",
                        padding: "8px",
                        marginBottom: "6px",
                        background: "var(--bg-card)",
                        border: "1px solid var(--border-color)",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontSize: "0.75rem",
                      }}
                    >
                      <div style={{ fontWeight: "700", color: "var(--primary-blue)", marginBottom: "2px" }}>
                        Ranura{" "}
                        {getSlotLabel(
                          slots.find((s) => s.rowNumber === d.rowNumber && s.columnNumber === d.columnNumber) || {},
                        )}
                      </div>
                      <div>{d.product?.name ?? "—"}</div>
                      <div style={{ color: "var(--text-muted)", marginTop: "2px" }}>
                        Qty: {d.quantityToRestock}
                      </div>
                    </button>
                  ))
                )}
              </div>
            </div>
          </section>
        </div>
      </div>

      {showDetailModal && selectedSlotDetail && (
        <Modal onClose={() => setShowDetailModal(false)}>
          <h3 className="section-label">
            Detalles de Mantenimiento - Ranura {getSlotLabel(selectedSlotDetail.slot)}
          </h3>

          <div style={{ marginTop: "16px" }}>
            <div className="input-group" style={{ marginBottom: "16px" }}>
              <label className="input-label">Producto</label>
              <div
                style={{
                  padding: "10px",
                  background: "var(--bg-dark)",
                  borderRadius: "6px",
                  marginTop: "6px",
                  fontSize: "0.95rem",
                  fontWeight: "600",
                }}
              >
                {selectedSlotDetail.slot?.product?.name ?? "Sin producto"}
              </div>
            </div>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(4, 1fr)",
                gap: "12px",
                marginBottom: "16px",
              }}
            >
              <div className="input-group" style={{ margin: 0 }}>
                <label className="input-label">Stock máximo ranura</label>
                <div
                  style={{
                    padding: "10px",
                    background: "var(--bg-dark)",
                    borderRadius: "6px",
                    marginTop: "6px",
                    fontSize: "0.9rem",
                    textAlign: "center",
                    fontWeight: "600",
                  }}
                >
                  {selectedSlotMaxCapacity}
                </div>
              </div>
              <div className="input-group" style={{ margin: 0 }}>
                <label className="input-label">Stock en la tarea</label>
                <div
                  title={exceedsCapacity ? "Supera la capacidad de la ranura" : undefined}
                  style={{
                    padding: "10px",
                    background: exceedsCapacity ? "rgba(239, 68, 68, 0.1)" : "var(--bg-dark)",
                    border: `1px solid ${exceedsCapacity ? "var(--primary-red)" : "transparent"}`,
                    borderRadius: "6px",
                    marginTop: "6px",
                    fontSize: "0.9rem",
                    textAlign: "center",
                    fontWeight: "600",
                    color: exceedsCapacity ? "var(--primary-red)" : "inherit",
                  }}
                >
                  {selectedSlotRequestedStock}
                  {exceedsCapacity && (
                    <span style={{ fontSize: "0.7rem", display: "block", marginTop: "2px" }}>
                      Supera capacidad
                    </span>
                  )}
                </div>
              </div>
              <div className="input-group" style={{ margin: 0 }}>
                <label className="input-label">Reabastecido total</label>
                <div
                  style={{
                    padding: "10px",
                    background: "var(--bg-dark)",
                    borderRadius: "6px",
                    marginTop: "6px",
                    fontSize: "0.9rem",
                    textAlign: "center",
                    fontWeight: "600",
                  }}
                >
                  {selectedSlotRestockedTotal}
                </div>
              </div>
              <div className="input-group" style={{ margin: 0 }}>
                <label className="input-label">Devuelto total</label>
                <div
                  style={{
                    padding: "10px",
                    background: "var(--bg-dark)",
                    borderRadius: "6px",
                    marginTop: "6px",
                    fontSize: "0.9rem",
                    textAlign: "center",
                    fontWeight: "600",
                  }}
                >
                  {selectedSlotReturnedTotal}
                </div>
              </div>
            </div>

            {!selectedSlotDetail.details || selectedSlotDetail.details.length === 0 ? (
              <div style={{ color: "var(--text-muted)" }}>Sin detalles</div>
            ) : (
              <div style={{ maxHeight: "400px", overflowY: "auto" }}>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: detailGridCols,
                    gap: "12px",
                    paddingBottom: "12px",
                    marginBottom: "12px",
                    borderBottom: "2px solid var(--border-color)",
                    fontWeight: "700",
                    fontSize: "0.75rem",
                    color: "var(--text-muted)",
                    textTransform: "uppercase",
                  }}
                >
                  <div>Fecha de Caducidad</div>
                  <div style={{ textAlign: "center" }}>Cantidad a Reabastecer</div>
                  <div style={{ textAlign: "center" }}>Reabastecida</div>
                  <div style={{ textAlign: "center" }}>Devuelta</div>
                  {isDraft && <div style={{ textAlign: "center" }}>Acciones</div>}
                </div>

                <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
                  {selectedSlotDetail.details.map((detail) => (
                    <div
                      key={detail.id}
                      style={{
                        display: "grid",
                        gridTemplateColumns: detailGridCols,
                        gap: "12px",
                        padding: "12px",
                        background: "var(--bg-card)",
                        borderRadius: "6px",
                        border: "1px solid var(--border-color)",
                        fontSize: "0.9rem",
                        alignItems: "center",
                      }}
                    >
                      <div>{detail.expirationDate ?? "—"}</div>
                      <div style={{ fontWeight: "600", color: "var(--primary-blue)", textAlign: "center" }}>
                        {detail.quantityToRestock}
                      </div>
                      <div
                        style={{
                          textAlign: "center",
                          color: detail.quantityRestocked !== null ? "var(--primary-blue)" : "var(--text-muted)",
                        }}
                      >
                        {detail.quantityRestocked ?? "—"}
                      </div>
                      <div
                        style={{
                          textAlign: "center",
                          color: detail.quantityReturned !== null ? "var(--primary-blue)" : "var(--text-muted)",
                        }}
                      >
                        {detail.quantityReturned ?? "—"}
                      </div>
                      {isDraft && (
                        <button
                          type="button"
                          onClick={() => handleDeleteMaintenanceDetail(detail.id)}
                          disabled={creatingDetail}
                          className="action-btn-red"
                          style={{ height: "32px", padding: "0 10px", fontSize: "0.75rem" }}
                        >
                          Eliminar
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {isDraft && (
              <>
                {!showCreateDetailForm ? (
                  <button
                    type="button"
                    onClick={() => setShowCreateDetailForm(true)}
                    className="btn-primary"
                    style={{ width: "100%", marginTop: "16px", marginBottom: "16px" }}
                  >
                    Agregar Detalle de Mantenimiento
                  </button>
                ) : (
                  <form
                    onSubmit={handleCreateMaintenanceDetail}
                    style={{
                      marginTop: "16px",
                      marginBottom: "16px",
                      padding: "12px",
                      background: "var(--bg-card)",
                      borderRadius: "6px",
                      border: "1px solid var(--border-color)",
                    }}
                  >
                    {detailError && (
                      <div
                        role="alert"
                        className="message-error"
                        style={{ marginBottom: "12px", padding: "8px", borderRadius: "4px" }}
                      >
                        {detailError}
                      </div>
                    )}
                    <div className="input-group" style={{ marginBottom: "12px" }}>
                      <label htmlFor="detail-qty" className="input-label">
                        Cantidad a Reabastecer
                      </label>
                      <input
                        id="detail-qty"
                        type="number"
                        className="dark-input"
                        min="1"
                        value={createDetailForm.quantityToRestock}
                        onChange={(e) =>
                          setCreateDetailForm({ ...createDetailForm, quantityToRestock: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div className="input-group" style={{ marginBottom: "12px" }}>
                      <label htmlFor="detail-expiry" className="input-label">
                        Fecha de Caducidad
                      </label>
                      <input
                        id="detail-expiry"
                        type="date"
                        className="dark-input"
                        value={createDetailForm.expirationDate}
                        onChange={(e) =>
                          setCreateDetailForm({ ...createDetailForm, expirationDate: e.target.value })
                        }
                        required
                      />
                    </div>
                    <div style={{ display: "flex", gap: "8px" }}>
                      <button
                        type="button"
                        onClick={() => {
                          setShowCreateDetailForm(false);
                          setDetailError("");
                          setCreateDetailForm({ quantityToRestock: "", expirationDate: "" });
                        }}
                        className="btn-secondary"
                        style={{ flex: 1 }}
                      >
                        Cancelar
                      </button>
                      <button
                        type="submit"
                        disabled={creatingDetail}
                        className="btn-primary"
                        style={{ flex: 1 }}
                      >
                        {creatingDetail ? "Creando..." : "Agregar"}
                      </button>
                    </div>
                  </form>
                )}
              </>
            )}

            <div style={{ display: "flex", gap: "8px", marginTop: "20px" }}>
              <button
                type="button"
                onClick={() => setShowDetailModal(false)}
                className="btn-secondary"
                style={{ flex: 1 }}
              >
                Cerrar
              </button>
            </div>
          </div>
        </Modal>
      )}
    </main>
  );
};

export default MaintenanceMachineView;
