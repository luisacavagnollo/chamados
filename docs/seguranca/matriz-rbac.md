# Matriz RBAC da API

## Escopo e decisão de autorização

A autorização desta etapa é aplicada na `SecurityFilterChain`, usando as autoridades
`ROLE_CLIENTE`, `ROLE_TECNICO` e `ROLE_ADMINISTRADOR` fornecidas pela identidade
autenticada. O campo `Tecnico.perfil` não participa das decisões de acesso.

A política é de negação por padrão: apenas combinações explícitas de método HTTP,
recurso e papel são permitidas. Rotas não previstas recebem `403` para usuários
autenticados; requisições sem autenticação continuam recebendo `401`. A proteção CSRF
e a política de sessão stateless permanecem ativas.
Os caminhos parametrizados aceitos pela autorização usam segmentos numéricos de ID;
um caminho estático novo ou um subcaminho novo não herda a permissão da rota existente.

Falhas de autenticação e autorização usam JSON genérico (`timestamp`, `status`,
`erro`, `detalhes`); requisições 401 também recebem o desafio HTTP Basic. As origens
CORS são explícitas e configuráveis por `APP_CORS_ALLOWED_ORIGINS`.

## Matriz de permissões de destino

| Operação | CLIENTE | TECNICO | ADMINISTRADOR |
| --- | --- | --- | --- |
| Criar chamados próprios | Permitido em `/chamados/meus`; proprietário vem da identidade autenticada | Não permitido | Permitido em `/chamados` |
| Listar/consultar chamados próprios | Permitido, limitado ao cliente vinculado | Permitido, limitado aos chamados atribuídos | Gerenciar chamados |
| Editar chamados próprios | Permitido em `/chamados/meus/{id}`, somente campos autorizados e chamado aberto | Não permitido | Permitido em `/chamados/{id}`, sujeito às regras de negócio |
| Alterar status de chamado atribuído | Não permitido | Permitido, somente nos chamados atribuídos | Gerenciar chamados |
| Atribuir técnico ou excluir chamados | Não permitido | Não permitido | Gerenciar chamados, sujeito às regras de negócio |
| Consultar o próprio cadastro de cliente | Permitido, somente o cadastro vinculado | Não permitido | Gerenciar clientes |
| Atualizar cadastro próprio de cliente | Permitido em `/clientes/me`, somente nome e telefone | Não permitido | Gerenciar clientes |
| Consultar dados de clientes | Não aplicável além do próprio cadastro | Restrito; sem endpoint próprio de consulta | Gerenciar clientes |
| Gerenciar técnicos | Não permitido | Não permitido | Permitido |
| Consultar categorias | Permitido | Permitido | Permitido |
| Criar/editar/excluir categorias | Não permitido | Não permitido | Permitido |
| Consultar comentários | Somente em chamados próprios | Somente em chamados atribuídos | Permitido |
| Criar comentários | Somente em chamados próprios; autoria da identidade | Somente em chamados atribuídos; autoria da identidade | Permitido |
| Gerenciar permissões | Não permitido | Não permitido | Não implementado nesta etapa |

## Permissões implementadas nesta etapa

As regras abaixo correspondem aos mapeamentos atuais dos controllers. `{id}` representa
um segmento numérico, compatível com os parâmetros `Long` existentes.

| Método e endpoint | Permissão aplicada |
| --- | --- |
| `GET /auth/me` | Os três papéis autenticados; retorna identidade minimizada |
| `GET /categorias`, `GET /categorias/{id}` | CLIENTE, TECNICO e ADMINISTRADOR |
| `GET /chamados`, `GET /chamados/filtro`, `GET /chamados/{id}` | Os três papéis, com escopo do cliente/técnico aplicado no serviço e repositório |
| `POST /chamados`, `PUT /chamados/{id}`, `DELETE /chamados/{id}` | Somente ADMINISTRADOR |
| `POST /chamados/meus`, `PUT /chamados/meus/{id}` | Somente CLIENTE; propriedade obtida da identidade e campos limitados por DTO |
| `PATCH /chamados/{id}/status` | TECNICO atribuído ao chamado ou ADMINISTRADOR |
| `PATCH /chamados/{id}/tecnico` | Somente ADMINISTRADOR |
| `GET /clientes`, `POST /clientes` | Somente ADMINISTRADOR |
| `GET /clientes/{id}` | CLIENTE vinculado ao cadastro ou ADMINISTRADOR |
| `PUT /clientes/me` | Somente CLIENTE autenticado, nome e telefone |
| `PUT /clientes/{id}` | Somente ADMINISTRADOR, gerencia o cadastro completo |
| `DELETE /clientes/{id}` | Somente ADMINISTRADOR |
| `GET /tecnicos`, `GET /tecnicos/{id}`, `POST /tecnicos` | Somente ADMINISTRADOR |
| `PUT /tecnicos/{id}`, `DELETE /tecnicos/{id}` | Somente ADMINISTRADOR |
| `GET /chamados/{id}/comentarios` | Cliente dono do chamado, técnico atribuído ou ADMINISTRADOR |
| `POST /chamados/{id}/comentarios` | Cliente dono, técnico atribuído ou ADMINISTRADOR |
| Qualquer outro método ou caminho, inclusive novas rotas nas famílias acima | Negada por padrão |

A permissão administrativa não contorna as regras dos serviços de negócio. Por
exemplo, um chamado fechado continua não podendo ser editado e uma categoria
duplicada continua sendo rejeitada.

## Permissões pendentes de autorização por recurso

As permissões ainda restritas são:

- consulta direta de dados de clientes por técnicos, até existir uma resposta com
  finalidade e minimização de dados;
- alterações de e-mail e credenciais pelo próprio cliente; nenhum endpoint de
  autoatendimento para esses campos está implementado.

As leituras de chamados e comentários de cliente e técnico são limitadas ao vínculo
de propriedade ou atribuição verificado no backend. Rotas não mapeadas continuam
negadas por padrão.

## Justificativa arquitetural e menor privilégio

As regras por método e rota protegem funções administrativas independentemente do
conteúdo do request. As permissões de leitura de categorias são seguras por papel.
As permissões de chamados e comentários, ao contrário, consultam o vínculo com o
recurso; a paginação e os filtros também são limitados na consulta ao banco.

O uso de `anyRequest().denyAll()` exige uma decisão explícita quando novos endpoints
são adicionados. As autoridades são as da identidade autenticada; não se usa o campo
de perfil legado do técnico.

`GET /chamados/filtro` limita o tamanho de página a 100 registros. Páginas negativas,
tamanho menor que 1 ou maior que 100 retornam HTTP 400. Eventos de autenticação,
negações/CSRF e alterações de chamados são registrados sem credenciais, e-mails,
tokens ou corpos de requisição; alterações de chamados só são registradas após
commit.

A segurança em nível de método não foi habilitada nesta etapa. As permissões por
recurso são centralizadas nos serviços de autorização/negócio e aplicadas nas
consultas relevantes do repositório; as rotas apenas liberam os papéis que podem
invocar essas verificações.

## OWASP API5: Broken Function Level Authorization

OWASP API5 trata da ausência de autorização apropriada por função/operação, que pode
permitir a perfis comuns executar funções administrativas. Esta matriz limita
explicitamente operações administrativas a `ROLE_ADMINISTRADOR`, diferencia métodos
HTTP e nega rotas não mapeadas. Os testes verificam que CLIENTE e TECNICO não
conseguem criar ou excluir categorias, mesmo apresentando autenticação válida e
token CSRF válido.

O controle por papel é complementado pelas verificações de recurso documentadas em
[autorizacao-recurso.md](./autorizacao-recurso.md), que reduzem o risco de OWASP
API1 — Broken Object Level Authorization.

## Evidências de testes

`RbacSecurityTests` cobre:

- leitura e CRUD administrativo de categorias;
- leitura de categorias por CLIENTE e TECNICO;
- negação de operações administrativas a CLIENTE e TECNICO com CSRF válido;
- resposta `401` sem autenticação;
- negação de operações condicionadas à propriedade/atribuição;
- negação de rotas não mapeadas, incluindo novos caminhos e subcaminhos dentro das
  famílias de chamados, clientes, técnicos e categorias;
- negação de escritas não mapeadas com token CSRF válido;
- preservação da regra de negócio que impede editar chamados fechados.

`AutorizacaoRecursoTests` cobre os escopos por propriedade/atribuição, listagens,
filtros, totais paginados, comentários, cadastro próprio, respostas HTTP e estado
persistido. `ProtecaoPropriedadesTests` cobre os contratos de autoatendimento,
autoria de comentários e validações de campos. A execução de
`.\mvnw.cmd clean test` passou com **66 testes, 0 falhas, 0 erros e 0 ignorados**
(JDK 26, target Java 21). `AuthMeControllerTests` cobre `/auth/me`, minimização da
resposta e auditoria de negações; `TratamentoErrosSegurancaTests` cobre os
contratos de erro, CORS, logs minimizados, configuração de teste e paginação. Os
cenários estão detalhados em
[autorizacao-recurso.md](./autorizacao-recurso.md).

## Limitações atuais

- Técnicos ainda não podem consultar diretamente cadastros de clientes.
- Clientes não podem alterar e-mail, papel ou credenciais por autoatendimento.
- O frontend usa Basic em memória; recarregar a página encerra a autenticação local.
- Não há gerenciamento de permissões, cadastro de contas ou fluxo de recuperação de
  acesso nesta entrega.
- Os logs não são um registro de auditoria imutável nem possuem retenção ou alertas
  configurados. Operações negadas são classificadas sem registrar caminhos
  arbitrários. Preflight CORS com origem rejeitada é bloqueado sem cabeçalho CORS e
  pode usar o corpo padrão do processador CORS, fora do JSON dos handlers de segurança.
