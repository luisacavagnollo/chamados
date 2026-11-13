
import { useEffect, useState } from "react";
import {
  Plus,
  Pencil,
  Trash2,
  X,
  RefreshCw,
} from "lucide-react";
import api from "../services/api";

export default function CadastroPage({
  titulo,
  descricao,
  endpoint,
  campos,
  acaoExtra,
}) {
  const [registros, setRegistros] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [modalAberto, setModalAberto] = useState(false);
  const [editandoId, setEditandoId] = useState(null);
  const [formulario, setFormulario] = useState({});
  const [busca, setBusca] = useState("");
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  const AcaoExtraIcon = acaoExtra?.icon;

  function obterErro(error) {
    const dados = error.response?.data;

    if (dados?.detalhes?.length) {
      return dados.detalhes.join("; ");
    }

    return (
      dados?.erro ||
      "Não foi possível concluir a operação."
    );
  }

  async function carregar() {
    try {
      setCarregando(true);
      setErro("");

      const resposta = await api.get(endpoint);
      setRegistros(resposta.data);
    } catch (error) {
      setErro(obterErro(error));
    } finally {
      setCarregando(false);
    }
  }

  useEffect(() => {
    let ativo = true;

    async function carregarInicial() {
      try {
        const resposta = await api.get(endpoint);

        if (ativo) {
          setRegistros(resposta.data);
          setErro("");
        }
      } catch (error) {
        if (ativo) setErro(obterErro(error));
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    carregarInicial();

    return () => {
      ativo = false;
    };
  }, [endpoint]);

  function abrirNovo() {
    const inicial = {};

    campos.forEach((campo) => {
      inicial[campo.nome] = "";
    });

    setFormulario(inicial);
    setEditandoId(null);
    setErro("");
    setMensagem("");
    setModalAberto(true);
  }

  function abrirEdicao(registro) {
    const valores = {};

    campos.forEach((campo) => {
      const valor = registro[campo.nome] ?? "";
      valores[campo.nome] = campo.formatar
        ? campo.formatar(valor)
        : valor;
    });

    setFormulario(valores);
    setEditandoId(registro.id);
    setErro("");
    setMensagem("");
    setModalAberto(true);
  }

  function fecharModal() {
    if (salvando) return;

    setModalAberto(false);
    setEditandoId(null);
    setErro("");
  }

  function alterarCampo(campo, valor) {
    const valorFormatado = campo.formatar
      ? campo.formatar(valor)
      : valor;

    setFormulario((anterior) => ({
      ...anterior,
      [campo.nome]: valorFormatado,
    }));
  }

  async function salvar(evento) {
    evento.preventDefault();

    try {
      setSalvando(true);
      setErro("");

      if (editandoId !== null) {
        await api.put(
          `${endpoint}/${editandoId}`,
          formulario
        );
      } else {
        await api.post(endpoint, formulario);
      }

      setModalAberto(false);
      setEditandoId(null);

      setMensagem(
        editandoId !== null
          ? "Registro atualizado com sucesso!"
          : "Registro cadastrado com sucesso!"
      );

      await carregar();
    } catch (error) {
      setErro(obterErro(error));
    } finally {
      setSalvando(false);
    }
  }

  async function excluir(registro) {
    const confirmou = window.confirm(
      `Deseja realmente excluir o registro #${registro.id}?`
    );

    if (!confirmou) return;

    try {
      setErro("");
      setMensagem("");

      await api.delete(`${endpoint}/${registro.id}`);

      setMensagem("Registro excluído com sucesso!");
      await carregar();
    } catch (error) {
      setErro(obterErro(error));
    }
  }

  const filtrados = registros.filter((registro) => {
    const termo = busca.trim().toLowerCase();

    return campos.some((campo) =>
      String(registro[campo.nome] ?? "")
        .toLowerCase()
        .includes(termo)
    );
  });

  return (
    <div className="pagina">
      <div className="page-header">
        <div>
          <h1>{titulo}</h1>
          <p>{descricao}</p>
        </div>

        <button
          className="btn-primary"
          onClick={abrirNovo}
        >
          <Plus size={18} />
          Novo cadastro
        </button>
      </div>

      {mensagem && (
        <div className="success-message" role="status">
          {mensagem}
        </div>
      )}

      {!modalAberto && erro && (
        <div className="error-message" role="alert">
          {erro}
        </div>
      )}

      <section className="card">
        <div className="table-toolbar">
          <input
            type="text"
            placeholder={`Pesquisar ${titulo.toLowerCase()}...`}
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />

          <button
            className="btn-secondary"
            onClick={carregar}
          >
            <RefreshCw size={17} />
            Atualizar
          </button>
        </div>

        {carregando ? (
          <p>Carregando registros...</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>ID</th>

                  {campos.map((campo) => (
                    <th key={campo.nome}>
                      {campo.label}
                    </th>
                  ))}

                  <th>Ações</th>
                </tr>
              </thead>

              <tbody>
                {filtrados.length === 0 ? (
                  <tr>
                    <td
                      colSpan={campos.length + 2}
                      className="empty-state"
                    >
                      Nenhum registro encontrado.
                    </td>
                  </tr>
                ) : (
                  filtrados.map((registro) => (
                    <tr key={registro.id}>
                      <td>#{registro.id}</td>

                      {campos.map((campo) => (
                        <td key={campo.nome}>
                          {registro[campo.nome]
                            ? campo.formatar
                              ? campo.formatar(
                                  registro[campo.nome]
                                )
                              : registro[campo.nome]
                            : "—"}
                        </td>
                      ))}

                      <td>
                        <div className="acoes-tabela">
                          {acaoExtra?.onClick && (
                            <button
                              className="btn-icon"
                              title={acaoExtra.label}
                              aria-label={`${acaoExtra.label} - registro ${registro.id}`}
                              onClick={() =>
                                acaoExtra.onClick(registro)
                              }
                            >
                              {AcaoExtraIcon && (
                                <AcaoExtraIcon size={17} />
                              )}
                            </button>
                          )}

                          <button
                            className="btn-icon"
                            title="Editar"
                            aria-label={`Editar registro ${registro.id}`}
                            onClick={() =>
                              abrirEdicao(registro)
                            }
                          >
                            <Pencil size={17} />
                          </button>

                          <button
                            className="btn-icon danger"
                            title="Excluir"
                            aria-label={`Excluir registro ${registro.id}`}
                            onClick={() =>
                              excluir(registro)
                            }
                          >
                            <Trash2 size={17} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {modalAberto && (
        <div
          className="modal-overlay"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) {
              fecharModal();
            }
          }}
        >
          <div
            className="modal-content"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cadastro-modal-titulo"
          >
            <div className="modal-header">
              <h2 id="cadastro-modal-titulo">
                {editandoId !== null
                  ? `Editar ${titulo}`
                  : `Novo cadastro - ${titulo}`}
              </h2>

              <button
                className="btn-icon"
                aria-label="Fechar"
                onClick={fecharModal}
                disabled={salvando}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={salvar}>
              <div className="modal-body">
                {campos.map((campo) => (
                  <div
                    className="form-group"
                    key={campo.nome}
                  >
                    <label htmlFor={campo.nome}>
                      {campo.label}
                      {campo.obrigatorio && " *"}
                    </label>

                    {campo.tipo === "textarea" ? (
                      <textarea
                        id={campo.nome}
                        value={
                          formulario[campo.nome] ?? ""
                        }
                        onChange={(e) =>
                          alterarCampo(
                            campo,
                            e.target.value
                          )
                        }
                        required={campo.obrigatorio}
                        maxLength={campo.maxLength}
                        rows={3}
                      />
                    ) : (
                      <input
                        id={campo.nome}
                        type={campo.tipo || "text"}
                        placeholder={campo.placeholder}
                        pattern={campo.pattern}
                        title={campo.title}
                        autoComplete={campo.autoComplete}
                        inputMode={campo.inputMode}
                        value={
                          formulario[campo.nome] ?? ""
                        }
                        onChange={(e) =>
                          alterarCampo(
                            campo,
                            e.target.value
                          )
                        }
                        required={campo.obrigatorio}
                        maxLength={campo.maxLength}
                      />
                    )}
                  </div>
                ))}

                {erro && (
                  <div
                    className="error-message"
                    role="alert"
                  >
                    {erro}
                  </div>
                )}
              </div>

              <div className="modal-footer">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={fecharModal}
                  disabled={salvando}
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  className="btn-primary"
                  disabled={salvando}
                >
                  {salvando
                    ? "Salvando..."
                    : "Salvar"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
