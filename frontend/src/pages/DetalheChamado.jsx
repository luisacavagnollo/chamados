
import { Link, useParams } from "react-router-dom";

export default function DetalheChamado() {
  const { id } = useParams();

  return (
    <div className="pagina">
      <Link to="/chamados">Voltar para chamados</Link>

      <h1>Chamado #{id}</h1>
      <p>Os detalhes deste chamado serão implementados aqui.</p>
    </div>
  );
}
