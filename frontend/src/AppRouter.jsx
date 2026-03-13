import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./auth/login";
import Register from "./auth/register";
import Profile from "./auth/profile";
import PrivateRoute from "./PrivateRoute";
import HomeMenu from "./home/HomeMenu";

const AppRouter = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/profile"
        element={
          <PrivateRoute>
            <Profile />
          </PrivateRoute>
        }
      />
      <Route
        path="/home"
        element={
          <PrivateRoute>
            <HomeMenu />
          </PrivateRoute>
        }
      />
      <Route path="/" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default AppRouter;
