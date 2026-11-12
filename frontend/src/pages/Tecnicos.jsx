
import CadastroPage from "../components/CadastroPage";
import { Ticket } from "lucide-react";
import { useNavigate } from "react-router-dom";

const campos = [
  {
    nome: "nome",
    label: "Nome completo",
    tipo: "text",
    obrigatorio: true,
    maxLength: 100,
  },
  {
    nome: "email",
    label: "E-mail",
    tipo: "text",
    placeholder: "nome@dominio.com",
    inputMode: "email",
    autoComplete: "email",
    pattern: "[^\\s@]+@[^\\s@]+",
    title: "Informe um e-mail com usuário, @ e domínio.",
    obrigatorio: true,
    maxLength: 120,
  },
  {
    nome: "perfil",
    label: "Perfil / Especialidade",
    tipo: "text",
    obrigatorio: false,
    maxLength: 50,
  },
];

export default function Tecnicos() {
  const navigate = useNavigate();

  return (
    <CadastroPage
      titulo="Técnicos"
      descricao="Gerencie os profissionais responsáveis pelos atendimentos."
      endpoint="/tecnicos"
      campos={campos}
      acaoExtra={{
        label: "Ver atendimentos",
        icon: Ticket,
        onClick: (tecnico) =>
          navigate(`/chamados?tecnicoId=${tecnico.id}`),
      }}
    />
  );
}
