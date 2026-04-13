import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import EyeIcon from "../components/EyeIcon";
import "./login.css";

const Login = ({ onLogin }) => {
  const [form, setForm] = useState({ email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const titleRef = useRef(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (titleRef.current) {
      titleRef.current.focus();
    }
  }, []);

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
        if (onLogin) onLogin(data);
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
    <main className="login-bg-center" id="main-content" role="main">
      <form
        className="login-form"
        onSubmit={handleSubmit}
        aria-labelledby="login-heading"
      >
        <h1
          id="login-heading"
          ref={titleRef}
          className="login-title"
          tabIndex="-1"
          style={{ outline: "none" }}
        >
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
