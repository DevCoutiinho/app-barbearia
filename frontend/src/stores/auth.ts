import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import authService, { type LoginResponse } from '../services/authService'
import type { LoginData, LoginDataGoogle } from '../schemas/auth.schema'
import type { ApiResponse } from '../types/api'

interface AuthUser {
  id: string
  name: string
  email: string
  roles: string[]
  permissions: string[]
  exp: number
}

const SESSION_KEY = 'accessToken'
const MAX_TIMEOUT = 2_147_483_647 // Limite máximo para setTimeout (32-bit int)


export const useAuthStore = defineStore('auth', () => {

  const accessToken = ref<string | null>(null)
  const user = ref<AuthUser | null>(null)
  const initialized = ref(false)
  let expirationTimer: ReturnType<typeof setTimeout> | undefined

  const isAuthenticated = computed(() => !!user.value)
  const firstName = computed(() => user.value?.name.trim().split(/\s+/)[0] ?? '')

  // ====================
  // Métodos Privados
  // ====================
  function clearSessionState() {
    clearTimeout(expirationTimer)
    accessToken.value = null
    user.value = null
  }

  function scheduleTokenExpiration() {
    clearTimeout(expirationTimer)
    
    if (!user.value) return

    const timeUntilExpiration = user.value.exp * 1000 - Date.now()
    
    if (timeUntilExpiration <= 0) {
      logout()
      return
    }

    expirationTimer = setTimeout(scheduleTokenExpiration, Math.min(timeUntilExpiration, MAX_TIMEOUT))
  }

  function applyTokenToSession(token: string, persist: boolean) {
    const decodedUser = jwtDecode<AuthUser>(token)
    
    console.log(JSON.stringify(decodedUser));
    

    if (persist) {
      localStorage.setItem(SESSION_KEY, token)
    }

    accessToken.value = token
    user.value = decodedUser
    scheduleTokenExpiration()
  }

  function handleLoginSuccess(response: ApiResponse<LoginResponse[]>) {
    const token = response.data?.[0]?.accessToken

    if (!token) {
      // Repassa a mensagem retornada pela API ou um fallback coerente caso a mensagem falte
      throw new Error(response.message || 'A API não retornou um token de acesso válido.')
    }

    applyTokenToSession(token, true)
    initialized.value = true
  }

  // ====================
  // Ações Públicas
  // ====================
  function restoreSession() {
    try {
      const token = localStorage.getItem(SESSION_KEY)
      
      if (token) {
        applyTokenToSession(token, false)
      } else {
        clearSessionState()
      }
    } catch {
      // Em caso de corrupção do localStorage ou token inválido
      clearSessionState()
      localStorage.removeItem(SESSION_KEY)
    } finally {
      initialized.value = true
    }
  }

  async function login(data: LoginData) {
    const response = await authService.login(data)
    handleLoginSuccess(response)
  }

  async function loginGoogle(data: LoginDataGoogle) {
    const response = await authService.loginGoogle(data)
    handleLoginSuccess(response)
  }

  async function refresh() {
    try {
      const response = await authService.refreshToken()
      handleLoginSuccess(response)
    } catch (error) {
      await logout()
      throw error // Mantém o repasse do erro gerado pela API
    }
  }

  async function logout() {
    try {
      if (accessToken.value) {
        await authService.logout()
      }
    } catch (error) {
      console.error('Falha ao deslogar pela API:', error)
    } finally {
      clearSessionState()
      localStorage.removeItem(SESSION_KEY)
    }
  }

  return {
    // Refs
    accessToken,
    user,
    initialized,
    
    // Computed
    isAuthenticated,
    firstName,
    
    // Actions
    login,
    loginGoogle,
    refresh,
    logout,
    restoreSession
  }
})
