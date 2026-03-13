import { BrowserRouter as Router } from "react-router-dom";
import AppRouter from "./AppRouter";
import Header from "./components/Header";

const App = () => {
  return (
    <Router>
      <Header />
      <AppRouter />
    </Router>
  );
};

export default App;
