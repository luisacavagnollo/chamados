import axios from "axios";

const configuredApiUrl =
  import.meta.env.VITE_API_URL || "http://localhost:8080";
const resolvedApiUrl = new URL(
  configuredApiUrl,
  window.location.origin
);
const apiBaseUrl = resolvedApiUrl.toString().replace(/\/+$/, "");
const apiOrigin = resolvedApiUrl.origin;

const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  withCredentials: true,
  withXSRFToken: (config) => {
    const target = new URL(config.url, config.baseURL || apiBaseUrl);
    return target.origin === apiOrigin;
  },
  xsrfCookieName: "XSRF-TOKEN",
  xsrfHeaderName: "X-XSRF-TOKEN",
});

let authorizationHeader = null;
let unauthorizedHandler = null;
let authEpoch = 0;

function createAuthStateError() {
  const error = new Error("A resposta pertence a uma sessão encerrada.");
  error.code = "ERR_STALE_AUTH_STATE";
  return error;
}

function assertApiOrigin(config) {
  const target = new URL(config.url, config.baseURL || apiBaseUrl);

  if (target.origin !== apiOrigin) {
    const error = new Error("Requisição bloqueada para uma origem não confiável.");
    error.code = "ERR_UNTRUSTED_API_ORIGIN";
    throw error;
  }
}

api.interceptors.request.use((config) => {
  assertApiOrigin(config);
  config.__authEpoch = authEpoch;

  if (config.__authHeader) {
    config.headers.set("Authorization", config.__authHeader);
  } else if (authorizationHeader) {
    config.headers.set("Authorization", authorizationHeader);
  } else {
    config.headers.delete("Authorization");
  }

  return config;
});

api.interceptors.response.use(
  (response) => {
    if (response.config.__authEpoch !== authEpoch) {
      return Promise.reject(createAuthStateError());
    }

    return response;
  },
  (error) => {
    const config = error.config || {};

    if (
      config.__authEpoch !== undefined &&
      config.__authEpoch !== authEpoch
    ) {
      return Promise.reject(createAuthStateError());
    }

    if (error.response?.status === 401 && !config.__skipAuthRedirect) {
      const handler = unauthorizedHandler;
      clearApiAuthentication(authEpoch);
      handler?.();
    }

    return Promise.reject(error);
  }
);

export function criarCabecalhoBasic(usuario, senha) {
  const bytes = new TextEncoder().encode(`${usuario}:${senha}`);
  let binario = "";

  bytes.forEach((byte) => {
    binario += String.fromCharCode(byte);
  });

  return `Basic ${window.btoa(binario)}`;
}

export function iniciarTentativaAutenticacao() {
  authEpoch += 1;
  authorizationHeader = null;
  unauthorizedHandler = null;
  return authEpoch;
}

export function ativarAutenticacao(
  header,
  handler,
  expectedEpoch
) {
  if (expectedEpoch !== authEpoch) return false;

  authorizationHeader = header;
  unauthorizedHandler = handler;
  authEpoch += 1;
  return true;
}

export function clearApiAuthentication(expectedEpoch) {
  if (
    expectedEpoch !== undefined &&
    expectedEpoch !== authEpoch
  ) {
    return false;
  }

  authorizationHeader = null;
  unauthorizedHandler = null;
  authEpoch += 1;
  return true;
}

export function obterEpocaAutenticacao() {
  return authEpoch;
}

export default api;
