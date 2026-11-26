# Proteção de propriedades (OWASP API3)

## Contratos REST implementados

| Método e endpoint | Papel | DTO de entrada | Efeito |
| --- | --- | --- | --- |
| `GET /auth/me` | CLIENTE, TECNICO, ADMINISTRADOR | — | Retorna papel, nome de exibição e vínculo aplicável sem e-mail/credenciais |
| `POST /chamados` | ADMINISTRADOR | `ChamadoRequest` | Criação administrativa com cliente e técnico fornecidos pelo administrador |
| `PUT /chamados/{id}` | ADMINISTRADOR | `ChamadoRequest` | Edição administrativa, sujeita às regras do chamado |
| `POST /chamados/meus` | CLIENTE | `ChamadoCriacaoClienteRequest` | Cria o chamado como o cliente autenticado, sem atribuir técnico |
| `PUT /chamados/meus/{id}` | CLIENTE | `ChamadoEdicaoClienteRequest` | Edita somente chamado próprio e aberto |
| `PUT /clientes/{id}` | ADMINISTRADOR | `ClienteRequest` | Gerencia o cadastro, inclusive e-mail |
| `PUT /clientes/me` | CLIENTE | `ClienteAtualizacaoRequest` | Atualiza nome e telefone do cliente autenticado |
| `POST /chamados/{id}/comentarios` | CLIENTE dono, TECNICO atribuído ou ADMINISTRADOR | `ComentarioRequest` | Cria comentário em chamado aberto, com autoria do servidor |

As rotas administrativas originais permanecem exclusivas do administrador. Consultas,
filtros, exclusão, atribuição e alteração de status mantêm os endpoints e as
autorizações documentados em [matriz-rbac.md](./matriz-rbac.md).

## Erros, logs e limites da API

Os erros tratados pela API usam o mesmo contrato JSON:

```json
{
  "timestamp": "2026-01-01T12:00:00Z",
  "status": 400,
  "erro": "Dados inválidos",
  "detalhes": []
}
```

O nome dos campos permanece em português (`erro`, `detalhes`). Erros de validação
retornam 400, conteúdo incompatível retorna 415, conflitos de negócio/persistência
retornam 409, recursos ausentes retornam 404 genérico, falhas inesperadas retornam
500 genérico. Autenticação ausente/inválida retorna 401 com `WWW-Authenticate`;
autorização e CSRF negados retornam 403. Mensagens de parser, SQL, stack traces e
valores de entrada não são enviados na resposta. CORS é configurável por
`APP_CORS_ALLOWED_ORIGINS`, limitado a origens explícitas e cabeçalhos necessários.

`GET /chamados/filtro` aceita página a partir de zero e tamanho de 1 a 100; valores
inválidos resultam em 400. O limite é validado no controller e aplicado também ao
resolver de `Pageable`.

Os logs registram eventos de autenticação com ID interno/papel, falhas sem nome de
usuário, negações com método, operação conhecida (ou `ROTA_DESCONHECIDA`) e papel,
além de alterações de chamados após o commit com ID interno do ator e do chamado.
Não registram caminho bruto, senha, hash, e-mail, token, conteúdo do chamado,
comentários ou corpo da requisição. Os eventos de negócio usam logging da aplicação,
não um histórico de auditoria imutável.

## Matriz de propriedades

| Propriedade | Cliente: chamado próprio | Técnico: chamado atribuído | Administrador |
| --- | --- | --- | --- |
| `titulo`, `descricao`, `prioridade`, `categoriaId` | Permitidas na criação/edição próprias | Não editáveis | Editáveis nos contratos administrativos |
| `clienteId` | Derivado do vínculo autenticado | Não editável | Permitido em `ChamadoRequest` |
| `tecnicoId` | Proibido; novo chamado fica sem atribuição | Não editável | Permitido em `ChamadoRequest` e DTO de atribuição |
| `status`, `dataAbertura`, `dataFechamento`, `id` | Proibidas na entrada de autoatendimento | Não editáveis pelo contrato de chamado | Status alterado pela rota própria; datas geridas pelo serviço |
| `nome`, `telefone` do próprio cadastro | Permitidas em `PUT /clientes/me` | Não permitido | Permitidos no contrato administrativo |
| `email`, `papel`, senha/hash e IDs no perfil próprio | Proibidos | Não permitido | E-mail alterado somente pela rota administrativa; papel/credenciais não são atualizados por estes endpoints |
| `autor` do comentário | Obtido da identidade e do nome da entidade cliente | Obtido da identidade e do nome da entidade técnico | Exibido como `Administrador` |

Os três DTOs de autoatendimento e `ComentarioRequest` rejeitam propriedades que não
fazem parte do contrato via setter Jackson específico por DTO. A rejeição é local
àqueles contratos e resulta em HTTP 400; a configuração global do Jackson não foi
alterada. Bean Validation (`@Valid`) valida campos obrigatórios, formatos e limites
dos DTOs. Os DTOs administrativos mantêm `@NotNull clienteId` e `@NotBlank email`.

## Por que contratos separados

Usar `ChamadoRequest` ou `ClienteRequest` nas rotas do cliente torna possível
representar propriedades administrativas no mesmo contrato, criando risco de
atribuição em massa (OWASP API3). Endpoints e DTOs separados reduzem a superfície de
entrada: o cliente não envia proprietário, técnico, estado ou datas e a identidade
autenticada é a única fonte do vínculo de propriedade. A autorização de recurso ainda
é verificada no serviço, inclusive quando ele é chamado fora do controller.

## Autoria de comentários e migração

`ComentarioRequest` aceita somente `texto`. O serviço consulta a identidade
autenticada, persiste o vínculo com o ID interno do `Usuario` e grava como nome de
exibição o nome do `Cliente` ou `Tecnico` vinculado. Para administrador, o nome é
`Administrador`; nenhum e-mail é exposto como autoria. `ComentarioResponse` contém
somente o nome de exibição, sem ID interno de usuário, e-mail ou credenciais.

A entidade `Comentario` ganhou a coluna nullable `autor_usuario_id` com vínculo para
`usuarios`. Nos perfis locais `dev`/`demo`, `ddl-auto=update` adiciona a coluna em
bancos existentes. Em ambientes geridos, a coluna deve ser criada por migração
versionada antes de executar com `ddl-auto=validate`. Ela permanece nula para
comentários antigos: não há atribuição retroativa de identidade. O valor textual
`autor` e o conteúdo dos comentários legados são preservados.

## Minimização de dados

Respostas de comentário não incluem e-mail, ID de identidade, senha ou hash. O nome
de exibição é o mínimo necessário para identificar a autoria na interface. Respostas
de chamados e clientes mantêm os campos contratuais preexistentes; a autorização limita quais usuários podem obtê-las. O
frontend usa o nome/papel para renderização por perfil e não persiste credenciais.

## Verificação e testes

`ProtecaoPropriedadesTests` usa identidades e dados fictícios, MockMvc, Basic Auth e
CSRF válido nas escritas. Testa criação/edição próprias e alheias, propriedades
proibidas, validação administrativa, atualização de perfil, comentários e autoria,
status e identidade inconsistente. `RbacSecurityTests` e
`AutorizacaoRecursoTests` mantêm a cobertura preexistente.

`AuthMeControllerTests` verifica a resposta minimizada de `/auth/me` para os três
papéis, negações de acesso e classificação segura de operações desconhecidas.
`TratamentoErrosSegurancaTests` verifica 401/403/400/404/409/415/500, desafio Basic,
CORS, configuração de teste, logs minimizados e limite de paginação. A execução real
de `.\mvnw.cmd clean test` terminou em **BUILD SUCCESS**: **66 testes, 0 falhas,
0 erros e 0 ignorados**. Por classe: `ChamadosApplicationTests` 1,
`RbacSecurityTests` 12, `SecurityConfigTests` 7, `ClienteRequestTests` 3,
`ChamadoRepositoryTests` 1, `AuthMeControllerTests` 5, `AutorizacaoRecursoTests` 12,
`ProtecaoPropriedadesTests` 11 e `TratamentoErrosSegurancaTests` 14. O build foi
executado com JDK 26 e compilou com `--release 21`.

Os testes do frontend usam Vitest/jsdom e dados fictícios: verificam login, logout,
redirecionamento/negação por papel, menus por perfil, criação de chamado próprio,
atualização limitada do cadastro, comentário sem autoria enviada pelo usuário,
preservação de autenticação em 403, limpeza após 401 e configuração de Basic/CSRF/
origem confiável. O smoke test integrado no navegador também passou com o perfil
`demo`: login, `/auth/me`, CORS e criação de chamado próprio com CSRF válido
retornaram sucesso; o banco H2 foi efêmero e o servidor foi encerrado ao final.

## Limitações e riscos residuais

- Autenticação continua sendo HTTP Basic para esta demonstração; implantação real
  deve usar HTTPS e uma estratégia apropriada de gestão de credenciais.
- O perfil local `dev` e o perfil `demo` usam `ddl-auto=update`; ambientes geridos
  devem aplicar migrações versionadas e executar com validação do schema.
- O banco H2 local é volátil; persistência, backups, retenção e migrações não são
  configurados nesta etapa.
- O nome de exibição de autoria é salvo como snapshot. Alterações posteriores ao nome
  do cliente/técnico não renomeiam comentários anteriores.
- Clientes não editam chamados fechados; técnicos só alteram status/comentam em
  chamados atribuídos. Administradores continuam sujeitos às regras de negócio.
- Não há endpoint de alteração de papel, senha ou hash nesta entrega.
- O frontend mantém a identidade e o cabeçalho Basic somente em memória; recarregar
  a aplicação exige login novamente. A segurança efetiva continua sendo aplicada
  pela API, não pelos menus/rotas React.
- Os logs de auditoria não são armazenados em repositório imutável nem possuem
  política de retenção/alertas configurada. Rejeições de preflight CORS são
  bloqueadas sem cabeçalho de origem permitido e podem usar a resposta padrão do
  processador CORS, em vez do JSON produzido pelos handlers de segurança.
