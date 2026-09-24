import assert from 'node:assert/strict'
import { registerHooks, stripTypeScriptTypes } from 'node:module'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { createPinia, setActivePinia } from 'pinia'

// Load the existing TypeScript store without adding a test dependency.
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

const { useAuthStore } = await import('../src/stores/auth.ts')
const { default: api, setupAuthInterceptors } = await import('../src/services/api.ts')
const key = 'barbershop.accessToken'
const storage = new Map()
globalThis.localStorage = {
  getItem: key => storage.get(key) ?? null,
  setItem: (key, value) => storage.set(key, value),
  removeItem: key => storage.delete(key),
}

// Synthetic tokens are isolated test fixtures, never used by the application.
function token(overrides = {}) {
  const claims = { id: 'test-id', name: 'João Teste', email: 'test@example.invalid', permissions: ['USER'], exp: Math.floor(Date.now() / 1000) + 3600, ...overrides }
  return `test.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.test`
}

test('authentication state, API contract, restoration and logout', async t => {
  const originalAdapter = api.defaults.adapter
  t.after(() => { api.defaults.adapter = originalAdapter })
  setActivePinia(createPinia())
  const auth = useAuthStore()
  t.after(() => auth.logout())
  auth.restoreSession()
  assert.equal(auth.initialized, true)
  assert.equal(auth.isAuthenticated, false)

  for (const method of ['login', 'loginGoogle']) {
    const accessToken = token()
    api.defaults.adapter = async config => {
      assert.equal(config.url, method === 'login' ? '/auth/login' : '/auth/login/google')
      return { data: { status: 200, data: [{ acessToken: accessToken }] }, status: 200, statusText: 'OK', headers: {}, config }
    }
    await auth[method](method === 'login' ? { email: 'test@example.invalid', password: 'test-only' } : { idToken: 'test-only' })
    assert.equal(auth.firstName, 'João')
    assert.equal(auth.isAuthenticated, true)
    assert.equal(storage.get(key), accessToken)
    auth.logout()
    assert.equal(auth.user, null)
    assert.equal(auth.accessToken, null)
    assert.equal(storage.has(key), false)
  }

  storage.set(key, token({ permissions: ['ADMIN'] }))
  auth.restoreSession()
  assert.equal(auth.role, 'ADMIN')
  assert.equal(auth.firstName, 'João')

  for (const invalid of ['invalid', token({ exp: 1 }), token({ name: null })]) {
    storage.set(key, invalid)
    auth.restoreSession()
    assert.equal(auth.isAuthenticated, false)
    assert.equal(storage.has(key), false)
  }

  api.defaults.adapter = async config => ({ data: { status: 200, data: [] }, status: 200, statusText: 'OK', headers: {}, config })
  await assert.rejects(auth.login({ email: 'test@example.invalid', password: 'test-only' }))
  assert.equal(auth.isAuthenticated, false)
})

test('HTTP authentication headers, stale responses and interceptor cleanup', async t => {
  const originalAdapter = api.defaults.adapter
  let logoutCount = 0
  const auth = { accessToken: 'first-token', logout() { logoutCount++ } }
  const removeInterceptors = setupAuthInterceptors(auth)
  t.after(() => {
    removeInterceptors()
    api.defaults.adapter = originalAdapter
  })

  api.defaults.adapter = async config => ({ data: null, status: 200, statusText: 'OK', headers: {}, config })
  assert.equal((await api.get('/appointments')).config.headers.Authorization, 'Bearer first-token')
  for (const url of ['/auth/login', '/auth/login/google', '/auth/register']) {
    assert.equal((await api.post(url)).config.headers.Authorization, undefined)
  }
  auth.accessToken = null
  assert.equal((await api.get('/appointments')).config.headers.Authorization, undefined)

  auth.accessToken = 'first-token'
  const staleError = { response: { status: 401 } }
  api.defaults.adapter = async config => {
    auth.accessToken = 'second-token'
    staleError.config = config
    throw staleError
  }
  await assert.rejects(api.get('/appointments'), error => error === staleError)
  assert.equal(logoutCount, 0)

  let status = 500
  api.defaults.adapter = async config => { throw { response: { status }, config } }
  await assert.rejects(api.get('/appointments'))
  assert.equal(logoutCount, 0)
  status = 401
  await assert.rejects(api.post('/auth/login'))
  assert.equal(logoutCount, 0)
  await assert.rejects(api.get('/appointments'))
  assert.equal(logoutCount, 1)

  removeInterceptors()
  // A subsequent mount must register only one active pair of interceptors.
  const removeRemountedInterceptors = setupAuthInterceptors(auth)
  t.after(removeRemountedInterceptors)
  await assert.rejects(api.get('/appointments'))
  assert.equal(logoutCount, 2)
  removeRemountedInterceptors()
  await assert.rejects(api.get('/appointments', { headers: { Authorization: 'Bearer second-token' } }))
  assert.equal(logoutCount, 2)
  api.defaults.adapter = async config => ({ data: null, status: 200, statusText: 'OK', headers: {}, config })
  assert.equal((await api.get('/appointments')).config.headers.Authorization, undefined)
})
