import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./login.css";

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

const Register = ({ onRegister }) => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    firstName: "",
    lastName: "",
    role: "",
  });

  const [showPass, setShowPass] = useState(false);
  const [showConfirmPass, setShowConfirmPass] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!form.role) return setError("Por favor, selecciona un rol de usuario.");
    if (form.password.length < 6)
      return setError("La contraseña debe tener al menos 6 caracteres.");
    if (form.password !== form.confirmPassword)
      return setError("Las contraseñas no coinciden.");

    try {
      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.email,
          password: form.password,
          role: form.role,
          firstName: form.firstName,
          lastName: form.lastName,
          deviceId: "web",
        }),
        credentials: "include",
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        let message = "No se pudo registrar.";
        if (
          data.errors &&
          Array.isArray(data.errors) &&
          data.errors.length > 0
        ) {
          message = data.errors[0].defaultMessage || "Error de validacion.";
        } else if (data.message) {
          message = data.message;
        }
        setError(message);
        return;
      }

      if (data && data.token) {
        localStorage.setItem("accessToken", data.token);
        navigate("/home");
      }
      if (onRegister) onRegister({ ...form, ...data });
    } catch (err) {
      console.error("Error en el registro:", err);
      setError("Error al conectar con el servidor.");
    }
  };

  return (
    <main className="login-bg-center" role="main">
      <form
        className="login-form"
        onSubmit={handleSubmit}
        aria-labelledby="register-heading"
      >
        <h1 id="register-heading" className="login-title">
          Crear cuenta
        </h1>

        {error && (
          <div className="login-error" role="alert" aria-live="assertive">
            {error}
          </div>
        )}

        <div style={{ display: "flex", gap: "10px" }}>
          <div className="login-field">
            <label htmlFor="firstName">Nombre</label>
            <input
              id="firstName"
              type="text"
              name="firstName"
              value={form.firstName}
              onChange={handleChange}
              required
              className="login-input"
              placeholder="Juan"
              maxLength={40}
              aria-required="true"
            />
          </div>
          <div className="login-field">
            <label htmlFor="lastName">Apellido</label>
            <input
              id="lastName"
              type="text"
              name="lastName"
              value={form.lastName}
              onChange={handleChange}
              required
              className="login-input"
              placeholder="Perez"
              maxLength={40}
              aria-required="true"
            />
          </div>
        </div>

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
          <label htmlFor="role">Rol de usuario</label>
          <select
            id="role"
            name="role"
            value={form.role}
            onChange={handleChange}
            required
            className="login-input"
            style={{ cursor: "pointer" }}
            aria-required="true"
          >
            <option value="" disabled>
              Selecciona un rol...
            </option>
            <option value="MAINTAINER">MAINTAINER</option>
            <option value="ADMINISTRATOR">ADMINISTRATOR</option>
          </select>
        </div>

        <div className="login-field">
          <label htmlFor="password">Contraseña (min. 6)</label>
          <div style={{ position: "relative", width: "100%" }}>
            <input
              id="password"
              type={showPass ? "text" : "password"}
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
                showPass ? "Ocultar contraseña" : "Mostrar contraseña"
              }
              style={{
                position: "absolute",
                right: "12px",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "#64748b",
                display: "flex",
              }}
              onClick={() => setShowPass(!showPass)}
            >
              <EyeIcon visible={showPass} />
            </button>
          </div>
        </div>

        <div className="login-field">
          <label htmlFor="confirmPassword">Confirmar contraseña</label>
          <div style={{ position: "relative", width: "100%" }}>
            <input
              id="confirmPassword"
              type={showConfirmPass ? "text" : "password"}
              name="confirmPassword"
              value={form.confirmPassword}
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
                showConfirmPass
                  ? "Ocultar confirmacion de contraseña"
                  : "Mostrar confirmacion de contraseña"
              }
              style={{
                position: "absolute",
                right: "12px",
                top: "50%",
                transform: "translateY(-50%)",
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "#64748b",
                display: "flex",
              }}
              onClick={() => setShowConfirmPass(!showConfirmPass)}
            >
              <EyeIcon visible={showConfirmPass} />
            </button>
          </div>
        </div>

        <button type="submit" className="login-btn">
          Registrarse
        </button>

        <footer className="login-footer" role="contentinfo">
          <p className="login-footer-text">¿Ya tienes cuenta?</p>
          <button
            type="button"
            className="login-link-btn"
            onClick={() => navigate("/login")}
            aria-label="Ir al inicio de sesion"
          >
            Inicia sesion aqui
          </button>
        </footer>
      </form>
    </main>
  );
};

export default Register;
