import { DAYS_OF_WEEK, type DayOfWeek, type ScheduleBlock, type TimeRange, type WeeklySchedule } from '../types/agenda'

const TIME_PATTERN = /^([01]\d|2[0-3]):[0-5]\d$/

/** As rotas /barbers/me/** exigem a role BARBER no servidor. */
export function isBarber(user: { roles?: string[] } | null | undefined): boolean {
  const roles = user?.roles ?? []
  return roles.includes('BARBER') || roles.includes('BARBEIRO')
}

/** A API serializa LocalTime como HH:mm ou HH:mm:ss; a interface trabalha sempre com HH:mm. */
export function toHHmm(value: string): string {
  return value.slice(0, 5)
}

export function isValidTime(value: string): boolean {
  return TIME_PATTERN.test(value)
}

export function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

export function fromMinutes(total: number): string {
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

/** Mesmas regras do servidor: término depois do início e intervalos sem sobreposição. */
export function intervalsError(intervals: readonly TimeRange[]): string | null {
  for (const range of intervals) {
    if (!range.start || !range.end) return 'Informe o início e o término de cada intervalo.'
    if (!isValidTime(range.start) || !isValidTime(range.end)) return 'Use horários no formato HH:mm.'
    if (toMinutes(range.end) <= toMinutes(range.start)) return 'O término deve ser depois do início.'
  }
  const sorted = [...intervals].sort((a, b) => toMinutes(a.start) - toMinutes(b.start))
  for (let index = 1; index < sorted.length; index++) {
    if (toMinutes(sorted[index].start) < toMinutes(sorted[index - 1].end)) return 'Os intervalos não podem se sobrepor.'
  }
  return null
}

/** Garante os 7 dias na ordem da semana, com intervalos ordenados. */
export function normalizeSchedule(schedule: WeeklySchedule | null | undefined): WeeklySchedule {
  return {
    days: DAYS_OF_WEEK.map(dayOfWeek => {
      const intervals = schedule?.days.find(day => day.dayOfWeek === dayOfWeek)?.intervals ?? []
      return {
        dayOfWeek,
        intervals: intervals
          .map(range => ({ start: toHHmm(range.start), end: toHHmm(range.end) }))
          .sort((a, b) => toMinutes(a.start) - toMinutes(b.start)),
      }
    }),
  }
}

export function hasWorkingHours(schedule: WeeklySchedule | null | undefined): boolean {
  return Boolean(schedule?.days.some(day => day.intervals.length > 0))
}

/** Pausas = espaços entre os intervalos de trabalho do mesmo dia. */
export function pausesBetween(intervals: readonly TimeRange[]): TimeRange[] {
  const pauses: TimeRange[] = []
  for (let index = 1; index < intervals.length; index++) {
    if (toMinutes(intervals[index].start) > toMinutes(intervals[index - 1].end)) {
      pauses.push({ start: intervals[index - 1].end, end: intervals[index].start })
    }
  }
  return pauses
}

/* ── Datas (sempre em horário local, sem conversão para UTC) ── */

export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

export function parseIsoDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function addDays(date: Date, days: number): Date {
  const result = new Date(date)
  result.setDate(result.getDate() + days)
  return result
}

/** Segunda-feira da semana da data. */
export function startOfWeek(date: Date): Date {
  const result = new Date(date.getFullYear(), date.getMonth(), date.getDate())
  return addDays(result, -((result.getDay() + 6) % 7))
}

export function weekDates(start: Date): Date[] {
  return Array.from({ length: 7 }, (_, index) => addDays(start, index))
}

export function dayOfWeekOf(date: Date): DayOfWeek {
  return DAYS_OF_WEEK[(date.getDay() + 6) % 7]
}

export function formatDayMonth(date: Date): string {
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
}

export function formatWeekRange(start: Date): string {
  const end = addDays(start, 6)
  const sameMonth = start.getMonth() === end.getMonth()
  const startLabel = start.toLocaleDateString('pt-BR', sameMonth ? { day: '2-digit' } : { day: '2-digit', month: 'short' })
  const endLabel = end.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
  return `${startLabel} a ${endLabel}`.replace(/\./g, '')
}

/* ── Instantes da API: já vêm no fuso da barbearia, então lemos data/hora direto do texto ── */

export function isoDatePart(value: string): string {
  return value.slice(0, 10)
}

export function isoTimePart(value: string): string {
  return value.slice(11, 16)
}

export function isFullDayBlock(block: Pick<ScheduleBlock, 'startsAt' | 'endsAt'>): boolean {
  return isoTimePart(block.startsAt) === '00:00'
    && isoTimePart(block.endsAt) === '00:00'
    && isoDatePart(block.endsAt) > isoDatePart(block.startsAt)
}

export function formatBlockPeriod(block: Pick<ScheduleBlock, 'startsAt' | 'endsAt'>): string {
  const date = parseIsoDate(isoDatePart(block.startsAt))
  const day = date.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' }).replace('.', '')
  return isFullDayBlock(block)
    ? `${day} · Dia inteiro`
    : `${day} · ${isoTimePart(block.startsAt)} às ${isoTimePart(block.endsAt)}`
}

/** Trecho do bloqueio que cai na data, em minutos desde 00:00 (bloqueios de vários dias são recortados). */
export function blockSegmentOn(block: Pick<ScheduleBlock, 'startsAt' | 'endsAt'>, isoDate: string): { start: number; end: number } | null {
  const startDate = isoDatePart(block.startsAt)
  const endDate = isoDatePart(block.endsAt)
  if (isoDate < startDate || isoDate > endDate) return null
  const start = isoDate === startDate ? toMinutes(isoTimePart(block.startsAt)) : 0
  const end = isoDate === endDate ? toMinutes(isoTimePart(block.endsAt)) : 24 * 60
  return end > start ? { start, end } : null
}

/** Faixa de horas exibida na grade: cobre todo o expediente, com 08:00–19:00 como padrão. */
export function visibleHourRange(schedule: WeeklySchedule | null | undefined): { start: number; end: number } {
  const intervals = schedule?.days.flatMap(day => day.intervals) ?? []
  if (!intervals.length) return { start: 8, end: 19 }
  const start = Math.min(...intervals.map(range => toMinutes(range.start)))
  const end = Math.max(...intervals.map(range => toMinutes(range.end)))
  return { start: Math.floor(start / 60), end: Math.max(Math.ceil(end / 60), Math.floor(start / 60) + 1) }
}

export function formatPrice(value: number): string {
  return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}

export const SCHEDULING_STATUS_CONFIG: Record<string, { label: string; badgeClass: string }> = {
  PENDING: { label: 'Pendente', badgeClass: 'bg-amber-50 text-amber-800 border-amber-200' },
  SCHEDULED: { label: 'Agendado', badgeClass: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
  COMPLETED: { label: 'Finalizado', badgeClass: 'bg-blue-50 text-blue-800 border-blue-200' },
  CANCELED: { label: 'Cancelado', badgeClass: 'bg-stone-100 text-stone-600 border-stone-200' },
}

export function formatSchedulingDateTime(isoString: string): string {
  if (!isoString) return ''
  const date = new Date(isoString)
  return date.toLocaleDateString('pt-BR', {
    weekday: 'short',
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  }).replace(/\./g, '')
}

export function isPastScheduling(isoString: string): boolean {
  if (!isoString) return false
  return new Date(isoString).getTime() < Date.now()
}
