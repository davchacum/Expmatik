import { Navigate } from "react-router-dom";
import { useAuthenticatedUser } from "./hooks/useAuthenticatedUser";

const PrivateRoute = ({ children, allowedRoles }) => {
  const { isCheckingAuth, isAuthenticated, user } = useAuthenticatedUser();

  if (isCheckingAuth) {
    return null;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    return <Navigate to="/home" replace />;
  }

  return children;
};

export default PrivateRoute;
