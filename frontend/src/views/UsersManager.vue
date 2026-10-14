<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight, Pencil, RefreshCw, Search, Trash2, Users, X } from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.vue'
import userService from '../services/userService'
import roleService from '../services/roleService'
import { useAuthStore } from '../stores/auth'
import { handleApiError } from '../utils/errorHandler'
import type { PageMetadata, User, UserFilters } from '../types/user'
import type { Role } from '../types/role'

const auth = useAuthStore()
const canReadUsers = computed(() => auth.user?.permissions?.includes('USER_READ') ?? false)
const canUpdateUsers = computed(() => auth.user?.permissions?.includes('USER_UPDATE') ?? false)
const canReadRoles = computed(() => auth.user?.permissions?.includes('ROLE_READ') ?? false)
const canDeleteUsers = computed(() => auth.user?.permissions?.includes('USER_DELETE') ?? false)
// O JWT atual usa sub; a store já mantém esse campo no objeto decodificado.
const currentUserId = computed(() => {
  const user = auth.user
  if (user && 'sub' in user && typeof user.sub === 'string') return user.sub
  return user?.id || null
})
const userToDelete = ref<User | null>(null)
const deleting = ref(false)
const deleteModal = ref<HTMLDivElement | null>(null)
let deleteTrigger: HTMLElement | null = null
const roleDialog = ref<HTMLDialogElement | null>(null)
const editingUser = ref<User | null>(null)
const availableRoles = ref<Role[]>([])
const selectedRoleIds = ref<string[]>([])
const loadingRoles = ref(false)
const savingRoles = ref(false)
const roleError = ref('')
let roleRequest = 0
const users = ref<User[]>([])
const loading = ref(false)
const hasLoaded = ref(false)
const error = ref('')
const nameFilter = ref('')
const emailFilter = ref('')
const activeFilter = ref<'' | 'true' | 'false'>('')
const appliedFilters = ref<UserFilters>({})
const pageSize = ref(10)
const requestedPage = ref(0)
const page = ref<PageMetadata>({ size: 10, number: 0, totalElements: 0, totalPages: 0 })
const failedAvatars = ref(new Set<string>())
const pageSizes = [10, 20, 50]
const busy = computed(() => loading.value || savingRoles.value || deleting.value)
const rolesChanged = computed(() => {
  const previous = editingUser.value?.roles.map(role => role.id).sort() ?? []
  const selected = [...selectedRoleIds.value].sort()
  return previous.length !== selected.length || previous.some((id, index) => id !== selected[index])
})

const hasAppliedFilters = computed(() => Boolean(
  appliedFilters.value.name || appliedFilters.value.email || appliedFilters.value.active !== undefined,
))
const firstItem = computed(() => users.value.length ? page.value.number * page.value.size + 1 : 0)
const lastItem = computed(() => users.value.length ? firstItem.value + users.value.length - 1 : 0)

function initials(name: string): string {
  return name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toLocaleUpperCase() || '?'
}

// A autenticação permanece na instância api utilizada pelo serviço.
async function loadUsers(targetPage = requestedPage.value) {
  if (loading.value || !canReadUsers.value) return

  requestedPage.value = targetPage
  loading.value = true
  error.value = ''

  try {
    const query = {
      size: pageSize.value,
      filters: { ...appliedFilters.value },
    }
    const fetchPage = async (pageNumber: number) => {
      const response = await userService.list({ ...query, page: pageNumber })
      const result = response.data?.[0]

      if (!result || !Array.isArray(result.content) || !result.page) {
        throw new Error('Resposta de listagem inválida.')
      }

      return result
    }
    let result = await fetchPage(targetPage)

    // Recupera uma página válida se a quantidade de usuários diminuir no servidor.
    const lastPage = Math.max(result.page.totalPages - 1, 0)
    if (result.page.number > lastPage) {
      requestedPage.value = lastPage
      result = await fetchPage(lastPage)
    }

    users.value = result.content
    page.value = result.page
    requestedPage.value = result.page.number
    failedAvatars.value.clear()
    hasLoaded.value = true
  } catch (cause) {
    error.value = 'Não foi possível carregar os usuários. Tente novamente.'

    if (isAxiosError(cause)) {
      if (cause.response?.status === 403) {
        error.value = 'Você não tem permissão para consultar os usuários.'
      } else if (cause.response?.status === 401) {
        error.value = 'Sua sessão não está disponível. Faça login novamente.'
      } else if (!cause.response) {
        error.value = 'Não foi possível conectar ao servidor. Verifique se ele está disponível.'
      }
    }

    handleApiError(cause, undefined, error.value)
  } finally {
    loading.value = false
  }
}

// A paginação usa os filtros aplicados, sem considerar uma busca ainda em edição.
async function applyFilters() {
  if (busy.value) return

  const filters: UserFilters = {}
  const name = nameFilter.value.trim()
  const email = emailFilter.value.trim()
  if (name) filters.name = name
  if (email) filters.email = email
  if (activeFilter.value !== '') filters.active = activeFilter.value === 'true'

  appliedFilters.value = filters
  await loadUsers(0)
}

async function clearFilters() {
  if (busy.value) return

  nameFilter.value = ''
  emailFilter.value = ''
  activeFilter.value = ''
  appliedFilters.value = {}
  await loadUsers(0)
}

async function changePage(targetPage: number) {
  if (busy.value || targetPage < 0 || targetPage >= page.value.totalPages) return
  await loadUsers(targetPage)
}

// Alterar o tamanho reinicia a consulta na página zero.
async function changePageSize(event: Event) {
  if (busy.value) return

  const size = Number((event.target as HTMLSelectElement).value)
  if (!pageSizes.includes(size)) return
  pageSize.value = size
  await loadUsers(0)
}

// O catálogo só é consultado ao abrir a edição, usando o serviço já existente.
async function loadAvailableRoles() {
  if (!editingUser.value || loadingRoles.value || !canUpdateUsers.value || !canReadRoles.value) return
  const request = ++roleRequest
  loadingRoles.value = true
  roleError.value = ''

  try {
    const response = await roleService.list()
    if (request !== roleRequest) return
    if (!Array.isArray(response.data)) throw new Error('Resposta de roles inválida.')
    availableRoles.value = response.data
    const ids = new Set(response.data.map(role => role.id))
    if (selectedRoleIds.value.some(id => !ids.has(id))) {
      roleError.value = 'Os papéis deste usuário mudaram. Feche a edição e atualize a listagem.'
    }
  } catch (cause) {
    if (request !== roleRequest) return
    roleError.value = 'Não foi possível carregar os papéis disponíveis. Tente novamente.'
    handleApiError(cause, undefined, roleError.value)
  } finally {
    if (request === roleRequest) loadingRoles.value = false
  }
}

async function openRoleEditor(user: User) {
  if (busy.value || editingUser.value || userToDelete.value || !canUpdateUsers.value || !canReadRoles.value) return
  editingUser.value = user
  selectedRoleIds.value = user.roles.map(role => role.id)
  availableRoles.value = []
  roleError.value = ''
  roleDialog.value?.showModal()
  await loadAvailableRoles()
}

function closeRoleEditor() {
  if (savingRoles.value) return
  ++roleRequest
  loadingRoles.value = false
  roleDialog.value?.close()
  editingUser.value = null
}

// Envia somente roleIds; o endpoint substitui todos os papéis do usuário.
async function saveRoles() {
  const user = editingUser.value
  if (!user || busy.value || loadingRoles.value || roleError.value || !rolesChanged.value
    || !canUpdateUsers.value || !canReadRoles.value) return

  const ids = new Set(availableRoles.value.map(role => role.id))
  if (selectedRoleIds.value.some(id => !ids.has(id))) return
  savingRoles.value = true

  try {
    await userService.updateRoles(user.id, { roleIds: [...selectedRoleIds.value] })
    roleDialog.value?.close()
    editingUser.value = null
    toast.success('Papéis do usuário atualizados com sucesso.')
    await loadUsers()
  } catch (cause) {
    handleApiError(cause, undefined, 'Não foi possível atualizar os papéis do usuário.')
  } finally {
    savingRoles.value = false
  }
}

function isOwnAccount(user: User): boolean {
  return user.id === currentUserId.value
}

function canDeleteUser(user: User): boolean {
  return canDeleteUsers.value && Boolean(currentUserId.value) && !isOwnAccount(user)
}

async function openDeleteConfirmation(user: User) {
  if (busy.value || editingUser.value || !canDeleteUser(user)) return
  deleteTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
  userToDelete.value = user
  await nextTick()
  deleteModal.value?.querySelector<HTMLButtonElement>('button')?.focus()
}

function closeDeleteConfirmation() {
  if (deleting.value) return
  userToDelete.value = null
  deleteTrigger?.focus()
}

function handleDeleteKeys(event: KeyboardEvent) {
  if (!userToDelete.value) return
  if (event.key === 'Escape') {
    event.preventDefault()
    closeDeleteConfirmation()
  }
  if (event.key !== 'Tab') return
  const buttons = deleteModal.value?.querySelectorAll<HTMLButtonElement>('button:not(:disabled)')
  const first = buttons?.[0]
  const last = buttons?.[buttons.length - 1]
  if (!first || !last) { event.preventDefault(); return }
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus() }
}

// Confere novamente a permissão e a própria conta antes de enviar DELETE.
async function deleteUser() {
  const user = userToDelete.value
  if (!user || busy.value || !canDeleteUser(user)) return
  deleting.value = true

  try {
    await userService.remove(user.id)
    userToDelete.value = null
    toast.success('Usuário excluído com sucesso.')
    await loadUsers()
  } catch (cause) {
    handleApiError(cause, undefined, 'Não foi possível excluir o usuário.')
  } finally {
    deleting.value = false
  }
}

onMounted(() => { if (canReadUsers.value) void loadUsers() })
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-8 text-ink" aria-labelledby="users-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
          <div>
            <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">ADMINISTRAÇÃO</p>
            <h1 id="users-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Usuários</h1>
            <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">
              Consulte os usuários cadastrados e seus papéis de acesso.
            </p>
          </div>
          <button
            type="button"
            class="inline-flex min-h-[40px] items-center justify-center gap-2 rounded-xl border border-line bg-white px-4 text-[13px] font-semibold hover:bg-[#ededeb] disabled:cursor-wait disabled:opacity-60"
            :disabled="busy || !canReadUsers"
            @click="loadUsers()"
          >
            <RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading }" aria-hidden="true" />
            Atualizar
          </button>
        </header>

        <p v-if="!canReadUsers" class="rounded-xl border border-line bg-white p-5 text-sm" role="alert">
          {{ auth.isAuthenticated ? 'Você não tem permissão para consultar os usuários.' : 'Faça login para consultar os usuários.' }}
        </p>
        <div v-else class="overflow-hidden rounded-2xl border border-line bg-white" :aria-busy="loading">
          <form class="border-b border-line px-4 py-4 sm:px-5" @submit.prevent="applyFilters">
            <fieldset :disabled="busy" class="grid min-w-0 gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_160px_auto] xl:items-end">
              <legend class="sr-only">Filtros de usuários</legend>
              <label class="block text-[12px] font-semibold text-[#55555e]">
                Nome
                <input
                  v-model="nameFilter"
                  type="search"
                  placeholder="Buscar pelo nome"
                  class="mt-1.5 block h-[38px] w-full rounded-xl border border-line bg-paper px-3 text-[13px] font-normal placeholder:text-[#74747f] disabled:opacity-60"
                />
              </label>
              <label class="block text-[12px] font-semibold text-[#55555e]">
                E-mail
                <input
                  v-model="emailFilter"
                  type="search"
                  placeholder="Buscar pelo e-mail"
                  class="mt-1.5 block h-[38px] w-full rounded-xl border border-line bg-paper px-3 text-[13px] font-normal placeholder:text-[#74747f] disabled:opacity-60"
                />
              </label>
              <label class="block text-[12px] font-semibold text-[#55555e]">
                Status
                <select
                  v-model="activeFilter"
                  class="mt-1.5 block h-[38px] w-full rounded-xl border border-line bg-paper px-3 text-[13px] font-normal disabled:opacity-60"
                >
                  <option value="">Todos</option>
                  <option value="true">Ativos</option>
                  <option value="false">Inativos</option>
                </select>
              </label>
              <div class="flex flex-wrap items-end gap-2 md:self-end">
                <button
                  type="submit"
                  class="inline-flex min-h-[38px] flex-1 items-center justify-center gap-2 rounded-xl bg-ink px-4 text-[13px] font-semibold text-white hover:bg-ink-soft disabled:cursor-wait disabled:opacity-60"
                >
                  <Search class="size-4" aria-hidden="true" />
                  Filtrar
                </button>
                <button
                  type="button"
                  class="inline-flex min-h-[38px] flex-1 items-center justify-center gap-2 rounded-xl border border-line px-4 text-[13px] font-semibold hover:bg-paper disabled:cursor-wait disabled:opacity-60"
                  @click="clearFilters"
                >
                  <X class="size-4" aria-hidden="true" />
                  Limpar
                </button>
              </div>
            </fieldset>
          </form>

          <div v-if="error" class="m-4 rounded-lg bg-red-50 px-4 py-3 text-[13px] text-red-800 sm:m-5" role="alert">
            <p>{{ error }}</p>
            <button
              type="button"
              class="mt-2 font-semibold underline disabled:opacity-60"
              :disabled="loading"
              @click="loadUsers()"
            >
              Tentar novamente
            </button>
          </div>
          <p v-if="loading" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">
            Carregando usuários...
          </p>
          <div v-else-if="hasLoaded && !error && users.length" class="overflow-x-auto">
            <table class="w-full min-w-[640px] text-left text-[13px]">
              <caption class="sr-only">Usuários cadastrados: nome, e-mail, roles e status</caption>
              <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
                <tr>
                  <th scope="col" class="px-5 py-3 font-semibold">Usuário</th>
                  <th scope="col" class="px-5 py-3 font-semibold">E-mail</th>
                  <th scope="col" class="px-5 py-3 font-semibold">Roles</th>
                  <th scope="col" class="px-5 py-3 font-semibold">Status</th>
                  <th v-if="canUpdateUsers || canDeleteUsers" scope="col" class="px-5 py-3 font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-line/70">
                <tr v-for="user in users" :key="user.id" class="hover:bg-paper/40">
                  <td class="px-5 py-3">
                    <div class="flex items-center gap-3">
                      <span class="flex size-9 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-ink text-[11px] font-bold text-white">
                        <img
                          v-if="user.avatar && !failedAvatars.has(user.id)"
                          :src="user.avatar"
                          alt=""
                          loading="lazy"
                          referrerpolicy="no-referrer"
                          class="size-full object-cover"
                          @error="failedAvatars.add(user.id)"
                        />
                        <span v-else aria-hidden="true">{{ initials(user.name) }}</span>
                      </span>
                      <span class="max-w-[240px] break-words font-semibold">{{ user.name }}</span>
                    </div>
                  </td>
                  <td class="max-w-[300px] break-all px-5 py-3 text-[#55555e]">{{ user.email }}</td>
                  <td class="px-5 py-3">
                    <div v-if="user.roles.length" class="flex flex-wrap gap-1.5">
                      <span
                        v-for="role in user.roles"
                        :key="role.id"
                        class="rounded-full bg-[#f5ede3] px-2.5 py-1 text-[10px] font-semibold text-[#a67039]"
                      >
                        {{ role.name }}
                      </span>
                    </div>
                    <span v-else class="text-xs text-[#74747f]">Sem roles</span>
                  </td>
                  <td class="px-5 py-3">
                    <span
                      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold"
                      :class="user.active ? 'bg-emerald-50 text-emerald-800' : 'bg-paper text-[#55555e]'"
                    >
                      <span class="size-1.5 rounded-full" :class="user.active ? 'bg-emerald-600' : 'bg-[#74747f]'" aria-hidden="true"></span>
                      {{ user.active ? 'Ativo' : 'Inativo' }}
                    </span>
                  </td>
                  <td v-if="canUpdateUsers || canDeleteUsers" class="px-5 py-3">
                    <div class="flex gap-2">
                      <button
                        v-if="canUpdateUsers"
                        type="button"
                        class="inline-flex size-9 items-center justify-center rounded-lg border border-line text-[#55555e] hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                        :aria-label="`Editar papéis de ${user.name}`"
                        :title="canReadRoles ? 'Editar papéis' : 'Seu acesso não permite consultar os papéis disponíveis'"
                        :disabled="busy || !canReadRoles"
                        @click="openRoleEditor(user)"
                      >
                        <Pencil class="size-4" aria-hidden="true" />
                      </button>
                      <button
                        v-if="canDeleteUsers"
                        type="button"
                        class="inline-flex size-9 items-center justify-center rounded-lg border border-line text-red-700 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40"
                        :aria-label="`Excluir ${user.name}`"
                        :title="isOwnAccount(user) ? 'Sua própria conta não pode ser excluída' : !currentUserId ? 'Faça login novamente para identificar sua conta' : 'Excluir usuário'"
                        :disabled="busy || !canDeleteUser(user)"
                        @click="openDeleteConfirmation(user)"
                      >
                        <Trash2 class="size-4" aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else-if="hasLoaded && !error" class="px-5 py-14 text-center" role="status">
            <Users class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
            <p class="text-sm font-semibold">Nenhum usuário encontrado.</p>
            <p class="mt-1 text-[13px] text-[#74747f]">
              {{ hasAppliedFilters ? 'Tente ajustar ou limpar os filtros da busca.' : 'Não há usuários para exibir.' }}
            </p>
            <button v-if="hasAppliedFilters" type="button" class="mt-4 text-[13px] font-semibold underline" @click="clearFilters">
              Limpar filtros
            </button>
          </div>

          <footer v-if="hasLoaded && !error && !loading" class="flex flex-wrap items-center justify-between gap-4 border-t border-line px-4 py-4 text-[12px] sm:px-5">
            <p class="text-[#74747f]" role="status">
              {{ firstItem }}–{{ lastItem }} de {{ page.totalElements }} usuários
            </p>
            <div class="flex flex-wrap items-center gap-4">
              <label class="flex items-center gap-2 text-[#55555e]">
                Por página
                <select :value="pageSize" :disabled="busy" class="h-9 rounded-lg border border-line bg-white px-2" @change="changePageSize">
                  <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <nav class="flex items-center gap-2" aria-label="Paginação de usuários">
                <button
                  type="button"
                  class="inline-flex size-9 items-center justify-center rounded-lg border border-line hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Página anterior"
                  :disabled="busy || page.number === 0 || page.totalPages === 0"
                  @click="changePage(page.number - 1)"
                >
                  <ChevronLeft class="size-4" aria-hidden="true" />
                </button>
                <span class="text-[#55555e]">
                  {{ page.totalPages ? `Página ${page.number + 1} de ${page.totalPages}` : 'Sem páginas' }}
                </span>
                <button
                  type="button"
                  class="inline-flex size-9 items-center justify-center rounded-lg border border-line hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Próxima página"
                  :disabled="busy || page.number + 1 >= page.totalPages"
                  @click="changePage(page.number + 1)"
                >
                  <ChevronRight class="size-4" aria-hidden="true" />
                </button>
              </nav>
            </div>
          </footer>
        </div>
      </div>
    </section>
    <dialog
      ref="roleDialog"
      class="m-auto max-h-[calc(100dvh-32px)] w-[calc(100%-32px)] max-w-[560px] overflow-y-auto rounded-2xl border border-line bg-white p-5 text-ink shadow-2xl backdrop:bg-black/40 sm:p-6"
      aria-labelledby="user-roles-title"
      @cancel.prevent="closeRoleEditor"
    >
      <header class="mb-5 flex items-start justify-between gap-4">
        <div>
          <p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">ACESSOS DO USUÁRIO</p>
          <h2 id="user-roles-title" class="text-xl font-bold">Editar papéis</h2>
          <p class="mt-2 break-words text-sm font-semibold">{{ editingUser?.name }}</p>
          <p class="break-all text-xs text-[#74747f]">{{ editingUser?.email }}</p>
        </div>
        <button type="button" class="inline-flex size-9 shrink-0 items-center justify-center rounded-lg border border-line hover:bg-paper disabled:opacity-50" aria-label="Fechar edição" :disabled="savingRoles" @click="closeRoleEditor">
          <X class="size-4" aria-hidden="true" />
        </button>
      </header>
      <p class="mb-4 text-[13px] leading-5 text-[#74747f]">Selecione os papéis de acesso. Ao salvar, a seleção substituirá os papéis atuais.</p>
      <p v-if="editingUser && isOwnAccount(editingUser)" class="mb-4 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900">Você está alterando seus próprios papéis. Seu acesso à aplicação poderá mudar.</p>
      <p v-if="loadingRoles" class="py-8 text-center text-sm text-[#74747f]" role="status">Carregando papéis...</p>
      <div v-else-if="roleError" class="rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">
        {{ roleError }}
        <button type="button" class="mt-2 block font-semibold underline" @click="loadAvailableRoles">Tentar novamente</button>
      </div>
      <form v-else @submit.prevent="saveRoles">
        <fieldset :disabled="savingRoles" class="overflow-hidden rounded-xl border border-line">
          <legend class="sr-only">Papéis disponíveis</legend>
          <label v-for="role in availableRoles" :key="role.id" class="flex cursor-pointer items-start gap-3 border-b border-line p-3 last:border-b-0 hover:bg-paper">
            <input v-model="selectedRoleIds" type="checkbox" :value="role.id" class="mt-1 size-4 shrink-0 accent-ink" />
            <span><span class="block text-[13px] font-semibold">{{ role.name }}</span><span v-if="role.description" class="block text-xs leading-5 text-[#74747f]">{{ role.description }}</span></span>
          </label>
          <p v-if="!availableRoles.length" class="p-4 text-sm text-[#74747f]">Nenhum papel disponível.</p>
        </fieldset>
        <p v-if="!selectedRoleIds.length" class="mt-3 rounded-lg bg-amber-50 p-3 text-xs leading-5 text-amber-900">Sem papéis, o usuário poderá perder o acesso à aplicação.</p>
        <div class="mt-5 flex justify-end gap-2">
          <button type="button" class="min-h-[38px] rounded-xl border border-line px-4 text-[13px] font-semibold hover:bg-paper disabled:opacity-60" :disabled="savingRoles" @click="closeRoleEditor">Cancelar</button>
          <button type="submit" class="min-h-[38px] rounded-xl bg-ink px-4 text-[13px] font-semibold text-white hover:bg-ink-soft disabled:cursor-not-allowed disabled:opacity-60" :disabled="busy || !rolesChanged || !canUpdateUsers || !canReadRoles">{{ savingRoles ? 'Salvando...' : 'Salvar papéis' }}</button>
        </div>
      </form>
    </dialog>
    <div ref="deleteModal" @keydown="handleDeleteKeys">
      <ConfirmDeleteModal
        :open="Boolean(userToDelete)"
        title="Excluir usuário"
        :description="`Tem certeza que deseja excluir ${userToDelete?.name}? Esta ação não pode ser desfeita.`"
        :loading="deleting"
        @close="closeDeleteConfirmation"
        @confirm="deleteUser"
      />
    </div>
  </DashboardLayout>
</template>

<style scoped>
button, input, select { outline-offset: 3px; }
button:not(:disabled) { cursor: pointer; }
button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #a67039; }
</style>
