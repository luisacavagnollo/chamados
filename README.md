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

Você pode verificar a listagem de chamados em:

**http://localhost:8080/chamados**

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

## Como usar o sistema

No menu lateral do HelpDesk, você encontrará:

- **Dashboard:** visão geral dos chamados e indicadores por status.
- **Chamados:** listagem, pesquisa, filtros, criação e acompanhamento dos chamados.
- **Clientes:** cadastro e gerenciamento dos solicitantes.
- **Técnicos:** cadastro dos profissionais responsáveis pelos atendimentos.
- **Categorias:** organização dos tipos de solicitação.

### Fluxo básico

1. Cadastre pelo menos um **cliente** e uma **categoria**.
2. Opcionalmente, cadastre um **técnico**.
3. Acesse **Chamados → Novo chamado**.
4. Preencha título, descrição, cliente, categoria, prioridade e, se desejar, técnico responsável.
5. Salve o chamado. Ele será criado com status **Aberto**.
6. Clique no registro da tabela para visualizar seus detalhes, atualizar o atendimento e registrar comentários.

Os estados de um chamado são **Aberto**, **Em atendimento**, **Aguardando cliente** e **Fechado**. Chamados fechados não aceitam novas edições nem comentários. Chamados que já possuem comentários não podem ser excluídos, conforme as regras do backend.

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

## Banco de dados H2

O backend utiliza o **H2**, com as propriedades configuradas em `src/main/resources/application.properties`.

O console H2 está habilitado em:

**http://localhost:8080/h2-console**

No projeto, o usuário configurado é `sa`, com senha vazia. A URL JDBC do banco deve ser conferida no log de inicialização do Spring Boot, pois **não está explicitamente definida** no `application.properties`.

**Atenção:** como não há uma URL JDBC de persistência em arquivo configurada, não conte com a preservação dos registros após reiniciar a aplicação. Para garantir persistência local entre execuções, será necessário configurar um banco H2 em arquivo ou outro banco persistente.

## Comandos úteis

Execute os comandos a seguir dentro da pasta `frontend`:

```bash
npm run dev      # Inicia o Vite em modo de desenvolvimento
npm run build    # Gera os arquivos de produção em frontend/dist
npm run preview  # Pré-visualiza o build de produção localmente
npm run lint     # Executa a verificação de código com Oxlint
```

Para parar os servidores, pressione **Ctrl + C** em cada terminal.

## Problemas comuns

**A página abre, mas os dados não carregam:** verifique se o Spring Boot está em execução na porta 8080 e se `http://localhost:8080/chamados` responde.

**Erro de CORS:** o backend está configurado para aceitar o frontend em `http://localhost:5173`. Se o Vite estiver usando outra porta, ajuste a origem permitida no `CorsConfig.java`.

**`npm` não é reconhecido:** instale o Node.js e reabra o terminal.

**`java` não é reconhecido ou a versão é incompatível:** instale o JDK 21 e confira o `JAVA_HOME`.

**A porta já está em uso:** encerre a execução anterior ou ajuste as portas e suas configurações correspondentes.

## Repositório

GitHub: https://github.com/luisacavagnollo/chamados
