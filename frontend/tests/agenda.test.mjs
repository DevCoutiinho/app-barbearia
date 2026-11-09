import assert from 'node:assert/strict'
import { registerHooks, stripTypeScriptTypes } from 'node:module'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

// Executa os módulos TypeScript sem adicionar dependências de teste.
registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('.') && context.parentURL?.endsWith('.ts') && !specifier.endsWith('.ts')) {
      return nextResolve(`${specifier}.ts`, context)
    }
    return nextResolve(specifier, context)
  },
  load(url, context, nextLoad) {
    if (url.endsWith('.ts')) {
      return { format: 'module', source: stripTypeScriptTypes(readFileSync(new URL(url), 'utf8')), shortCircuit: true }
    }
    return nextLoad(url, context)
  },
})

const {
  intervalsError, normalizeSchedule, pausesBetween, startOfWeek, toIsoDate, dayOfWeekOf,
  isFullDayBlock, blockSegmentOn, visibleHourRange, formatBlockPeriod,
} = await import('../src/utils/agenda.ts')
const { weeklyScheduleSchema, createBlockSchema } = await import('../src/schemas/agenda.schema.ts')
const { agendaErrorMessage, BLOCK_CONFLICT_MESSAGE } = await import('../src/utils/agendaErrors.ts')
const { AxiosError } = await import('axios')

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const editorDays = (overrides = {}) => DAYS.map(dayOfWeek => ({ dayOfWeek, enabled: false, intervals: [], ...overrides[dayOfWeek] }))

test('intervalos: término depois do início e sem sobreposição, como no servidor', () => {
  assert.equal(intervalsError([{ start: '09:00', end: '12:00' }, { start: '13:00', end: '18:00' }]), null)
  assert.equal(intervalsError([{ start: '09:00', end: '12:00' }, { start: '12:00', end: '18:00' }]), null)
  assert.match(intervalsError([{ start: '18:00', end: '09:00' }]), /término deve ser depois/)
  assert.match(intervalsError([{ start: '09:00', end: '09:00' }]), /término deve ser depois/)
  assert.match(intervalsError([{ start: '09:00', end: '13:00' }, { start: '12:00', end: '18:00' }]), /sobrepor/)
  assert.match(intervalsError([{ start: '', end: '12:00' }]), /Informe o início/)
  assert.match(intervalsError([{ start: '9:00', end: '12:00' }]), /HH:mm/)
})

test('grade da API é normalizada: 7 dias em ordem, HH:mm e intervalos ordenados', () => {
  const schedule = normalizeSchedule({ days: [
    { dayOfWeek: 'FRIDAY', intervals: [{ start: '13:00:00', end: '18:00:00' }, { start: '09:00:00', end: '12:00:00' }] },
  ] })
  assert.deepEqual(schedule.days.map(day => day.dayOfWeek), DAYS)
  assert.deepEqual(schedule.days[4].intervals, [{ start: '09:00', end: '12:00' }, { start: '13:00', end: '18:00' }])
  assert.deepEqual(pausesBetween(schedule.days[4].intervals), [{ start: '12:00', end: '13:00' }])
  assert.deepEqual(schedule.days[0].intervals, [])
})

test('editor da grade: folga zera intervalos e dia ativo exige horários válidos', () => {
  const valid = weeklyScheduleSchema.parse({ days: editorDays({
    MONDAY: { enabled: true, intervals: [{ start: '09:00', end: '12:00' }, { start: '13:00', end: '18:00' }] },
    TUESDAY: { enabled: false, intervals: [{ start: '09:00', end: '18:00' }] },
  }) })
  assert.equal(valid.days.length, 7)
  assert.deepEqual(valid.days[0].intervals, [{ start: '09:00', end: '12:00' }, { start: '13:00', end: '18:00' }])
  assert.deepEqual(valid.days[1].intervals, [])
  assert.equal('enabled' in valid.days[0], false)

  const inverted = weeklyScheduleSchema.safeParse({ days: editorDays({ MONDAY: { enabled: true, intervals: [{ start: '18:00', end: '09:00' }] } }) })
  assert.equal(inverted.success, false)
  assert.match(inverted.error.issues[0].message, /^Segunda-feira: O término deve ser depois/)
  assert.deepEqual(inverted.error.issues[0].path, ['days', 0])

  const empty = weeklyScheduleSchema.safeParse({ days: editorDays({ SUNDAY: { enabled: true, intervals: [] } }) })
  assert.match(empty.error.issues[0].message, /^Domingo: Adicione ao menos um horário/)
})

test('bloqueio: dia inteiro omite horários e período exige início antes do fim', () => {
  const schema = createBlockSchema('2026-11-10')
  const base = { date: '2026-11-20', fullDay: true, startTime: '', endTime: '', reason: 'HOLIDAY', description: '  Proclamação  ' }
  assert.deepEqual(schema.parse(base), { date: '2026-11-20', reason: 'HOLIDAY', description: 'Proclamação' })
  assert.deepEqual(
    schema.parse({ ...base, fullDay: false, startTime: '12:00', endTime: '13:30', reason: 'LUNCH', description: '' }),
    { date: '2026-11-20', reason: 'LUNCH', startTime: '12:00', endTime: '13:30' },
  )

  const errorFor = (input, field) => schema.safeParse({ ...base, ...input }).error?.issues.find(issue => issue.path[0] === field)?.message
  assert.match(errorFor({ date: '' }, 'date'), /Informe a data/)
  assert.match(errorFor({ date: '2026-11-09' }, 'date'), /já passou/)
  assert.match(errorFor({ date: '20/11/2026' }, 'date'), /data válida/)
  assert.match(errorFor({ reason: '' }, 'reason'), /Selecione o motivo/)
  assert.match(errorFor({ reason: 'VACATION' }, 'reason'), /motivo válido/)
  assert.match(errorFor({ fullDay: false, startTime: '', endTime: '13:00' }, 'startTime'), /horário inicial/)
  assert.match(errorFor({ fullDay: false, startTime: '14:00', endTime: '13:00' }, 'endTime'), /depois do inicial/)
  assert.match(errorFor({ fullDay: false, startTime: '14:00', endTime: '14:00' }, 'endTime'), /depois do inicial/)
  assert.match(errorFor({ description: 'a'.repeat(256) }, 'description'), /255 caracteres/)
  assert.equal(schema.safeParse({ ...base, date: '2026-11-10' }).success, true, 'hoje ainda pode ser bloqueado')
})

test('semana começa na segunda e datas não sofrem conversão de fuso', () => {
  assert.equal(toIsoDate(startOfWeek(new Date(2026, 10, 9))), '2026-11-09')  // segunda
  assert.equal(toIsoDate(startOfWeek(new Date(2026, 10, 15))), '2026-11-09') // domingo
  assert.equal(dayOfWeekOf(new Date(2026, 10, 15)), 'SUNDAY')
  assert.equal(dayOfWeekOf(new Date(2026, 10, 9)), 'MONDAY')
})

test('bloqueios da API: dia inteiro, recorte por data e rótulo do período', () => {
  const fullDay = { startsAt: '2026-11-20T00:00:00-03:00', endsAt: '2026-11-21T00:00:00-03:00' }
  const lunch = { startsAt: '2026-11-20T12:00:00-03:00', endsAt: '2026-11-20T13:30:00-03:00' }
  assert.equal(isFullDayBlock(fullDay), true)
  assert.equal(isFullDayBlock(lunch), false)
  assert.deepEqual(blockSegmentOn(fullDay, '2026-11-20'), { start: 0, end: 1440 })
  assert.equal(blockSegmentOn(fullDay, '2026-11-21'), null)
  assert.deepEqual(blockSegmentOn(lunch, '2026-11-20'), { start: 720, end: 810 })
  assert.equal(blockSegmentOn(lunch, '2026-11-19'), null)
  assert.match(formatBlockPeriod(fullDay), /20\/11 · Dia inteiro$/)
  assert.match(formatBlockPeriod(lunch), /20\/11 · 12:00 às 13:30$/)
})

test('grade visual cobre todo o expediente e usa 08–19 sem configuração', () => {
  assert.deepEqual(visibleHourRange(null), { start: 8, end: 19 })
  assert.deepEqual(visibleHourRange(normalizeSchedule({ days: [
    { dayOfWeek: 'MONDAY', intervals: [{ start: '07:30', end: '12:00' }] },
    { dayOfWeek: 'SATURDAY', intervals: [{ start: '13:00', end: '20:15' }] },
  ] })), { start: 7, end: 21 })
})

test('erros da API: conflito usa a mensagem do servidor e falha de rede é identificada', () => {
  const httpError = (status, data) => new AxiosError('erro', 'ERR', undefined, undefined, { status, data, statusText: '', headers: {}, config: {} })
  assert.equal(agendaErrorMessage(httpError(409, { message: BLOCK_CONFLICT_MESSAGE }), 'falhou'), BLOCK_CONFLICT_MESSAGE)
  assert.equal(agendaErrorMessage(httpError(409, {}), 'falhou', BLOCK_CONFLICT_MESSAGE), BLOCK_CONFLICT_MESSAGE)
  assert.equal(agendaErrorMessage(httpError(400, { message: 'Erro de validação', errors: [{ field: 'date', message: 'A data é obrigatória' }] }), 'falhou'), 'A data é obrigatória')
  assert.match(agendaErrorMessage(httpError(403, {}), 'falhou'), /permissão/)
  assert.equal(agendaErrorMessage(httpError(500, { message: 'stack interno' }), 'Não foi possível salvar.'), 'Não foi possível salvar.')
  assert.match(agendaErrorMessage(new AxiosError('Network Error', 'ERR_NETWORK'), 'falhou'), /conectar ao servidor/)
})
