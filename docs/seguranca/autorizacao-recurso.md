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
  padrão. Libera as rotas de autoatendimento e comentários aos papéis autorizados;
  rotas administrativas originais permanecem exclusivas do administrador.
- **Identidade autenticada:** `UsuarioAutenticadoService` consulta a identidade
  persistida correspondente ao principal do Spring Security. Produz um contexto
  com ID interno, papel, nome de exibição e IDs dos vínculos; não expõe senha/hash
  nem usa dados de perfil fornecidos pelo cliente. Tanto a autenticação
  (`UsuarioDetailsService`) quanto a autorização rejeitam vínculos inconsistentes.
- **AutorizacaoRecursoService:** centraliza as decisões reutilizáveis de acesso a
  chamados, cadastro de cliente e operações administrativas.
- **Services:** `ChamadoService` aplica as verificações antes de retornar ou alterar
  recursos; `ClienteService` permite ao cliente somente o cadastro vinculado;
  `ComentarioService` verifica o acesso ao chamado antes de criá-lo e define a
  autoria a partir do usuário autenticado.
- **Repositories:** consultas por ID, listagens e filtros impõem a propriedade ou
  atribuição no banco. Filtros opcionais do request podem restringir o resultado,
  mas não substituir nem ampliar o escopo autenticado.
- **Controllers:** separam os contratos administrativos dos DTOs de autoatendimento
  e não decidem identidade com dados enviados pelo frontend.

## Políticas implementadas

### CLIENTE

- Pode consultar e listar chamados cujo `cliente_id` corresponde ao vínculo da
  identidade autenticada.
- Pode filtrar por status, prioridade, categoria e parâmetros existentes; `clienteId`
  informado na URL não altera o escopo fixado pelo vínculo.
- Pode consultar somente seu próprio cadastro em `GET /clientes/{id}`.
- Pode criar e editar seus próprios chamados por `POST /chamados/meus` e
  `PUT /chamados/meus/{id}`, somente com título, descrição, prioridade e categoria.
- Pode atualizar nome e telefone em `PUT /clientes/me`.
- Pode ler comentários somente quando tem acesso ao chamado associado.
- Pode comentar em chamados próprios abertos; o nome de exibição é obtido do cadastro
  vinculado, e o ID interno do usuário fica associado ao registro.
- Não pode alterar status, atribuir técnico, excluir recursos, alterar e-mail ou
  credenciais.

### TECNICO

- Pode consultar e listar somente chamados cujo `tecnico_id` corresponde ao vínculo
  da identidade autenticada.
- Pode aplicar filtros adicionais sem ampliar o escopo. Um `tecnicoId` solicitado
  diferente do vínculo resulta em conjunto vazio.
- Pode alterar o status somente de chamado atribuído a si, respeitando a regra que
  impede alterações em chamados fechados.
- Pode ler comentários somente de chamados atribuídos.
- Pode comentar em chamados atribuídos abertos; o nome de exibição é obtido do
  cadastro vinculado, e o ID interno do usuário fica associado ao registro.
- Não pode consultar diretamente cadastros de clientes, atribuir técnicos ou
  executar operações administrativas.

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
O endpoint aceita páginas a partir de zero e tamanho de 1 a 100; valores inválidos
retornam 400, e o limite máximo também é aplicado ao resolver de paginação.

## Códigos HTTP

- **200:** consulta autorizada.
- **401:** não autenticado, credenciais inválidas ou identidade com vínculo
  inconsistente rejeitada durante autenticação. O corpo JSON contém mensagem
  genérica e o desafio HTTP Basic.
- **403:** autenticado sem papel/autorização para o recurso ou token CSRF ausente ou
  inválido. Os handlers retornam JSON genérico, sem dados do recurso.
- **404:** recurso inexistente.
- **409:** conflito com regra de negócio, como alteração de chamado fechado.
- **400:** DTO inválido, JSON malformado ou parâmetro de paginação fora dos limites.
- **405/415:** método ou tipo de conteúdo não suportado.
- **500:** falha inesperada, com resposta genérica sem detalhes técnicos.

A distinção entre `403` para recurso existente sem acesso e `404` para recurso
inexistente pode revelar a existência de um recurso. Uma política uniforme de `404`
para certos acessos negados deverá ser avaliada antes de uma implantação real.
Rejeições de preflight CORS são feitas pelo processador CORS antes dos handlers e
podem não usar o mesmo corpo JSON.

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
- regra de negócio de chamado fechado e rejeição de identidade com vínculo
  inconsistente durante autenticação.

## Resultados de validação

`.\mvnw.cmd clean test` terminou com **BUILD SUCCESS**: 61 testes, 0 falhas, 0 erros
e 0 ignorados. O build compilou com `--release 21`, usando JDK 26.
`TratamentoErrosSegurancaTests` cobre os novos formatos de erro, autenticação,
negações, CORS, paginação e ausência de dados sensíveis nos logs. A cobertura da
proteção de propriedades está em [protecao-propriedades.md](./protecao-propriedades.md).

## Limitações e riscos residuais

- A criação/edição de chamados pelo cliente é limitada às rotas próprias e aos DTOs
  específicos; propriedades extras são rejeitadas com HTTP 400.
- Comentários de cliente/técnico são criados por `ComentarioRequest` com somente
  `texto`; o ID e o nome de exibição do autor são derivados do usuário autenticado.
- Técnicos não recebem uma representação reduzida dos dados de cliente; a consulta
  direta de cadastro continua restrita. A resposta atual de chamado ainda contém
  nome/ID do cliente conforme o contrato REST existente.
- HTTP Basic permanece uma demonstração local; implantação real exige HTTPS e uma
  estratégia definitiva de autenticação.
- Os testes e dados usados são fictícios e locais. A política uniforme de `404` e a
  persistência/alerta de auditoria não são configuradas nesta entrega.
- Os perfis locais `dev` e `demo` usam schema automático e dados em H2; ambientes
  geridos exigem migrações versionadas e validação de schema.
