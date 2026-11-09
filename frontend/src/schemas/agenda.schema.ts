import * as z from 'zod'
import { BLOCK_REASON_VALUES, DAY_LABELS, DAYS_OF_WEEK, type ScheduleBlockPayload, type WeeklySchedule } from '../types/agenda'
import { intervalsError, isValidTime, toMinutes } from '../utils/agenda'

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/

/* ── Grade semanal ── */

const timeRangeInput = z.object({ start: z.string(), end: z.string() })

export const weeklyScheduleSchema = z.object({
  days: z.array(z.object({
    dayOfWeek: z.enum(DAYS_OF_WEEK),
    enabled: z.boolean(),
    intervals: z.array(timeRangeInput),
  })).length(7),
}).superRefine((value, context) => {
  value.days.forEach((day, index) => {
    if (!day.enabled) return
    const label = DAY_LABELS[day.dayOfWeek].long
    const message = day.intervals.length ? intervalsError(day.intervals) : 'Adicione ao menos um horário ou marque o dia como folga.'
    if (message) context.addIssue({ code: z.ZodIssueCode.custom, path: ['days', index], message: `${label}: ${message}` })
  })
}).transform((value): WeeklySchedule => ({
  days: value.days.map(day => ({
    dayOfWeek: day.dayOfWeek,
    intervals: day.enabled ? day.intervals.map(range => ({ start: range.start, end: range.end })) : [],
  })),
}))

export type WeeklyScheduleInput = z.input<typeof weeklyScheduleSchema>

/* ── Bloqueio pontual ── */

/** `today` (YYYY-MM-DD) impede bloquear datas passadas. */
export function createBlockSchema(today: string) {
  return z.object({
    date: z.string({ required_error: 'Informe a data.' }).trim(),
    fullDay: z.boolean(),
    startTime: z.string().trim(),
    endTime: z.string().trim(),
    reason: z.string({ required_error: 'Selecione o motivo.' }).trim(),
    description: z.string().trim().max(255, 'A descrição deve ter no máximo 255 caracteres.'),
  }).superRefine((value, context) => {
    const issue = (path: string, message: string) => context.addIssue({ code: z.ZodIssueCode.custom, path: [path], message })

    if (!value.date) issue('date', 'Informe a data.')
    else if (!ISO_DATE.test(value.date) || Number.isNaN(Date.parse(value.date))) issue('date', 'Informe uma data válida.')
    else if (value.date < today) issue('date', 'Não é possível bloquear uma data que já passou.')

    if (!value.reason) issue('reason', 'Selecione o motivo.')
    else if (!(BLOCK_REASON_VALUES as readonly string[]).includes(value.reason)) issue('reason', 'Selecione um motivo válido.')

    if (value.fullDay) return
    if (!value.startTime) issue('startTime', 'Informe o horário inicial.')
    else if (!isValidTime(value.startTime)) issue('startTime', 'Use o formato HH:mm.')
    if (!value.endTime) issue('endTime', 'Informe o horário final.')
    else if (!isValidTime(value.endTime)) issue('endTime', 'Use o formato HH:mm.')
    else if (isValidTime(value.startTime) && toMinutes(value.endTime) <= toMinutes(value.startTime)) {
      issue('endTime', 'O horário final deve ser depois do inicial.')
    }
  }).transform((value): ScheduleBlockPayload => ({
    date: value.date,
    reason: value.reason as ScheduleBlockPayload['reason'],
    ...(value.fullDay ? {} : { startTime: value.startTime, endTime: value.endTime }),
    ...(value.description ? { description: value.description } : {}),
  }))
}

export type BlockInput = z.input<ReturnType<typeof createBlockSchema>>
export type BlockData = z.output<ReturnType<typeof createBlockSchema>>
