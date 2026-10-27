import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

type SessionUser = { roles?: string[]; permissions?: string[] } | null | undefined

/** Admin gerencia o estoque; barbeiro (ou quem tiver INVENTORY_READ) apenas consulta. */
export function stockAccess(user: SessionUser) {
  const roles = user?.roles ?? []
  const isAdmin = roles.includes('ADMIN')
  const isBarber = roles.includes('BARBER') || roles.includes('BARBEIRO')
  const canView = isAdmin || isBarber || (user?.permissions?.includes('INVENTORY_READ') ?? false)
  return { canView, canManage: isAdmin }
}

export function useStockPermissions() {
  const auth = useAuthStore()
  const access = computed(() => stockAccess(auth.user))
  return {
    canView: computed(() => access.value.canView),
    canManage: computed(() => access.value.canManage),
  }
}
