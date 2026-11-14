import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

type SessionUser = { roles?: string[]; permissions?: string[] } | null | undefined

export function stockAccess(user: SessionUser) {
  const permissions = user?.permissions ?? []
  const roles = user?.roles ?? []
  const isAdmin = roles.includes('ADMIN')
  const isBarber = roles.includes('BARBER') || roles.includes('BARBEIRO')

  const canView = isAdmin || isBarber || permissions.includes('INVENTORY_READ')
  const canCreate = isAdmin || permissions.includes('INVENTORY_CREATE')
  const canUpdate = isAdmin || permissions.includes('INVENTORY_UPDATE')
  const canManage = canCreate || canUpdate
  return { canView, canCreate, canUpdate, canManage }
}

export function useStockPermissions() {
  const auth = useAuthStore()
  const access = computed(() => stockAccess(auth.user))
  return {
    canView: computed(() => access.value.canView),
    canCreate: computed(() => access.value.canCreate),
    canUpdate: computed(() => access.value.canUpdate),
    canManage: computed(() => access.value.canManage),
  }
}
