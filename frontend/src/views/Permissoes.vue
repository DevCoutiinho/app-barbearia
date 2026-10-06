<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { toast } from 'vue-sonner'
import { RouterLink } from 'vue-router'
import {
  ArrowLeft,
  KeyRound,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from '@lucide/vue'

import permissionService, {
  type PermissionCreate,
} from '../services/permissionService'

import type { ApiResponse } from '../types/api'
import type { Permission } from '../types/role'

const PERMISSION_NAMES = [
  'PERMISSION_TEST',
  'ADMIN_ACCESS',
  'USER_READ',
  'USER_CREATE',
  'USER_UPDATE',
  'USER_DELETE',
  'ROLE_READ',
  'ROLE_CREATE',
  'ROLE_UPDATE',
  'ROLE_DELETE',
  'PERMISSION_READ',
  'PERMISSION_CREATE',
  'PERMISSION_UPDATE',
  'PERMISSION_DELETE',
]

const permissions = ref<Permission[]>([])
const search = ref('')
const loading = ref(false)
const hasLoaded = ref(false)
const saving = ref(false)
const deletingPermissionId = ref<string | null>(null)

const error = ref('')
const formError = ref('')
const modalOpen = ref(false)

const form = ref<PermissionCreate>({
  name: '',
  description: '',
})

const filteredPermissions = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()

  if (!query) return permissions.value

  return permissions.value.filter(permission =>
    permission.name.toLocaleLowerCase().includes(query),
  )
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

  if (cause instanceof Error && cause.message) {
    return cause.message
  }

  return fallback
}

async function loadPermissions() {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    permissions.value = await permissionService.list()
  } catch (cause) {
    error.value = getErrorMessage(
      cause,
      'Não foi possível carregar as permissões.',
    )
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

function openCreate() {
  form.value = {
    name: '',
    description: '',
  }

  formError.value = ''
  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return

  modalOpen.value = false
}

function validateForm(): boolean {
  if (!form.value.name) {
    formError.value = 'Selecione o nome da permissão.'
    return false
  }

  if (form.value.description.trim().length > 255) {
    formError.value = 'A descrição deve ter no máximo 255 caracteres.'
    return false
  }

  formError.value = ''
  return true
}

async function savePermission() {
  if (saving.value || !validateForm()) return

  saving.value = true
  formError.value = ''

  try {
    const permission = await permissionService.create({
      name: form.value.name,
      description: form.value.description.trim(),
    })

    permissions.value = [...permissions.value, permission]

    modalOpen.value = false

    toast.success('Permissão criada com sucesso.')
  } catch (cause) {
    formError.value = getErrorMessage(
      cause,
      'Não foi possível criar a permissão.',
    )
  } finally {
    saving.value = false
  }
}

async function removePermission(permission: Permission) {
  if (deletingPermissionId.value) return

  const confirmed = window.confirm(
    `A permissão "${permission.name}" será excluída. Deseja continuar?`,
  )

  if (!confirmed) return

  deletingPermissionId.value = permission.id
  error.value = ''

  try {
    await permissionService.remove(permission.id)

    permissions.value = permissions.value.filter(
      item => item.id !== permission.id,
    )

    toast.success('Permissão excluída com sucesso.')
  } catch (cause) {
    const message = getErrorMessage(
      cause,
      'Não foi possível excluir a permissão.',
    )

    error.value = message
    toast.error(message)
  } finally {
    deletingPermissionId.value = null
  }
}

onMounted(loadPermissions)
</script>

<template>
  <main class="min-h-screen bg-paper text-ink">
    <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
      <RouterLink
        to="/dashboard"
        class="mb-5 inline-flex items-center gap-2 text-xs font-medium text-[#74747f] hover:text-ink"
      >
        <ArrowLeft class="size-4" aria-hidden="true" />
        Voltar ao dashboard
      </RouterLink>

      <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
        <div>
          <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">
            ADMINISTRAÇÃO
          </p>

          <h1 class="text-[27px] leading-9 font-bold tracking-[-1px]">
            Permissões
          </h1>

          <p class="mt-1 max-w-[610px] text-[14px] leading-[21px] text-[#74747f]">
            Gerencie as permissões disponíveis para as Roles do sistema.
          </p>
        </div>

        <button
          type="button"
          class="inline-flex min-h-[40px] items-center justify-center gap-2 rounded-lg border border-ink bg-ink px-4 text-[13px] font-semibold text-white hover:bg-ink-soft disabled:cursor-wait disabled:opacity-60"
          :disabled="saving || Boolean(deletingPermissionId)"
          @click="openCreate"
        >
          <Plus class="size-4" aria-hidden="true" />
          Nova Permissão
        </button>
      </header>

      <section class="overflow-hidden rounded-2xl border border-line bg-white">
        <div
          class="flex flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-4 sm:px-5"
        >
          <label
            class="flex h-[38px] w-full max-w-[380px] items-center gap-2 rounded-xl border border-line bg-paper px-3"
          >
            <Search class="size-4 shrink-0 text-[#74747f]" />

            <span class="sr-only">
              Buscar permissões pelo nome
            </span>

            <input
              v-model="search"
              type="search"
              placeholder="Buscar permissão pelo nome"
              class="w-full min-w-0 bg-transparent text-[13px] outline-none placeholder:text-[#74747f]"
            />
          </label>

          <button
            type="button"
            class="inline-flex min-h-[38px] items-center justify-center gap-2 rounded-xl border border-line px-4 text-[13px] font-semibold transition-colors hover:bg-[#ededeb] disabled:cursor-wait disabled:opacity-60"
            :disabled="loading"
            @click="loadPermissions"
          >
            <RefreshCw
              class="size-4"
              :class="{ 'animate-spin': loading }"
            />
            Atualizar
          </button>
        </div>

        <p
          v-if="error"
          class="mx-4 mt-4 rounded-lg bg-red-50 px-4 py-3 text-[13px] text-red-800 sm:mx-5"
          role="alert"
        >
          {{ error }}

          <button
            v-if="!loading"
            type="button"
            class="ml-2 font-semibold underline"
            @click="loadPermissions"
          >
            Tentar novamente
          </button>
        </p>

        <p
          v-if="loading && !hasLoaded"
          class="px-5 py-14 text-center text-sm text-[#74747f]"
        >
          Carregando permissões...
        </p>

        <div
          v-else-if="filteredPermissions.length"
          class="grid gap-4 p-4 sm:grid-cols-2 sm:p-5 xl:grid-cols-3"
        >
          <article
            v-for="permission in filteredPermissions"
            :key="permission.id"
            class="rounded-xl border border-line bg-white p-5 shadow-[0_2px_8px_#1a1a1c08]"
          >
            <div class="flex items-start justify-between gap-3">
              <span
                class="flex size-10 items-center justify-center rounded-xl bg-[#f5ede3] text-[#a67039]"
              >
                <KeyRound
                  class="size-[19px]"
                  :stroke-width="1.7"
                />
              </span>

              <button
                type="button"
                class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line bg-white text-red-700 hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                :aria-label="`Excluir ${permission.name}`"
                :disabled="Boolean(deletingPermissionId)"
                @click="removePermission(permission)"
              >
                <Trash2 class="size-4" />
              </button>
            </div>

            <div class="mt-4">
              <h2 class="text-[15px] font-semibold">
                {{ permission.name }}
              </h2>

              <p class="mt-2 min-h-10 text-[13px] leading-5 text-[#74747f]">
                {{ permission.description || 'Nenhuma descrição informada.' }}
              </p>
            </div>
          </article>
        </div>

        <div
          v-else-if="hasLoaded && !error"
          class="px-5 py-14 text-center"
        >
          <p class="text-sm font-semibold">
            Nenhuma permissão encontrada.
          </p>

          <p
            v-if="search"
            class="mt-1 text-[13px] text-[#74747f]"
          >
            Tente ajustar os termos da busca.
          </p>

          <button
            v-else
            type="button"
            class="mt-4 inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-2.5 text-[13px] font-semibold text-white hover:bg-ink-soft"
            @click="openCreate"
          >
            <Plus class="size-4" />
            Criar primeira Permissão
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="modalOpen"
      class="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/40 p-4"
      @click.self="closeModal"
    >
      <section
        class="my-auto w-full max-w-[480px] rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6"
        role="dialog"
        aria-modal="true"
        aria-labelledby="permission-form-title"
      >
        <header class="mb-5 flex items-start justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">
              ACESSOS DA EQUIPE
            </p>

            <h2
              id="permission-form-title"
              class="text-xl font-bold"
            >
              Nova Permissão
            </h2>
          </div>

          <button
            type="button"
            class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line text-[#55555e] hover:bg-paper disabled:opacity-50"
            :disabled="saving"
            @click="closeModal"
          >
            <X class="size-4" />
          </button>
        </header>

        <form
          class="space-y-4"
          @submit.prevent="savePermission"
        >
          <label class="block text-[13px] font-semibold">
            Nome
            <span class="text-red-700">*</span>

            <select
              v-model="form.name"
              :disabled="saving"
              class="mt-2 block h-10 w-full rounded-lg border border-line px-3 text-sm font-normal outline-none focus:border-[#a67039] disabled:bg-paper"
            >
              <option value="" disabled>
                Selecione uma permissão
              </option>

              <option
                v-for="name in PERMISSION_NAMES"
                :key="name"
                :value="name"
              >
                {{ name }}
              </option>
            </select>
          </label>

          <label class="block text-[13px] font-semibold">
            Descrição

            <textarea
              v-model="form.description"
              rows="3"
              maxlength="255"
              placeholder="Descreva a permissão"
              :disabled="saving"
              class="mt-2 block w-full resize-y rounded-lg border border-line px-3 py-2.5 text-sm font-normal outline-none focus:border-[#a67039] disabled:bg-paper"
            />
          </label>

          <p
            v-if="formError"
            class="rounded-lg bg-red-50 px-3 py-2.5 text-[13px] text-red-800"
            role="alert"
          >
            {{ formError }}
          </p>

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
              {{ saving ? 'Salvando...' : 'Criar Permissão' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>