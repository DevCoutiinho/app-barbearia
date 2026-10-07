<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { createPermissionSchema } from '../schemas/permission.schema'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.vue'
import { handleApiError } from '../utils/errorHandler'
import {
  KeyRound,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  X,
} from '@lucide/vue'

import permissionService from '../services/permissionService'

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

const createSchema = toTypedSchema(createPermissionSchema);

const permissions = ref<Permission[]>([])
const search = ref('')
const loading = ref(false)
const hasLoaded = ref(false)
const saving = ref(false)
const deletingPermissionId = ref<string | null>(null)
const permissionToDelete = ref<Permission | null>(null)

const error = ref('')
const modalOpen = ref(false)

const { handleSubmit, errors, resetForm, defineField, setFieldError } = useForm({
  validationSchema: createSchema,
  initialValues: {
    name: '',
    description: '',
  }
});

const [name, nameProps] = defineField('name');
const [description, descriptionProps] = defineField('description');

const filteredPermissions = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()

  if (!query) return permissions.value

  return permissions.value.filter(permission =>
    permission.name.toLocaleLowerCase().includes(query),
  )
})

async function loadPermissions() {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    const response = await permissionService.list()
    permissions.value = response.data || []
  } catch (cause) {
    error.value = 'Não foi possível carregar as permissões.'
    handleApiError(cause, undefined, error.value)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

function openCreate() {
  resetForm({
    values: {
      name: '',
      description: '',
    }
  })

  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return

  modalOpen.value = false
}

const savePermission = handleSubmit(async (values) => {
  if (saving.value) return

  saving.value = true

  try {
    await permissionService.create({
      name: values.name,
      description: values.description?.trim() || null,
    })

    await loadPermissions()

    modalOpen.value = false

    toast.success('Permissão criada com sucesso.')
  } catch (cause) {
    handleApiError(cause, setFieldError, 'Não foi possível criar a permissão.')
  } finally {
    saving.value = false
  }
});

async function confirmRemovePermission(permission: Permission) {
  if (deletingPermissionId.value) return
  permissionToDelete.value = permission
}

async function removePermission() {
  const permission = permissionToDelete.value
  if (!permission || deletingPermissionId.value) return

  deletingPermissionId.value = permission.id
  error.value = ''

  try {
    await permissionService.remove(permission.id)
    await loadPermissions()

    toast.success('Permissão excluída com sucesso.')
    permissionToDelete.value = null
  } catch (cause) {
    handleApiError(cause, undefined, 'Não foi possível excluir a permissão.')
  } finally {
    deletingPermissionId.value = null
  }
}

onMounted(loadPermissions)
</script>

<template>
  <DashboardLayout>
    <main class="bg-paper text-ink pb-8">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">

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
                  @click="confirmRemovePermission(permission)"
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
            novalidate
          >
            <label class="block text-[13px] font-semibold">
              Nome
              <span class="text-red-700">*</span>

              <select
                v-model="name"
                v-bind="nameProps"
                :disabled="saving"
                class="mt-2 block h-10 w-full rounded-lg border bg-white px-3 text-sm font-normal outline-none focus:ring-2 disabled:bg-paper disabled:text-[#74747f]"
                :class="errors?.name ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15' : 'border-line focus:border-[#a67039]'"
              >
                <option value="" disabled>
                  Selecione uma permissão
                </option>

                <option
                  v-for="permName in PERMISSION_NAMES"
                  :key="permName"
                  :value="permName"
                >
                  {{ permName }}
                </option>
              </select>
              <span v-if="errors?.name" class="mt-1 block text-xs font-medium text-red-500">
                  {{ errors.name }}
              </span>
            </label>

            <label class="block text-[13px] font-semibold">
              Descrição

              <textarea
                v-model="description"
                v-bind="descriptionProps"
                rows="3"
                maxlength="255"
                placeholder="Descreva a permissão"
                :disabled="saving"
                class="mt-2 block w-full resize-y rounded-lg border px-3 py-2.5 text-sm font-normal outline-none focus:ring-2 disabled:bg-paper"
                :class="errors?.description ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15' : 'border-line focus:border-[#a67039]'"
              ></textarea>
              <span v-if="errors?.description" class="mt-1 block text-xs font-medium text-red-500">
                  {{ errors.description }}
              </span>
            </label>

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

      <ConfirmDeleteModal
        :open="Boolean(permissionToDelete)"
        title="Excluir Permissão"
        :description="`Tem certeza que deseja excluir a permissão &quot;${permissionToDelete?.name}&quot;? Ela será removida de todas as Roles que a possuem atualmente.`"
        :loading="Boolean(deletingPermissionId)"
        @close="permissionToDelete = null"
        @confirm="removePermission"
      />
    </main>
  </DashboardLayout>
</template>