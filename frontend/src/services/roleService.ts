import api from './api'
import type { ApiResponse } from '../types/api'
import type { Role } from '../types/role'

const roleService = {
  async list(): Promise<Role[]> {
    const response = await api.get<ApiResponse<Role[][]>>('/admin/roles')
    const roles = response.data.data?.[0]

    if (!Array.isArray(roles)) {
      throw new Error('A API retornou uma resposta inválida ao listar as roles.')
    }

    return roles
  },

  async create(payload: Pick<Role, 'name' | 'description'>): Promise<Role> {
    const response = await api.post<ApiResponse<Role[]>>('/admin/roles', payload)
    const role = response.data.data?.[0]

    if (!role) {
      throw new Error('A API não retornou a role criada.')
    }

    return role
  },

  async update(id: string, payload: Pick<Role, 'name' | 'description'>): Promise<Role> {
    const response = await api.put<ApiResponse<Role[]>>(`/admin/roles/${encodeURIComponent(id)}`, payload)
    const role = response.data.data?.[0]

    if (!role) {
      throw new Error('A API não retornou a role atualizada.')
    }

    return role
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/admin/roles/${encodeURIComponent(id)}`)
  },
}

export default roleService
