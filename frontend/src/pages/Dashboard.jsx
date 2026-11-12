
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  Ticket,
  CircleDot,
  Clock3,
  Hourglass,
  CheckCircle2,
  Plus,
  ArrowRight,
  RefreshCw,
  CalendarDays,
} from "lucide-react";

import api from "../services/api";
import ModalChamado from "../components/ModalChamado";

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

function formatarData(data) {
  if (!data) return "—";

  const valor = new Date(data);

  if (Number.isNaN(valor.getTime())) return "—";

  return valor.toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export default function Dashboard() {
  const navigate = useNavigate();

  const [chamados, setChamados] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [modalAberto, setModalAberto] = useState(false);
  const [recarregar, setRecarregar] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function carregarDashboard() {
      try {
        setCarregando(true);
        setErro("");

        const resposta = await api.get("/chamados", {
          signal: controller.signal,
        });

        if (!controller.signal.aborted) {
          setChamados(resposta.data);
        }
      } catch (error) {
        if (
          error.code === "ERR_CANCELED" ||
          controller.signal.aborted
        ) {
          return;
        }

        console.error("Erro ao carregar dashboard:", error);

        setErro(
          "Não foi possível carregar os dados da Dashboard."
        );
      } finally {
        if (!controller.signal.aborted) {
          setCarregando(false);
        }
      }
    }

    carregarDashboard();

    return () => controller.abort();
  }, [recarregar]);

  function atualizarDashboard() {
    setRecarregar((anterior) => anterior + 1);
  }

  function chamadoSalvo() {
    setModalAberto(false);
    setMensagem("Chamado cadastrado com sucesso!");
    atualizarDashboard();
  }

  // Indicadores calculados a partir dos dados reais.
  const total = chamados.length;

  const abertos = chamados.filter(
    (chamado) => chamado.status === "ABERTO"
  ).length;

  const emAtendimento = chamados.filter(
    (chamado) => chamado.status === "EM_ATENDIMENTO"
  ).length;

  const aguardando = chamados.filter(
    (chamado) => chamado.status === "AGUARDANDO_CLIENTE"
  ).length;

  const fechados = chamados.filter(
    (chamado) => chamado.status === "FECHADO"
  ).length;

  // Seleciona os cinco chamados mais recentes.
  const recentes = [...chamados]
    .sort((a, b) => {
      return (
        new Date(b.dataAbertura).getTime() -
        new Date(a.dataAbertura).getTime()
      );
    })
    .slice(0, 5);

  const indicadores = [
    {
      titulo: "Total de chamados",
      valor: total,
      icone: Ticket,
      cor: "azul",
      filtro: "",
    },
    {
      titulo: "Abertos",
      valor: abertos,
      icone: CircleDot,
      cor: "azul",
      filtro: "ABERTO",
    },
    {
      titulo: "Em atendimento",
      valor: emAtendimento,
      icone: Clock3,
      cor: "laranja",
      filtro: "EM_ATENDIMENTO",
    },
    {
      titulo: "Aguardando cliente",
      valor: aguardando,
      icone: Hourglass,
      cor: "cinza",
      filtro: "AGUARDANDO_CLIENTE",
    },
    {
      titulo: "Fechados",
      valor: fechados,
      icone: CheckCircle2,
      cor: "verde",
      filtro: "FECHADO",
    },
  ];

  function abrirListaFiltrada(status) {
    if (!status) {
      navigate("/chamados");
      return;
    }

    navigate(
      `/chamados?status=${encodeURIComponent(status)}`
    );
  }

  return (
    <div className="pagina dashboard-page">
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p>
            Acompanhe os indicadores e os atendimentos
            do sistema.
          </p>
        </div>

        <div className="dashboard-header-acoes">
          <button
            className="btn-secondary"
            onClick={atualizarDashboard}
            disabled={carregando}
          >
            <RefreshCw size={17} />
            Atualizar
          </button>

          <button
            className="btn-primary"
            onClick={() => setModalAberto(true)}
          >
            <Plus size={18} />
            Novo chamado
          </button>
        </div>
      </div>

      {mensagem && (
        <div className="success-message" role="status">
          {mensagem}
        </div>
      )}

      {erro && (
        <div className="error-message" role="alert">
          {erro}
        </div>
      )}

      {carregando ? (
        <div className="card">
          <p>Carregando informações da Dashboard...</p>
        </div>
      ) : !erro && (
        <>
          {/* INDICADORES */}
          <section className="dashboard-indicadores">
            {indicadores.map((indicador) => {
              const Icone = indicador.icone;

              return (
                <button
                  type="button"
                  className={`indicador-card ${indicador.cor}`}
                  key={indicador.titulo}
                  onClick={() =>
                    abrirListaFiltrada(indicador.filtro)
                  }
                  title={`Ver chamados: ${indicador.titulo}`}
                >
                  <div className="indicador-topo">
                    <span>{indicador.titulo}</span>

                    <div className="indicador-icone">
                      <Icone size={20} />
                    </div>
                  </div>

                  <strong className="indicador-valor">
                    {indicador.valor}
                  </strong>

                  <span className="indicador-link">
                    Ver chamados
                    <ArrowRight size={14} />
                  </span>
                </button>
              );
            })}
          </section>

          {/* CHAMADOS RECENTES */}
          <section className="card dashboard-recentes">
            <div className="dashboard-secao-header">
              <div>
                <h2>Chamados recentes</h2>
                <p>
                  Últimas solicitações registradas
                  no sistema.
                </p>
              </div>

              <button
                className="btn-link"
                onClick={() => navigate("/chamados")}
              >
                Ver todos
                <ArrowRight size={17} />
              </button>
            </div>

            {recentes.length === 0 ? (
              <div className="dashboard-vazio">
                <Ticket size={32} />

                <h3>Nenhum chamado cadastrado</h3>

                <p>
                  Comece registrando a primeira
                  solicitação de suporte.
                </p>

                <button
                  className="btn-primary"
                  onClick={() => setModalAberto(true)}
                >
                  <Plus size={17} />
                  Criar chamado
                </button>
              </div>
            ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Título</th>
                      <th>Cliente</th>
                      <th>Prioridade</th>
                      <th>Status</th>
                      <th>Data</th>
                    </tr>
                  </thead>

                  <tbody>
                    {recentes.map((chamado) => (
                      <tr
                        key={chamado.id}
                        className="clickable-row"
                        tabIndex={0}
                        role="link"
                        onClick={() =>
                          navigate(
                            `/chamados/${chamado.id}`
                          )
                        }
                        onKeyDown={(e) => {
                          if (
                            e.key === "Enter" ||
                            e.key === " "
                          ) {
                            e.preventDefault();
                            navigate(
                              `/chamados/${chamado.id}`
                            );
                          }
                        }}
                      >
                        <td>#{chamado.id}</td>

                        <td className="dashboard-titulo-chamado">
                          {chamado.titulo}
                        </td>

                        <td>{chamado.clienteNome}</td>

                        <td>
                          <span
                            className={`badge-prioridade ${chamado.prioridade}`}
                          >
                            {
                              prioridadeLabels[
                                chamado.prioridade
                              ]
                            }
                          </span>
                        </td>

                        <td>
                          <span
                            className={`badge-status ${chamado.status}`}
                          >
                            {
                              statusLabels[
                                chamado.status
                              ]
                            }
                          </span>
                        </td>

                        <td>
                          <span className="dashboard-data">
                            <CalendarDays size={14} />
                            {formatarData(
                              chamado.dataAbertura
                            )}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </>
      )}

      {/* MODAL DE NOVO CHAMADO */}
      <ModalChamado
        aberto={modalAberto}
        onClose={() => setModalAberto(false)}
        onSalvo={chamadoSalvo}
      />
    </div>
  );
}
