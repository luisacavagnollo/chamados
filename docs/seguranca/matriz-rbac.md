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

## Matriz de permissões de destino

| Operação | CLIENTE | TECNICO | ADMINISTRADOR |
| --- | --- | --- | --- |
| Criar chamados próprios | Permitido, após vincular a identidade ao cliente | Não aplicável | Gerenciar chamados |
| Listar/consultar chamados próprios | Permitido, limitado aos próprios chamados | Permitido, limitado aos chamados atribuídos | Gerenciar chamados |
| Editar campos autorizados de chamados próprios | Permitido, com validação dos campos | Não aplicável | Gerenciar chamados |
| Alterar status de chamado atribuído | Não permitido | Permitido, somente nos chamados atribuídos | Gerenciar chamados |
| Atribuir técnico ou excluir chamados | Não permitido | Não permitido | Gerenciar chamados, sujeito às regras de negócio |
| Consultar/atualizar o próprio cadastro de cliente | Permitido, somente o cadastro vinculado | Não permitido | Gerenciar clientes |
| Consultar dados de clientes | Não aplicável além do próprio cadastro | Somente os dados necessários a atendimentos atribuídos | Gerenciar clientes |
| Gerenciar técnicos | Não permitido | Não permitido | Permitido |
| Consultar categorias | Permitido | Permitido | Permitido |
| Criar/editar/excluir categorias | Não permitido | Não permitido | Permitido |
| Consultar/criar comentários | Somente em chamados próprios | Somente em chamados atribuídos | Permitido |
| Gerenciar permissões | Não permitido | Não permitido | Não implementado nesta etapa |

## Permissões implementadas nesta etapa

As regras abaixo correspondem aos mapeamentos atuais dos controllers. `{id}` representa
um segmento numérico, compatível com os parâmetros `Long` existentes.

| Método e endpoint | Permissão aplicada |
| --- | --- |
| `GET /categorias`, `GET /categorias/{id}` | CLIENTE, TECNICO e ADMINISTRADOR |
| `GET /chamados`, `GET /chamados/filtro`, `GET /chamados/{id}` | Somente ADMINISTRADOR |
| `POST /chamados`, `PUT /chamados/{id}`, `DELETE /chamados/{id}` | Somente ADMINISTRADOR |
| `PATCH /chamados/{id}/status`, `PATCH /chamados/{id}/tecnico` | Somente ADMINISTRADOR |
| `GET /clientes`, `GET /clientes/{id}`, `POST /clientes` | Somente ADMINISTRADOR |
| `PUT /clientes/{id}`, `DELETE /clientes/{id}` | Somente ADMINISTRADOR |
| `GET /tecnicos`, `GET /tecnicos/{id}`, `POST /tecnicos` | Somente ADMINISTRADOR |
| `PUT /tecnicos/{id}`, `DELETE /tecnicos/{id}` | Somente ADMINISTRADOR |
| `GET /chamados/{id}/comentarios`, `POST /chamados/{id}/comentarios` | Somente ADMINISTRADOR |
| Qualquer outro método ou caminho, inclusive novas rotas nas famílias acima | Negada por padrão |

A permissão administrativa não contorna as regras dos serviços de negócio. Por
exemplo, um chamado fechado continua não podendo ser editado e uma categoria
duplicada continua sendo rejeitada.

## Permissões pendentes de autorização por recurso

CLIENTE e TECNICO ainda não recebem acesso às operações de chamados, clientes ou
comentários, inclusive leitura. A liberação exige verificar o vínculo do usuário com
o cliente ou técnico do recurso. Também permanecem restritas:

- criação de chamado por cliente, que exige impor o cliente autenticado em vez de
  confiar no `clienteId` enviado pelo request;
- listagem, busca e edição de chamados próprios, com filtragem e campos permitidos;
- alteração de status por técnico, limitada aos chamados atribuídos;
- leitura e criação de comentários, limitada ao titular/atendente do chamado;
- consulta dos dados de cliente por técnico, limitada aos atendimentos autorizados;
- leitura e atualização do cadastro próprio de cliente.

Não há permissão temporária ampla para substituir essas verificações. Essas operações
permanecem disponíveis somente ao administrador (ou, para rotas ainda não previstas,
negadas a todos).

## Justificativa arquitetural e menor privilégio

As regras por método e rota protegem funções administrativas independentemente do
conteúdo do request. As permissões de leitura de categorias são seguras por papel:
não dependem da identidade de um recurso específico. Em contraste, as operações de
chamados, clientes e comentários dependem de propriedade, atribuição ou campos
editáveis; concedê-las somente por papel permitiria acesso horizontal indevido.

O uso de `anyRequest().denyAll()` exige uma decisão explícita quando novos endpoints
são adicionados. As autoridades são as da identidade autenticada; não se usa o campo
de perfil legado do técnico.

A segurança em nível de método não foi habilitada nesta etapa. As regras atualmente
implementadas são de rota e papel, centralizadas na cadeia de filtros; duplicá-las
com anotações nos controllers ou serviços não acrescentaria uma verificação de
propriedade. Na etapa seguinte, a autorização por recurso deverá ser aplicada junto
à consulta/mutação do serviço de domínio, usando uma estratégia centralizada e
testável, sem duplicar a mesma matriz em várias camadas.

## OWASP API5: Broken Function Level Authorization

OWASP API5 trata da ausência de autorização apropriada por função/operação, que pode
permitir a perfis comuns executar funções administrativas. Esta matriz limita
explicitamente operações administrativas a `ROLE_ADMINISTRADOR`, diferencia métodos
HTTP e nega rotas não mapeadas. Os testes verificam que CLIENTE e TECNICO não
conseguem criar ou excluir categorias, mesmo apresentando autenticação válida e
token CSRF válido.

O controle por papel não resolve autorização em nível de objeto. As verificações
para impedir que um cliente acesse chamados alheios ou que um técnico altere
chamados não atribuídos são pendências da próxima etapa.

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

Execução final de `.\mvnw.cmd test`: **24 testes executados, 0 falhas, 0 erros e
0 ignorados**, incluindo **12 testes RBAC** em `RbacSecurityTests`.

## Limitações atuais

- Não há autorização por propriedade, atribuição ou campos de request.
- Somente o administrador pode usar endpoints de chamados e comentários nesta etapa.
- O frontend ainda não foi integrado ao RBAC, HTTP Basic ou CSRF.
- Não há gerenciamento de permissões, cadastro de contas ou fluxo de recuperação de
  acesso nesta entrega.
