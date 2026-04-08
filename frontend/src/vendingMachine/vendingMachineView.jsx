import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Modal from "../components/Modal";
import "../global-list.css";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const VendingMachineList = () => {
  const token = localStorage.getItem("accessToken");
  const navigate = useNavigate();
  useRequireTokenRedirect(token, navigate);

  const [machines, setMachines] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const pageSize = 10;

  const [showAddModal, setShowAddModal] = useState(false);

  const [newMachine, setNewMachine] = useState({
    name: "",
    location: "",
    columnCount: "",
    rowCount: "",
    maxCapacityPerSlot: "",
  });

  const fetchMachines = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page, size: pageSize });
      const response = await fetch(`/api/vending-machines?${params}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (response.ok) {
        const data = await response.json();
        setMachines(data.content || []);
        setTotalPages(data.totalPages || 0);
      }
    } catch (err) {
      setMessage({
        text: "Error al conectar con el servidor",
        type: "error",
        error: err,
      });
    } finally {
      setLoading(false);
    }
  }, [token, page]);

  useEffect(() => {
    fetchMachines();
  }, [fetchMachines]);

  const handleCreateMachine = async (e) => {
    e.preventDefault();
    try {
      const response = await fetch("/api/vending-machines", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(newMachine),
      });

      if (response.ok) {
        setMessage({ text: "Máquina creada correctamente", type: "success" });
        setShowAddModal(false);
        setNewMachine({
          name: "",
          location: "",
          columnCount: "",
          rowCount: "",
          maxCapacityPerSlot: "",
        });
        fetchMachines();
      } else {
        setMessage({ text: "Error al crear la máquina", type: "error" });
      }
    } catch (err) {
      setMessage({ text: "Error de red", type: "error", error: err });
    }
  };

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ maxWidth: "1300px" }}>
        <header
          className="list-header-actions"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "flex-end",
            marginBottom: "10px",
          }}
        >
          <div style={{ maxWidth: "70%" }}>
            <h1
              style={{
                margin: 0,
                fontSize: "1rem",
                fontWeight: "500",
                letterSpacing: "0.02em",
                color: "var(--text-main)",
              }}
            >
              Gestión de Máquinas Expendedoras: Crea, edita y organiza tus
              máquinas
            </h1>
          </div>

          <button
            className="action-btn-green"
            onClick={() => setShowAddModal(true)}
            style={{ whiteSpace: "nowrap" }}
            aria-haspopup="dialog"
          >
            Añadir Nueva Máquina
          </button>
        </header>

        <div className="divider-dark" style={{ marginTop: "15px" }} />

        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="polite"
            style={{ marginBottom: "20px" }}
          >
            {message.text}
          </div>
        )}

        <div className="table-responsive">
          <table
            className="dark-table"
            aria-label="Lista de máquinas expendedoras"
          >
            <thead>
              <tr>
                <th scope="col">NOMBRE</th>
                <th scope="col">UBICACIÓN</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Columnas x Filas
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  ACCIONES
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {machines.map((m) => (
                <tr key={m.id}>
                  <td style={{ verticalAlign: "middle" }}>
                    <strong>{m.name}</strong>
                  </td>
                  <td
                    style={{
                      verticalAlign: "middle",
                      color: "var(--text-muted)",
                    }}
                  >
                    {m.location}
                  </td>
                  <td style={{ textAlign: "center", verticalAlign: "middle" }}>
                    <div
                      style={{
                        display: "flex",
                        gap: "4px",
                        justifyContent: "center",
                        alignItems: "center",
                      }}
                      aria-label={`${m.columnCount} columnas por ${m.rowCount} filas`}
                    >
                      <span className="badge badge-blue" aria-hidden="true">
                        {m.columnCount}
                      </span>
                      <span
                        style={{
                          color: "var(--text-muted)",
                          fontSize: "0.8rem",
                        }}
                        aria-hidden="true"
                      >
                        x
                      </span>
                      <span className="badge badge-blue" aria-hidden="true">
                        {m.rowCount}
                      </span>
                    </div>
                  </td>
                  <td style={{ textAlign: "center", verticalAlign: "middle" }}>
                    <button
                      className="btn-primary"
                      style={{ margin: "0 auto" }}
                      onClick={() =>
                        navigate(`/vending-machines/${m.id}/details`)
                      }
                      aria-label={`Ver detalles de la máquina ${m.name}`}
                    >
                      Ver Detalles
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <nav className="list-footer" aria-label="Paginación de máquinas">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            aria-label="Ir a la página anterior"
          >
            Anterior
          </button>
          <span aria-live="status">
            Página <strong>{page + 1}</strong> de <strong>{totalPages}</strong>
          </span>
          <button
            className="page-btn"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            aria-label="Ir a la página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>

      {showAddModal && (
        <Modal onClose={() => setShowAddModal(false)}>
          <h2
            id="modal-title"
            className="section-label"
            style={{ marginBottom: "20px" }}
          >
            Nueva Máquina Expendedora
          </h2>
          <form onSubmit={handleCreateMachine} aria-labelledby="modal-title">
            <div className="modal-content-group">
              <div className="input-group" style={{ marginBottom: "15px" }}>
                <label htmlFor="machine-name" className="input-label">
                  Nombre Identificador
                </label>
                <input
                  id="machine-name"
                  className="dark-input"
                  required
                  value={newMachine.name}
                  onChange={(e) =>
                    setNewMachine({ ...newMachine, name: e.target.value })
                  }
                  placeholder="Ej: Máquina Central"
                />
              </div>
              <div className="input-group" style={{ marginBottom: "15px" }}>
                <label htmlFor="machine-location" className="input-label">
                  Ubicación Física
                </label>
                <input
                  id="machine-location"
                  className="dark-input"
                  required
                  value={newMachine.location}
                  onChange={(e) =>
                    setNewMachine({ ...newMachine, location: e.target.value })
                  }
                  placeholder="Ej: Planta 0 - Entrada"
                />
              </div>
              <div
                className="search-row"
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr 1fr",
                  gap: "15px",
                }}
              >
                <div className="input-group">
                  <label htmlFor="machine-cols" className="input-label">
                    Nº Columnas
                  </label>
                  <input
                    id="machine-cols"
                    type="number"
                    className="dark-input"
                    value={newMachine.columnCount}
                    min={1}
                    onChange={(e) =>
                      setNewMachine({
                        ...newMachine,
                        columnCount: parseInt(e.target.value) || 0,
                      })
                    }
                  />
                </div>
                <div className="input-group">
                  <label htmlFor="machine-rows" className="input-label">
                    Nº Filas
                  </label>
                  <input
                    id="machine-rows"
                    type="number"
                    className="dark-input"
                    value={newMachine.rowCount}
                    min={1}
                    onChange={(e) =>
                      setNewMachine({
                        ...newMachine,
                        rowCount: parseInt(e.target.value) || 0,
                      })
                    }
                  />
                </div>
                <div className="input-group">
                  <label htmlFor="machine-capacity" className="input-label">
                    Capacidad Max Ranura
                  </label>
                  <input
                    id="machine-capacity"
                    type="number"
                    className="dark-input"
                    value={newMachine.maxCapacityPerSlot}
                    min={0}
                    required
                    onChange={(e) =>
                      setNewMachine({
                        ...newMachine,
                        maxCapacityPerSlot: parseInt(e.target.value) || 0,
                      })
                    }
                  />
                </div>
              </div>
            </div>
            <div
              className="modal-footer"
              style={{ display: "flex", gap: "12px", marginTop: "25px" }}
            >
              <button
                type="button"
                className="action-btn-red"
                onClick={() => setShowAddModal(false)}
              >
                Cancelar
              </button>
              <button type="submit" className="action-btn-green">
                Crear Máquina
              </button>
            </div>
          </form>
        </Modal>
      )}
    </main>
  );
};

export default VendingMachineList;
