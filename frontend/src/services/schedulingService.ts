import api from './api'
import type { ApiResponse } from '../types/api'
import type {
  AvailableSlot,
  BarberSummary,
  SchedulingRequest,
  SchedulingResponse,
  ServiceCatalogItem,
} from '../types/scheduling'

const schedulingService = {
  /**
   * Consulta horários disponíveis para um barbeiro/serviço/data.
   * Endpoint confirmado: GET /availability/barbers/{barberId}/slots?serviceId=&date=
   * Autenticação: qualquer usuário autenticado.
   */
  async getAvailableSlots(
    barberId: string,
    serviceId: string,
    date: string, // "YYYY-MM-DD"
  ): Promise<ApiResponse<AvailableSlot[]>> {
    const response = await api.get<ApiResponse<AvailableSlot[]>>(
      `/availability/barbers/${encodeURIComponent(barberId)}/slots`,
      { params: { serviceId, date } },
    )
    return response.data
  },

  /**
   * Cria um agendamento para o cliente autenticado.
   * Endpoint confirmado: POST /schedulings
   * Autenticação: qualquer usuário autenticado (cliente).
   * Status inicial definido pelo backend: PENDING.
   */
  async create(payload: SchedulingRequest): Promise<ApiResponse<void>> {
    const response = await api.post<ApiResponse<void>>('/schedulings', payload)
    return response.data
  },

  /**
   * Retorna os agendamentos do cliente autenticado.
   * Endpoint confirmado: GET /schedulings/me
   * Autenticação: qualquer usuário autenticado (cliente).
   */
  async getMySchedulings(): Promise<ApiResponse<SchedulingResponse[]>> {
    const response = await api.get<ApiResponse<SchedulingResponse[]>>('/schedulings/me')
    return response.data
  },

  /**
   * ENDPOINT AUSENTE — lista barbeiros para o cliente.
   * Não existe no backend da branch origin/feature/fluxo-agendamento.
   * Necessário implementar: GET /barbers (público ou autenticado).
   * Retorna lista vazia até o backend disponibilizar.
   */
  async getBarbers(): Promise<ApiResponse<BarberSummary[]>> {
    const response = await api.get<ApiResponse<BarberSummary[]>>('/barbers')
    return response.data
  },

  /**
   * ENDPOINT AUSENTE — catálogo de serviços para o cliente.
   * Não existe no backend da branch origin/feature/fluxo-agendamento.
   * O endpoint /barber-services exige role BARBER e filtra pelo barbeiro autenticado.
   * Necessário implementar: GET /catalog/services ou similar (autenticado, sem role).
   * Retorna lista vazia até o backend disponibilizar.
   */
  async getCatalog(): Promise<ApiResponse<ServiceCatalogItem[]>> {
    const response = await api.get<ApiResponse<ServiceCatalogItem[]>>('/catalog/services')
    return response.data
  },
}

export default schedulingService
