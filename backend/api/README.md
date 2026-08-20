# Barbearia API ✂️💈

Este é o repositório do backend do **App Barbearia**, desenvolvido em **Java** com o framework **Spring Boot**. A API expõe os serviços de agendamento, autenticação, controle de acessos, notificações e regras de negócio para a barbearia.

---

## 🛠️ Stack Tecnológica

O projeto utiliza as seguintes tecnologias em sua stack principal:

- **Linguagem:** [Java 21](https://www.oracle.com/java/technologies/downloads/) (LTS)
- **Framework Principal:** [Spring Boot 4.0.7](https://spring.io/projects/spring-boot)
- **Gerenciador de Dependências:** [Maven](https://maven.apache.org/)
- **Banco de Dados:** [PostgreSQL](https://www.postgresql.org/) (com migrações gerenciadas pelo [Flyway](https://flywaydb.org/))
- **Segurança & Autenticação:** Spring Security com suporte a OAuth2 Resource Server (JWT)
- **Mensageria (Opcional/Configurada):** RabbitMQ / Spring AMQP
- **Documentação de API:** [Swagger / OpenAPI 3](https://swagger.io/)

---

## 📦 Explicação dos Pacotes e Dependências (`pom.xml`)

Abaixo estão explicadas as principais dependências declaradas no arquivo [pom.xml](file:///c:/Users/venic/Documents/app-barbearia/backend/api/pom.xml) e para que servem:

1. **`spring-boot-starter-webmvc`**: Provê a estrutura básica de MVC e REST, permitindo a criação de Controllers HTTP e endpoints da API.
2. **`spring-boot-starter-data-jpa`**: Facilita a persistência de dados utilizando a especificação JPA e o Hibernate como ORM para comunicação com o PostgreSQL.
3. **`spring-boot-starter-security`**: Adiciona camadas de segurança à API, controlando autenticação, autorização e proteção contra vulnerabilidades comuns.
4. **`spring-boot-starter-security-oauth2-resource-server`**: Permite que a aplicação atue como um servidor de recursos OAuth2, decodificando e validando Tokens JWT.
5. **`spring-boot-starter-amqp`**: Habilita a integração com servidores de mensageria (RabbitMQ) usando a especificação AMQP.
6. **`spring-boot-starter-mail`**: Habilita o envio de e-mails de forma simplificada a partir da aplicação.
7. **`spring-boot-starter-validation`**: Habilita validações automáticas com anotações Bean Validation (ex: `@NotNull`, `@Size`, `@Email`) nos payloads recebidos.
8. **`spring-boot-starter-flyway`** & **`flyway-database-postgresql`**: Executam scripts de migração automática do banco de dados na inicialização do sistema, garantindo consistência na estrutura das tabelas.
9. **`postgresql`**: Driver oficial de conexão do Java com o banco de dados PostgreSQL.
10. **`springdoc-openapi-starter-webmvc-ui`**: Gera automaticamente a documentação interativa da API (Swagger UI) disponível na rota `/api/docs`.
11. **`mapstruct`**: Framework para mapeamento de objetos Java (por exemplo, transformando DTOs em Entidades de forma rápida e segura em tempo de compilação).
12. **`lombok`**: Biblioteca que reduz a verbosidade do código Java gerando getters, setters, construtores e outros métodos repetitivos via anotações.

---

## ⚙️ Pré-requisitos para Rodar o Projeto

Antes de iniciar, você precisará ter instalado em sua máquina:
1. **Java Development Kit (JDK) 21** ou superior instalado e configurado no PATH.
2. **Maven 3.x** instalado (ou utilize o wrapper `./mvnw` incluso no projeto).
3. **PostgreSQL** instalado localmente ou rodando via Docker.

---

## 🚀 Como Clonar, Instalar e Rodar o Projeto Passo a Passo

### 1. Clonar o Repositório
Abra o seu terminal e execute o comando abaixo para clonar o repositório da barbearia:
```bash
git clone <https://github.com/DevCoutiinho/app-barbearia.git>
cd app-barbearia/backend/api
```

### 2. Configurar as Variáveis de Ambiente
A aplicação necessita de algumas credenciais do banco de dados expostas em variáveis de ambiente, conforme definido no [application.yaml]

Você pode defini-las no seu terminal ou criar um script/configuração na sua IDE de preferência:

#### No Windows (PowerShell):
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/barbearia_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="suasenha_aqui"
```

#### No Linux/macOS:
```bash
export DB_URL="jdbc:postgresql://localhost:5432/barbearia_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="suasenha_aqui"
```

#### No IntelliJ IDEA:
Caso utilize o **IntelliJ IDEA**, você pode configurá-las diretamente nas configurações de execução:
1. Clique no menu suspenso de **Run/Debug Configurations** no topo e selecione **Edit Configurations...**
2. Em **Spring Boot** -> **ApiApplication**, localize o campo **Environment variables** (ou clique em *Modify options* se não estiver visível).
3. Adicione as variáveis com o seguinte formato:
   `DB_URL=jdbc:postgresql://localhost:5432/barbearia_db;DB_USERNAME=postgres;DB_PASSWORD=suasenha_aqui`
4. Clique em **Apply** e depois em **OK**.

> [!IMPORTANT]
> Certifique-se de que o banco de dados chamado `barbearia_db` já esteja criado no seu PostgreSQL antes de rodar o projeto.

### 3. Instalar as Dependências (Build)
Use o Maven para baixar as dependências e buildar o projeto:
```bash
# Utilizando o Wrapper do Maven incluso no projeto:
./mvnw clean install

# Ou se você possui o Maven instalado globalmente:
mvn clean install
```

### 4. Executar a Aplicação
Após o build concluído com sucesso, você pode iniciar o servidor de desenvolvimento:

```bash
./mvnw spring-boot:run
```

A API estará de pé e escutando na porta **8080**!

---

## 📝 Documentação Interativa da API (Swagger)

Com o projeto em execução, você pode visualizar e testar todos os endpoints disponíveis acessando no seu navegador:

🔗 **[http://localhost:8080/api/docs](http://localhost:8080/api/docs)**
