# Proteção de propriedades (OWASP API3)

## Objetivo

Esta etapa impede que um usuário autenticado altere propriedades que não lhe pertencem ou que não fazem parte do contrato autorizado para seu papel.

## Contratos de entrada

- Administração de chamados continua usando `ChamadoRequest`, incluindo proprietário, técnico e demais campos administrativos.
- Clientes usam `ChamadoCriacaoClienteRequest` e `ChamadoEdicaoClienteRequest`. Esses DTOs permitem somente título, descrição, prioridade e categoria.
- A criação e edição por cliente rejeitam campos como `clienteId`, `tecnicoId`, `status`, `dataAbertura` e `dataFechamento`.
- Clientes usam `ClienteAtualizacaoRequest`, limitado a `nome` e `telefone`.
- `ComentarioRequest` contém somente `texto`; `autor` é derivado da identidade autenticada.

## Regras por papel

- Cliente cria chamado como proprietário vinculado à sua identidade e edita somente chamados próprios ainda abertos.
- Cliente pode comentar somente em chamado próprio.
- Técnico pode comentar somente em chamado atribuído.
- Administrador mantém as operações administrativas existentes.

## Exposição de dados

Os DTOs de resposta não incluem senha ou hash. Dados pessoais são expostos somente em operações já autorizadas; o autor de comentário é identificado pelo usuário autenticado. O proprietário e o técnico são representados por IDs e nomes necessários ao contrato da aplicação.

## Autorização

`UsuarioAutenticadoService` fornece papel e vínculos da identidade atual. `AutorizacaoRecursoService` confirma propriedade de chamado, atribuição de técnico e acesso ao cadastro de cliente antes de cada operação sensível.
