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

const { createServiceSchema, updateServiceSchema } = await import('../src/schemas/service.schema.ts')
const { default: serviceService } = await import('../src/services/serviceService.ts')
const { default: api } = await import('../src/services/api.ts')

const categoryId = '550e8400-e29b-41d4-a716-446655440000'
const serviceId = '3fa85f64-5717-4562-b3fc-2c963f66afa6'
const validInput = {
  name: 'Corte',
  technique: 'MACHINE',
  price: '50.00',
  active: true,
  durationTime: '30',
  categoryId,
}

function rejectField(schema, field, value) {
  const result = schema.safeParse({ ...validInput, [field]: value })
  assert.equal(result.success, false, `${field} não deve aceitar ${JSON.stringify(value)}`)
  const issue = result.error.issues.find(issue => issue.path[0] === field)
  assert.ok(issue, `Deve haver um erro no campo ${field}`)
  return issue.message
}

test('formulários rejeitam nome, preço e duração inválidos antes do envio', () => {
  for (const schema of [createServiceSchema, updateServiceSchema]) {
    for (const value of ['', '   ', 'a'.repeat(256)]) rejectField(schema, 'name', value)

    assert.equal(rejectField(schema, 'price', ''), 'Informe o preço do serviço.')
    for (const value of ['0', '-1', '-0.01']) {
      assert.equal(rejectField(schema, 'price', value), 'O preço deve ser maior que zero.')
    }
    assert.equal(rejectField(schema, 'price', '10000'), 'O preço máximo permitido é R$ 9.999,99.')
    for (const value of ['50.001', '50,001', 'R$ 50,00', 'abc']) rejectField(schema, 'price', value)

    assert.equal(rejectField(schema, 'durationTime', ''), 'Informe a duração do serviço.')
    for (const value of ['0', '-1', '1.5', '1,5', 'abc', '2147483648']) {
      rejectField(schema, 'durationTime', value)
    }
    rejectField(schema, 'technique', '')
    rejectField(schema, 'technique', 'Máquina')
    rejectField(schema, 'categoryId', 'categoria-inválida')
  }
  rejectField(createServiceSchema, 'categoryId', '')
  rejectField(createServiceSchema, 'categoryId', undefined)
})

test('formulários normalizam valores válidos e preservam o contrato numérico', () => {
  for (const schema of [createServiceSchema, updateServiceSchema]) {
    for (const [input, expected] of [['0,01', 0.01], ['9999.99', 9999.99], [' 50,20 ', 50.2]]) {
      const parsed = schema.parse({ ...validInput, name: '  Corte  ', price: input, durationTime: ' 30 ', active: false })
      assert.equal(parsed.name, 'Corte')
      assert.equal(parsed.price, expected)
      assert.equal(parsed.durationTime, 30)
      assert.equal(parsed.active, false)
      assert.equal(parsed.categoryId, categoryId)
    }
    assert.equal(schema.parse({ ...validInput, name: 'a'.repeat(255), durationTime: '1' }).durationTime, 1)
    for (const technique of ['MACHINE', 'SCISSORS', 'RAZOR', 'MIXED', 'TWEEZERS', 'WAX']) {
      assert.equal(schema.parse({ ...validInput, technique }).technique, technique)
    }
  }
  for (const categoryId of ['', undefined]) {
    assert.equal(updateServiceSchema.parse({ ...validInput, categoryId }).categoryId, undefined)
  }
})

test('listagem usa params, preserva inativos e retorna a paginação do backend', async t => {
  const body = {
    status: 200,
    message: 'Serviços retornados com sucesso',
    data: [{ content: [], page: { size: 20, number: 2, totalElements: 45, totalPages: 3 } }],
  }
  const requests = []
  t.mock.method(api, 'get', async (url, config) => {
    requests.push({ url, config })
    return { data: body }
  })

  const result = await serviceService.list({
    page: 2,
    size: 20,
    sort: ['name,asc', 'technique,asc'],
    filters: { name: '  Corte  ', technique: 'MACHINE', active: false, categoryId, minPrice: 0, maxPrice: 100 },
  })
  assert.equal(result, body)
  assert.equal(requests[0].url, '/barber-services')
  assert.deepEqual(requests[0].config.params, {
    page: 2,
    size: 20,
    name: 'Corte',
    technique: 'MACHINE',
    active: false,
    categoryId,
    minPrice: 0,
    maxPrice: 100,
    sort: ['name,asc', 'technique,asc'],
  })

  await serviceService.list()
  assert.deepEqual(requests[1].config.params, { page: 0, size: 10 })
  await serviceService.list({ filters: { name: '   ' } })
  assert.deepEqual(requests[2].config.params, { page: 0, size: 10 })
})

async function inspectMultipart(formData, config) {
  assert.ok(formData instanceof FormData)
  const data = formData.get('data')
  assert.ok(data instanceof Blob)
  assert.equal(data.type, 'application/json')
  assert.equal(Object.hasOwn(config.headers, 'Content-Type'), true)
  assert.equal(config.headers['Content-Type'], undefined)
  assert.equal(config.headers.Authorization, undefined)
  return JSON.parse(await data.text())
}

test('cadastro envia JSON multipart numérico, com imagem opcional somente no cadastro', async t => {
  const payload = createServiceSchema.parse(validInput)
  const body = { status: 200, message: 'Serviço criado com sucesso', data: null }
  const requests = []
  t.mock.method(api, 'post', async (url, formData, config) => {
    requests.push({ url, formData, payload: await inspectMultipart(formData, config) })
    return { data: body }
  })

  assert.equal(await serviceService.create(payload), body)
  assert.equal(requests[0].url, '/barber-services')
  assert.deepEqual(requests[0].payload, payload)
  assert.deepEqual([...requests[0].formData.keys()], ['data'])

  const image = new File(['imagem de teste'], 'servico.png', { type: 'image/png' })
  await serviceService.create(payload, image)
  assert.equal(requests[1].formData.get('image').name, 'servico.png')
  assert.equal(await requests[1].formData.get('image').text(), 'imagem de teste')
  assert.deepEqual(requests[1].payload, payload)
})

test('edição e mudança de status enviam os dados completos sem excluir ou trocar imagem', async t => {
  const payload = createServiceSchema.parse(validInput)
  const body = { status: 200, message: 'Serviço atualizado com sucesso', data: null }
  const requests = []
  t.mock.method(api, 'put', async (url, formData, config) => {
    requests.push({ url, formData, payload: await inspectMultipart(formData, config) })
    return { data: body }
  })
  t.mock.method(api, 'delete', async () => assert.fail('A administração não deve excluir fisicamente serviços'))
  assert.equal(await serviceService.update(serviceId, payload), body)
  assert.equal(requests[0].url, `/barber-services/${serviceId}`)
  assert.deepEqual(requests[0].payload, payload)
  assert.deepEqual([...requests[0].formData.keys()], ['data'])

  const service = {
    ...payload,
    id: serviceId,
    category: { id: categoryId, name: 'Cortes' },
    images: [{ id: 'imagem-teste', url: 'imagem.png' }],
  }
  await serviceService.changeStatus(service, false)
  assert.deepEqual(requests[1].payload, { ...payload, active: false })
  assert.equal(service.active, true)
  await serviceService.changeStatus({ ...service, active: false }, true)
  assert.deepEqual(requests[2].payload, payload)
  assert.deepEqual([...requests[2].formData.keys()], ['data'])

  const { categoryId: omittedCategory, ...withoutCategory } = payload
  await serviceService.update(serviceId, withoutCategory)
  assert.equal(Object.hasOwn(requests[3].payload, 'categoryId'), false)
  assert.deepEqual(requests[3].payload, withoutCategory)
})

test('consulta de categorias preserva as categorias inativas retornadas pelo backend', async t => {
  const body = {
    status: 200,
    message: 'Categorias retornadas com sucesso',
    data: [{ id: categoryId, name: 'Cortes', description: null, active: false }],
  }
  t.mock.method(api, 'get', async url => {
    assert.equal(url, '/service-categories')
    return { data: body }
  })
  assert.equal(await serviceService.listCategories(), body)
})
