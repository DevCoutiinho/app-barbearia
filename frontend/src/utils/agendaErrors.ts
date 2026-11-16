import { isAxiosError } from 'axios'
import type { ApiResponse } from '../types/api'

export const BLOCK_CONFLICT_MESSAGE = 'Existe um agendamento neste horário. Remarque ou cancele o atendimento antes de bloquear.'

/** Mensagem amigável para falhas da agenda. Conflitos (409) usam a mensagem do servidor. */
export function agendaErrorMessage(cause: unknown, fallback: string, conflictFallback = fallback): string {
  if (isAxiosError(cause)) {
    if (!cause.response) return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
    const { status } = cause.response
    const body = cause.response.data as ApiResponse | undefined
    const message = body?.errors?.[0]?.message || body?.message
    if (status === 401) return 'Sua sessão expirou. Faça login novamente.'
    if (status === 403) return 'Você não tem permissão para acessar esta agenda.'
    if (status === 409) return message || conflictFallback
    if (status >= 500) return fallback
    return message || fallback
  }
  return cause instanceof Error && cause.message ? cause.message : fallback
}
