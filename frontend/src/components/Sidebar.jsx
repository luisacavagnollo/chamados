import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  Ticket,
  Users,
  Wrench,
  Tags,
  Headset,
  UserRound,
  LogOut,
} from "lucide-react";
import { useAuth } from "../context/useAuth";

const menu = [
  { nome: "Dashboard", rota: "/", icone: LayoutDashboard },
  { nome: "Chamados", rota: "/chamados", icone: Ticket },
  { nome: "Meu cadastro", rota: "/meu-cadastro", icone: UserRound, papeis: ["CLIENTE"] },
  { nome: "Clientes", rota: "/clientes", icone: Users, papeis: ["ADMINISTRADOR"] },
  { nome: "Técnicos", rota: "/tecnicos", icone: Wrench, papeis: ["ADMINISTRADOR"] },
  { nome: "Categorias", rota: "/categorias", icone: Tags, papeis: ["ADMINISTRADOR"] },
];

const papeis = {
  CLIENTE: "Cliente",
  TECNICO: "Técnico",
  ADMINISTRADOR: "Administrador",
};

export default function Sidebar() {
  const { identity, logout } = useAuth();
  const navigate = useNavigate();

  function sair() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <Headset size={27} />
        <span>HelpDesk</span>
      </div>

      <div className="sidebar-user">
        <strong>{identity.nomeExibicao}</strong>
        <span>{papeis[identity.papel]}</span>
      </div>

      <nav className="sidebar-menu">
        {menu
          .filter(({ papeis: permitidos }) =>
            !permitidos || permitidos.includes(identity.papel)
          )
          .map(({ nome, rota, icone: Icone }) => (
            <NavLink
              key={rota}
              to={rota}
              end={rota === "/"}
              className={({ isActive }) =>
                `menu-item ${isActive ? "active" : ""}`
              }
            >
              <Icone size={19} />
              <span>{nome}</span>
            </NavLink>
          ))}
      </nav>

      <button className="sidebar-logout" type="button" onClick={sair}>
        <LogOut size={18} />
        <span>Sair</span>
      </button>
    </aside>
  );
}
