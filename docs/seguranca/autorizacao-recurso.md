# Autorização por recurso

## Problema arquitetural

A autenticação e o RBAC por papel identificam quem faz a requisição, mas um papel
isolado não determina se o usuário pode acessar um chamado específico. Sem validação
de propriedade ou atribuição na camada de negócio, um cliente poderia consultar
chamados de outro cliente e um técnico poderia alterar chamados que não lhe foram
atribuídos. Filtrar somente depois de carregar a lista também deixaria paginação e
contagens incorretas.

## Conceito e responsabilidades das camadas

- **SecurityFilterChain:** mantém HTTP Basic, CSRF, sessões stateless e negação por
  padrão. Libera aos papéis somente os endpoints que possuem validação de recurso;
  criação/edição de chamados, criação de comentários e operações administrativas
  continuam restritas conforme a matriz RBAC.
- **Identidade autenticada:** `UsuarioAutenticadoService` consulta a identidade
  persistida correspondente ao principal do Spring Security. Produz um contexto
  limitado a papel e IDs dos vínculos com cliente/técnico; não expõe senha, hash ou
  dados de perfil fornecidos pelo cliente. Identidade ausente, inativa, com
  autoridade divergente ou vínculo inconsistente não recebe permissão.
- **AutorizacaoRecursoService:** centraliza as decisões reutilizáveis de acesso a
  chamados, cadastro de cliente e operações administrativas.
- **Services:** `ChamadoService` aplica as verificações antes de retornar ou alterar
  recursos; `ClienteService` permite ao cliente somente o cadastro vinculado;
  `ComentarioService` verifica primeiro o acesso ao chamado associado. Alterações
  ainda não autorizadas continuam administrativas.
- **Repositories:** consultas por ID, listagens e filtros impõem a propriedade ou
  atribuição no banco. Filtros opcionais do request podem restringir o resultado,
  mas não substituir nem ampliar o escopo autenticado.
- **Controllers:** preservam os contratos REST existentes e não decidem identidade
  com dados enviados pelo frontend.

## Políticas implementadas

### CLIENTE

- Pode consultar e listar chamados cujo `cliente_id` corresponde ao vínculo da
  identidade autenticada.
- Pode filtrar por status, prioridade, categoria e parâmetros existentes; `clienteId`
  informado na URL não altera o escopo fixado pelo vínculo.
- Pode consultar somente seu próprio cadastro em `GET /clientes/{id}`.
- Pode ler comentários somente quando tem acesso ao chamado associado.
- Não pode criar ou editar chamados, atualizar o cadastro, alterar status, atribuir
  técnico, excluir recursos ou criar comentários nesta etapa.

### TECNICO

- Pode consultar e listar somente chamados cujo `tecnico_id` corresponde ao vínculo
  da identidade autenticada.
- Pode aplicar filtros adicionais sem ampliar o escopo. Um `tecnicoId` solicitado
  diferente do vínculo resulta em conjunto vazio.
- Pode alterar o status somente de chamado atribuído a si, respeitando a regra que
  impede alterações em chamados fechados.
- Pode ler comentários somente de chamados atribuídos.
- Não pode consultar diretamente cadastros de clientes nem criar comentários,
  atribuir técnicos ou executar operações administrativas.

### ADMINISTRADOR

- Pode consultar, listar, filtrar e gerenciar chamados de todos os clientes.
- Pode consultar cadastros e comentários e executar as operações administrativas
  previamente autorizadas.
- Continua sujeito às regras de negócio, incluindo a proibição de editar chamados
  fechados e de comentar em chamados fechados.

## Consultas, filtros e paginação

`GET /chamados` mantém a resposta em lista e é escopado no método de listagem do
repositório. `GET /chamados/filtro` preserva os filtros e a paginação; a consulta
aplica o vínculo autenticado como condição obrigatória adicional. A consulta de
contagem da página usa as mesmas condições, portanto `totalElements` e `totalPages`
não incluem chamados fora do escopo. Busca por ID usa consultas combinadas por ID e
proprietário/atribuição, sem retornar o objeto antes da autorização.

## Códigos HTTP

- **200:** consulta autorizada.
- **401:** não autenticado ou credenciais inválidas.
- **403:** autenticado sem papel, vínculo ou autorização para o recurso. A resposta
  de acesso negado usa mensagem genérica e não inclui dados do recurso.
- **404:** recurso inexistente.
- **409:** conflito com regra de negócio, como alteração de chamado fechado.

A distinção entre `403` para recurso existente sem acesso e `404` para recurso
inexistente pode revelar a existência de um recurso. Uma política uniforme de `404`
para certos acessos negados deverá ser avaliada antes de uma implantação real. A
padronização completa dos corpos JSON de erro permanece para a etapa 5.

## Relação com OWASP API1

OWASP API1 — Broken Object Level Authorization (BOLA) trata do acesso a objetos
usando identificadores sem validar se o usuário tem relação autorizada com cada
objeto. As consultas combinadas por ID e vínculo, a filtragem obrigatória no banco e
as verificações de serviço reduzem esse risco para chamados, cadastro próprio de
cliente e leitura de comentários. A autorização não depende de esconder IDs ou de
regras da interface React.

## Cenários testados

`AutorizacaoRecursoTests` prepara dois clientes, dois técnicos (com uma identidade
autenticada), um administrador e chamados com proprietários/atribuições diferentes.
Os testes validam:

- leitura do chamado próprio e negação de chamado de outro cliente;
- escopo de listagem, filtro, conteúdo e totais paginados para clientes;
- leitura e listagem de chamados atribuídos, com exclusão de outras atribuições;
- alteração autorizada de status e negação sem modificação persistida quando não há
  atribuição, sempre com autenticação e CSRF válidos;
- consulta administrativa de chamados pertencentes a clientes diferentes;
- consulta de cadastro próprio e negação de cadastro alheio;
- leitura de comentários condicionada ao acesso ao chamado;
- `404` para chamado inexistente e `401` para leitura sem autenticação;
- regra de negócio de chamado fechado e negação de identidade com vínculo
  inconsistente.

## Resultados de validação

Na execução final solicitada com `.\mvnw.cmd clean test`, foram executados **36
testes**, com **0 falhas, 0 erros e 0 ignorados**. A classe
`AutorizacaoRecursoTests` contém **12 testes novos**. O build compilou com a versão
alvo configurada no Maven (`--release 21`); os testes foram executados por um runtime
Java 22.

## Limitações e riscos residuais

- Clientes ainda não podem criar/editar chamados nem atualizar o cadastro. Esses
  fluxos precisam vincular o cliente autenticado e limitar os campos editáveis.
- A criação de comentários por cliente/técnico continua administrativa até que a
  autoria seja obtida da identidade autenticada, não do DTO.
- Técnicos não recebem uma representação reduzida dos dados de cliente; a consulta
  direta de cadastro continua restrita. A resposta atual de chamado ainda contém
  nome/ID do cliente conforme o contrato REST existente.
- HTTP Basic permanece uma demonstração local; implantação real exige HTTPS e uma
  estratégia definitiva de autenticação.
- Os testes e dados usados são fictícios e locais. A política uniforme de `404` e a
  padronização dos corpos de erro seguem pendentes.
