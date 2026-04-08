import { useEffect } from "react";

export const useRequireTokenRedirect = (token, navigate) => {
  useEffect(() => {
    if (!token) {
      navigate("/login", { replace: true });
    }
  }, [token, navigate]);
};
