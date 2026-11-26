import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "../services/api";
import { AuthProvider } from "./AuthProvider";
import { useAuth } from "./useAuth";
import ProtectedRoute from "../components/ProtectedRoute";
import Sidebar from "../components/Sidebar";
import Login from "../pages/Login";
import ModalChamado from "../components/ModalChamado";
import DetalheChamado from "../pages/DetalheChamado";
import MeuCadastro from "../pages/MeuCadastro";

vi.mock("../services/api", () => {
  let epoch = 0;
  const instance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  };

  return {
    default: instance,
    iniciarTentativaAutenticacao: vi.fn(() => {
      epoch += 1;
      return epoch;
    }),
    ativarAutenticacao: vi.fn((_header, _handler, expectedEpoch) => {
      if (expectedEpoch !== epoch) return false;
      epoch += 1;
      return true;
    }),
    clearApiAuthentication: vi.fn((expectedEpoch) => {
      if (expectedEpoch !== undefined && expectedEpoch !== epoch) return false;
      epoch += 1;
      return true;
    }),
    criarCabecalhoBasic: vi.fn((usuario, senha) => `Basic ${usuario}:${senha}`),
    obterEpocaAutenticacao: vi.fn(() => epoch),
  };
});

const cliente = {
  usuarioId: 31,
  nomeExibicao: "Cliente Demonstração",
  papel: "CLIENTE",
  clienteId: 12,
};

const administrador = {
  usuarioId: 1,
  nomeExibicao: "Administradora",
  papel: "ADMINISTRADOR",
};

function ConteudoProtegido() {
  const { identity } = useAuth();
  return (
    <>
      <Sidebar />
      <p>Identidade: {identity.nomeExibicao}</p>
    </>
  );
}

function montarAplicacao(initialEntry, component = <ConteudoProtegido />) {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/privado" element={component} />
            <Route
              path="/admin"
              element={<ProtectedRoute roles={["ADMINISTRADOR"]} />}
            >
              <Route index element={<p>Conteúdo administrativo</p>} />
            </Route>
            <Route path="/cadastro" element={<MeuCadastro />} />
            <Route
              path="/chamados/:id"
              element={<DetalheChamado />}
            />
            <Route
              path="/novo-chamado"
              element={
                <ModalChamado
                  aberto
                  onClose={vi.fn()}
                  onSalvo={vi.fn()}
                />
              }
            />
          </Route>
        </Routes>
      </MemoryRouter>
    </AuthProvider>
  );
}

async function entrar(identity = cliente) {
  api.get.mockImplementation(async (path) => {
    if (path === "/auth/me") return { data: identity };
    if (path === "/categorias") {
      return { data: [{ id: 8, nome: "Rede" }] };
    }
    if (path === "/chamados/15") {
      return {
        data: {
          id: 15,
          titulo: "Sem conexão",
          descricao: "Falha na rede",
          clienteId: 12,
          categoriaId: 8,
          categoriaNome: "Rede",
          clienteNome: "Cliente Demonstração",
          tecnicoId: null,
          tecnicoNome: null,
          prioridade: "MEDIA",
          status: "ABERTO",
          dataAbertura: "2026-03-01T10:00:00",
        },
      };
    }
    if (path === "/chamados/15/comentarios") return { data: [] };
    if (path === "/clientes/12") {
      return {
        data: {
          id: 12,
          nome: "Cliente Demonstração",
          email: "cliente@example.test",
          telefone: "11999990000",
        },
      };
    }
    throw new Error(`Request inesperado nos testes: ${path}`);
  });
  fireEvent.change(screen.getByLabelText("E-mail"), {
    target: { value: "cliente@example.test" },
  });
  fireEvent.change(screen.getByLabelText("Senha"), {
    target: { value: "senha-ficticia" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
}

describe("autenticação e experiências por perfil", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("redireciona rotas protegidas para login e mantém o destino solicitado", async () => {
    montarAplicacao("/privado");

    expect(await screen.findByRole("heading", { name: "Entrar" })).toBeVisible();
    expect(screen.queryByText("Identidade:")).not.toBeInTheDocument();
  });

  it("autentica cliente em memória, mostra somente os menus permitidos e encerra sessão", async () => {
    montarAplicacao("/privado");
    await entrar();

    expect(
      await screen.findByText("Identidade: Cliente Demonstração")
    ).toBeVisible();
    expect(screen.getByRole("link", { name: /Meu cadastro/ })).toBeVisible();
    expect(screen.queryByRole("link", { name: "Clientes" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Técnicos" })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /Sair/ }));
    expect(await screen.findByRole("heading", { name: "Entrar" })).toBeVisible();
  });

  it("mantém os menus administrativos apenas para administradores", async () => {
    montarAplicacao("/privado");
    await entrar(administrador);

    expect(await screen.findByRole("link", { name: "Clientes" })).toBeVisible();
    expect(screen.getByRole("link", { name: "Técnicos" })).toBeVisible();
    expect(screen.queryByRole("link", { name: /Meu cadastro/ })).not.toBeInTheDocument();
  });

  it("nega a rota administrativa para o perfil de cliente", async () => {
    montarAplicacao("/admin");
    await entrar();

    expect(
      await screen.findByRole("heading", { name: "Acesso não autorizado" })
    ).toBeVisible();
    expect(screen.queryByText("Conteúdo administrativo")).not.toBeInTheDocument();
  });

  it("mostra erro genérico quando a autenticação falha", async () => {
    api.get.mockRejectedValue({ response: { status: 401 } });
    montarAplicacao("/privado");

    fireEvent.change(screen.getByLabelText("E-mail"), {
      target: { value: "cliente@example.test" },
    });
    fireEvent.change(screen.getByLabelText("Senha"), {
      target: { value: "senha-incorreta" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Usuário ou senha inválidos."
    );
    expect(screen.queryByText("Identidade:")).not.toBeInTheDocument();
  });

  it("cria chamado do cliente usando somente o contrato de autoatendimento", async () => {
    api.post.mockResolvedValue({
      data: { id: 15, titulo: "Sem conexão" },
    });
    montarAplicacao("/novo-chamado");
    await entrar();

    fireEvent.change(await screen.findByLabelText("Título do chamado *"), {
      target: { value: "Sem conexão" },
    });
    fireEvent.change(screen.getByLabelText("Descrição do problema *"), {
      target: { value: "Falha na rede" },
    });
    fireEvent.change(screen.getByLabelText("Categoria *"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Salvar chamado" }));

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith("/chamados/meus", {
        titulo: "Sem conexão",
        descricao: "Falha na rede",
        categoriaId: 8,
        prioridade: "MEDIA",
      });
    });
  });

  it("atualiza somente nome e telefone no cadastro próprio", async () => {
    api.put.mockResolvedValue({
      data: {
        id: 12,
        nome: "Cliente Atualizado",
        email: "cliente@example.test",
        telefone: "11911112222",
      },
    });
    montarAplicacao("/cadastro");
    await entrar();

    fireEvent.change(await screen.findByLabelText("Nome"), {
      target: { value: "Cliente Atualizado" },
    });
    fireEvent.change(screen.getByLabelText("Telefone"), {
      target: { value: "11911112222" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: "Salvar alterações" })
    );

    await waitFor(() => {
      expect(api.put).toHaveBeenCalledWith("/clientes/me", {
        nome: "Cliente Atualizado",
        telefone: "11911112222",
      });
    });
    expect(screen.getByLabelText("E-mail (não editável)")).toHaveAttribute(
      "readonly"
    );
    expect(screen.queryByLabelText(/Senha|Papel/)).not.toBeInTheDocument();
  });

  it("envia comentário sem aceitar autoria fornecida pelo usuário", async () => {
    api.post.mockResolvedValue({
      data: {
        id: 7,
        autor: "Cliente Demonstração",
        texto: "Ainda sem conexão",
        data: "2026-03-01T11:00:00",
      },
    });
    montarAplicacao("/chamados/15");
    await entrar();

    fireEvent.change(
      await screen.findByLabelText("Novo comentário *"),
      { target: { value: "Ainda sem conexão" } }
    );
    fireEvent.click(
      screen.getByRole("button", { name: "Enviar comentário" })
    );

    await waitFor(() => {
      expect(api.post).toHaveBeenCalledWith(
        "/chamados/15/comentarios",
        { texto: "Ainda sem conexão" }
      );
    });
    expect(screen.queryByLabelText("Seu nome *")).not.toBeInTheDocument();
  });
});
