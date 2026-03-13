import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login.css";

const Login = ({ onLogin }) => {
  const [form, setForm] = useState({ email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (form.email === "" || form.password === "") {
      setError("Por favor, rellena todos los campos.");
      return;
    }

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
          deviceId: "web",
        }),
        credentials: "include",
      });

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        setError(data.message || "Credenciales incorrectas.");
        return;
      }

      const data = await response.json();
      const token = data.token || data.accessToken;

      if (token) {
        localStorage.setItem("accessToken", token);
        localStorage.setItem("user", JSON.stringify(data));
        if (onLogin) {
          onLogin(data);
        }
        navigate("/home");
      } else {
        setError("Error: El servidor no devolvió un token válido.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Error al conectar con el servidor.");
    }
  };

  return (
    <div className="login-bg-center">
      <form className="login-form" onSubmit={handleSubmit}>
        <h2 className="login-title">Iniciar sesión</h2>

        {error && <div className="login-error">{error}</div>}

        <div className="login-field">
          <label htmlFor="email">Email:</label>
          <input
            id="email"
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
            className="login-input"
            placeholder="ejemplo@correo.com"
          />
        </div>

        <div className="login-field">
          <label htmlFor="password">Contraseña:</label>
          <div style={{ position: "relative", width: "100%" }}>
            <input
              id="password"
              type={showPassword ? "text" : "password"}
              name="password"
              value={form.password}
              onChange={handleChange}
              required
              className="login-input"
              placeholder="••••••••"
              style={{ paddingRight: "45px" }} // Un poco más de espacio para el ojo
            />
            <button
              type="button"
              tabIndex={-1}
              className="password-toggle-btn"
              style={{
                position: "absolute",
                right: "12px",
                top: "50%",
                transform: "translateY(-50%)", // Centrado vertical perfecto respecto al input
                background: "none",
                border: "none",
                cursor: "pointer",
                padding: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#64748b",
                height: "24px", // Tamaño fijo para el área del botón
                width: "24px",
              }}
              onClick={() => setShowPassword((v) => !v)}
            >
              {showPassword ? (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M17.94 17.94A10.94 10.94 0 0 1 12 19C7 19 2.73 15.11 1 12c.74-1.32 1.81-2.87 3.11-4.19M9.53 9.53A3.5 3.5 0 0 1 12 8.5c1.93 0 3.5 1.57 3.5 3.5 0 .47-.09.92-.26 1.33M14.47 14.47A3.5 3.5 0 0 1 12 15.5c-1.93 0-3.5-1.57-3.5-3.5 0-.47.09-.92.26-1.33" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              ) : (
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  width="20"
                  height="20"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <path d="M1 12S5 5 12 5s11 7 11 7-4 7-11 7S1 12 1 12z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              )}
            </button>
          </div>
        </div>

        <button type="submit" className="login-btn">
          Entrar
        </button>

        <div className="login-footer">
          <p className="login-footer-text">¿No tienes cuenta?</p>
          <button
            type="button"
            className="login-link-btn"
            onClick={() => navigate("/register")}
          >
            Regístrate aquí
          </button>
        </div>
      </form>
    </div>
  );
};

export default Login;
