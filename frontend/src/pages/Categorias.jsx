
import CadastroPage from "../components/CadastroPage";
import { Ticket } from "lucide-react";
import { useNavigate } from "react-router-dom";

const campos = [
  {
    nome: "nome",
    label: "Nome da categoria",
    tipo: "text",
    obrigatorio: true,
    maxLength: 100,
  },
  {
    nome: "descricao",
    label: "Descrição",
    tipo: "textarea",
    obrigatorio: false,
    maxLength: 255,
  },
];

export default function Categorias() {
  const navigate = useNavigate();

  return (
    <CadastroPage
      titulo="Categorias"
      descricao="Organize os tipos de solicitações de suporte."
      endpoint="/categorias"
      campos={campos}
      acaoExtra={{
        label: "Ver chamados",
        icon: Ticket,
        onClick: (categoria) =>
          navigate(`/chamados?categoriaId=${categoria.id}`),
      }}
    />
  );
}
