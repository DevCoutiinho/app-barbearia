import api from './api'
import type { ApiResponse } from '../types/api'
import type { Permission } from '../types/role'
import type { CreatePermissionData } from '../schemas/permission.schema'

const permissionService = {

  async list(): Promise<ApiResponse<Permission[]>> {
    const response = await api.get<ApiResponse<Permission[]>>('/admin/permissions')
    return response.data
  },

  async create(data: CreatePermissionData): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>('/admin/permissions', data)
    return response.data
  },

  async remove(id: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/admin/permissions/${encodeURIComponent(id)}`)
    return response.data
  },
}

export default permissionService