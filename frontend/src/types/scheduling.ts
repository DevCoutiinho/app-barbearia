// Tipos do fluxo de agendamento — baseados nos contratos reais do backend

export interface AvailableSlot {
  startTime: string // "HH:mm:ss" (LocalTime serializado pelo Jackson)
  endTime: string
}

export type SchedulingStatus = 'PENDING' | 'SCHEDULED' | 'COMPLETED' | 'CANCELED'

export const SCHEDULING_STATUS_LABELS: Record<SchedulingStatus, string> = {
  PENDING: 'Pendente',
  SCHEDULED: 'Agendado',
  COMPLETED: 'Concluído',
  CANCELED: 'Cancelado',
}

export interface SchedulingResponse {
  id: string
  serviceName: string
  barberName: string
  scheduledAt: string // Instant ISO-8601
  durationTime: number // minutos
  finalPrice: number
  status: SchedulingStatus
}

export interface SchedulingRequest {
  barberId: string
  serviceId: string
  scheduledAt: string // Instant ISO-8601
}

// Representa um barbeiro retornado pela API (endpoint a ser implementado pelo backend)
export interface BarberSummary {
  id: string
  name: string
  bio: string | null
}

// Representa um serviço retornado pela API (endpoint a ser implementado pelo backend)
export interface ServiceCatalogItem {
  id: string
  name: string
  price: number
  durationTime: number // minutos
  technique: string
  images: { id: string; url: string }[]
}

// Estado interno do wizard de agendamento
export interface BookingSelection {
  service: ServiceCatalogItem | null
  barber: BarberSummary | null
  date: string | null // "YYYY-MM-DD"
  slot: AvailableSlot | null
}
