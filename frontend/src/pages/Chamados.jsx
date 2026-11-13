
import { useEffect, useState } from "react";
import {
  useNavigate,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import {
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  X,
  ChevronLeft,
  ChevronRight,
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

const TAMANHO_PAGINA = 10;

export default function Chamados() {
  const navigate = useNavigate();
  const location = useLocation();

  const [searchParams, setSearchParams] = useSearchParams();

  const status = searchParams.get("status") || "";
  const prioridade = searchParams.get("prioridade") || "";
  const clienteId = searchParams.get("clienteId") || "";
  const tecnicoId = searchParams.get("tecnicoId") || "";
  const categoriaId = searchParams.get("categoriaId") || "";
  const busca = searchParams.get("busca") || "";

  const paginaParam = Number(searchParams.get("page") || 0);

  const pagina =
    Number.isInteger(paginaParam) && paginaParam >= 0
      ? paginaParam
      : 0;

  const [chamados, setChamados] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [tecnicos, setTecnicos] = useState([]);
  const [categorias, setCategorias] = useState([]);

  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalElementos, setTotalElementos] = useState(0);

  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [erroOpcoesFiltro, setErroOpcoesFiltro] = useState("");
  const [mensagem, setMensagem] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [recarregar, setRecarregar] = useState(0);

  // Carrega as opções dos filtros.
  useEffect(() => {
    let ativo = true;

    async function carregarOpcoesFiltro() {
      try {
        const [clientesResposta, tecnicosResposta, categoriasResposta] =
          await Promise.all([
            api.get("/clientes"),
            api.get("/tecnicos"),
            api.get("/categorias"),
          ]);

        if (ativo) {
          setClientes(clientesResposta.data);
          setTecnicos(tecnicosResposta.data);
          setCategorias(categoriasResposta.data);
          setErroOpcoesFiltro("");
        }
      } catch (error) {
        console.error("Erro ao carregar opções dos filtros:", error);
        if (ativo) {
          setErroOpcoesFiltro(
            "Não foi possível carregar as opções dos filtros."
          );
        }
      }
    }

    carregarOpcoesFiltro();

    return () => {
      ativo = false;
    };
  }, []);

  // Carrega os chamados usando filtros e paginação.
  useEffect(() => {
    const controller = new AbortController();

    async function carregarChamados() {
      try {
        setCarregando(true);
        setErro("");

        const params = {
          page: pagina,
          size: TAMANHO_PAGINA,
          sort: "dataAbertura,desc",
        };

        if (status) params.status = status;
        if (prioridade) params.prioridade = prioridade;
        if (clienteId) params.clienteId = clienteId;
        if (tecnicoId) params.tecnicoId = tecnicoId;
        if (categoriaId) params.categoriaId = categoriaId;

        const resposta = await api.get("/chamados/filtro", {
          params,
          signal: controller.signal,
        });

        const dados = resposta.data;

        setChamados(dados.content || []);
        setTotalPaginas(dados.totalPages || 0);
        setTotalElementos(dados.totalElements || 0);
      } catch (error) {
        if (
          error.code === "ERR_CANCELED" ||
          controller.signal.aborted
        ) {
          return;
        }

        console.error("Erro ao carregar chamados:", error);
        setErro("Não foi possível carregar os chamados.");
      } finally {
        if (!controller.signal.aborted) {
          setCarregando(false);
        }
      }
    }

    carregarChamados();

    return () => controller.abort();
  }, [
    pagina,
    status,
    prioridade,
    clienteId,
    tecnicoId,
    categoriaId,
    recarregar,
  ]);

  function alterarFiltro(nome, valor) {
    const novos = new URLSearchParams(searchParams);

    if (valor) {
      novos.set(nome, valor);
    } else {
      novos.delete(nome);
    }

    // Ao alterar filtros, voltamos para a primeira página.
    if (nome !== "page") {
      novos.delete("page");
    }

    setSearchParams(novos);
  }

  function limparFiltros() {
    setSearchParams({});
  }

  function mudarPagina(novaPagina) {
    if (
      novaPagina < 0 ||
      novaPagina >= totalPaginas
    ) {
      return;
    }

    alterarFiltro("page", String(novaPagina));
  }

  function atualizar() {
    setRecarregar((valor) => valor + 1);
  }

  function chamadoSalvo() {
    setMensagem("Chamado cadastrado com sucesso!");
    setModalAberto(false);

    // Volta para a página inicial após criar.
    alterarFiltro("page", "0");
    atualizar();
  }

  // Pesquisa local nos chamados da página recebida.
  const chamadosFiltrados = chamados.filter((chamado) => {
    const termo = busca.trim().toLowerCase();

    if (!termo) return true;

    return (
      chamado.titulo.toLowerCase().includes(termo) ||
      String(chamado.id).includes(termo)
    );
  });

  function abrirChamado(id) {
    navigate(`/chamados/${id}`, {
      state: {
        retorno: location.pathname + location.search,
      },
    });
  }

  return (
    <div className="pagina">
      <div className="page-header">
        <div>
          <h1>Chamados</h1>
          <p>
            Gerencie e acompanhe as solicitações de suporte.
          </p>
        </div>

        <button
          className="btn-primary"
          onClick={() => setModalAberto(true)}
        >
          <Plus size={18} />
          Novo chamado
        </button>
      </div>

      {mensagem && (
        <div className="success-message" role="status">
          {mensagem}
        </div>
      )}

      {/* FILTROS */}
      <section className="card filtros-card">
        <div className="filtros-header">
          <h2>
            <SlidersHorizontal size={19} />
            Filtros
          </h2>

          <button
            className="btn-limpar-filtros"
            onClick={limparFiltros}
          >
            <X size={16} />
            Limpar filtros
          </button>
        </div>

        {erroOpcoesFiltro && (
          <div className="error-message" role="alert">
            {erroOpcoesFiltro}
          </div>
        )}

        <div className="filtros-grid">
          <div className="form-group">
            <label htmlFor="buscaChamado">
              Pesquisar
            </label>

            <div className="input-com-icone">
              <Search size={18} />

              <input
                id="buscaChamado"
                type="text"
                placeholder="Título ou ID..."
                value={busca}
                onChange={(e) =>
                  alterarFiltro("busca", e.target.value)
                }
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="filtroStatus">
              Status
            </label>

            <select
              id="filtroStatus"
              value={status}
              onChange={(e) =>
                alterarFiltro("status", e.target.value)
              }
            >
              <option value="">Todos os status</option>

              {Object.entries(statusLabels).map(
                ([valor, nome]) => (
                  <option key={valor} value={valor}>
                    {nome}
                  </option>
                )
              )}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="filtroPrioridade">
              Prioridade
            </label>

            <select
              id="filtroPrioridade"
              value={prioridade}
              onChange={(e) =>
                alterarFiltro("prioridade", e.target.value)
              }
            >
              <option value="">Todas as prioridades</option>

              {Object.entries(prioridadeLabels).map(
                ([valor, nome]) => (
                  <option key={valor} value={valor}>
                    {nome}
                  </option>
                )
              )}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="filtroCliente">
              Cliente
            </label>

            <select
              id="filtroCliente"
              value={clienteId}
              onChange={(e) =>
                alterarFiltro("clienteId", e.target.value)
              }
            >
              <option value="">Todos os clientes</option>

              {clientes.map((cliente) => (
                <option
                  key={cliente.id}
                  value={cliente.id}
                >
                  {cliente.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="filtroTecnico">
              Técnico
            </label>

            <select
              id="filtroTecnico"
              value={tecnicoId}
              onChange={(e) =>
                alterarFiltro("tecnicoId", e.target.value)
              }
            >
              <option value="">Todos os técnicos</option>

              {tecnicos.map((tecnico) => (
                <option
                  key={tecnico.id}
                  value={tecnico.id}
                >
                  {tecnico.nome}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="filtroCategoria">
              Categoria
            </label>

            <select
              id="filtroCategoria"
              value={categoriaId}
              onChange={(e) =>
                alterarFiltro("categoriaId", e.target.value)
              }
            >
              <option value="">Todas as categorias</option>

              {categorias.map((categoria) => (
                <option
                  key={categoria.id}
                  value={categoria.id}
                >
                  {categoria.nome}
                </option>
              ))}
            </select>
          </div>
        </div>
      </section>

      {/* TABELA */}
      <section className="card">
        <div className="tabela-header">
          <div>
            <h2>Lista de chamados</h2>

            <p>
              {totalElementos} registro(s)
              {busca ? " antes da pesquisa local" : ""}
            </p>
          </div>

          <button
            className="btn-secondary"
            onClick={atualizar}
            disabled={carregando}
          >
            <RefreshCw size={17} />
            Atualizar
          </button>
        </div>

        {erro && (
          <div className="error-message" role="alert">
            {erro}
          </div>
        )}

        {carregando ? (
          <p>Carregando chamados...</p>
        ) : !erro && (
          <>
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
                      <td
                        colSpan={7}
                        className="empty-state"
                      >
                        {busca
                          ? "Nenhum chamado corresponde à pesquisa nesta página."
                          : "Nenhum chamado encontrado para os filtros selecionados."}
                      </td>
                    </tr>
                  ) : (
                    chamadosFiltrados.map((chamado) => (
                      <tr
                        key={chamado.id}
                        className="clickable-row"
                        onClick={() =>
                          abrirChamado(chamado.id)
                        }
                      >
                        <td>#{chamado.id}</td>
                        <td>{chamado.titulo}</td>
                        <td>{chamado.clienteNome}</td>
                        <td>{chamado.categoriaNome}</td>

                        <td>
                          {chamado.tecnicoNome ||
                            "Não atribuído"}
                        </td>

                        <td>
                          <span
                            className={`badge-prioridade ${chamado.prioridade}`}
                          >
                            {prioridadeLabels[
                              chamado.prioridade
                            ]}
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

            {/* PAGINAÇÃO */}
            {totalPaginas > 0 && (
              <div className="paginacao">
                <span>
                  Página {pagina + 1} de {totalPaginas}
                </span>

                <div className="paginacao-botoes">
                  <button
                    className="btn-secondary"
                    disabled={pagina === 0}
                    onClick={() =>
                      mudarPagina(pagina - 1)
                    }
                  >
                    <ChevronLeft size={17} />
                    Anterior
                  </button>

                  <button
                    className="btn-secondary"
                    disabled={
                      pagina >= totalPaginas - 1
                    }
                    onClick={() =>
                      mudarPagina(pagina + 1)
                    }
                  >
                    Próximo
                    <ChevronRight size={17} />
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </section>

      {/* MODAL DE CADASTRO */}
      <ModalChamado
        aberto={modalAberto}
        onClose={() => setModalAberto(false)}
        onSalvo={chamadoSalvo}
      />
    </div>
  );
}
