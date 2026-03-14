import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../home/home.css";
import "./profile.css";

const Profile = () => {
  const navigate = useNavigate();

  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchProfile = async () => {
      const token = localStorage.getItem("accessToken");

      if (!token) {
        navigate("/login", { replace: true });
        return;
      }

      try {
        const response = await fetch("/api/auth/profile", {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        });

        if (response.ok) {
          const freshData = await response.json();
          setUser(freshData);
          localStorage.setItem("user", JSON.stringify(freshData));
        } else {
          localStorage.clear();
          navigate("/login", { replace: true });
        }
      } catch (err) {
        console.error("Error al obtener perfil:", err);
        setError("No se pudo sincronizar con el servidor.");
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [navigate]);

  const handleLogout = async () => {
    try {
      await fetch("/api/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ deviceId: "web" }),
        credentials: "include",
      });
    } catch (err) {
      console.error("Error al cerrar sesion:", err);
    }
    localStorage.clear();
    navigate("/login", { replace: true });
  };

  if (!user && !loading) return null;

  return (
    <main className="home-container" id="main-content" role="main">
      <div
        className="profile-card"
        role="region"
        aria-labelledby="profile-info-title"
      >
        <h2 id="profile-info-title" className="profile-card-title">
          Informacion de la Cuenta
        </h2>

        {error && (
          <div
            className="login-error"
            role="alert"
            aria-live="assertive"
            style={{ marginBottom: "20px" }}
          >
            {error}
          </div>
        )}

        <div
          className="profile-details"
          aria-busy={loading}
          style={{ opacity: loading ? 0.6 : 1 }}
        >
          <div className="profile-info-group">
            <label htmlFor="user-name">Nombre completo</label>
            <p id="user-name">
              {user?.firstName} {user?.lastName}
            </p>
          </div>

          <div className="profile-info-group">
            <label htmlFor="user-email">Correo electronico</label>
            <p id="user-email">{user?.email}</p>
          </div>

          <div className="profile-info-group">
            <label>Rol asignado</label>
            <span
              className={`profile-role-badge ${user?.role === "MAINTAINER" ? "role-maintainer" : "role-admin"}`}
              role="status"
              aria-label={`Rol actual: ${user?.role}`}
            >
              {user?.role}
            </span>
          </div>
        </div>

        <div className="profile-actions">
          <button
            className="logout-danger-btn"
            onClick={handleLogout}
            aria-label="Cerrar sesion y salir de la cuenta"
          >
            Cerrar sesion
          </button>
        </div>
      </div>
    </main>
  );
};

export default Profile;