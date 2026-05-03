import { Navigate, Route, Routes } from "react-router-dom";
import Login from "./auth/login";
import Profile from "./auth/profile";
import Register from "./auth/register";
import HomeMenu from "./home/HomeMenu";
import EditProductInventory from "./inventory/editProductInventory";
import InventoryList from "./inventory/inventoryView";
import CreateInvoice from "./invoice/createInvoice";
import EditInvoice from "./invoice/editInvoice";
import ImportInvoiceCSV from "./invoice/importInvoiceCSV";
import Invoices from "./invoice/invoiceView";
import MaintenancesView from "./maintenance/maintenanceView";
import NotificationsView from "./notification/notificationView";
import PrivateRoute from "./PrivateRoute";
import CreateCustomProduct from "./product/createCustomProduct";
import Products from "./product/productView";
import ImportSaleCSV from "./sale/importSaleCSV";
import Sales from "./sale/saleView";
import VendingMachineDetails from "./vendingMachine/vendingMachineDetails";
import VendingMachineList from "./vendingMachine/vendingMachineView";

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
      <Route
        path="/invoices/:id/edit"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <EditInvoice />
          </PrivateRoute>
        }
      />
      <Route
        path="/invoices/create/csv"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <ImportInvoiceCSV />
          </PrivateRoute>
        }
      />
      <Route
        path="/inventory"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <InventoryList />
          </PrivateRoute>
        }
      />
      <Route
        path="/inventory/:id/edit"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <EditProductInventory />
          </PrivateRoute>
        }
      />
      <Route
        path="/vending-machines"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <VendingMachineList />
          </PrivateRoute>
        }
      />

      <Route
        path="/vending-machines/:id/details"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <VendingMachineDetails />
          </PrivateRoute>
        }
      />

      <Route
        path="/sales"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <Sales />
          </PrivateRoute>
        }
      />
      <Route
        path="/sales/create/csv"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR"]}>
            <ImportSaleCSV />
          </PrivateRoute>
        }
      />

      <Route
        path="/maintenance"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR", "MAINTAINER"]}>
            <MaintenancesView />
          </PrivateRoute>
        }
      />

      <Route
        path="/notifications"
        element={
          <PrivateRoute allowedRoles={["ADMINISTRATOR", "MAINTAINER"]}>
            <NotificationsView />
          </PrivateRoute>
        }
      />
      <Route path="/" element={<Navigate to="/login" replace />} />
    </Routes>
  );
};

export default AppRouter;
