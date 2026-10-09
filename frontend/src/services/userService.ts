import api from './api'
import type { ApiResponse } from '../types/api'
import type {
  PaginatedResponse,
  UpdateUserRolesPayload,
  User,
  UserFilters,
} from '../types/user'

export interface UserListParams {
  page?: number
  size?: number
  filters?: UserFilters
}

// A instância api cuida da autenticação e da renovação do token.
const userService = {
  // Lista os usuários com filtros e paginação iniciada em zero.
  async list({ page = 0, size = 10, filters = {} }: UserListParams = {}): Promise<ApiResponse<PaginatedResponse<User>[]>> {
    const params: UserFilters & { page: number; size: number; sort: string } = {
      page,
      size,
      // Evita a ordenação padrão inválida definida no backend.
      sort: 'name,asc',
    }

    // Envia somente os filtros preenchidos; false também é válido.
    if (filters.name) params.name = filters.name
    if (filters.email) params.email = filters.email
    if (filters.active !== undefined) params.active = filters.active

    const response = await api.get<ApiResponse<PaginatedResponse<User>[]>>('/admin/users', { params })
    return response.data
  },

  // Substitui as roles atuais pelos IDs informados no payload.
  async updateRoles(id: string, data: UpdateUserRolesPayload): Promise<ApiResponse<[]>> {
    const response = await api.put<ApiResponse<[]>>(`/admin/users/${encodeURIComponent(id)}`, data)
    return response.data
  },

  // Exclui o usuário; as validações da operação permanecem no backend.
  async remove(id: string): Promise<ApiResponse<[]>> {
    const response = await api.delete<ApiResponse<[]>>(`/admin/users/${encodeURIComponent(id)}`)
    return response.data
  },
}

export default userService
