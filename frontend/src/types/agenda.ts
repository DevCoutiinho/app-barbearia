export const DAYS_OF_WEEK = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'] as const
export type DayOfWeek = typeof DAYS_OF_WEEK[number]

export const DAY_LABELS: Record<DayOfWeek, { short: string; long: string }> = {
  MONDAY: { short: 'Seg', long: 'Segunda-feira' },
  TUESDAY: { short: 'Ter', long: 'Terça-feira' },
  WEDNESDAY: { short: 'Qua', long: 'Quarta-feira' },
  THURSDAY: { short: 'Qui', long: 'Quinta-feira' },
  FRIDAY: { short: 'Sex', long: 'Sexta-feira' },
  SATURDAY: { short: 'Sáb', long: 'Sábado' },
  SUNDAY: { short: 'Dom', long: 'Domingo' },
}

/** Horários no formato HH:mm, sempre no fuso da barbearia. */
export interface TimeRange {
  start: string
  end: string
}

export interface WorkingDay {
  dayOfWeek: DayOfWeek
  // Vários intervalos no mesmo dia = expediente com pausas. Lista vazia = folga.
  intervals: TimeRange[]
}

export interface WeeklySchedule {
  days: WorkingDay[]
}

export const BLOCK_REASON_VALUES = ['LUNCH', 'DAY_OFF', 'HOLIDAY', 'PERSONAL_APPOINTMENT', 'LEAVE'] as const
export type BlockReason = typeof BLOCK_REASON_VALUES[number]

export const BLOCK_REASONS: ReadonlyArray<{ value: BlockReason; label: string }> = [
  { value: 'DAY_OFF', label: 'Folga' },
  { value: 'HOLIDAY', label: 'Feriado' },
  { value: 'LUNCH', label: 'Almoço' },
  { value: 'LEAVE', label: 'Saída' },
  { value: 'PERSONAL_APPOINTMENT', label: 'Compromisso pessoal' },
]

export interface ScheduleBlock {
  id: string
  // ISO com offset do fuso da barbearia (ex.: 2026-11-20T12:00:00-03:00).
  startsAt: string
  endsAt: string
  reason: BlockReason
  reasonLabel: string
  description: string | null
}

/** Sem startTime/endTime o bloqueio vale para o dia inteiro. */
export interface ScheduleBlockPayload {
  date: string
  startTime?: string
  endTime?: string
  reason: BlockReason
  description?: string
}

export interface AvailableSlot {
  startAt: string
  endAt: string
}

export interface BarberSummary {
  id: string
  name: string
  avatar: string | null
  telephone: string | null
}

export interface CatalogService {
  id: string
  name: string
  technique: string
  price: number
  durationTime: number
  barber: BarberSummary
}

export type SchedulingStatus = 'CANCELED' | 'COMPLETED' | 'SCHEDULED' | 'PENDING'

export interface Scheduling {
  id: string
  barberId: string
  serviceId: string
  serviceName: string
  startAt: string
  endAt: string
  durationTime?: number
  status: SchedulingStatus
  finalPrice: number
  previousStartAt?: string | null
  previousEndAt?: string | null
  rescheduleCount?: number
}

export interface SchedulingPayload {
  barberId?: string
  serviceId: string
  startAt: string
}

export interface SchedulingReschedulePayload {
  newStartAt: string
}

export interface BarberAppointmentHistory {
  id: string
  clientId: string
  clientName: string
  serviceId: string
  serviceName: string
  status: SchedulingStatus
  scheduledAt: string
  endsAt: string
  durationTime: number
  price: number
}

export interface BarberSchedulingFilters {
  startDate?: string
  endDate?: string
  clientName?: string
  status?: SchedulingStatus
  page?: number
  size?: number
}
