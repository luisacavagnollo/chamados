
import { BrowserRouter, Routes, Route } from "react-router-dom";

import "./styles/global.css";
import Sidebar from "./components/Sidebar";
import Dashboard from "./pages/Dashboard";
import Chamados from "./pages/Chamados";
import DetalheChamado from "./pages/DetalheChamado";
import Clientes from "./pages/Clientes";
import Tecnicos from "./pages/Tecnicos";
import Categorias from "./pages/Categorias";

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <Sidebar />

        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/chamados" element={<Chamados />} />
            <Route
              path="/chamados/:id"
              element={<DetalheChamado />}
            />
            <Route path="/clientes" element={<Clientes />} />
            <Route path="/tecnicos" element={<Tecnicos />} />
            <Route path="/categorias" element={<Categorias />} />
            <Route
              path="*"
              element={<h2>Página não encontrada</h2>}
            />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
