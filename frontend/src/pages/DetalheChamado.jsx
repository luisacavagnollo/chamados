
import { useEffect, useState } from "react";
import {
  useNavigate,
  useParams,
  useLocation,
} from "react-router-dom";
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Send,
  Save,
  X,
  CalendarDays,
  UserRound,
  MessageSquare,
} from "lucide-react";

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

const formularioVazio = {
  titulo: "",
  descricao: "",
  clienteId: "",
  categoriaId: "",
  prioridade: "MEDIA",
  tecnicoId: "",
};

function obterErro(error) {
  const dados = error.response?.data;

  if (dados?.detalhes?.length) {
    return dados.detalhes.join("; ");
  }

  return dados?.erro || "Não foi possível concluir a operação.";
}

function formatarData(data) {
  if (!data) return "—";

  const valor = new Date(data);

  if (Number.isNaN(valor.getTime())) return "—";

  return valor.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function DetalheChamado() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const voltarParaChamados =
    location.state?.retorno?.startsWith("/chamados?")
      ? location.state.retorno
      : "/chamados";

  const [chamado, setChamado] = useState(null);
  const [comentarios, setComentarios] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [tecnicos, setTecnicos] = useState([]);

  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");

  const [modalEdicao, setModalEdicao] = useState(false);
  const [formulario, setFormulario] = useState(formularioVazio);

  const [novoStatus, setNovoStatus] = useState("");
  const [tecnicoSelecionado, setTecnicoSelecionado] = useState("");

  const [autor, setAutor] = useState("");
  const [textoComentario, setTextoComentario] = useState("");

  const fechado = chamado?.status === "FECHADO";

  useEffect(() => {
    let ativo = true;

    async function carregarInicial() {
      try {
        setCarregando(true);
        setErro("");

        const [
          respostaChamado,
          respostaComentarios,
          respostaClientes,
          respostaCategorias,
          respostaTecnicos,
        ] = await Promise.all([
          api.get(`/chamados/${id}`),
          api.get(`/chamados/${id}/comentarios`),
          api.get("/clientes"),
          api.get("/categorias"),
          api.get("/tecnicos"),
        ]);

        if (!ativo) return;

        const dados = respostaChamado.data;

        setChamado(dados);
        setComentarios(respostaComentarios.data);
        setClientes(respostaClientes.data);
        setCategorias(respostaCategorias.data);
        setTecnicos(respostaTecnicos.data);

        setNovoStatus(dados.status);
        setTecnicoSelecionado(
          dados.tecnicoId != null
            ? String(dados.tecnicoId)
            : ""
        );
      } catch (error) {
        if (ativo) {
          setErro(obterErro(error));
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    carregarInicial();

    return () => {
      ativo = false;
    };
  }, [id]);

  async function executarAcao(acao, sucesso) {
    try {
      setSalvando(true);
      setErro("");
      setMensagem("");

      const resposta = await acao();

      setMensagem(sucesso);

      return resposta;
    } catch (error) {
      setErro(obterErro(error));
      return null;
    } finally {
      setSalvando(false);
    }
  }

  function abrirEdicao() {
    if (!chamado || fechado) return;

    setFormulario({
      titulo: chamado.titulo,
      descricao: chamado.descricao,
      clienteId: String(chamado.clienteId),
      categoriaId: String(chamado.categoriaId),
      prioridade: chamado.prioridade,
      tecnicoId:
        chamado.tecnicoId != null
          ? String(chamado.tecnicoId)
          : "",
    });

    setErro("");
    setModalEdicao(true);
  }

  function alterarCampo(evento) {
    const { name, value } = evento.target;

    setFormulario((anterior) => ({
      ...anterior,
      [name]: value,
    }));
  }

  async function salvarEdicao(evento) {
    evento.preventDefault();

    if (salvando) return;

    const dados = {
      titulo: formulario.titulo.trim(),
      descricao: formulario.descricao.trim(),
      prioridade: formulario.prioridade,
      clienteId: Number(formulario.clienteId),
      categoriaId: Number(formulario.categoriaId),
      tecnicoId: formulario.tecnicoId
        ? Number(formulario.tecnicoId)
        : null,
    };

    const resposta = await executarAcao(
      () => api.put(`/chamados/${id}`, dados),
      "Chamado atualizado com sucesso!"
    );

    if (resposta) {
      setChamado(resposta.data);
      setTecnicoSelecionado(
        resposta.data.tecnicoId != null
          ? String(resposta.data.tecnicoId)
          : ""
      );
      setModalEdicao(false);
    }
  }

  async function alterarStatus() {
    if (salvando || fechado || novoStatus === chamado.status) {
      return;
    }

    if (novoStatus === "FECHADO") {
      const confirmou = window.confirm(
        "Deseja fechar este chamado? Depois disso, ele não poderá ser editado."
      );

      if (!confirmou) return;
    }

    const resposta = await executarAcao(
      () =>
        api.patch(`/chamados/${id}/status`, {
          status: novoStatus,
        }),
      "Status atualizado com sucesso!"
    );

    if (resposta) {
      setChamado(resposta.data);
      setNovoStatus(resposta.data.status);
    }
  }

  async function atribuirTecnico() {
    if (salvando || fechado || !tecnicoSelecionado) {
      return;
    }

    const resposta = await executarAcao(
      () =>
        api.patch(`/chamados/${id}/tecnico`, {
          tecnicoId: Number(tecnicoSelecionado),
        }),
      "Técnico atribuído com sucesso!"
    );

    if (resposta) {
      setChamado(resposta.data);
    }
  }

  async function enviarComentario(evento) {
    evento.preventDefault();

    if (
      salvando ||
      fechado ||
      !autor.trim() ||
      !textoComentario.trim()
    ) {
      return;
    }

    const resposta = await executarAcao(
      () =>
        api.post(`/chamados/${id}/comentarios`, {
          autor: autor.trim(),
          texto: textoComentario.trim(),
        }),
      "Comentário adicionado com sucesso!"
    );

    if (resposta) {
      setComentarios((anteriores) => [
        ...anteriores,
        resposta.data,
      ]);

      setTextoComentario("");
    }
  }

  async function excluirChamado() {
    if (salvando) return;

    const confirmou = window.confirm(
      "Tem certeza que deseja excluir este chamado? Essa ação não pode ser desfeita."
    );

    if (!confirmou) return;

    const resposta = await executarAcao(
      () => api.delete(`/chamados/${id}`),
      "Chamado excluído com sucesso!"
    );

    if (resposta) {
      navigate("/chamados");
    }
  }

  if (carregando) {
    return (
      <div className="pagina">
        <p>Carregando detalhes do chamado...</p>
      </div>
    );
  }

  if (!chamado) {
    return (
      <div className="pagina">
        <button
          className="btn-secondary"
          onClick={() => navigate(voltarParaChamados)}
        >
          <ArrowLeft size={17} />
          Voltar
        </button>

        <div className="error-message">
          {erro || "Chamado não encontrado."}
        </div>
      </div>
    );
  }

  return (
    <div className="pagina detalhes-page">
      <button
        className="btn-voltar"
        onClick={() => navigate(voltarParaChamados)}
      >
        <ArrowLeft size={18} />
        Voltar para chamados
      </button>

      <div className="detalhes-header">
        <div>
          <span className="detalhes-id">
            Chamado #{chamado.id}
          </span>

          <h1>{chamado.titulo}</h1>

          <p>
            <CalendarDays size={15} />
            Aberto em {formatarData(chamado.dataAbertura)}
          </p>
        </div>

        <span className={`badge-status ${chamado.status}`}>
          {statusLabels[chamado.status]}
        </span>
      </div>

      {mensagem && (
        <div className="success-message" role="status">
          {mensagem}
        </div>
      )}

      {erro && !modalEdicao && (
        <div className="error-message" role="alert">
          {erro}
        </div>
      )}

      <div className="detalhes-grid">
        {/* COLUNA PRINCIPAL */}
        <div className="detalhes-principal">
          <section className="card detalhe-card">
            <div className="detalhe-card-header">
              <h2>Descrição do problema</h2>

              {!fechado && (
                <button
                  className="btn-secondary"
                  onClick={abrirEdicao}
                >
                  <Pencil size={16} />
                  Editar
                </button>
              )}
            </div>

            <p className="descricao-chamado">
              {chamado.descricao}
            </p>
          </section>

          <section className="card detalhe-card">
            <div className="detalhe-card-header">
              <h2>
                <MessageSquare size={19} />
                Comentários
              </h2>

              <span className="contador-comentarios">
                {comentarios.length}
              </span>
            </div>

            <div className="lista-comentarios">
              {comentarios.length === 0 ? (
                <p className="sem-comentarios">
                  Nenhum comentário registrado.
                </p>
              ) : (
                comentarios.map((comentario) => (
                  <div
                    className="comentario"
                    key={comentario.id}
                  >
                    <div className="comentario-avatar">
                      <UserRound size={18} />
                    </div>

                    <div className="comentario-conteudo">
                      <div className="comentario-header">
                        <strong>{comentario.autor}</strong>

                        <span>
                          {formatarData(comentario.data)}
                        </span>
                      </div>

                      <p>{comentario.texto}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

            {!fechado ? (
              <form
                className="form-comentario"
                onSubmit={enviarComentario}
              >
                <div className="form-group">
                  <label htmlFor="autorComentario">
                    Seu nome *
                  </label>

                  <input
                    id="autorComentario"
                    type="text"
                    value={autor}
                    onChange={(e) => setAutor(e.target.value)}
                    placeholder="Nome de quem está comentando"
                    maxLength={100}
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="textoComentario">
                    Novo comentário *
                  </label>

                  <textarea
                    id="textoComentario"
                    value={textoComentario}
                    onChange={(e) =>
                      setTextoComentario(e.target.value)
                    }
                    placeholder="Escreva uma atualização sobre o atendimento..."
                    maxLength={1000}
                    rows={3}
                    required
                  />
                </div>

                <div className="comentario-footer">
                  <button
                    className="btn-primary"
                    type="submit"
                    disabled={
                      salvando ||
                      !autor.trim() ||
                      !textoComentario.trim()
                    }
                  >
                    <Send size={17} />
                    Enviar comentário
                  </button>
                </div>
              </form>
            ) : (
              <div className="aviso-fechado">
                Este chamado está fechado e não aceita
                novos comentários.
              </div>
            )}
          </section>
        </div>

        {/* PAINEL LATERAL */}
        <aside className="detalhes-lateral">
          <section className="card detalhe-card">
            <h2>Informações do chamado</h2>

            <div className="info-lista">
              <div className="info-item">
                <span>Status</span>
                <strong>
                  {statusLabels[chamado.status]}
                </strong>
              </div>

              <div className="info-item">
                <span>Prioridade</span>
                <strong>
                  <span
                    className={`badge-prioridade ${chamado.prioridade}`}
                  >
                    {prioridadeLabels[chamado.prioridade]}
                  </span>
                </strong>
              </div>

              <div className="info-item">
                <span>Cliente</span>
                <strong>{chamado.clienteNome}</strong>
              </div>

              <div className="info-item">
                <span>Categoria</span>
                <strong>{chamado.categoriaNome}</strong>
              </div>

              <div className="info-item">
                <span>Técnico responsável</span>
                <strong>
                  {chamado.tecnicoNome || "Não atribuído"}
                </strong>
              </div>

              <div className="info-item">
                <span>Data de abertura</span>
                <strong>
                  {formatarData(chamado.dataAbertura)}
                </strong>
              </div>

              {chamado.dataFechamento && (
                <div className="info-item">
                  <span>Data de fechamento</span>
                  <strong>
                    {formatarData(chamado.dataFechamento)}
                  </strong>
                </div>
              )}
            </div>
          </section>

          {!fechado && (
            <section className="card detalhe-card">
              <h2>Gerenciar atendimento</h2>

              <div className="form-group">
                <label htmlFor="novoStatus">
                  Alterar status
                </label>

                <select
                  id="novoStatus"
                  value={novoStatus}
                  onChange={(e) =>
                    setNovoStatus(e.target.value)
                  }
                >
                  {Object.entries(statusLabels).map(
                    ([valor, nome]) => (
                      <option key={valor} value={valor}>
                        {nome}
                      </option>
                    )
                  )}
                </select>

                <button
                  className="btn-primary btn-largura-total"
                  onClick={alterarStatus}
                  disabled={
                    salvando ||
                    novoStatus === chamado.status
                  }
                >
                  <Save size={16} />
                  Salvar status
                </button>
              </div>

              <div className="form-group">
                <label htmlFor="tecnicoResponsavel">
                  Atribuir técnico
                </label>

                <select
                  id="tecnicoResponsavel"
                  value={tecnicoSelecionado}
                  onChange={(e) =>
                    setTecnicoSelecionado(e.target.value)
                  }
                >
                  <option value="">
                    Selecione um técnico
                  </option>

                  {tecnicos.map((tecnico) => (
                    <option
                      key={tecnico.id}
                      value={tecnico.id}
                    >
                      {tecnico.nome}
                    </option>
                  ))}
                </select>

                <button
                  className="btn-secondary btn-largura-total"
                  onClick={atribuirTecnico}
                  disabled={
                    salvando ||
                    !tecnicoSelecionado ||
                    String(chamado.tecnicoId ?? "") ===
                      tecnicoSelecionado
                  }
                >
                  Atribuir responsável
                </button>
              </div>
            </section>
          )}

          <section className="card detalhe-card">
            <h2>Outras ações</h2>

            <button
              className="btn-excluir"
              onClick={excluirChamado}
              disabled={salvando || comentarios.length > 0}
              title={
                comentarios.length > 0
                  ? "Chamados com comentários não podem ser excluídos"
                  : "Excluir chamado"
              }
            >
              <Trash2 size={17} />
              Excluir chamado
            </button>

            {comentarios.length > 0 && (
              <p className="aviso-exclusao">
                Chamados com comentários não podem ser
                excluídos.
              </p>
            )}
          </section>
        </aside>
      </div>

      {/* MODAL DE EDIÇÃO */}
      {modalEdicao && (
        <div className="modal-overlay">
          <div
            className="modal-content modal-chamado"
            role="dialog"
            aria-modal="true"
            aria-labelledby="titulo-edicao-chamado"
          >
            <div className="modal-header">
              <h2 id="titulo-edicao-chamado">
                Editar chamado #{chamado.id}
              </h2>

              <button
                className="btn-icon"
                onClick={() => {
                  setModalEdicao(false);
                  setErro("");
                }}
                disabled={salvando}
                aria-label="Fechar"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={salvarEdicao}>
              <div className="modal-body">
                {erro && (
                  <div className="error-message">
                    {erro}
                  </div>
                )}

                <div className="form-group">
                  <label htmlFor="editarTitulo">
                    Título *
                  </label>

                  <input
                    id="editarTitulo"
                    name="titulo"
                    value={formulario.titulo}
                    onChange={alterarCampo}
                    maxLength={150}
                    required
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="editarDescricao">
                    Descrição *
                  </label>

                  <textarea
                    id="editarDescricao"
                    name="descricao"
                    value={formulario.descricao}
                    onChange={alterarCampo}
                    maxLength={2000}
                    rows={4}
                    required
                  />
                </div>

                <div className="form-grid">
                  <div className="form-group">
                    <label htmlFor="editarCliente">
                      Cliente *
                    </label>

                    <select
                      id="editarCliente"
                      name="clienteId"
                      value={formulario.clienteId}
                      onChange={alterarCampo}
                      required
                    >
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
                    <label htmlFor="editarCategoria">
                      Categoria *
                    </label>

                    <select
                      id="editarCategoria"
                      name="categoriaId"
                      value={formulario.categoriaId}
                      onChange={alterarCampo}
                      required
                    >
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

                  <div className="form-group">
                    <label htmlFor="editarPrioridade">
                      Prioridade *
                    </label>

                    <select
                      id="editarPrioridade"
                      name="prioridade"
                      value={formulario.prioridade}
                      onChange={alterarCampo}
                    >
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
                    <label htmlFor="editarTecnico">
                      Técnico responsável
                    </label>

                    <select
                      id="editarTecnico"
                      name="tecnicoId"
                      value={formulario.tecnicoId}
                      onChange={alterarCampo}
                    >
                      <option value="">
                        Não atribuído
                      </option>

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
                </div>
              </div>

              <div className="modal-footer">
                <button
                  className="btn-secondary"
                  type="button"
                  onClick={() => {
                    setModalEdicao(false);
                    setErro("");
                  }}
                  disabled={salvando}
                >
                  Cancelar
                </button>

                <button
                  className="btn-primary"
                  type="submit"
                  disabled={
                    salvando ||
                    !formulario.titulo.trim() ||
                    !formulario.descricao.trim()
                  }
                >
                  <Save size={17} />
                  {salvando
                    ? "Salvando..."
                    : "Salvar alterações"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
