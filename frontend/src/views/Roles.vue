<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { toast } from 'vue-sonner'
import { RouterLink } from 'vue-router'
import { ArrowLeft, KeyRound, Pencil, Plus, RefreshCw, Search, ShieldCheck, Trash2, Users, X } from '@lucide/vue'
import roleService from '../services/roleService'
import type { ApiResponse } from '../types/api'
import type { Role } from '../types/role'

const SYSTEM_ROLES = ['ADMIN', 'USER', 'BARBER']

const roles = ref<Role[]>([])
const search = ref('')
const loading = ref(false)
const hasLoaded = ref(false)
const saving = ref(false)
const deletingRoleId = ref<string | null>(null)
const error = ref('')
const formError = ref('')
const modalOpen = ref(false)
const editing = ref<Role | null>(null)
const form = ref({ name: '', description: '' })

const filteredRoles = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()
  return roles.value.filter(role => role.name.toLocaleLowerCase().includes(query))
})

function getErrorMessage(cause: unknown, fallback: string): string {
  if (cause instanceof AxiosError) {
    const responseBody = cause.response?.data as ApiResponse | undefined
    if (responseBody?.message) return responseBody.message

    if (cause.response?.status === 403) {
      return 'Você não tem permissão para executar esta ação.'
    }

    if (!cause.response) {
      return 'Não foi possível conectar ao servidor. Verifique sua conexão.'
    }
  }

  if (cause instanceof Error && cause.message) return cause.message
  return fallback
}

function isSystemRole(role: Role): boolean {
  return SYSTEM_ROLES.includes(role.name.toUpperCase())
}

async function loadRoles() {
  if (loading.value) return
  loading.value = true
  error.value = ''

  try {
    roles.value = await roleService.list()
  } catch (cause) {
    error.value = getErrorMessage(cause, 'Não foi possível carregar as roles.')
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

function openCreate() {
  editing.value = null
  form.value = { name: '', description: '' }
  formError.value = ''
  modalOpen.value = true
}

function openEdit(role: Role) {
  editing.value = role
  form.value = { name: role.name, description: role.description ?? '' }
  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return
  modalOpen.value = false
}

function validateForm(): boolean {
  const name = form.value.name.trim()
  const description = form.value.description.trim()

  if (!name) {
    formError.value = 'Informe o nome da role.'
  } else if (name.length > 50) {
    formError.value = 'O nome deve ter no máximo 50 caracteres.'
  } else if (description.length > 255) {
    formError.value = 'A descrição deve ter no máximo 255 caracteres.'
  } else {
    formError.value = ''
    return true
  }

  return false
}

async function saveRole() {
  if (saving.value || !validateForm()) return

  const currentRole = editing.value
  const payload = {
    name: form.value.name.trim(),
    description: form.value.description.trim() || null,
  }

  saving.value = true
  formError.value = ''

  try {
    const savedRole = currentRole
      ? await roleService.update(currentRole.id, payload)
      : await roleService.create(payload)

    if (currentRole) {
      roles.value = roles.value.map(role => role.id === savedRole.id ? savedRole : role)
    } else {
      roles.value = [...roles.value, savedRole]
    }

    error.value = ''
    modalOpen.value = false
    toast.success(currentRole ? 'Role atualizada com sucesso.' : 'Role criada com sucesso.')
  } catch (cause) {
    formError.value = getErrorMessage(cause, 'Não foi possível salvar a role.')
  } finally {
    saving.value = false
  }
}

async function removeRole(role: Role) {
  if (deletingRoleId.value || isSystemRole(role)) return

  const confirmed = window.confirm(
    `A role "${role.name}" só pode ser excluída se não estiver atribuída a usuários. Deseja continuar?`,
  )
  if (!confirmed) return

  deletingRoleId.value = role.id
  error.value = ''

  try {
    await roleService.remove(role.id)
    roles.value = roles.value.filter(item => item.id !== role.id)
    toast.success('Role excluída com sucesso.')
  } catch (cause) {
    const message = getErrorMessage(cause, 'Não foi possível excluir a role.')
    error.value = message
    toast.error(message)
  } finally {
    deletingRoleId.value = null
  }
}

onMounted(loadRoles)
</script>

<template>
  <main class="min-h-screen bg-paper text-ink">
    <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
      <RouterLink to="/dashboard" class="mb-5 inline-flex items-center gap-2 text-xs font-medium text-[#74747f] hover:text-ink">
        <ArrowLeft class="size-4" aria-hidden="true" />
        Voltar ao dashboard
      </RouterLink>

      <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
        <div>
          <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">ADMINISTRAÇÃO</p>
          <h1 class="text-[27px] leading-9 font-bold tracking-[-1px]">Roles</h1>
          <p class="mt-1 max-w-[610px] text-[14px] leading-[21px] text-[#74747f]">
            Gerencie os papéis de acesso utilizados pelas pessoas no sistema.
          </p>
        </div>
        <button
          type="button"
          class="inline-flex min-h-[40px] items-center justify-center gap-2 rounded-lg border border-ink bg-ink px-4 text-[13px] font-semibold text-white hover:bg-ink-soft disabled:cursor-wait disabled:opacity-60"
          :disabled="saving || Boolean(deletingRoleId)"
          @click="openCreate"
        >
          <Plus class="size-4" aria-hidden="true" />
          Nova Role
        </button>
      </header>

      <section class="overflow-hidden rounded-2xl border border-line bg-white">
        <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-4 sm:px-5">
          <label class="flex h-[38px] w-full max-w-[380px] items-center gap-2 rounded-xl border border-line bg-paper px-3">
            <Search class="size-4 shrink-0 text-[#74747f]" aria-hidden="true" />
            <span class="sr-only">Buscar roles pelo nome</span>
            <input
              v-model="search"
              type="search"
              placeholder="Buscar role pelo nome"
              class="w-full min-w-0 bg-transparent text-[13px] outline-none placeholder:text-[#74747f]"
            />
          </label>
          <button
            type="button"
            class="inline-flex min-h-[38px] items-center justify-center gap-2 rounded-xl border border-line px-4 text-[13px] font-semibold transition-colors hover:bg-[#ededeb] disabled:cursor-wait disabled:opacity-60"
            :disabled="loading"
            @click="loadRoles"
          >
            <RefreshCw class="size-4" :class="{ 'animate-spin': loading }" aria-hidden="true" />
            Atualizar
          </button>
        </div>

        <p v-if="error" class="mx-4 mt-4 rounded-lg bg-red-50 px-4 py-3 text-[13px] text-red-800 sm:mx-5" role="alert">
          {{ error }}
          <button v-if="!loading" type="button" class="ml-2 font-semibold underline" @click="loadRoles">Tentar novamente</button>
        </p>

        <p v-if="loading && !hasLoaded" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">
          Carregando roles...
        </p>

        <div v-else-if="filteredRoles.length" class="grid gap-4 p-4 sm:grid-cols-2 sm:p-5 xl:grid-cols-3">
          <article
            v-for="role in filteredRoles"
            :key="role.id"
            class="rounded-xl border border-line bg-white p-5 shadow-[0_2px_8px_#1a1a1c08]"
          >
            <div class="flex items-start justify-between gap-3">
              <span class="flex size-10 items-center justify-center rounded-xl bg-[#f5ede3] text-[#a67039]">
                <ShieldCheck class="size-[19px]" :stroke-width="1.7" aria-hidden="true" />
              </span>
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line bg-white text-[#55555e] hover:bg-paper disabled:opacity-50"
                  :aria-label="`Editar ${role.name}`"
                  :disabled="Boolean(deletingRoleId)"
                  @click="openEdit(role)"
                >
                  <Pencil class="size-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line bg-white text-red-700 hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                  :aria-label="`Excluir ${role.name}`"
                  :title="isSystemRole(role) ? 'Roles do sistema não podem ser excluídas' : 'Excluir role'"
                  :disabled="isSystemRole(role) || Boolean(deletingRoleId)"
                  @click="removeRole(role)"
                >
                  <Trash2 class="size-4" aria-hidden="true" />
                </button>
              </div>
            </div>

            <div class="mt-4">
              <div class="flex flex-wrap items-center gap-2">
                <h2 class="text-[15px] font-semibold">{{ role.name }}</h2>
                <span v-if="isSystemRole(role)" class="rounded-full bg-paper px-2 py-0.5 text-[10px] font-semibold text-[#74747f]">
                  Sistema
                </span>
              </div>
              <p class="mt-1 min-h-10 text-[13px] leading-5 text-[#74747f]">
                {{ role.description || 'Nenhuma descrição informada.' }}
              </p>
            </div>

            <footer class="mt-4 flex flex-wrap gap-x-5 gap-y-2 border-t border-line pt-3 text-xs text-[#74747f]">
              <span class="inline-flex items-center gap-1.5"><KeyRound class="size-3.5" aria-hidden="true" />{{ role.permissionsCount }} permissões</span>
              <span class="inline-flex items-center gap-1.5"><Users class="size-3.5" aria-hidden="true" />{{ role.usersCount }} usuários</span>
            </footer>
          </article>
        </div>

        <div v-else-if="hasLoaded && !error" class="px-5 py-14 text-center">
          <p class="text-sm font-semibold">{{ search ? 'Nenhuma role encontrada.' : 'Nenhuma role encontrada.' }}</p>
          <p v-if="search" class="mt-1 text-[13px] text-[#74747f]">Tente ajustar os termos da busca.</p>
          <button
            v-else
            type="button"
            class="mt-4 inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-2.5 text-[13px] font-semibold text-white hover:bg-ink-soft"
            @click="openCreate"
          >
            <Plus class="size-4" aria-hidden="true" />
            Criar primeira Role
          </button>
        </div>
      </section>
    </div>

    <div v-if="modalOpen" class="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/40 p-4" @click.self="closeModal">
      <section
        class="my-auto w-full max-w-[480px] rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="role-form-title"
      >
        <header class="mb-5 flex items-start justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">ACESSOS DA EQUIPE</p>
            <h2 id="role-form-title" class="text-xl font-bold">{{ editing ? 'Editar Role' : 'Nova Role' }}</h2>
            <p v-if="editing && isSystemRole(editing)" class="mt-1 text-xs text-[#74747f]">
              O nome desta Role do sistema não pode ser alterado.
            </p>
          </div>
          <button
            type="button"
            class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line text-[#55555e] hover:bg-paper disabled:opacity-50"
            aria-label="Fechar formulário"
            :disabled="saving"
            @click="closeModal"
          >
            <X class="size-4" aria-hidden="true" />
          </button>
        </header>

        <form class="space-y-4" @submit.prevent="saveRole">
          <label class="block text-[13px] font-semibold">
            Nome <span class="text-red-700">*</span>
            <input
              v-model="form.name"
              type="text"
              required
              maxlength="50"
              autocomplete="off"
              placeholder="Ex.: GERENTE"
              :disabled="Boolean(editing && isSystemRole(editing)) || saving"
              class="mt-2 block h-10 w-full rounded-lg border border-line px-3 text-sm font-normal outline-none focus:border-[#a67039] disabled:bg-paper disabled:text-[#74747f]"
            />
          </label>

          <label class="block text-[13px] font-semibold">
            Descrição
            <textarea
              v-model="form.description"
              rows="3"
              maxlength="255"
              placeholder="Descreva o papel de acesso"
              :disabled="saving"
              class="mt-2 block w-full resize-y rounded-lg border border-line px-3 py-2.5 text-sm font-normal outline-none focus:border-[#a67039] disabled:bg-paper"
            />
          </label>

          <p v-if="formError" class="rounded-lg bg-red-50 px-3 py-2.5 text-[13px] text-red-800" role="alert">{{ formError }}</p>

          <div class="flex justify-end gap-2 pt-2">
            <button
              type="button"
              class="inline-flex min-h-[38px] items-center justify-center rounded-xl border border-line px-4 text-[13px] font-semibold hover:bg-paper disabled:opacity-60"
              :disabled="saving"
              @click="closeModal"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="inline-flex min-h-[38px] items-center justify-center rounded-lg bg-ink px-4 text-[13px] font-semibold text-white hover:bg-ink-soft disabled:cursor-wait disabled:opacity-60"
              :disabled="saving"
            >
              {{ saving ? 'Salvando...' : editing ? 'Salvar alterações' : 'Criar Role' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
