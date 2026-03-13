import { useLocation, useNavigate } from "react-router-dom";
import "../home/home.css";

const Header = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const isAuth = !!localStorage.getItem("accessToken");

  const isAuthPage = ["/login", "/register"].includes(location.pathname);

  if (isAuthPage) return null;

  const handleLogout = async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deviceId: "web" }),
        credentials: "include",
      });
    } catch (e) {
      console.error("Error durante el logout:", e);
    }
    localStorage.clear();
    navigate("/login", { replace: true });
  };
  
  const getTitle = () => {
    if (location.pathname === "/home") return "Panel Principal";
    return "Mi Aplicación";
  };

  const showBack = location.pathname !== "/home";

  const showHome = location.pathname !== "/home";

  return (
    <div className="header-wrapper">
      <header className="home-header">
        <div className="header-left">
          {showBack && (
            <button
              className="header-btn"
              title="Volver"
              onClick={() => navigate(-1)}
            >
              ←
            </button>
          )}
          {showHome && (
            <button
              className="header-btn"
              title="Ir al Home"
              onClick={() => navigate("/home")}
            >
              🏠
            </button>
          )}
        </div>
        <div className="header-center">
          <span className="header-title">{getTitle()}</span>
        </div>
        <div className="header-right">
          {isAuth && (
            <button className="logout-btn" onClick={handleLogout}>
              Cerrar sesión
            </button>
          )}
        </div>
      </header>
    </div>
  );
};

export default Header;
