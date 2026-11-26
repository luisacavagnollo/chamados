import {
  useCallback,
  useMemo,
  useState,
} from "react";
import { AuthContext } from "./authContext";
import api, {
  ativarAutenticacao,
  clearApiAuthentication,
  criarCabecalhoBasic,
  iniciarTentativaAutenticacao,
  obterEpocaAutenticacao,
} from "../services/api";

function identidadeValida(identidade) {
  return Boolean(
    identidade &&
      Number.isInteger(identidade.usuarioId) &&
      typeof identidade.nomeExibicao === "string" &&
      ["CLIENTE", "TECNICO", "ADMINISTRADOR"].includes(
        identidade.papel
      )
  );
}

export function AuthProvider({ children }) {
  const [identity, setIdentity] = useState(null);
  const [loading, setLoading] = useState(false);
  const [sessionVersion, setSessionVersion] = useState(0);

  const limparEstadoAutenticacao = useCallback(() => {
    setIdentity(null);
    setLoading(false);
    setSessionVersion((current) => current + 1);
  }, []);

  const encerrarSessao = useCallback(() => {
    clearApiAuthentication();
    limparEstadoAutenticacao();
  }, [limparEstadoAutenticacao]);

  const login = useCallback(
    async (usuario, senha) => {
      const authAttempt = iniciarTentativaAutenticacao();
      const header = criarCabecalhoBasic(usuario, senha);
      setIdentity(null);
      setLoading(true);

      try {
        const response = await api.get("/auth/me", {
          __authHeader: header,
          __skipAuthRedirect: true,
        });

        if (obterEpocaAutenticacao() !== authAttempt) {
          return { sucesso: false, mensagem: "A tentativa foi substituída." };
        }

        if (!identidadeValida(response.data)) {
          throw new Error("A API retornou uma identidade inválida.");
        }

        const ativada = ativarAutenticacao(
          header,
          limparEstadoAutenticacao,
          authAttempt
        );

        if (!ativada) {
          return { sucesso: false, mensagem: "A tentativa foi substituída." };
        }

        setIdentity(response.data);
        setLoading(false);
        setSessionVersion((current) => current + 1);
        return { sucesso: true };
      } catch (error) {
        if (obterEpocaAutenticacao() !== authAttempt) {
          return { sucesso: false, mensagem: "A tentativa foi substituída." };
        }

        clearApiAuthentication(authAttempt);
        setIdentity(null);
        setLoading(false);

        if (error.response?.status === 401) {
          return {
            sucesso: false,
            mensagem: "Usuário ou senha inválidos.",
          };
        }

        return {
          sucesso: false,
          mensagem:
            error.response?.data?.erro ||
            "Não foi possível conectar à API. Verifique a conexão e tente novamente.",
        };
      }
    },
    [limparEstadoAutenticacao]
  );

  const hasRole = useCallback(
    (...roles) => Boolean(identity && roles.includes(identity.papel)),
    [identity]
  );

  const value = useMemo(
    () => ({
      identity,
      loading,
      isAuthenticated: Boolean(identity),
      sessionVersion,
      login,
      logout: encerrarSessao,
      hasRole,
    }),
    [
      encerrarSessao,
      hasRole,
      identity,
      loading,
      login,
      sessionVersion,
    ]
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
