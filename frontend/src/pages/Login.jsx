import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { Headset, LogIn } from "lucide-react";
import { useAuth } from "../context/useAuth";

export default function Login() {
  const { login, isAuthenticated, loading } = useAuth();
  const location = useLocation();
  const [usuario, setUsuario] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");

  const destino = location.state?.from?.pathname;
  const retornarPara =
    destino?.startsWith("/") && destino !== "/login" ? destino : "/";

  if (isAuthenticated) {
    return <Navigate to={retornarPara} replace />;
  }

  async function enviar(evento) {
    evento.preventDefault();
    setErro("");

    const resultado = await login(usuario.trim(), senha);
    setSenha("");

    if (!resultado.sucesso && resultado.mensagem !== "A tentativa foi substituída.") {
      setErro(resultado.mensagem);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card">
        <div className="login-brand">
          <Headset size={30} />
          <span>HelpDesk</span>
        </div>
        <h1>Entrar</h1>
        <p>Use sua conta para acessar os chamados.</p>

        <form onSubmit={enviar}>
          <div className="form-group">
            <label htmlFor="usuario">E-mail</label>
            <input
              id="usuario"
              type="email"
              autoComplete="username"
              value={usuario}
              onChange={(event) => setUsuario(event.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="senha">Senha</label>
            <input
              id="senha"
              type="password"
              autoComplete="current-password"
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
              required
            />
          </div>

          {erro && (
            <div className="error-message" role="alert">
              {erro}
            </div>
          )}

          <button
            className="btn-primary login-submit"
            type="submit"
            disabled={loading || !usuario.trim() || !senha}
          >
            <LogIn size={17} />
            {loading ? "Entrando..." : "Entrar"}
          </button>
        </form>
      </section>
    </main>
  );
}
