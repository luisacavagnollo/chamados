import { useEffect, useState } from "react";
import { Save } from "lucide-react";
import { useAuth } from "../context/useAuth";
import api from "../services/api";

export default function MeuCadastro() {
  const { identity } = useAuth();
  const [formulario, setFormulario] = useState({
    nome: "",
    telefone: "",
    email: "",
  });
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");

  useEffect(() => {
    let ativo = true;

    async function carregarCadastro() {
      try {
        const resposta = await api.get(`/clientes/${identity.clienteId}`);

        if (ativo) {
          setFormulario({
            nome: resposta.data.nome || "",
            telefone: resposta.data.telefone || "",
            email: resposta.data.email || "",
          });
        }
      } catch (error) {
        if (ativo) {
          setErro(
            error.response?.data?.erro ||
              "Não foi possível carregar seu cadastro."
          );
        }
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    if (identity.clienteId != null) {
      carregarCadastro();
    }

    return () => {
      ativo = false;
    };
  }, [identity.clienteId]);

  async function salvar(evento) {
    evento.preventDefault();
    setSalvando(true);
    setErro("");
    setMensagem("");

    try {
      const resposta = await api.put("/clientes/me", {
        nome: formulario.nome.trim(),
        telefone: formulario.telefone.trim(),
      });

      setFormulario((atual) => ({
        ...atual,
        nome: resposta.data.nome,
        telefone: resposta.data.telefone || "",
      }));
      setMensagem("Cadastro atualizado com sucesso.");
    } catch (error) {
      setErro(
        error.response?.data?.erro ||
          "Não foi possível atualizar seu cadastro."
      );
    } finally {
      setSalvando(false);
    }
  }

  const cadastroVinculado = identity.clienteId != null;
  const mensagemErro =
    !cadastroVinculado
      ? "A identidade não possui um cadastro de cliente vinculado."
      : erro;

  return (
    <div className="pagina">
      <div className="page-header">
        <div>
          <h1>Meu cadastro</h1>
          <p>Consulte seus dados e atualize seu nome ou telefone.</p>
        </div>
      </div>

      {mensagem && (
        <div className="success-message" role="status">{mensagem}</div>
      )}
      {mensagemErro && (
        <div className="error-message" role="alert">{mensagemErro}</div>
      )}

      <section className="card cadastro-proprio">
        {!cadastroVinculado ? null : carregando ? (
          <p>Carregando cadastro...</p>
        ) : (
          <form onSubmit={salvar}>
            <div className="form-group">
              <label htmlFor="cadastroNome">Nome</label>
              <input
                id="cadastroNome"
                value={formulario.nome}
                onChange={(event) =>
                  setFormulario((atual) => ({
                    ...atual,
                    nome: event.target.value,
                  }))
                }
                maxLength={100}
                required
              />
            </div>
            <div className="form-group">
              <label htmlFor="cadastroEmail">E-mail (não editável)</label>
              <input id="cadastroEmail" value={formulario.email} readOnly />
            </div>
            <div className="form-group">
              <label htmlFor="cadastroTelefone">Telefone</label>
              <input
                id="cadastroTelefone"
                type="tel"
                value={formulario.telefone}
                onChange={(event) =>
                  setFormulario((atual) => ({
                    ...atual,
                    telefone: event.target.value,
                  }))
                }
                maxLength={20}
              />
            </div>
            <button
              type="submit"
              className="btn-primary"
              disabled={salvando || !formulario.nome.trim()}
            >
              <Save size={17} />
              {salvando ? "Salvando..." : "Salvar alterações"}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
