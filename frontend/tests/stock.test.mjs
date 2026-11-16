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

const { stockStatus, sortByCriticality, stockLevelPercent, sortChronologically, countRecentExits, toDateInput, formatQuantity } = await import('../src/utils/stock.ts')
const { productSchema, createMovementSchema } = await import('../src/schemas/stock.schema.ts')
const { createMockStockApi, httpStockApi } = await import('../src/services/stockService.ts')
const { stockAccess } = await import('../src/composables/useStockPermissions.ts')
const { default: api } = await import('../src/services/api.ts')

const product = (name, quantity, minimum) => ({ id: name, name, category: 'Cabelo', unit: 'UN', quantity, minimum, updatedAt: '2026-10-01' })
const user = { id: 'u1', name: 'Admin' }

test('status do estoque: esgotado, baixo (abaixo do mínimo) e em dia', () => {
  assert.equal(stockStatus(product('a', 0, 5)), 'OUT')
  assert.equal(stockStatus(product('a', 0, 0)), 'OUT')
  assert.equal(stockStatus(product('a', 4.99, 5)), 'LOW')
  assert.equal(stockStatus(product('a', 5, 5)), 'OK')
  assert.equal(stockStatus(product('a', 1, 0)), 'OK')
})

test('lista ordena itens críticos primeiro e depois por nome', () => {
  const sorted = sortByCriticality([product('Zeta', 10, 1), product('Beta', 2, 5), product('Alfa', 0, 5), product('Alfa ok', 9, 1), product('Abacate', 1, 5)])
  assert.deepEqual(sorted.map(item => item.name), ['Alfa', 'Abacate', 'Beta', 'Alfa ok', 'Zeta'])
})

test('quantidades usam símbolos de unidade legíveis', () => {
  assert.equal(formatQuantity(2, 'L'), '2 L')
  assert.equal(formatQuantity(1500.5, 'ML'), '1.500,5 ml')
  assert.equal(formatQuantity(3, 'UN'), '3 un')
})

test('barra de progresso considera o dobro do mínimo como cheio', () => {
  assert.equal(stockLevelPercent(product('a', 0, 5)), 0)
  assert.equal(stockLevelPercent(product('a', 5, 5)), 50)
  assert.equal(stockLevelPercent(product('a', 50, 5)), 100)
  assert.equal(stockLevelPercent(product('a', 3, 0)), 100)
})

test('histórico em ordem cronológica e contagem de baixas nos últimos 7 dias', () => {
  const now = new Date('2026-10-27T12:00:00')
  const movement = (id, type, date, createdAt = `${date}T10:00:00Z`) => ({ id, type, date, createdAt })
  const movements = [
    movement('c', 'EXIT', '2026-10-27'), movement('a', 'ENTRY', '2026-10-10'),
    movement('b2', 'EXIT', '2026-10-21', '2026-10-21T11:00:00Z'), movement('b1', 'EXIT', '2026-10-21', '2026-10-21T09:00:00Z'),
    movement('d', 'EXIT', '2026-10-20'), movement('e', 'ENTRY', '2026-10-26'),
  ]
  assert.deepEqual(sortChronologically(movements).map(item => item.id), ['a', 'd', 'b1', 'b2', 'e', 'c'])
  // Janela de 7 dias inclui hoje e os 6 anteriores (21 a 27).
  assert.equal(countRecentExits(movements, 7, now), 3)
})

test('formulário de produto valida campos obrigatórios e normaliza números', () => {
  const valid = { name: '  Pomada  ', category: 'Cabelo', unit: 'UN', initialQuantity: '0', minimum: '2,5' }
  assert.deepEqual(productSchema.parse(valid), { name: 'Pomada', category: 'Cabelo', unit: 'UN', initialQuantity: 0, minimum: 2.5 })
  for (const [field, value] of [['name', ''], ['name', 'a'.repeat(101)], ['category', ''], ['category', 'Inexistente'], ['unit', 'KG'], ['initialQuantity', '-1'], ['minimum', ''], ['minimum', '1,234'], ['minimum', 'abc']]) {
    const result = productSchema.safeParse({ ...valid, [field]: value })
    assert.equal(result.success, false, `${field} não deve aceitar ${JSON.stringify(value)}`)
    assert.ok(result.error.issues.some(issue => issue.path[0] === field))
  }
})

test('baixa não pode ultrapassar o estoque atual nem usar data futura', () => {
  const today = '2026-10-27'
  const exit = createMovementSchema(10, today)
  assert.equal(exit.parse({ quantity: '10', date: today }).quantity, 10)
  assert.match(exit.safeParse({ quantity: '10,01', date: today }).error.issues[0].message, /não pode ultrapassar o estoque atual/)
  assert.equal(exit.safeParse({ quantity: '0', date: today }).success, false)
  assert.equal(exit.safeParse({ quantity: '1', date: '2026-10-28' }).success, false)
  assert.equal(exit.safeParse({ quantity: '1', date: '' }).success, false)

  const entry = createMovementSchema(undefined, today)
  const parsed = entry.parse({ quantity: '1000', date: '2026-10-01', note: '  ' })
  assert.equal(parsed.quantity, 1000)
  assert.equal(parsed.note, undefined)
  assert.equal(entry.parse({ quantity: '1', date: today, note: ' compra ' }).note, 'compra')
})

test('RBAC: admin gerencia, barbeiro e INVENTORY_READ apenas leem, cliente não acessa', () => {
  assert.deepEqual(stockAccess({ roles: ['ADMIN'], permissions: [] }), { canView: true, canManage: true })
  // Mesmo com INVENTORY_UPDATE vindo do backend, o barbeiro fica somente leitura.
  assert.deepEqual(stockAccess({ roles: ['BARBER'], permissions: ['INVENTORY_READ', 'INVENTORY_UPDATE'] }), { canView: true, canManage: false })
  assert.deepEqual(stockAccess({ roles: ['CLIENT'], permissions: ['INVENTORY_READ'] }), { canView: true, canManage: false })
  assert.deepEqual(stockAccess({ roles: ['CLIENT'], permissions: [] }), { canView: false, canManage: false })
  assert.deepEqual(stockAccess(null), { canView: false, canManage: false })
})

test('mock: entrada, baixa com responsável, bloqueio de estoque insuficiente e exclusão', async () => {
  const stock = createMockStockApi(0)
  const created = await stock.createProduct({ name: 'Gel', category: 'Cabelo', unit: 'G', initialQuantity: 0, minimum: 100 })
  await assert.rejects(stock.createProduct({ name: 'gel', category: 'Cabelo', unit: 'G', initialQuantity: 0, minimum: 1 }), /Já existe/)

  const date = toDateInput()
  await stock.registerEntry(created.id, { quantity: 250, date, note: 'Compra', user })
  await stock.registerExit(created.id, { quantity: 50.5, date, user })
  await assert.rejects(stock.registerExit(created.id, { quantity: 1000, date, user }), /Estoque insuficiente/)

  const saved = (await stock.listProducts()).find(item => item.id === created.id)
  assert.equal(saved.quantity, 199.5)
  const history = await stock.listMovements(created.id)
  assert.deepEqual(history.map(item => [item.type, item.quantity, item.user.name]), [['ENTRY', 250, 'Admin'], ['EXIT', 50.5, 'Admin']])
  assert.equal(history[0].note, 'Compra')

  await stock.deleteProduct(created.id)
  assert.equal((await stock.listProducts()).some(item => item.id === created.id), false)
  await assert.rejects(stock.listMovements(created.id), /não encontrado/)
})

test('mock inicial tem quantidades coerentes com o histórico de movimentações', async () => {
  const stock = createMockStockApi(0)
  for (const item of await stock.listProducts()) {
    const total = (await stock.listMovements(item.id)).reduce((sum, movement) => sum + (movement.type === 'ENTRY' ? movement.quantity : -movement.quantity), 0)
    assert.equal(total, item.quantity, item.name)
  }
})

test('adapter HTTP segue o contrato e aceita corpo puro ou envelope ApiResponse', async t => {
  const calls = []
  const reply = data => async (url, body) => { calls.push({ url, body }); return { data } }
  t.mock.method(api, 'get', reply({ status: 200, message: 'ok', data: [product('a', 1, 1)] }))
  t.mock.method(api, 'post', reply({ id: 'm1' }))
  t.mock.method(api, 'delete', reply(undefined))

  assert.equal((await httpStockApi.listProducts())[0].name, 'a')
  assert.deepEqual(await httpStockApi.registerEntry('p/1', { quantity: 1, date: '2026-10-27', user }), { id: 'm1' })
  await httpStockApi.registerExit('p1', { quantity: 1, date: '2026-10-27', user })
  await httpStockApi.createProduct({ name: 'x', category: 'Cabelo', unit: 'UN', initialQuantity: 0, minimum: 0 })
  await httpStockApi.deleteProduct('p1')
  await httpStockApi.listMovements('p1')

  assert.deepEqual(calls.map(call => call.url), ['/products', '/products/p%2F1/entries', '/products/p1/exits', '/products', '/products/p1', '/products/p1/movements'])
  assert.deepEqual(calls[1].body.user, user)
})
