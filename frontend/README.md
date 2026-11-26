# Frontend HelpDesk

Aplicação React/Vite para a API Spring Boot protegida por HTTP Basic e CSRF.

## Configuração

Por padrão, a API é `http://localhost:8080`. Para outra URL, configure
`VITE_API_URL` no ambiente usado para iniciar o Vite. O backend precisa aceitar a
origem do frontend em `APP_CORS_ALLOWED_ORIGINS`.

As credenciais são mantidas somente na memória da aplicação e não são gravadas em
cookies, `localStorage` ou `sessionStorage`. Ao recarregar a página, autentique-se
novamente. O frontend consulta `GET /auth/me` para validar as credenciais e obter o
papel/nome de exibição. O próprio backend emite o cookie `XSRF-TOKEN`; o Axios o
envia como `X-XSRF-TOKEN` nas operações de escrita.
Uma resposta 401 encerra a autenticação local; um 403 mantém a sessão e apresenta
o erro da operação negada.

## Perfis

- **Cliente:** dashboard e chamados próprios, criação/edição pelo contrato
  `/chamados/meus`, comentários e atualização limitada em `/clientes/me`.
- **Técnico:** dashboard, chamados acessíveis ao vínculo, comentários e alteração
  de status permitida pelo backend.
- **Administrador:** gestão de chamados e cadastros, atribuição de técnicos e
  ações administrativas.

As rotas e menus do cliente são controles de interface, não substituem a
autorização da API. A API continua sendo a autoridade final para cada operação.

## Comandos

Na pasta `frontend`:

```powershell
npm install
npm run dev
npm test
npm run lint
npm run build
```

Os testes utilizam Vitest, jsdom e Testing Library; não precisam de API ou
credenciais externas.
