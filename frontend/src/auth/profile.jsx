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
    <div className="home-container">
      <div className="profile-card">
        <h2 className="profile-card-title">Informacion de la Cuenta</h2>

        {error && (
          <div className="login-error" style={{ marginBottom: "20px" }}>
            {error}
          </div>
        )}

        <div className="profile-details" style={{ opacity: loading ? 0.6 : 1 }}>
          <div className="profile-info-group">
            <label>Nombre completo</label>
            <p>
              {user?.firstName} {user?.lastName}
            </p>
          </div>

          <div className="profile-info-group">
            <label>Correo electronico</label>
            <p>{user?.email}</p>
          </div>

          <div className="profile-info-group">
            <label>Rol asignado</label>
            <span
              className={`profile-role-badge ${user?.role === "MAINTAINER" ? "role-maintainer" : "role-admin"}`}
            >
              {user?.role}
            </span>
          </div>
        </div>

        <div className="profile-actions">
          <button className="logout-danger-btn" onClick={handleLogout}>
            Cerrar sesion
          </button>
        </div>
      </div>
    </div>
  );
};

export default Profile;
