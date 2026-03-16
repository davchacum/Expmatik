import { useState,useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./home.css";

const HomeMenu = () => {
  const navigate = useNavigate();

  const [user] = useState(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      navigate("/login", { replace: true });
      return;
    }
  }, [navigate]);

  const allItems = [
    {
      title: "Catálogo de Productos",
      desc: "Buscador global y creación de productos.",
      path: "/products",
      roles: ["ADMINISTRATOR"],
    },
    {
      title: "Inventario",
      desc: "Control de stock y productos.",
      path: "/inventory",
      roles: ["ADMINISTRATOR", "MAINTAINER"],
    },
    {
      title: "Analíticas",
      desc: "Análisis de datos y rendimiento.",
      path: "/analytics",
      roles: ["ADMINISTRATOR"],
    },
    {
      title: "Facturas",
      desc: "Carga y gestión de facturas.",
      path: "/invoices",
      roles: ["ADMINISTRATOR"],
    },
    {
      title: "Máquinas",
      desc: "Administración de máquinas.",
      path: "/machines",
      roles: ["ADMINISTRATOR", "MAINTAINER"],
    },
    {
      title: "Ventas",
      desc: "Informes y estadísticas de ventas.",
      path: "/sales",
      roles: ["ADMINISTRATOR"],
    },
    {
      title: "Tareas de mantenimiento",
      desc: "Ver registro de mantenimientos.",
      path: "/maintenance",
      roles: ["ADMINISTRATOR", "MAINTAINER"],
    },
    {
      title: "Notificaciones",
      desc: "Alertas y mensajes del sistema.",
      path: "/notifications",
      roles: ["ADMINISTRATOR", "MAINTAINER"],
    },
  ];

  const menuItems = allItems.filter((item) => item.roles.includes(user?.role));

  return (
    <main className="home-container">
      <div className="dashboard-wrapper">
        <div className="menu-grid">
          {menuItems.map((item, idx) => (
            <button
              key={idx}
              className="menu-card-btn"
              onClick={() => navigate(item.path)}
            >
              <h3 className="card-title">{item.title}</h3>
              <p className="card-desc">{item.desc}</p>
            </button>
          ))}
          <div className="menu-card-empty"></div>
          <div className="menu-card-empty"></div>
        </div>
      </div>
    </main>
  );
};

export default HomeMenu;
