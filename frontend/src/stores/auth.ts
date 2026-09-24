import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import authService, { type LoginResponse } from '../services/authService'
import type { LoginData, LoginDataGoogle } from '../schemas/auth.schema'
import type { ApiResponse } from '../types/api'

interface AuthUser {
  id: string
  name: string
  email: string
  permissions: string[]
  exp: number
}

const sessionKey = 'barbershop.accessToken'

// Claims control presentation only. The API validates signatures and permissions.
function readUser(token: string): AuthUser {
  const payload = token.split('.')[1]
  if (!payload) throw new Error('Token inválido')
  const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
  const bytes = Uint8Array.from(atob(base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')), char => char.charCodeAt(0))
  const user = JSON.parse(new TextDecoder().decode(bytes))
  if (typeof user.id !== 'string' || typeof user.name !== 'string' || !user.name.trim()
    || typeof user.email !== 'string' || !Array.isArray(user.permissions)
    || !user.permissions.every((permission: unknown) => typeof permission === 'string')
    || typeof user.exp !== 'number' || !Number.isFinite(user.exp) || user.exp * 1000 <= Date.now()) {
    throw new Error('Sessão inválida ou expirada')
  }
  return user
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)
  const initialized = ref(false)
  let expirationTimer: ReturnType<typeof setTimeout> | undefined
  const isAuthenticated = computed(() => user.value !== null)
  const firstName = computed(() => user.value?.name.trim().split(/\s+/)[0] ?? '')
  const role = computed(() => ['ADMIN', 'BARBER', 'USER'].find(value => user.value?.permissions.includes(value)))

  function clearSession() {
    clearTimeout(expirationTimer)
    accessToken.value = null
    user.value = null
  }

  function logout() {
    clearSession()
    localStorage.removeItem(sessionKey)
  }

  function scheduleExpiration() {
    clearTimeout(expirationTimer)
    if (!user.value) return
    const remaining = user.value.exp * 1000 - Date.now()
    if (remaining <= 0) logout()
    else expirationTimer = setTimeout(scheduleExpiration, Math.min(remaining, 2_147_483_647))
  }

  function setSession(token: string, persist: boolean) {
    const authenticatedUser = readUser(token)
    if (persist) localStorage.setItem(sessionKey, token)
    accessToken.value = token
    user.value = authenticatedUser
    scheduleExpiration()
  }

  function restoreSession() {
    try {
      const token = localStorage.getItem(sessionKey)
      if (token) setSession(token, false)
      else clearSession()
    } catch {
      clearSession()
      try { localStorage.removeItem(sessionKey) } catch { /* Storage may be unavailable. */ }
    } finally {
      initialized.value = true
    }
  }

  function acceptLogin(response: ApiResponse<LoginResponse[]>) {
    const token = response.data?.[0]?.acessToken
    if (!token) throw new Error('Resposta de autenticação inválida')
    setSession(token, true)
    initialized.value = true
  }

  async function login(data: LoginData) {
    acceptLogin(await authService.login(data))
  }

  async function loginGoogle(data: LoginDataGoogle) {
    acceptLogin(await authService.loginGoogle(data))
  }

  return { accessToken, user, initialized, isAuthenticated, firstName, role, login, loginGoogle, logout, restoreSession }
})
