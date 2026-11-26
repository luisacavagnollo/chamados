# Proteção de propriedades (OWASP API3)

## Contratos REST implementados

| Método e endpoint | Papel | DTO de entrada | Efeito |
| --- | --- | --- | --- |
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
`usuarios`. Com `spring.jpa.hibernate.ddl-auto=update`, Hibernate adiciona a coluna
em bancos existentes. Ela permanece nula para comentários antigos: não há atribuição
retroativa de identidade. O valor textual `autor` e o conteúdo dos comentários
legados são preservados.

## Minimização de dados

Respostas de comentário não incluem e-mail, ID de identidade, senha ou hash. O nome
de exibição é o mínimo necessário para identificar a autoria na interface. Respostas
de chamados e clientes mantêm os campos contratuais preexistentes; a autorização
limita quais usuários podem obtê-las. A revisão não altera o frontend nem os outros
contratos de resposta.

## Verificação e testes

`ProtecaoPropriedadesTests` usa identidades e dados fictícios, MockMvc, Basic Auth e
CSRF válido nas escritas. Testa criação/edição próprias e alheias, propriedades
proibidas, validação administrativa, atualização de perfil, comentários e autoria,
status e identidade inconsistente. `RbacSecurityTests` e
`AutorizacaoRecursoTests` mantêm a cobertura preexistente.

Resultado de `.\mvnw.cmd clean test`: **BUILD SUCCESS**, com **47 testes, 0 falhas,
0 erros e 0 ignorados**. `ProtecaoPropriedadesTests`: 11 testes;
`RbacSecurityTests`: 12; `AutorizacaoRecursoTests`: 12; demais testes: 12. O build
usou JDK 26 e compilou com `--release 21`.

## Limitações e riscos residuais

- Autenticação continua sendo HTTP Basic para esta demonstração; implantação real
  deve usar HTTPS e uma estratégia apropriada de gestão de credenciais.
- O schema é atualizado por `ddl-auto=update`; ambientes geridos devem converter a
  nova coluna em migração versionada e revisada antes de produção.
- O nome de exibição de autoria é salvo como snapshot. Alterações posteriores ao nome
  do cliente/técnico não renomeiam comentários anteriores.
- Clientes não editam chamados fechados; técnicos só alteram status/comentam em
  chamados atribuídos. Administradores continuam sujeitos às regras de negócio.
- Não há endpoint de alteração de papel, senha ou hash nesta entrega.
