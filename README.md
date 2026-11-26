# HelpDesk — Sistema de Gerenciamento de Chamados

Sistema web para registrar, organizar e acompanhar chamados de suporte técnico. O projeto utiliza **Spring Boot** no backend e **React com Vite** no frontend.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Data JPA, Maven Wrapper |
| Banco de dados | H2 |
| Frontend | React, Vite, JavaScript, React Router |
| Requisições HTTP | Axios |
| Interface | CSS e Lucide React |

## Pré-requisitos

Antes de iniciar, instale:

- **JDK 21** (confira com `java -version`);
- **Node.js e npm** (confira com `node -v` e `npm -v`);
- **Git**, caso vá clonar o repositório.

O repositório já inclui o **Maven Wrapper** (`mvnw` e `mvnw.cmd`), portanto não é necessário instalar o Maven separadamente.

## Como executar o projeto localmente

O sistema possui dois servidores que devem ficar ligados **ao mesmo tempo**, cada um em seu próprio terminal.

### 1. Obter o código

Clone o repositório:

```bash
git clone https://github.com/luisacavagnollo/chamados.git
cd chamados
```

> Se você já tem a pasta do projeto no computador, basta abri-la no VS Code e abrir dois terminais.
>
> Se a versão em desenvolvimento estiver em outra branch, selecione essa branch antes de executar o projeto.

### 2. Iniciar o backend (Spring Boot)

No **primeiro terminal**, na raiz do projeto (`chamados`):

**Windows / PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

Na primeira execução, o Maven pode levar um pouco mais de tempo para baixar as dependências.

Por padrão, a API ficará disponível em:

**http://localhost:8080**

Na execução local, o perfil `dev` é ativado por padrão e o servidor escuta somente
em `127.0.0.1`. O banco H2 é em memória, portanto os registros são descartados ao
reiniciar a aplicação. Todas as rotas da API exigem autenticação HTTP Basic. A
execução normal não cria contas de usuário; use o perfil `demo` descrito na seção
abaixo para habilitar dados fictícios locais.

Com o perfil `demo` ativo, a aplicação escuta somente em `127.0.0.1`.
O endereço `/chamados` pode ser consultado por um cliente HTTP configurado com as
credenciais de demonstração.

Deixe esse terminal aberto enquanto estiver utilizando o sistema.

### 3. Iniciar o frontend (React + Vite)

No **segundo terminal**, a partir da raiz do projeto:

```powershell
cd frontend
npm install
npm run dev
```

- `npm install`: instala as dependências descritas no `package.json`.
- `npm run dev`: inicia o servidor de desenvolvimento do **Vite**, que executa a interface React e atualiza a tela automaticamente quando o código é salvo.

O Vite normalmente informa no terminal um endereço como:

**http://localhost:5173**

Abra esse endereço no navegador para acessar o HelpDesk. Se o Vite escolher outra porta, utilize a URL exibida no terminal; nesse caso, talvez seja necessário ajustar a configuração de CORS do backend.

> **Importante:** o frontend depende do backend para carregar e salvar os dados. Os dois terminais devem permanecer em execução.

O frontend solicita as credenciais na tela de login e mantém a autorização Basic
somente em memória. Ao recarregar a página, será necessário autenticar novamente.
O primeiro `GET /auth/me` valida a identidade e permite que o backend emita o cookie
`XSRF-TOKEN`; o Axios o envia como `X-XSRF-TOKEN` nas operações de escrita. O
frontend não persiste senha ou token em `localStorage` ou `sessionStorage`.

Defina `VITE_API_URL` se a API não estiver em `http://localhost:8080`. A origem do
frontend também precisa estar autorizada em `APP_CORS_ALLOWED_ORIGINS` no processo
do backend. Mais detalhes estão em [frontend/README.md](./frontend/README.md).

### Perfil local de demonstração

O perfil `demo` é opcional e não é ativado por padrão. Ele cria, sem sobrescrever
registros existentes, dois clientes fictícios, um técnico, um administrador, uma
categoria e dois chamados. As contas são `ana.cliente@exemplo.invalid`,
`bruno.cliente@exemplo.invalid`, `caio.tecnico@exemplo.invalid` e
`admin@exemplo.invalid`.

No PowerShell, defina senhas locais antes de iniciar o backend; a entrada é oculta e
os valores ficam somente no ambiente desse processo:

```powershell
function Set-DemoPassword($name) {
    $secure = Read-Host "Senha para $name" -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        [Environment]::SetEnvironmentVariable(
            $name,
            [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer),
            "Process"
        )
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        $secure.Dispose()
    }
}

Set-DemoPassword "DEMO_CLIENTE_1_PASSWORD"
Set-DemoPassword "DEMO_CLIENTE_2_PASSWORD"
Set-DemoPassword "DEMO_TECNICO_PASSWORD"
Set-DemoPassword "DEMO_ADMIN_PASSWORD"
$env:SPRING_PROFILES_ACTIVE = "demo"
.\mvnw.cmd spring-boot:run
```

Em Linux/macOS, configure as quatro variáveis `DEMO_*_PASSWORD` por um mecanismo
seguro do ambiente e `SPRING_PROFILES_ACTIVE=demo` no processo que executa
`./mvnw spring-boot:run`. Se alguma senha não estiver configurada, a inicialização
do perfil falha explicitamente.

HTTP Basic é somente para demonstração local. Em qualquer implantação real, exige
HTTPS para proteger as credenciais em trânsito. A estratégia definitiva de
autenticação será avaliada posteriormente. A proteção CSRF permanece ativa: operações
que alteram dados também precisam enviar o token CSRF disponibilizado no cookie
`XSRF-TOKEN` pelo backend.

## Como usar o sistema

No menu lateral do HelpDesk, você encontrará:

- **Dashboard:** visão geral dos chamados e indicadores por status.
- **Chamados:** listagem, pesquisa, filtros, criação e acompanhamento dos chamados.
- **Clientes:** cadastro e gerenciamento dos solicitantes.
- **Técnicos:** cadastro dos profissionais responsáveis pelos atendimentos.
- **Categorias:** organização dos tipos de solicitação.

### Fluxo administrativo básico

1. Cadastre pelo menos um **cliente** e uma **categoria**.
2. Opcionalmente, cadastre um **técnico**.
3. Acesse **Chamados → Novo chamado**.
4. Preencha título, descrição, cliente, categoria, prioridade e, se desejar, técnico responsável.
5. Salve o chamado. Ele será criado com status **Aberto**.
6. Clique no registro da tabela para visualizar seus detalhes, atualizar o atendimento e registrar comentários.

Os estados de um chamado são **Aberto**, **Em atendimento**, **Aguardando cliente** e **Fechado**. Chamados fechados não aceitam novas edições nem comentários. Chamados que já possuem comentários não podem ser excluídos, conforme as regras do backend.

O acesso à interface depende do papel autenticado. Clientes criam e editam seus
chamados por `/chamados/meus` e atualizam somente nome/telefone em `/clientes/me`;
técnicos podem alterar status apenas quando autorizados pelo vínculo do chamado;
administradores mantêm cadastros, atribuições e operações administrativas. Os
controles de tela não substituem as autorizações aplicadas na API.

## Estrutura resumida do projeto

```text
chamados/
├── frontend/
│   ├── src/
│   │   ├── components/   # Componentes reutilizáveis e modais
│   │   ├── pages/        # Telas da aplicação
│   │   ├── services/     # Configuração do Axios
│   │   └── styles/       # Estilos CSS
│   └── package.json
├── src/
│   ├── main/
│   │   ├── java/br/com/sistemas/chamados/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   └── resources/application.properties
│   └── test/
├── mvnw.cmd
├── mvnw
└── pom.xml
```

## Banco de dados e configurações locais

O perfil local `dev` utiliza o **H2** em memória. A configuração cria/atualiza o
schema para desenvolvimento local; ela não deve ser usada em produção. O console H2
fica desabilitado e o log de SQL também.

O perfil `test`, ativado automaticamente nos testes, usa outro banco H2 em memória,
isolado do perfil local. Para qualquer ambiente fora de `dev`, `demo` e `test`, a
configuração padrão valida o schema em vez de alterá-lo. Este projeto ainda não inclui
migrações versionadas; prepare e aplique migrações antes de implantar uma base gerida.

As propriedades de conexão podem ser fornecidas por `DATABASE_URL`,
`DATABASE_USERNAME` e `DATABASE_PASSWORD`. `DDL_AUTO` pode sobrescrever a estratégia
do Hibernate nos perfis `dev` e `demo`; fora deles, o padrão é `validate`. Para
ambientes geridos, mantenha `validate` e nunca habilite a atualização automática de
schema. Não use credenciais reais no código ou em arquivos versionados.

O CORS aceita por padrão `http://localhost:5173`, com métodos e cabeçalhos explícitos.
Para outra origem local, configure `APP_CORS_ALLOWED_ORIGINS` com uma lista separada
por vírgulas. A API limita páginas de `GET /chamados/filtro` a 100 itens; valores
inválidos de página/tamanho retornam HTTP 400.

Respostas de erro da API usam JSON com `timestamp`, `status`, `erro` e `detalhes`.
Requisições sem autenticação recebem 401 e desafio HTTP Basic; requisições negadas ou
sem CSRF válido recebem 403. Eventos de autenticação, negações e alterações de
chamados são registrados sem incluir senhas, e-mails, tokens ou corpos de requisição.

## Comandos úteis

Execute os comandos a seguir dentro da pasta `frontend`:

```bash
npm test       # Executa os testes de comportamento do frontend
npm run dev      # Inicia o Vite em modo de desenvolvimento
npm run build    # Gera os arquivos de produção em frontend/dist
npm run preview  # Pré-visualiza o build de produção localmente
npm run lint     # Executa a verificação de código com Oxlint
```

Para parar os servidores, pressione **Ctrl + C** em cada terminal.

## Problemas comuns

**O login não consegue validar a conta:** verifique se o backend está em execução,
se a URL configurada em `VITE_API_URL` está correta e se a conta existe no perfil
ativo. O perfil `demo` exige as quatro senhas fictícias de demonstração.

**Erro de CORS ou CSRF:** confirme que `APP_CORS_ALLOWED_ORIGINS` inclui exatamente
a origem do Vite, sem curingas. Não remova CSRF: a interface obtém o cookie
`XSRF-TOKEN` durante a consulta autenticada a `/auth/me`.

**Erro de CORS:** o backend aceita por padrão `http://localhost:5173`. Se o Vite estiver usando outra porta, defina `APP_CORS_ALLOWED_ORIGINS` no processo do backend.

**`npm` não é reconhecido:** instale o Node.js e reabra o terminal.

**`java` não é reconhecido ou a versão é incompatível:** instale o JDK 21 e confira o `JAVA_HOME`.

**A porta já está em uso:** encerre a execução anterior ou ajuste as portas e suas configurações correspondentes.

## Repositório

GitHub: https://github.com/luisacavagnollo/chamados
