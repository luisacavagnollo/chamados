
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, RefreshCw } from "lucide-react";
import api from "../services/api";

const statusLabels = {
  ABERTO: "Aberto",
  EM_ATENDIMENTO: "Em atendimento",
  AGUARDANDO_CLIENTE: "Aguardando cliente",
  FECHADO: "Fechado",
};

const prioridadeLabels = {
  BAIXA: "Baixa",
  MEDIA: "Média",
  ALTA: "Alta",
  CRITICA: "Crítica",
};

export default function Chamados() {
  const [chamados, setChamados] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [busca, setBusca] = useState("");

  const navigate = useNavigate();

  async function carregarChamados() {
    try {
      setCarregando(true);
      setErro("");

      const resposta = await api.get("/chamados");
      setChamados(resposta.data);
    } catch (error) {
      console.error(error);
      setErro("Não foi possível carregar os chamados.");
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    carregarChamados();
  }, []);

  const chamadosFiltrados = chamados.filter((chamado) => {
    const termo = busca.toLowerCase().trim();

    return (
      chamado.titulo.toLowerCase().includes(termo) ||
      String(chamado.id).includes(termo)
    );
  });

  return (
    <div className="pagina">
      <div className="page-header">
        <div>
          <h1>Chamados</h1>
          <p>Gerencie as solicitações de suporte técnico.</p>
        </div>

        <button
          className="btn-primary"
          onClick={() =>
            alert("Vamos implementar o cadastro na próxima etapa.")
          }
        >
          <Plus size={18} />
          Novo chamado
        </button>
      </div>

      <section className="card">
        <div className="table-toolbar">
          <input
            type="text"
            placeholder="Pesquisar por título ou ID..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />

          <button
            className="btn-secondary"
            onClick={carregarChamados}
          >
            <RefreshCw size={17} />
            Atualizar
          </button>
        </div>

        {carregando && <p>Carregando chamados...</p>}

        {erro && (
          <div className="error-message">
            {erro}
            <button onClick={carregarChamados}>
              Tentar novamente
            </button>
          </div>
        )}

        {!carregando && !erro && (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Título</th>
                  <th>Cliente</th>
                  <th>Categoria</th>
                  <th>Técnico</th>
                  <th>Prioridade</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {chamadosFiltrados.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="empty-state">
                      Nenhum chamado encontrado.
                    </td>
                  </tr>
                ) : (
                  chamadosFiltrados.map((chamado) => (
                    <tr
                      key={chamado.id}
                      className="clickable-row"
                      onClick={() =>
                        navigate(`/chamados/${chamado.id}`)
                      }
                    >
                      <td>#{chamado.id}</td>
                      <td>{chamado.titulo}</td>
                      <td>{chamado.clienteNome}</td>
                      <td>{chamado.categoriaNome}</td>
                      <td>
                        {chamado.tecnicoNome || "Não atribuído"}
                      </td>
                      <td>
                        <span
                          className={`badge-prioridade ${chamado.prioridade}`}
                        >
                          {prioridadeLabels[chamado.prioridade]}
                        </span>
                      </td>
                      <td>
                        <span
                          className={`badge-status ${chamado.status}`}
                        >
                          {statusLabels[chamado.status]}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
