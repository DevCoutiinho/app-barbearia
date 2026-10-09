<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { isAxiosError } from 'axios'
import { ChevronLeft, ChevronRight, RefreshCw, Search, Users, X } from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import userService from '../services/userService'
import { handleApiError } from '../utils/errorHandler'
import type { PageMetadata, User, UserFilters } from '../types/user'

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
  if (loading.value) return

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
  if (loading.value) return

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
  if (loading.value) return

  nameFilter.value = ''
  emailFilter.value = ''
  activeFilter.value = ''
  appliedFilters.value = {}
  await loadUsers(0)
}

async function changePage(targetPage: number) {
  if (loading.value || targetPage < 0 || targetPage >= page.value.totalPages) return
  await loadUsers(targetPage)
}

// Alterar o tamanho reinicia a consulta na página zero.
async function changePageSize(event: Event) {
  if (loading.value) return

  const size = Number((event.target as HTMLSelectElement).value)
  if (!pageSizes.includes(size)) return
  pageSize.value = size
  await loadUsers(0)
}

onMounted(() => loadUsers())
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
            :disabled="loading"
            @click="loadUsers()"
          >
            <RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading }" aria-hidden="true" />
            Atualizar
          </button>
        </header>

        <div class="overflow-hidden rounded-2xl border border-line bg-white" :aria-busy="loading">
          <form class="border-b border-line px-4 py-4 sm:px-5" @submit.prevent="applyFilters">
            <fieldset :disabled="loading" class="grid min-w-0 gap-3 md:grid-cols-2 xl:grid-cols-[1fr_1fr_160px_auto] xl:items-end">
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
                <select :value="pageSize" class="h-9 rounded-lg border border-line bg-white px-2" @change="changePageSize">
                  <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
                </select>
              </label>
              <nav class="flex items-center gap-2" aria-label="Paginação de usuários">
                <button
                  type="button"
                  class="inline-flex size-9 items-center justify-center rounded-lg border border-line hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                  aria-label="Página anterior"
                  :disabled="page.number === 0 || page.totalPages === 0"
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
                  :disabled="page.number + 1 >= page.totalPages"
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
  </DashboardLayout>
</template>

<style scoped>
button, input, select { outline-offset: 3px; }
button:not(:disabled) { cursor: pointer; }
button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #a67039; }
</style>
