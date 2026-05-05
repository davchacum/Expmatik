import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { Client } from "@stomp/stompjs";
import "../global-form.css";
import "../global-list.css";
import { useAuthenticatedUser } from "../hooks/useAuthenticatedUser";
import { useRequireTokenRedirect } from "../hooks/useRequireTokenRedirect";

const SENDABLE_STATUSES = new Set(["PENDING", "DELAYED"]);

const MaintenanceChat = () => {
  const { id: maintenanceId } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem("accessToken");
  useRequireTokenRedirect(token, navigate);
  const { user } = useAuthenticatedUser();

  const [maintenance, setMaintenance] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const clientRef = useRef(null);
  const messagesEndRef = useRef(null);

  const canSend = maintenance && SENDABLE_STATUSES.has(maintenance.status) && connected;

  const markAsRead = useCallback(
    async (messageId) => {
      try {
        await fetch(`/api/chats/${messageId}/read`, {
          method: "PATCH",
          headers: { Authorization: `Bearer ${token}` },
        });
        setMessages((prev) =>
          prev.map((m) => (m.id === messageId ? { ...m, readAt: new Date().toISOString() } : m)),
        );
      } catch (err) {
        console.error("Error al marcar mensaje como leído:", err);
      }
    },
    [token],
  );

  useEffect(() => {
    const headers = { Authorization: `Bearer ${token}` };
    Promise.all([
      fetch(`/api/maintenances/${maintenanceId}`, { headers }).then((r) => r.json()),
      fetch(`/api/chats/maintenance/${maintenanceId}`, { headers }).then((r) => (r.ok ? r.json() : [])),
    ])
      .then(([mt, msgs]) => {
        setMaintenance(mt);
        setMessages(msgs);
        msgs.forEach((m) => {
          if (!m.readAt && m.sender?.email !== user?.email) markAsRead(m.id);
        });
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [maintenanceId, token, user, markAsRead]);

  useEffect(() => {
    const protocol = window.location.protocol === "https:" ? "wss" : "ws";
    const client = new Client({
      brokerURL: `${protocol}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/user/queue/chat/${maintenanceId}`, (frame) => {
          const msg = JSON.parse(frame.body);
          setMessages((prev) => {
            const exists = prev.some((m) => m.id === msg.id);
            if (exists) return prev.map((m) => (m.id === msg.id ? msg : m));
            return [...prev, msg];
          });
          if (!msg.readAt && msg.sender?.email !== user?.email) markAsRead(msg.id);
        });
      },
      onDisconnect: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [maintenanceId, token, user, markAsRead]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleSend = (e) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || !canSend) return;
    clientRef.current.publish({
      destination: `/app/chat/${maintenanceId}`,
      body: JSON.stringify({ content: trimmed }),
    });
    setInput("");
  };

  const formatTime = (dateStr) => {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
  };

  const getSenderLabel = (sender) => {
    const name = `${sender?.firstName ?? ""} ${sender?.lastName ?? ""}`.trim();
    return name || sender?.email || "—";
  };

  if (loading) return <div className="form-container">Cargando...</div>;

  return (
    <main className="home-container" role="main">
      <div className="list-container" style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 80px)" }}>

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            gap: "16px",
            marginBottom: "18px",
            padding: "16px",
            background: "var(--bg-card)",
            borderRadius: "8px",
            border: "1px solid var(--border-color)",
            flexShrink: 0,
          }}
        >
          <div style={{ flex: 1, minWidth: 0 }}>
            <h2 className="section-label" style={{ marginBottom: "4px" }}>Chat</h2>
            <p style={{ fontSize: "0.85rem", color: "var(--text-muted)", margin: 0 }}>
              {maintenance?.description ?? "—"}
            </p>
          </div>
          <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
            <button
              onClick={() => navigate(`/maintenance/${maintenanceId}`)}
              className="btn-secondary"
              style={{ padding: "8px 14px", fontSize: "0.85rem", whiteSpace: "nowrap" }}
            >
              Ver tarea de mantenimiento
            </button>
          </div>
        </div>

        <section
          className="form-container"
          aria-label="Chat de la tarea de mantenimiento"
          style={{ flex: 1, padding: "16px", display: "flex", flexDirection: "column", minHeight: 0 }}
        >
          <div
            style={{ flex: 1, overflowY: "auto", display: "flex", flexDirection: "column", gap: "8px", paddingRight: "4px" }}
          >
            {messages.length === 0 && (
              <div
                style={{ color: "var(--text-muted)", fontSize: "0.85rem", textAlign: "center", marginTop: "24px" }}
              >
                No hay mensajes aún
              </div>
            )}
            {messages.map((msg) => {
              const isOwn = msg.sender?.email === user?.email;
              return (
                <div
                  key={msg.id}
                  style={{ display: "flex", flexDirection: "column", alignItems: isOwn ? "flex-end" : "flex-start" }}
                >
                  {!isOwn && (
                    <span
                      style={{
                        fontSize: "0.72rem",
                        fontWeight: "700",
                        color: "var(--primary-blue)",
                        marginBottom: "2px",
                        paddingLeft: "4px",
                      }}
                    >
                      {getSenderLabel(msg.sender)}
                    </span>
                  )}
                  <div
                    style={{
                      maxWidth: "65%",
                      padding: "8px 12px 6px",
                      borderRadius: isOwn ? "14px 14px 2px 14px" : "14px 14px 14px 2px",
                      background: isOwn ? "var(--primary-blue)" : "var(--bg-card)",
                      border: isOwn ? "none" : "1px solid var(--border-color)",
                      fontSize: "0.9rem",
                      color: isOwn ? "#fff" : "inherit",
                      wordBreak: "break-word",
                    }}
                  >
                    <div>{msg.content}</div>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        alignItems: "center",
                        gap: "4px",
                        marginTop: "4px",
                      }}
                    >
                      <span
                        style={{
                          fontSize: "0.65rem",
                          color: isOwn ? "rgba(255,255,255,0.65)" : "var(--text-muted)",
                        }}
                      >
                        {formatTime(msg.sentAt)}
                      </span>
                      {isOwn && (
                        <span
                          style={{
                            fontSize: "0.75rem",
                            color: msg.readAt ? "#1d4ed8" : "rgba(255,255,255,0.65)",
                          }}
                        >
                          {msg.readAt ? "✓✓" : "✓"}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
            <div ref={messagesEndRef} />
          </div>

          <form
            onSubmit={handleSend}
            style={{
              marginTop: "12px",
              display: "flex",
              gap: "8px",
              borderTop: "1px solid var(--border-color)",
              paddingTop: "12px",
              flexShrink: 0,
            }}
          >
            <input
              type="text"
              className="dark-input"
              placeholder={canSend ? "Escribe un mensaje..." : "Chat deshabilitado"}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              disabled={!canSend}
              maxLength={1000}
              style={{ flex: 1 }}
              aria-label="Mensaje de chat"
            />
            <button
              type="submit"
              className="btn-primary"
              disabled={!canSend || !input.trim()}
              style={{ padding: "8px 20px", whiteSpace: "nowrap" }}
            >
              Enviar
            </button>
          </form>
        </section>

      </div>
    </main>
  );
};

export default MaintenanceChat;
