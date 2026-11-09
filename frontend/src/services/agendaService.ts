import api from './api'
import type { ApiResponse } from '../types/api'
import type {
  AvailableSlot,
  CatalogService,
  ScheduleBlock,
  ScheduleBlockPayload,
  Scheduling,
  SchedulingPayload,
  WeeklySchedule,
} from '../types/agenda'
import { normalizeSchedule } from '../utils/agenda'

const WORKING_HOURS_PATH = '/barbers/me/working-hours'
const BLOCKS_PATH = '/barbers/me/blocks'

const blockPath = (id: string) => `${BLOCKS_PATH}/${encodeURIComponent(id)}`
const availabilityPath = (barberId: string) => `/barbers/${encodeURIComponent(barberId)}/availability`

/** A API sempre envolve o recurso em `data: [item]`. */
function single<T>(body: ApiResponse<T[]>, resource: string): T {
  const item = body.data?.[0]
  if (!item) throw new Error(`Resposta de ${resource} inválida.`)
  return item
}

function list<T>(body: ApiResponse<T[]>): T[] {
  return Array.isArray(body.data) ? body.data : []
}

const agendaService = {
  /* ── Grade semanal do barbeiro logado ── */
  async getWorkingHours(): Promise<WeeklySchedule> {
    const response = await api.get<ApiResponse<WeeklySchedule[]>>(WORKING_HOURS_PATH)
    return normalizeSchedule(single(response.data, 'grade semanal'))
  },

  /** Substitui a grade inteira. O servidor não altera agendamentos já existentes. */
  async saveWorkingHours(schedule: WeeklySchedule): Promise<WeeklySchedule> {
    const response = await api.put<ApiResponse<WeeklySchedule[]>>(WORKING_HOURS_PATH, schedule)
    return normalizeSchedule(single(response.data, 'grade semanal'))
  },

  /* ── Bloqueios pontuais (o servidor limita o período a 93 dias) ── */
  async listBlocks(params: { from?: string; to?: string } = {}): Promise<ScheduleBlock[]> {
    const response = await api.get<ApiResponse<ScheduleBlock[]>>(BLOCKS_PATH, { params })
    return list(response.data)
  },

  async createBlock(payload: ScheduleBlockPayload): Promise<ScheduleBlock> {
    const response = await api.post<ApiResponse<ScheduleBlock[]>>(BLOCKS_PATH, payload)
    return single(response.data, 'bloqueio')
  },

  async deleteBlock(id: string): Promise<void> {
    await api.delete(blockPath(id))
  },

  /* ── Visão do cliente ── */
  async listCatalog(): Promise<CatalogService[]> {
    const response = await api.get<ApiResponse<CatalogService[]>>('/services/catalog')
    return list(response.data)
  },

  /** Horários livres calculados pelo servidor (grade − bloqueios − agendamentos). */
  async listAvailableSlots(barberId: string, serviceId: string, date: string): Promise<AvailableSlot[]> {
    const response = await api.get<ApiResponse<AvailableSlot[]>>(availabilityPath(barberId), { params: { serviceId, date } })
    return list(response.data)
  },

  async createScheduling(payload: SchedulingPayload): Promise<Scheduling> {
    const response = await api.post<ApiResponse<Scheduling[]>>('/schedulings', payload)
    return single(response.data, 'agendamento')
  },
}

export default agendaService
