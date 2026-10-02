import axios from "axios";

// Cria uma instância padrão do Axios.
// Todas as chamadas para o backend usarão esta configuração.
const api = axios.create({
  // Endereço base da API Spring Boot.
  baseURL: "http://localhost:8080",

  // Define JSON como formato padrão.
  headers: {
    "Content-Type": "application/json",
  },

  // Mantém suporte a credenciais, caso necessário.
  withCredentials: true,
});

// Intercepta todas as requisições antes de enviá-las.
api.interceptors.request.use((config) => {
  // Busca o token JWT salvo após o login.
  const token = localStorage.getItem("accessToken");

  // Se existir token, adiciona no cabeçalho Authorization.
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Retorna a configuração já preparada.
  return config;
});

export default api;