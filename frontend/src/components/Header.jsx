import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../home/home.css";
import "./header.css";

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const titleRef = useRef(null);

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
    if( path === "/invoices/create/csv") return "Importar Factura desde CSV";
    if (path === "/machines") return "Máquinas";
    if (path === "/sales") return "Ventas";
    if (path === "/maintenance") return "Tareas de Mantenimiento";
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
