# 💈 App Barbearia - Frontend

Este é o frontend da aplicação **App Barbearia**, desenvolvido com **Vue 3**, **TypeScript** e **Vite**. O objetivo desta interface é oferecer uma experiência moderna, rápida e intuitiva para o agendamento e gerenciamento de serviços de barbearia.

---

## 🛠️ Stack Tecnológica

A aplicação foi estruturada utilizando tecnologias modernas do ecossistema JavaScript/TypeScript para garantir performance, tipagem estática e facilidade de manutenção:

*   **Vue 3 (Composition API):** Framework progressivo para construção de interfaces de usuário com alto desempenho.
*   **TypeScript:** Superconjunto do JavaScript que adiciona tipagem estática opcional, ajudando a prevenir erros em tempo de desenvolvimento.
*   **Vite:** Build tool extremamente rápida que melhora significativamente a experiência de desenvolvimento (HMR imediato).
*   **Tailwind CSS (v4):** Framework CSS utilitário integrado de forma nativa ao Vite para estilização rápida e responsiva.

---

## 📦 Pacotes & Dependências

Abaixo está a lista detalhada das dependências utilizadas no projeto e a utilidade de cada uma:

### Dependências de Produção (`dependencies`)

| Pacote | Finalidade / Utilidade |
| :--- | :--- |
| **`vue`** | Núcleo do framework Vue 3. |
| **`vue-router`** | Roteador oficial do Vue, utilizado para gerenciar a navegação entre as páginas do sistema. |
| **`pinia`** | Store de gerenciamento de estado global (substituto moderno do Vuex). |
| **`axios`** | Cliente HTTP baseado em Promessas para realizar requisições à API do backend. |
| **`tailwindcss`** & **`@tailwindcss/vite`** | Framework de estilização utilitária e sua integração nativa de alta performance com o Vite. |
| **`@lucide/vue`** | Conjunto de ícones vetoriais modernos, limpos e fáceis de usar como componentes Vue. |
| **`vue-sonner`** | Biblioteca de notificações tipo "toast" bonita, fluida e altamente customizável. |
| **`vee-validate`** | Biblioteca de validação de formulários flexível e focada na experiência de desenvolvimento. |
| **`zod`** | Biblioteca de declaração e validação de esquemas com tipagem estática automática. |
| **`@vee-validate/zod`** | Integração para utilizar validações do Zod de forma transparente no Vee-Validate. |
| **`@vueuse/core`** | Coleção essencial de utilitários e funções reutilizáveis da Composition API. |

### Dependências de Desenvolvimento (`devDependencies`)

| Pacote | Finalidade / Utilidade |
| :--- | :--- |
| **`typescript`** | Compilador e suporte ao TypeScript. |
| **`vite`** | Servidor de desenvolvimento e empacotador de produção. |
| **`@vitejs/plugin-vue`** | Plugin oficial do Vite para suportar Single File Components (`.vue`). |
| **`vue-tsc`** | Ferramenta de verificação de tipos e compilação focada em arquivos Vue + TypeScript. |
| **`@types/node`** | Definições de tipos do TypeScript para o ambiente Node.js. |
| **`@vue/tsconfig`** | Configurações base do TypeScript recomendadas para projetos Vue. |

---

## 🚀 Como Iniciar o Projeto

Siga os passos abaixo para clonar, instalar e executar o projeto localmente.

### Pré-requisitos
Certifique-se de ter instalado em sua máquina:
*   [Node.js](https://nodejs.org/) (recomendado versão LTS)
*   [NPM](https://www.npmjs.com/) ou [Yarn](https://yarnpkg.com/)

---

### 1. Clonar o Repositório
Abra o seu terminal e execute o comando abaixo para clonar o repositório do projeto:

```bash
git clone https://github.com/DevCoutiinho/app-barbearia.git
```

Em seguida, navegue até a pasta do frontend:

```bash
cd app-barbearia/frontend
```

---

### 2. Instalar as Dependências
Instale todos os pacotes necessários definidos no `package.json`:

```bash
npm install --legacy-peer-deps
```
> **Nota:** O parâmetro `--legacy-peer-deps` ajuda a mitigar possíveis conflitos de versões de peer dependencies herdadas.

---

### 3. Rodar em Ambiente de Desenvolvimento
Com as dependências instaladas, inicialize o servidor local de desenvolvimento do Vite:

```bash
npm run dev
```

O terminal exibirá a URL local (geralmente `http://localhost:5173/`). Abra este endereço no seu navegador para ver e testar a aplicação!

---

### 4. Compilar para Produção (Build)
Para gerar os arquivos otimizados e prontos para produção na pasta `dist/`, execute:

```bash
npm run build
```
