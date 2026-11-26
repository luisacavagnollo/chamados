import { Link } from "react-router-dom";
import { ShieldX } from "lucide-react";

export default function AccessDenied() {
  return (
    <section className="access-denied card" role="alert">
      <ShieldX size={32} />
      <h1>Acesso não autorizado</h1>
      <p>Seu perfil não tem permissão para acessar esta página.</p>
      <Link to="/" className="btn-primary">
        Voltar ao início
      </Link>
    </section>
  );
}
