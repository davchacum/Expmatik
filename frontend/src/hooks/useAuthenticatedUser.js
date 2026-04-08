import { useEffect, useState } from "react";

export const useAuthenticatedUser = () => {
  const [isCheckingAuth, setIsCheckingAuth] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(() => {
    const storedUser = localStorage.getItem("user");
    return storedUser ? JSON.parse(storedUser) : null;
  });
  const [error, setError] = useState("");

  useEffect(() => {
    const validateSession = async () => {
      const token = localStorage.getItem("accessToken");

      if (!token) {
        setIsAuthenticated(false);
        setUser(null);
        setIsCheckingAuth(false);
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

        if (!response.ok) {
          localStorage.clear();
          setIsAuthenticated(false);
          setUser(null);
          setError("");
          return;
        }

        const userProfile = await response.json();
        localStorage.setItem("user", JSON.stringify(userProfile));
        setIsAuthenticated(true);
        setUser(userProfile);
        setError("");
      } catch {
        localStorage.clear();
        setIsAuthenticated(false);
        setUser(null);
        setError("No se pudo sincronizar con el servidor.");
      } finally {
        setIsCheckingAuth(false);
      }
    };

    validateSession();
  }, []);

  return {
    isCheckingAuth,
    isAuthenticated,
    user,
    error,
  };
};
