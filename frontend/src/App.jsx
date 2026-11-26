import { BrowserRouter, Outlet, Route, Routes } from "react-router-dom";
import "./styles/global.css";
import { AuthProvider } from "./context/AuthProvider";
import { useAuth } from "./context/useAuth";
import ProtectedRoute from "./components/ProtectedRoute";
import Sidebar from "./components/Sidebar";
import AccessDenied from "./pages/AccessDenied";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Chamados from "./pages/Chamados";
import DetalheChamado from "./pages/DetalheChamado";
import Clientes from "./pages/Clientes";
import Tecnicos from "./pages/Tecnicos";
import Categorias from "./pages/Categorias";
import MeuCadastro from "./pages/MeuCadastro";

function AppLayout() {
  const { identity, sessionVersion } = useAuth();

  return (
    <div className="app-container" key={`${identity.usuarioId}-${sessionVersion}`}>
      <Sidebar />
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
              <Route index element={<Dashboard />} />
              <Route element={<ProtectedRoute roles={["CLIENTE", "TECNICO", "ADMINISTRADOR"]} />}>
                <Route path="chamados" element={<Chamados />} />
                <Route path="chamados/:id" element={<DetalheChamado />} />
              </Route>
              <Route element={<ProtectedRoute roles={["CLIENTE"]} />}>
                <Route path="meu-cadastro" element={<MeuCadastro />} />
              </Route>
              <Route element={<ProtectedRoute roles={["ADMINISTRADOR"]} />}>
                <Route path="clientes" element={<Clientes />} />
                <Route path="tecnicos" element={<Tecnicos />} />
                <Route path="categorias" element={<Categorias />} />
              </Route>
              <Route path="acesso-negado" element={<AccessDenied />} />
              <Route path="*" element={<h2>Página não encontrada</h2>} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
