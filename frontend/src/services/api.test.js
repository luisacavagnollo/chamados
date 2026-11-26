import { AxiosError } from "axios";
import resolveConfig from "axios/unsafe/helpers/resolveConfig.js";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api, {
  ativarAutenticacao,
  clearApiAuthentication,
  iniciarTentativaAutenticacao,
} from "./api";

describe("cliente HTTP autenticado", () => {
  let requests;
  let statusDaProximaResposta;

  beforeEach(() => {
    requests = [];
    statusDaProximaResposta = 200;
    document.cookie = "XSRF-TOKEN=csrf-ficticio; path=/";
    api.defaults.adapter = async (config) => {
      requests.push(config);
      const status = statusDaProximaResposta;
      statusDaProximaResposta = 200;
      const response = {
        config,
        data: {},
        headers: {},
        status,
        statusText: status === 200 ? "OK" : "Erro",
      };

      if (status >= 400) {
        throw new AxiosError(
          "Request failed",
          AxiosError.ERR_BAD_REQUEST,
          config,
          undefined,
          response
        );
      }

      return response;
    };
  });

  afterEach(() => {
    clearApiAuthentication();
    document.cookie = "XSRF-TOKEN=; Max-Age=0; path=/";
  });

  it("configura Basic e CSRF para a origem da API e preserva autenticação em 403", async () => {
    const handler = vi.fn();
    const attempt = iniciarTentativaAutenticacao();
    ativarAutenticacao("Basic ficticio", handler, attempt);
    statusDaProximaResposta = 403;

    await expect(api.get("/recurso")).rejects.toMatchObject({
      response: { status: 403 },
    });

    expect(handler).not.toHaveBeenCalled();
    expect(requests[0].headers.get("Authorization")).toBe("Basic ficticio");
    const configResolvida = resolveConfig(requests[0]);
    expect(configResolvida.headers.get("X-XSRF-TOKEN")).toBe("csrf-ficticio");
    expect(requests[0].withCredentials).toBe(true);

    await api.get("/recurso");
    expect(requests[1].headers.get("Authorization")).toBe("Basic ficticio");

    await expect(api.get("https://nao-confiavel.invalid/recurso"))
      .rejects.toMatchObject({ code: "ERR_UNTRUSTED_API_ORIGIN" });
    expect(requests).toHaveLength(2);
  });

  it("limpa credenciais em memória quando a API responde 401", async () => {
    const handler = vi.fn();
    const attempt = iniciarTentativaAutenticacao();
    ativarAutenticacao("Basic ficticio", handler, attempt);
    statusDaProximaResposta = 401;

    await expect(api.get("/recurso")).rejects.toMatchObject({
      response: { status: 401 },
    });
    expect(handler).toHaveBeenCalledOnce();

    await api.get("/recurso");
    expect(requests[0].headers.get("Authorization")).toBe("Basic ficticio");
    expect(requests[1].headers.has("Authorization")).toBe(false);
  });
});
