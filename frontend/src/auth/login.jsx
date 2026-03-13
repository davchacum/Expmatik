import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login.css";

// Definido fuera para mantener coherencia con Register y mejorar rendimiento
const EyeIcon = ({ visible }) =>
  visible ? (
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
      aria-hidden="true"
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
      aria-hidden="true"
    >
      <path d="M1 12S5 5 12 5s11 7 11 7-4 7-11 7S1 12 1 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );

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
        setError("Error: El servidor no devolvio un token valido.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Error al conectar con el servidor.");
    }
  };

  return (
    <main className="login-bg-center" role="main">
      <form
        className="login-form"
        onSubmit={handleSubmit}
        aria-labelledby="login-heading"
      >
        <h1 id="login-heading" className="login-title">
          Iniciar sesion
        </h1>

        {error && (
          <div className="login-error" role="alert" aria-live="assertive">
            {error}
          </div>
        )}

        <div className="login-field">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            name="email"
            value={form.email}
            onChange={handleChange}
            required
            className="login-input"
            placeholder="ejemplo@correo.com"
            aria-required="true"
          />
        </div>

        <div className="login-field">
          <label htmlFor="password">Contraseña</label>
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
              style={{ paddingRight: "45px" }}
              aria-required="true"
            />
            <button
              type="button"
              className="password-toggle-btn"
              aria-label={
                showPassword ? "Ocultar contraseña" : "Mostrar contraseña"
              }
              style={{
                position: "absolute",
                right: "12px",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                padding: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#64748b",
                height: "24px",
                width: "24px",
              }}
              onClick={() => setShowPassword((v) => !v)}
            >
              <EyeIcon visible={showPassword} />
            </button>
          </div>
        </div>

        <button type="submit" className="login-btn">
          Entrar
        </button>

        <footer className="login-footer" role="contentinfo">
          <p className="login-footer-text">¿No tienes cuenta?</p>
          <button
            type="button"
            className="login-link-btn"
            onClick={() => navigate("/register")}
            aria-label="Ir a la pagina de registro"
          >
            Registrate aqui
          </button>
        </footer>
      </form>
    </main>
  );
};

export default Login;
