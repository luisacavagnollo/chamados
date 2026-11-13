
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  Ticket,
  Users,
  Wrench,
  Tags,
  Headset,
} from "lucide-react";

const menu = [
  { nome: "Dashboard", rota: "/", icone: LayoutDashboard },
  { nome: "Chamados", rota: "/chamados", icone: Ticket },
  { nome: "Clientes", rota: "/clientes", icone: Users },
  { nome: "Técnicos", rota: "/tecnicos", icone: Wrench },
  { nome: "Categorias", rota: "/categorias", icone: Tags },
];

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <Headset size={27} />
        <span>HelpDesk</span>
      </div>

      <nav className="sidebar-menu">
        {menu.map(({ nome, rota, icone: Icone }) => (
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
    </aside>
  );
}
