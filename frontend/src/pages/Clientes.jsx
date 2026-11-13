
import CadastroPage from "../components/CadastroPage";
import { Ticket } from "lucide-react";
import { useNavigate } from "react-router-dom";

function formatarTelefone(valor) {
  let digitos = String(valor ?? "").replace(/\D/g, "");

  if (
    (digitos.length === 12 || digitos.length === 13) &&
    digitos.startsWith("55")
  ) {
    digitos = digitos.slice(2);
  }

  digitos = digitos.slice(0, 11);

  if (digitos.length <= 2) {
    return digitos ? `(${digitos}` : "";
  }

  const ddd = digitos.slice(0, 2);
  const numero = digitos.slice(2);
  const tamanhoPrefixo = digitos.length > 10 ? 5 : 4;
  const prefixo = numero.slice(0, tamanhoPrefixo);
  const sufixo = numero.slice(tamanhoPrefixo);

  return `(${ddd}) ${prefixo}${sufixo ? `-${sufixo}` : ""}`;
}

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
    nome: "telefone",
    label: "Telefone",
    tipo: "tel",
    placeholder: "(11) 99999-9999",
    inputMode: "tel",
    autoComplete: "tel",
    pattern: "(?:\\d{10,11}|\\(\\d{2}\\) \\d{4,5}-\\d{4})",
    title: "Informe o DDD e um telefone com 8 ou 9 dígitos.",
    formatar: formatarTelefone,
    obrigatorio: false,
    maxLength: 20,
  },
];

export default function Clientes() {
  const navigate = useNavigate();

  return (
    <CadastroPage
      titulo="Clientes"
      descricao="Gerencie os clientes que solicitam suporte."
      endpoint="/clientes"
      campos={campos}
      acaoExtra={{
        label: "Ver chamados",
        icon: Ticket,
        onClick: (cliente) =>
          navigate(`/chamados?clienteId=${cliente.id}`),
      }}
    />
  );
}
