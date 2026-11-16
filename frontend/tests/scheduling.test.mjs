import assert from 'node:assert/strict'
import { registerHooks, stripTypeScriptTypes } from 'node:module'
import { readFileSync } from 'node:fs'
import { test, describe, beforeEach } from 'node:test'

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

const { default: schedulingService } = await import('../src/services/schedulingService.ts')
const { default: api } = await import('../src/services/api.ts')

const barberId = '11111111-1111-1111-1111-111111111111'
const serviceId = '22222222-2222-2222-2222-222222222222'
const date = '2025-08-15'

const mockSlots = [
  { startTime: '09:00:00', endTime: '09:30:00' },
  { startTime: '09:30:00', endTime: '10:00:00' },
]

const mockSchedulings = [
  {
    id: 'aaaa-bbbb',
    serviceName: 'Corte',
    barberName: 'João',
    scheduledAt: '2025-08-15T12:00:00Z',
    durationTime: 30,
    finalPrice: 50.0,
    status: 'PENDING',
  },
]

// ─── Testes de contrato do schedulingService ───────────────────────────────

test('getAvailableSlots envia barberId, serviceId e date corretos', async t => {
  const requests = []
  t.mock.method(api, 'get', async (url, config) => {
    requests.push({ url, config })
    return { data: { status: 200, message: 'ok', data: mockSlots } }
  })

  const result = await schedulingService.getAvailableSlots(barberId, serviceId, date)
  assert.equal(requests[0].url, `/availability/barbers/${barberId}/slots`)
  assert.deepEqual(requests[0].config.params, { serviceId, date })
  assert.deepEqual(result.data, mockSlots)
})

test('getAvailableSlots retorna lista vazia quando não há horários', async t => {
  t.mock.method(api, 'get', async () => ({
    data: { status: 200, message: 'Nenhum horário', data: [] },
  }))
  const result = await schedulingService.getAvailableSlots(barberId, serviceId, date)
  assert.deepEqual(result.data, [])
})

test('create envia barberId, serviceId e scheduledAt como Instant ISO-8601', async t => {
  const requests = []
  t.mock.method(api, 'post', async (url, payload) => {
    requests.push({ url, payload })
    return { data: { status: 201, message: 'Agendamento criado com sucesso' } }
  })

  const instant = '2025-08-15T12:00:00.000Z'
  await schedulingService.create({ barberId, serviceId, scheduledAt: instant })

  assert.equal(requests[0].url, '/schedulings')
  assert.equal(requests[0].payload.barberId, barberId)
  assert.equal(requests[0].payload.serviceId, serviceId)
  assert.equal(requests[0].payload.scheduledAt, instant)
})

test('create não envia campo status — responsabilidade do backend', async t => {
  const requests = []
  t.mock.method(api, 'post', async (url, payload) => {
    requests.push({ url, payload })
    return { data: { status: 201, message: 'ok' } }
  })

  await schedulingService.create({
    barberId,
    serviceId,
    scheduledAt: '2025-08-15T12:00:00.000Z',
  })

  assert.equal(Object.hasOwn(requests[0].payload, 'status'), false,
    'O frontend não deve enviar o campo status')
})

test('getMySchedulings chama GET /schedulings/me', async t => {
  const requests = []
  t.mock.method(api, 'get', async url => {
    requests.push(url)
    return { data: { status: 200, message: 'ok', data: mockSchedulings } }
  })

  const result = await schedulingService.getMySchedulings()
  assert.equal(requests[0], '/schedulings/me')
  assert.deepEqual(result.data, mockSchedulings)
})

test('getMySchedulings retorna lista vazia quando cliente não tem agendamentos', async t => {
  t.mock.method(api, 'get', async () => ({
    data: { status: 200, message: 'ok', data: [] },
  }))
  const result = await schedulingService.getMySchedulings()
  assert.deepEqual(result.data, [])
})

// ─── Testes de tratamento de conflito ──────────────────────────────────────

test('create propaga erro HTTP 409 para tratamento de conflito no frontend', async t => {
  const conflictError = {
    response: {
      status: 409,
      data: {
        status: 409,
        message: 'Este horário acabou de ser reservado por outra pessoa. Por favor, escolha outro.',
        errors: [],
      },
    },
  }
  t.mock.method(api, 'post', async () => { throw conflictError })

  await assert.rejects(
    () => schedulingService.create({ barberId, serviceId, scheduledAt: '2025-08-15T12:00:00Z' }),
    err => {
      assert.equal(err.response.status, 409)
      assert.ok(err.response.data.message.includes('reservado'))
      return true
    },
  )
})

// ─── Testes de labels de status ────────────────────────────────────────────

test('SCHEDULING_STATUS_LABELS mapeia todos os status em português', async () => {
  const { SCHEDULING_STATUS_LABELS } = await import('../src/types/scheduling.ts')
  assert.equal(SCHEDULING_STATUS_LABELS['PENDING'], 'Pendente')
  assert.equal(SCHEDULING_STATUS_LABELS['SCHEDULED'], 'Agendado')
  assert.equal(SCHEDULING_STATUS_LABELS['COMPLETED'], 'Concluído')
  assert.equal(SCHEDULING_STATUS_LABELS['CANCELED'], 'Cancelado')
})

// ─── Testes de disponibilidade — invalidação ao mudar seleção ──────────────

test('getAvailableSlots é chamado com os parâmetros corretos ao mudar data', async t => {
  const calls = []
  t.mock.method(api, 'get', async (url, config) => {
    calls.push({ url, params: config?.params })
    return { data: { status: 200, message: 'ok', data: [] } }
  })

  const date1 = '2025-08-15'
  const date2 = '2025-08-16'

  await schedulingService.getAvailableSlots(barberId, serviceId, date1)
  await schedulingService.getAvailableSlots(barberId, serviceId, date2)

  assert.equal(calls[0].params.date, date1)
  assert.equal(calls[1].params.date, date2)
  assert.equal(calls.length, 2, 'Deve consultar novamente ao mudar a data')
})

test('getAvailableSlots é chamado com barbeiro diferente ao trocar barbeiro', async t => {
  const barberId2 = '33333333-3333-3333-3333-333333333333'
  const calls = []
  t.mock.method(api, 'get', async (url, config) => {
    calls.push({ url, params: config?.params })
    return { data: { status: 200, message: 'ok', data: [] } }
  })

  await schedulingService.getAvailableSlots(barberId, serviceId, date)
  await schedulingService.getAvailableSlots(barberId2, serviceId, date)

  assert.ok(calls[0].url.includes(barberId))
  assert.ok(calls[1].url.includes(barberId2))
})

// ─── Testes de endpoints ausentes ──────────────────────────────────────────

test('getCatalog chama /catalog/services — endpoint a ser implementado no backend', async t => {
  const requests = []
  t.mock.method(api, 'get', async url => {
    requests.push(url)
    return { data: { status: 200, message: 'ok', data: [] } }
  })
  await schedulingService.getCatalog()
  assert.equal(requests[0], '/catalog/services',
    'Endpoint de catálogo para clientes ainda não existe no backend')
})

test('getBarbers chama /barbers — endpoint a ser implementado no backend', async t => {
  const requests = []
  t.mock.method(api, 'get', async url => {
    requests.push(url)
    return { data: { status: 200, message: 'ok', data: [] } }
  })
  await schedulingService.getBarbers()
  assert.equal(requests[0], '/barbers',
    'Endpoint de listagem de barbeiros para clientes ainda não existe no backend')
})
