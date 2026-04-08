import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../home/home.css";
import { useNotificationUnreadCount } from "../hooks/useNotificationUnreadCount";
import "./header.css";

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const titleRef = useRef(null);
  const unreadCount = useNotificationUnreadCount();

  const isAuth = !!localStorage.getItem("accessToken");
  const isAuthPage = ["/login", "/register"].includes(location.pathname);

  const getTitleText = (path) => {
    if (path === "/home") {
      const user = JSON.parse(localStorage.getItem("user"));
      return user?.role === "MAINTAINER"
        ? "Dashboard Reponedor"
        : "Dashboard Administrador";
    }
    if (path === "/profile") return "Mi Perfil";
    if (path === "/products") return "Catálogo de Productos";
    if (path === "/products/create-custom")
      return "Crear Producto Personalizado";
    if (path === "/inventory") return "Gestión de Inventario";
    if (path.startsWith("/inventory/") && path.endsWith("/edit"))
      return "Editar Artículo de Inventario";
    if (path === "/analytics") return "Analíticas";
    if (path === "/invoices") return "Gestión de Facturas";
    if (path === "/invoices/create") return "Crear Nueva Factura";
    if (path.startsWith("/invoices/") && path.endsWith("/edit"))
      return "Editar Factura";
    if (path === "/invoices/create/csv") return "Importar Factura desde CSV";
    if (path === "/vending-machines") return "Gestión de Máquinas Expendedoras";
    if (path.startsWith("/vending-machines/") && path.endsWith("/details"))
      return "Detalles de la Máquina Expendedora";
    if (path === "/sales") return "Ventas";
    if (path === "/maintenance") return "Tareas de Mantenimiento";
    if (path === "/notifications") return "Notificaciones";
    return "Expmatik";
  };

  useEffect(() => {
    if (!isAuthPage) {
      const currentTitle = getTitleText(location.pathname);

      document.title = currentTitle;

      if (titleRef.current) {
        titleRef.current.focus();
      }
    }
  }, [location.pathname, isAuthPage]);

  if (isAuthPage) return null;

  return (
    <div className="header-wrapper">
      <header className="home-header" role="banner">
        <nav className="header-left" aria-label="Navegacion secundaria">
          {location.pathname !== "/home" && (
            <>
              <button
                className="header-btn"
                aria-label="Volver a la pagina anterior"
                onClick={() => navigate(-1)}
              >
                Volver
              </button>
              <button
                className="header-btn"
                aria-label="Ir al panel principal"
                onClick={() => navigate("/home")}
              >
                Inicio
              </button>
            </>
          )}
        </nav>

        <div className="header-center">
          <h1
            ref={titleRef}
            className="header-title"
            aria-live="polite"
            tabIndex="-1"
            style={{ outline: "none" }}
          >
            {getTitleText(location.pathname)}
          </h1>
        </div>

        <div className="header-right" aria-label="Opciones de usuario">
          {isAuth && (
            <button
              className="header-btn header-notification-btn"
              aria-label="Abrir notificaciones"
              onClick={() => navigate("/notifications")}
            >
              <span className="notification-bell" aria-hidden="true">
                <svg
                  viewBox="0 0 24 24"
                  width="18"
                  height="18"
                  fill="currentColor"
                >
                  <path d="M12 2a6 6 0 0 0-6 6v3.7l-1.8 3a1 1 0 0 0 .86 1.5h13.88a1 1 0 0 0 .86-1.5l-1.8-3V8a6 6 0 0 0-6-6Zm0 20a3 3 0 0 0 2.83-2H9.17A3 3 0 0 0 12 22Z" />
                </svg>
              </span>
              {unreadCount > 0 && (
                <span className="header-notification-badge">{unreadCount}</span>
              )}
            </button>
          )}

          {isAuth && location.pathname !== "/profile" && (
            <button
              className="header-btn"
              aria-label="Ver mi perfil de usuario"
              onClick={() => navigate("/profile")}
            >
              Ver Perfil
            </button>
          )}
        </div>
      </header>
    </div>
  );
};

export default Header;
