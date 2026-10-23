import api from './api'
import type { ApiResponse } from '../types/api'
import type { PaginatedResponse } from '../types/user'
import type {
  BarberService,
  CreateServicePayload,
  ServiceCategory,
  ServiceFilters,
  ServiceListParams,
  UpdateServicePayload,
} from '../types/service'

function buildServiceFormData(payload: CreateServicePayload | UpdateServicePayload, image?: File): FormData {
  const formData = new FormData()
  formData.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' }))
  if (image) formData.append('image', image)
  return formData
}

const serviceService = {
  async list({ page = 0, size = 10, sort, filters = {} }: ServiceListParams = {}): Promise<ApiResponse<PaginatedResponse<BarberService>[]>> {
    const params: ServiceFilters & Pick<ServiceListParams, 'page' | 'size' | 'sort'> = { page, size }

    if (filters.name?.trim()) params.name = filters.name.trim()
    if (filters.technique) params.technique = filters.technique
    if (filters.active !== undefined) params.active = filters.active
    if (filters.categoryId) params.categoryId = filters.categoryId
    if (filters.minPrice !== undefined) params.minPrice = filters.minPrice
    if (filters.maxPrice !== undefined) params.maxPrice = filters.maxPrice
    if (sort) params.sort = sort

    const response = await api.get<ApiResponse<PaginatedResponse<BarberService>[]>>('/barber-services', {
      params,
      paramsSerializer: { indexes: null },
    })
    return response.data
  },

  async create(payload: CreateServicePayload, image?: File): Promise<ApiResponse<null>> {
    const response = await api.post<ApiResponse<null>>('/barber-services', buildServiceFormData(payload, image), {
      // O navegador define o Content-Type e o boundary do multipart.
      headers: { 'Content-Type': undefined },
    })
    return response.data
  },

  async update(id: string, payload: UpdateServicePayload): Promise<ApiResponse<null>> {
    const response = await api.put<ApiResponse<null>>(`/barber-services/${encodeURIComponent(id)}`, buildServiceFormData(payload), {
      headers: { 'Content-Type': undefined },
    })
    return response.data
  },

  async changeStatus(service: BarberService, active: boolean): Promise<ApiResponse<null>> {
    return serviceService.update(service.id, {
      name: service.name,
      technique: service.technique,
      price: service.price,
      active,
      durationTime: service.durationTime,
      categoryId: service.category.id,
    })
  },

  async listCategories(): Promise<ApiResponse<ServiceCategory[]>> {
    const response = await api.get<ApiResponse<ServiceCategory[]>>('/service-categories')
    return response.data
  },
}

export default serviceService
