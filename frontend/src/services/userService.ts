import api from "./api";

// Representa o formato de usuário retornado pelo backend.
export interface UserAdmin {
  id: string;
  name: string;
  avatar: string | null;
  email: string;
  telephone: string | null;
  active: boolean;
  roles: string[];
}

// Serviço responsável pelas operações administrativas de usuário.
const userService = {

  // Lista todos os usuários.
  // GET /users
  async findAll(): Promise<UserAdmin[]> {
    const response = await api.get<UserAdmin[]>("/users");

    return response.data;
  },

  // Busca um usuário pelo id.
  // GET /users/{id}
  async findById(id: string): Promise<UserAdmin> {
    const response = await api.get<UserAdmin>(`/users/${id}`);

    return response.data;
  },

  // Atualiza as roles/perfis do usuário.
  // PUT /users/{id}/roles
  async updateRoles(id: string, roles: string[]): Promise<void> {
    await api.put(`/users/${id}/roles`, {
      roles,
    });
  },

  // Exclui um usuário.
  // DELETE /users/{id}
  async delete(id: string): Promise<void> {
    await api.delete(`/users/${id}`);
  },
};

export default userService;