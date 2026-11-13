
import { useEffect, useState } from "react";
import { X, Save } from "lucide-react";
import api from "../services/api";

const formularioInicial = {
  titulo: "",
  descricao: "",
  clienteId: "",
  categoriaId: "",
  prioridade: "MEDIA",
  tecnicoId: "",
};

function obterMensagemErro(error) {
  const dados = error.response?.data;

  if (dados?.detalhes?.length) {
    return dados.detalhes.join("; ");
  }

  return dados?.erro || "Não foi possível salvar o chamado.";
}

export default function ModalChamado({
  aberto,
  onClose,
  onSalvo,
}) {
  const [formulario, setFormulario] = useState(formularioInicial);
  const [clientes, setClientes] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [tecnicos, setTecnicos] = useState([]);

  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");

  useEffect(() => {
    if (!aberto) return;

    let ativo = true;

    setFormulario({ ...formularioInicial });
    setErro("");
    setCarregando(true);

    async function carregarDados() {
      try {
        const [resClientes, resCategorias, resTecnicos] =
          await Promise.all([
            api.get("/clientes"),
            api.get("/categorias"),
            api.get("/tecnicos"),
          ]);

        if (!ativo) return;

        setClientes(resClientes.data);
        setCategorias(resCategorias.data);
        setTecnicos(resTecnicos.data);
      } catch (error) {
        if (ativo) {
          setErro(
            "Não foi possível carregar clientes, categorias e técnicos."
          );
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    carregarDados();

    return () => {
      ativo = false;
    };
  }, [aberto]);

  if (!aberto) return null;

  function alterarCampo(evento) {
    const { name, value } = evento.target;

    setFormulario((anterior) => ({
      ...anterior,
      [name]: value,
    }));
  }

  async function salvar(evento) {
    evento.preventDefault();

    if (salvando || carregando) return;

    setSalvando(true);
    setErro("");

    const dados = {
      titulo: formulario.titulo.trim(),
      descricao: formulario.descricao.trim(),
      clienteId: Number(formulario.clienteId),
      categoriaId: Number(formulario.categoriaId),
      prioridade: formulario.prioridade,
      tecnicoId: formulario.tecnicoId
        ? Number(formulario.tecnicoId)
        : null,
    };

    try {
      await api.post("/chamados", dados);

      onSalvo();
      onClose();
    } catch (error) {
      setErro(obterMensagemErro(error));
    } finally {
      setSalvando(false);
    }
  }

  const podeSalvar =
    !carregando &&
    !salvando &&
    formulario.titulo.trim() !== "" &&
    formulario.descricao.trim() !== "" &&
    formulario.clienteId !== "" &&
    formulario.categoriaId !== "";

  return (
    <div
      className="modal-overlay"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !salvando) {
          onClose();
        }
      }}
    >
      <div
        className="modal-content modal-chamado"
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-modal-chamado"
      >
        <div className="modal-header">
          <div>
            <h2 id="titulo-modal-chamado">Novo chamado</h2>
            <p>Registre uma nova solicitação de suporte.</p>
          </div>

          <button
            type="button"
            className="btn-icon"
            onClick={onClose}
            disabled={salvando}
            aria-label="Fechar"
          >
            <X size={20} />
          </button>
        </div>

        {carregando ? (
          <div className="modal-body">
            <p>Carregando dados do formulário...</p>
          </div>
        ) : (
          <form onSubmit={salvar}>
            <div className="modal-body">
              {erro && (
                <div className="error-message" role="alert">
                  {erro}
                </div>
              )}

              {(clientes.length === 0 ||
                categorias.length === 0) && (
                <div className="error-message">
                  Cadastre pelo menos um cliente e uma
                  categoria antes de abrir um chamado.
                </div>
              )}

              <div className="form-group">
                <label htmlFor="titulo">
                  Título do chamado *
                </label>

                <input
                  id="titulo"
                  name="titulo"
                  type="text"
                  placeholder="Ex.: Computador não liga"
                  value={formulario.titulo}
                  onChange={alterarCampo}
                  maxLength={150}
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="descricao">
                  Descrição do problema *
                </label>

                <textarea
                  id="descricao"
                  name="descricao"
                  placeholder="Descreva detalhadamente o problema..."
                  value={formulario.descricao}
                  onChange={alterarCampo}
                  maxLength={2000}
                  rows={4}
                  required
                />
              </div>

              <div className="form-grid">
                <div className="form-group">
                  <label htmlFor="clienteId">Cliente *</label>

                  <select
                    id="clienteId"
                    name="clienteId"
                    value={formulario.clienteId}
                    onChange={alterarCampo}
                    required
                  >
                    <option value="">Selecione um cliente</option>

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
                  <label htmlFor="categoriaId">
                    Categoria *
                  </label>

                  <select
                    id="categoriaId"
                    name="categoriaId"
                    value={formulario.categoriaId}
                    onChange={alterarCampo}
                    required
                  >
                    <option value="">
                      Selecione uma categoria
                    </option>

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
                  <label htmlFor="prioridade">
                    Prioridade *
                  </label>

                  <select
                    id="prioridade"
                    name="prioridade"
                    value={formulario.prioridade}
                    onChange={alterarCampo}
                    required
                  >
                    <option value="BAIXA">Baixa</option>
                    <option value="MEDIA">Média</option>
                    <option value="ALTA">Alta</option>
                    <option value="CRITICA">Crítica</option>
                  </select>
                </div>

                <div className="form-group">
                  <label htmlFor="tecnicoId">
                    Técnico responsável
                  </label>

                  <select
                    id="tecnicoId"
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

              <div className="form-info">
                O chamado será criado com status
                <strong> Aberto</strong>.
              </div>
            </div>

            <div className="modal-footer">
              <button
                type="button"
                className="btn-secondary"
                onClick={onClose}
                disabled={salvando}
              >
                Cancelar
              </button>

              <button
                type="submit"
                className="btn-primary"
                disabled={
                  !podeSalvar ||
                  clientes.length === 0 ||
                  categorias.length === 0 ||
                  Boolean(erro && !clientes.length)
                }
              >
                <Save size={17} />
                {salvando ? "Salvando..." : "Salvar chamado"}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
