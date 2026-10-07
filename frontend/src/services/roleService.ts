import api from './api'
import type { ApiResponse } from '../types/api'
import type { Role } from '../types/role'
import type { CreateRoleData, UpdateRoleData } from '../schemas/role.schema'

const roleService = {
  async list(): Promise<ApiResponse<Role[]>> {
    const response = await api.get<ApiResponse<Role[]>>('/admin/roles')
    return response.data
  },

  async create(data: CreateRoleData): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>('/admin/roles', data)
    return response.data;
  },

  async update(id: string, data: UpdateRoleData): Promise<ApiResponse<void>> {
    const response = await api.put<ApiResponse<void>>(`/admin/roles/${encodeURIComponent(id)}`, data)
    return response.data;
  },

  async remove(id: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/admin/roles/${encodeURIComponent(id)}`)
    return response.data;
  },
}

export default roleService