import api from './api'
import type { ApiResponse } from '../types/api'
import type { Permission } from '../types/role'

export interface PermissionCreate {
  name: string
  description: string
}

const permissionService = {
  async list(): Promise<Permission[]> {
    const response = await api.get<ApiResponse<Permission[][]>>('/admin/permissions')
    const permissions = response.data.data?.[0]

    if (!Array.isArray(permissions)) {
      throw new Error('A API retornou uma resposta inválida ao listar as permissões.')
    }

    return permissions
  },

  async getById(id: string): Promise<Permission> {
    const response = await api.get<ApiResponse<Permission[]>>(
      `/admin/permissions/${encodeURIComponent(id)}`
    )

    const permission = response.data.data?.[0]

    if (!permission) {
      throw new Error('A API não retornou a permissão solicitada.')
    }

    return permission
  },

  async create(payload: PermissionCreate): Promise<Permission> {
    const response = await api.post<ApiResponse<Permission[]>>(
      '/admin/permissions',
      payload
    )

    const permission = response.data.data?.[0]

    if (!permission) {
      throw new Error('A API não retornou a permissão criada.')
    }

    return permission
  },

  async remove(id: string): Promise<void> {
    await api.delete(`/admin/permissions/${encodeURIComponent(id)}`)
  },
}

export default permissionService