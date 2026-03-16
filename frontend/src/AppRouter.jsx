import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./auth/login";
import Register from "./auth/register";
import Profile from "./auth/profile";
import PrivateRoute from "./PrivateRoute";
import Products from "./product/productView";
import Invoices from "./invoice/invoiceView";
import CreateCustomProduct from "./product/createCustomProduct";
import HomeMenu from "./home/HomeMenu";
import CreateInvoice from "./invoice/invoiceCreate";

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

      <Route
        path="/products"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <Products />
          </PrivateRoute>
        }
      />
      <Route
        path="/products/create-custom"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <CreateCustomProduct />
          </PrivateRoute>
        }
      />
      <Route
        path="/invoices"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <Invoices />
          </PrivateRoute>
        }
      />
      <Route
        path="/invoices/create"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <CreateInvoice />
          </PrivateRoute>
        }
      />
      <Route path="/" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default AppRouter;

