import { isAxiosError } from 'axios'
import type { ApiResponse } from '../types/api'

/** Mensagem amigável para falhas da camada de estoque (HTTP ou mock). */
export function stockErrorMessage(cause: unknown, fallback: string): string {
  if (isAxiosError(cause)) {
    if (cause.response?.status === 403) return 'Você não tem permissão para essa ação no estoque.'
    if (cause.response?.status === 401) return 'Sua sessão expirou. Faça login novamente.'
    if (!cause.response) return 'Não foi possível conectar ao servidor. Tente novamente.'
    const body = cause.response.data as ApiResponse | undefined
    return body?.errors?.[0]?.message || body?.message || fallback
  }
  return cause instanceof Error && cause.message ? cause.message : fallback
}
