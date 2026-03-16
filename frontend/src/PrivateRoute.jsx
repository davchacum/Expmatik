import { Navigate } from "react-router-dom";

const PrivateRoute = ({ children, allowedRoles }) => {
  const token = localStorage.getItem("accessToken");
  const user = JSON.parse(localStorage.getItem("user"));

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user?.role)) {
    return <Navigate to="/home" replace />;
  }

  return children;
};

export default PrivateRoute;
