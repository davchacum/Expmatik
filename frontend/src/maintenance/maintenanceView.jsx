import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import EyeIcon from "../components/EyeIcon";
import Modal from "../components/Modal";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const STATUS_LABELS = {
  DRAFT: "Borrador",
  PENDING: "Pendiente",
  DELAYED: "Retrasada",
  COMPLETED: "Completada",
  REJECTED_EXPIRED: "Expirada",
};

const STATUS_BADGE = {
  DRAFT: "badge-blue",
  PENDING: "badge-yellow",
  DELAYED: "badge-orange",
  COMPLETED: "badge-green",
  REJECTED_EXPIRED: "badge-red",
};

const formatDate = (dateStr) => {
  if (!dateStr) return "—";
  const [year, month, day] = dateStr.split("-");
  return `${day}/${month}/${year}`;
};

const userDisplayName = (user) => {
  if (!user) return "—";
  if (user.firstName && user.lastName)
    return `${user.firstName} ${user.lastName}`;
  return user.email ?? "—";
};

const getTodayFormatted = () => {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const EMPTY_FILTERS = { status: "", machineId: "", startDate: "", endDate: "" };

const Maintenances = () => {
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);

  const [maintenances, setMaintenances] = useState([]);
  const [machines, setMachines] = useState([]);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [search, setSearch] = useState(EMPTY_FILTERS);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [creatingMaintenance, setCreatingMaintenance] = useState(false);
  const [createForm, setCreateForm] = useState({
    maintenanceDate: "",
    description: "",
    maintainerEmail: "",
    vendingMachineName: "",
  });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });
  const [createError, setCreateError] = useState("");

  useEffect(() => {
    const fetchMachines = async () => {
      try {
        const params = new URLSearchParams({ size: 100 });
        const res = await fetch(`/api/vending-machines?${params}`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setMachines(data.content || []);
        }
      } catch (err) {
        console.error(err);
      }
    };
    fetchMachines();
  }, [token]);

  const fetchMaintenances = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", page);
      params.append("size", 10);
      if (search.status) params.append("status", search.status);
      if (search.machineId) {
        const selected = machines.find((m) => m.id === search.machineId);
        if (selected?.name) params.append("machineName", selected.name);
      }
      if (search.startDate) params.append("startDate", search.startDate);
      if (search.endDate) params.append("endDate", search.endDate);

      const response = await fetch(`/api/maintenances?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setMaintenances(data.content);
        setTotalPages(data.totalPages);
      } else if (response.status === 401) {
        setMessage({
          text: "Tu sesión ha expirado. Por favor, inicia sesión de nuevo.",
          type: "error",
        });
        setMaintenances([]);
        setTotalPages(0);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [page, search, token, machines]);

  useEffect(() => {
    fetchMaintenances();
  }, [fetchMaintenances]);

  const handleApplySearch = () => {
    setPage(0);
    setSearch({ ...filters });
  };

  const handleCreateMaintenance = async (event) => {
    event.preventDefault();
    setCreatingMaintenance(true);
    setCreateError("");

    if (createForm.maintenanceDate < getTodayFormatted()) {
      setCreateError("La fecha de mantenimiento no puede ser en el pasado.");
      setCreatingMaintenance(false);
      return;
    }

    try {
      const response = await fetch("/api/maintenances", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(createForm),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage =
          errorData.message || "No se pudo crear la tarea de mantenimiento.";
        setCreateError(errorMessage);
        return;
      }

      setMessage({
        text: "Tarea de mantenimiento creada correctamente.",
        type: "success",
      });
      setCreateForm({
        maintenanceDate: "",
        description: "",
        maintainerEmail: "",
        vendingMachineName: "",
      });
      setShowCreateModal(false);
      await fetchMaintenances();
    } catch (error) {
      setCreateError(
        error.message || "No se pudo crear la tarea de mantenimiento.",
      );
    } finally {
      setCreatingMaintenance(false);
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <div className="list-header-actions" style={{ marginBottom: "0" }}>
          <div className="input-group">
            <span className="section-label">Gestión</span>
            <button
              className="btn-secondary"
              onClick={() => {
                setCreateError("");
                setShowCreateModal(true);
              }}
              aria-label="Crear nueva tarea de mantenimiento"
            >
              Nueva Tarea de Mantenimiento
            </button>
          </div>
        </div>

        {showCreateModal && (
          <Modal onClose={() => setShowCreateModal(false)}>
            <h3 className="section-label">Nueva Tarea de Mantenimiento</h3>

            {createError && (
              <div
                className="message-error"
                style={{
                  marginTop: "12px",
                  marginBottom: "16px",
                  padding: "10px",
                  borderRadius: "4px",
                }}
              >
                {createError}
              </div>
            )}

            <form
              onSubmit={handleCreateMaintenance}
              style={{ marginTop: "16px" }}
            >
              <div className="input-group" style={{ marginBottom: "12px" }}>
                <label
                  htmlFor="create-maintenance-date"
                  className="input-label"
                >
                  Fecha de mantenimiento
                </label>
                <input
                  id="create-maintenance-date"
                  type="date"
                  className="dark-input"
                  min={getTodayFormatted()}
                  value={createForm.maintenanceDate}
                  onChange={(e) =>
                    setCreateForm({
                      ...createForm,
                      maintenanceDate: e.target.value,
                    })
                  }
                  required
                />
              </div>

              <div className="input-group" style={{ marginBottom: "12px" }}>
                <label
                  htmlFor="create-maintenance-description"
                  className="input-label"
                >
                  Descripción
                </label>
                <textarea
                  id="create-maintenance-description"
                  className="dark-input"
                  rows={3}
                  value={createForm.description}
                  onChange={(e) =>
                    setCreateForm({
                      ...createForm,
                      description: e.target.value,
                    })
                  }
                  required
                  maxLength={255}
                />
              </div>

              <div className="input-group" style={{ marginBottom: "12px" }}>
                <label
                  htmlFor="create-maintainer-email"
                  className="input-label"
                >
                  Email del mantenedor
                </label>
                <input
                  id="create-maintainer-email"
                  type="email"
                  className="dark-input"
                  value={createForm.maintainerEmail}
                  onChange={(e) =>
                    setCreateForm({
                      ...createForm,
                      maintainerEmail: e.target.value,
                    })
                  }
                  required
                />
              </div>

              <div className="input-group" style={{ marginBottom: "16px" }}>
                <label htmlFor="create-vending-machine" className="input-label">
                  Máquina
                </label>
                <select
                  id="create-vending-machine"
                  className="dark-input"
                  value={createForm.vendingMachineName}
                  onChange={(e) =>
                    setCreateForm({
                      ...createForm,
                      vendingMachineName: e.target.value,
                    })
                  }
                  required
                >
                  <option value="">Selecciona una máquina</option>
                  {machines.map((machine) => (
                    <option key={machine.id} value={machine.name}>
                      {machine.name}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: "flex", gap: "8px" }}>
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowCreateModal(false)}
                  style={{ flex: 1 }}
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={creatingMaintenance}
                  style={{ flex: 1 }}
                >
                  {creatingMaintenance ? "Creando..." : "Crear"}
                </button>
              </div>
            </form>
          </Modal>
        )}

        <div className="divider-dark" aria-hidden="true" />

        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "16px" }}
          >
            {message.text}
          </div>
        )}

        <section className="search-section" aria-labelledby="filter-heading">
          <span id="filter-heading" className="section-label">
            Filtrar tareas de mantenimiento
          </span>
          <div
            className="search-row"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "15px",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label htmlFor="filter-status" className="input-label">
                Estado
              </label>
              <select
                id="filter-status"
                className="dark-input"
                value={filters.status}
                onChange={(e) =>
                  setFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="">Todos</option>
                {Object.entries(STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="filter-machine" className="input-label">
                Máquina
              </label>
              <select
                id="filter-machine"
                className="dark-input"
                value={filters.machineId}
                onChange={(e) =>
                  setFilters({ ...filters, machineId: e.target.value })
                }
              >
                <option value="">Todas</option>
                {machines.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="filter-start-date" className="input-label">
                Fecha desde
              </label>
              <input
                id="filter-start-date"
                type="date"
                className="dark-input"
                value={filters.startDate}
                onChange={(e) =>
                  setFilters({ ...filters, startDate: e.target.value })
                }
              />
            </div>

            <div className="input-group">
              <label htmlFor="filter-end-date" className="input-label">
                Fecha hasta
              </label>
              <input
                id="filter-end-date"
                type="date"
                className="dark-input"
                value={filters.endDate}
                onChange={(e) =>
                  setFilters({ ...filters, endDate: e.target.value })
                }
              />
            </div>

            <button className="btn-primary" onClick={handleApplySearch}>
              Buscar
            </button>
          </div>
        </section>

        <div className="table-responsive">
          <table className="dark-table">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col">Máquina</th>
                <th scope="col">Estado</th>
                <th scope="col">Mantenedor</th>
                <th scope="col">Administrador</th>
                <th scope="col">Descripción</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {maintenances.length === 0 && !loading ? (
                <tr>
                  <td
                    colSpan={7}
                    style={{
                      textAlign: "center",
                      color: "var(--text-muted)",
                      padding: "32px",
                    }}
                  >
                    No se encontraron tareas de mantenimiento.
                  </td>
                </tr>
              ) : (
                maintenances.map((m) => (
                  <tr key={m.id}>
                    <td>{formatDate(m.maintenanceDate)}</td>
                    <td>
                      <strong>{m.vendingMachine?.name ?? "—"}</strong>
                    </td>
                    <td>
                      <span
                        className={`badge ${STATUS_BADGE[m.status] ?? "badge-blue"}`}
                      >
                        {STATUS_LABELS[m.status] ?? m.status}
                      </span>
                    </td>
                    <td>{userDisplayName(m.maintainer)}</td>
                    <td>{userDisplayName(m.administrator)}</td>
                    <td
                      style={{
                        maxWidth: "260px",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                        color: "var(--text-muted)",
                      }}
                      title={m.description}
                    >
                      {m.description}
                    </td>
                    <td style={{ textAlign: "center" }}>
                      <button
                        type="button"
                        className="view-desc-btn"
                        onClick={() => {
                          if (m.vendingMachine?.id && m.id) {
                            navigate(
                              `/maintenance/${m.id}`,
                            );
                          }
                        }}
                        aria-label={`Ver detalles de mantenimiento del ${formatDate(m.maintenanceDate)}`}
                      >
                        <EyeIcon visible={false} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <nav
          className="list-footer"
          aria-label="Paginación de tareas de mantenimiento"
        >
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            aria-label="Página anterior"
          >
            Anterior
          </button>
          <span className="page-info" aria-live="polite">
            Página <strong>{page + 1}</strong> de{" "}
            <strong>{totalPages || 1}</strong>
          </span>
          <button
            className="page-btn"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>
    </main>
  );
};

export default Maintenances;
