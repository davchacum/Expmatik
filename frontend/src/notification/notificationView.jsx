import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../global-list.css";
import { useNotificationUnreadCount } from "../hooks/useNotificationUnreadCount";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const NOTIFICATION_TYPE_OPTIONS = [
  "PRODUCT_LOW_STOCK",
  "PRODUCT_OUT_OF_STOCK",
  "EXPIRATION_WARNING",
  "PRODUCT_EXPIRED",
  "ASSIGNED_RESTOCKING",
  "COMPLETED_RESTOCKING",
  "INVENTORY_STOCK_LOW",
  "INVENTORY_OUT_OF_STOCK",
  "FAILURE_SALE",
];

const TYPE_LABELS = {
  PRODUCT_LOW_STOCK: "Stock bajo (máquina)",
  PRODUCT_OUT_OF_STOCK: "Sin stock (máquina)",
  EXPIRATION_WARNING: "Caducidad próxima",
  PRODUCT_EXPIRED: "Producto caducado",
  ASSIGNED_RESTOCKING: "Reposición asignada",
  COMPLETED_RESTOCKING: "Reposición completada",
  INVENTORY_STOCK_LOW: "Stock bajo (inventario)",
  INVENTORY_OUT_OF_STOCK: "Sin stock (inventario)",
  FAILURE_SALE: "Fallo de venta",
};

const toIsoStartOfDay = (date) => (date ? `${date}T00:00:00` : "");
const toIsoEndOfDay = (date) => (date ? `${date}T23:59:59` : "");

const NotificationsView = () => {
  const token = localStorage.getItem("accessToken");
  const navigate = useNavigate();
  useRequireTokenRedirect(token, navigate);
  const unreadCount = useNotificationUnreadCount();
  const previousUnreadCountRef = useRef(null);

  const [notifications, setNotifications] = useState([]);
  const [filters, setFilters] = useState({
    notificationType: "",
    startDate: "",
    endDate: "",
  });
  const [search, setSearch] = useState({
    notificationType: "",
    startDate: "",
    endDate: "",
  });
  const [showAll, setShowAll] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ text: "", type: "" });
  const [hasSearched, setHasSearched] = useState(false);

  const isReadParam = useMemo(() => (showAll ? null : false), [showAll]);

  const fetchNotifications = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      params.append("page", String(page));
      params.append("size", "10");

      if (isReadParam !== null) {
        params.append("isRead", String(isReadParam));
      }

      if (search.notificationType) {
        params.append("notificationType", search.notificationType);
      }

      if (search.startDate) {
        params.append("startDate", toIsoStartOfDay(search.startDate));
      }

      if (search.endDate) {
        params.append("endDate", toIsoEndOfDay(search.endDate));
      }

      const response = await fetch(`/api/notifications?${params.toString()}`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (response.ok) {
        const data = await response.json();
        setNotifications(data.content || []);
        setTotalPages(data.totalPages || 0);
        if (hasSearched) {
          setMessage({ text: "Búsqueda realizada con éxito", type: "success" });
        }
      } else {
        let msg = "Error al buscar notificaciones.";
        try {
          const errData = await response.json();
          msg = errData.message || msg;
        } catch {
          console.error("Failed to parse error response");
        }
        setMessage({ text: msg, type: "error" });
        setNotifications([]);
        setTotalPages(0);
      }
    } catch (err) {
      setMessage({ text: `Error de conexión: ${err.message}`, type: "error" });
    } finally {
      setLoading(false);
    }
  }, [page, token, search, isReadParam, hasSearched]);

  useEffect(() => {
    fetchNotifications();
  }, [fetchNotifications]);

  useEffect(() => {
    if (previousUnreadCountRef.current === null) {
      previousUnreadCountRef.current = unreadCount;
      return;
    }

    if (previousUnreadCountRef.current !== unreadCount) {
      fetchNotifications();
      previousUnreadCountRef.current = unreadCount;
    }
  }, [unreadCount, fetchNotifications]);

  const handleApplySearch = () => {
    setPage(0);
    setSearch({ ...filters });
    setHasSearched(true);
    setMessage({ text: "", type: "" });
  };

  const handleToggleReadMode = () => {
    setShowAll((prev) => !prev);
    setPage(0);
    setMessage({ text: "", type: "" });
  };

  const handleMarkAsRead = async (notificationId) => {
    try {
      const response = await fetch(
        `/api/notifications/${notificationId}/mark-as-read`,
        {
          method: "PATCH",
          headers: { Authorization: `Bearer ${token}` },
        },
      );

      if (!response.ok) {
        setMessage({
          text: "No se pudo marcar la notificación como leída.",
          type: "error",
        });
        return;
      }

      setMessage({ text: "Notificación marcada como leída.", type: "success" });
      fetchNotifications();
    } catch (err) {
      setMessage({ text: `Error de conexión: ${err.message}`, type: "error" });
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      const response = await fetch("/api/notifications/mark-all-as-read", {
        method: "PATCH",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) {
        setMessage({
          text: "No se pudieron marcar todas como leídas.",
          type: "error",
        });
        return;
      }

      setMessage({
        text: "Todas las notificaciones se marcaron como leídas.",
        type: "success",
      });
      fetchNotifications();
    } catch (err) {
      setMessage({ text: `Error de conexión: ${err.message}`, type: "error" });
    }
  };

  const getTypeLabel = (type) => TYPE_LABELS[type] || type;

  const totalPagesSafe = totalPages > 0 ? totalPages : 1;

  return (
    <main className="home-container" role="main">
      <div className="list-container">
        <div className="list-header-actions">
          <div className="input-group">
            <h2 className="section-label">Modo de visualización</h2>
            <button
              className={showAll ? "action-btn-orange" : "action-btn-blue"}
              onClick={handleToggleReadMode}
              style={{ width: "100%", height: "44px" }}
            >
              {showAll ? "Mostrar solo no leídas" : "Mostrar todas"}
            </button>
          </div>
          <div className="input-group">
            <h2 className="section-label">Acciones rápidas</h2>
            <button
              className="action-btn-green"
              onClick={handleMarkAllAsRead}
              style={{ width: "100%", height: "44px" }}
            >
              Marcar todas como leídas
            </button>
          </div>
        </div>

        <div className="divider-dark" style={{ marginBottom: "20px" }} />

        <section aria-labelledby="filter-heading">
          <h2 id="filter-heading" className="section-label">
            Filtrar notificaciones
          </h2>
          <div
            className="search-row"
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
              gap: "16px",
              marginBottom: "20px",
              alignItems: "end",
            }}
          >
            <div className="input-group">
              <label htmlFor="f-type" className="input-label">
                Tipo de notificación
              </label>
              <select
                id="f-type"
                className="dark-input"
                value={filters.notificationType}
                onChange={(e) =>
                  setFilters({ ...filters, notificationType: e.target.value })
                }
              >
                <option value="">Todas</option>
                {NOTIFICATION_TYPE_OPTIONS.map((type) => (
                  <option key={type} value={type}>
                    {getTypeLabel(type)}
                  </option>
                ))}
              </select>
            </div>
            <div className="input-group">
              <label htmlFor="f-start" className="input-label">
                Desde
              </label>
              <input
                id="f-start"
                type="date"
                className="dark-input"
                value={filters.startDate}
                onChange={(e) =>
                  setFilters({ ...filters, startDate: e.target.value })
                }
              />
            </div>
            <div className="input-group">
              <label htmlFor="f-end" className="input-label">
                Hasta
              </label>
              <input
                id="f-end"
                type="date"
                className="dark-input"
                value={filters.endDate}
                onChange={(e) =>
                  setFilters({ ...filters, endDate: e.target.value })
                }
              />
            </div>
            <button
              className="action-btn-blue"
              onClick={handleApplySearch}
              style={{ height: "44px" }}
              aria-label="Buscar notificaciones"
            >
              Buscar
            </button>
          </div>
        </section>

        {message.text && (
          <div
            className={
              message.type === "error" ? "message-error" : "message-success"
            }
            role="alert"
            aria-live="polite"
            style={{ marginBottom: "16px" }}
          >
            {message.text}
          </div>
        )}

        <div className="table-responsive">
          <table className="dark-table" aria-label="Listado de notificaciones">
            <thead>
              <tr>
                <th scope="col">Fecha</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Tipo
                </th>
                <th scope="col">Mensaje</th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Estado
                </th>
                <th scope="col" style={{ textAlign: "center" }}>
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody style={{ opacity: loading ? 0.5 : 1 }}>
              {notifications.map((notification) => (
                <tr key={notification.id}>
                  <td>{new Date(notification.createdAt).toLocaleString()}</td>
                  <td style={{ textAlign: "center" }}>
                    <span className="badge badge-blue">
                      {getTypeLabel(notification.type)}
                    </span>
                  </td>
                  <td style={{ fontWeight: 700 }}>{notification.message}</td>
                  <td style={{ textAlign: "center" }}>
                    <span
                      className={`badge ${notification.isRead ? "badge-green" : "badge-orange"}`}
                    >
                      {notification.isRead ? "Leída" : "No leída"}
                    </span>
                  </td>
                  <td
                    style={{
                      textAlign: "center",
                      display: "flex",
                      flexDirection: "column",
                      gap: "6px",
                      alignItems: "center",
                    }}
                  >
                    {!notification.isRead && (
                      <button
                        className="action-btn-green"
                        onClick={() => handleMarkAsRead(notification.id)}
                      >
                        Marcar leída
                      </button>
                    )}
                    {notification.link && notification.link !== "Unknown" && (
                      <button
                        className="action-btn-blue"
                        style={{ marginTop: 4 }}
                        onClick={() => navigate(notification.link)}
                        aria-label="Ir al sitio relacionado"
                      >
                        Ir al sitio
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {notifications.length === 0 && !loading && (
                <tr>
                  <td
                    colSpan={5}
                    style={{ textAlign: "center", color: "#94a3b8" }}
                  >
                    No hay notificaciones para los filtros seleccionados.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <nav className="list-footer" aria-label="Navegación de resultados">
          <button
            className="page-btn"
            disabled={page === 0}
            onClick={() => setPage((prev) => prev - 1)}
            aria-label="Página anterior"
          >
            Anterior
          </button>
          <span className="page-info">
            Página <strong>{page + 1}</strong> de{" "}
            <strong>{totalPagesSafe}</strong>
          </span>
          <button
            className="page-btn"
            disabled={page >= totalPagesSafe - 1}
            onClick={() => setPage((prev) => prev + 1)}
            aria-label="Página siguiente"
          >
            Siguiente
          </button>
        </nav>
      </div>
    </main>
  );
};

export default NotificationsView;
