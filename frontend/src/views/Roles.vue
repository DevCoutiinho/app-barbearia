<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { createRoleSchema, updateRoleSchema } from '../schemas/role.schema'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.vue'
import { handleApiError } from '../utils/errorHandler'
import {
  KeyRound,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Trash2,
  X,
} from '@lucide/vue'

import roleService from '../services/roleService'
import permissionService from '../services/permissionService'

import type {
  Permission,
  Role,
} from '../types/role'

const ROLE_NAMES = ['USER', 'ADMIN', 'BARBER']

const createSchema = toTypedSchema(createRoleSchema);
const updateSchema = toTypedSchema(updateRoleSchema);

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])

const search = ref('')

const loading = ref(false)
const hasLoaded = ref(false)
const saving = ref(false)
const deletingRoleId = ref<string | null>(null)
const roleToDelete = ref<Role | null>(null)

const error = ref('')

const modalOpen = ref(false)
const editing = ref<Role | null>(null)

// Computed schema baseado no estado de edição
const validationSchema = computed(() => editing.value ? updateSchema : createSchema);

const { handleSubmit, errors, resetForm, defineField, setFieldError } = useForm({
  validationSchema,
  initialValues: {
    name: '',
    description: '',
    permissionIds: [] as string[],
  }
});

const [name, nameProps] = defineField('name');
const [description, descriptionProps] = defineField('description');
const [permissionIds] = defineField('permissionIds');

const filteredRoles = computed(() => {
  const query = search.value.trim().toLocaleLowerCase()

  if (!query) return roles.value

  return roles.value.filter(role =>
    role.name.toLocaleLowerCase().includes(query),
  )
})

function isSystemRole(role: Role): boolean {
  return ROLE_NAMES.includes(role.name.toUpperCase())
}

function isPermissionSelected(permissionId: string): boolean {
  return permissionIds.value?.includes(permissionId) || false
}

function togglePermission(permissionId: string) {
  const current = permissionIds.value || []
  if (current.includes(permissionId)) {
    permissionIds.value = current.filter(id => id !== permissionId)
  } else {
    permissionIds.value = [...current, permissionId]
  }
}

function selectAllPermissions() {
  permissionIds.value = permissions.value.map(permission => permission.id)
}

function clearPermissions() {
  permissionIds.value = []
}

async function loadData() {
  if (loading.value) return

  loading.value = true
  error.value = ''

  try {
    const [rolesResponse, permissionsResponse] = await Promise.all([
      roleService.list(),
      permissionService.list(),
    ])

    roles.value = rolesResponse.data || []
    permissions.value = permissionsResponse.data || []
  } catch (cause) {
    error.value = 'Não foi possível carregar as roles e permissões.'
    handleApiError(cause, undefined, error.value)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

function openCreate() {
  editing.value = null

  resetForm({
    values: {
      name: '',
      description: '',
      permissionIds: [],
    }
  })

  modalOpen.value = true
}

function openEdit(role: Role) {
  editing.value = role

  resetForm({
    values: {
      name: role.name,
      description: role.description ?? '',
      permissionIds: role.permissions.map(permission => permission.id),
    }
  })

  modalOpen.value = true
}

function closeModal() {
  if (saving.value) return

  modalOpen.value = false
}

const saveRole = handleSubmit(async (values) => {
  if (saving.value) return

  const currentRole = editing.value

  saving.value = true

  try {
    if (currentRole) {
      await roleService.update(currentRole.id, values as any)
    } else {
      await roleService.create(values as any)
    }

    await loadData()

    error.value = ''
    modalOpen.value = false

    toast.success(
      currentRole
        ? 'Role atualizada com sucesso.'
        : 'Role criada com sucesso.',
    )
  } catch (cause) {
    handleApiError(cause, setFieldError, 'Não foi possível salvar a role.')
  } finally {
    saving.value = false
  }
});

async function confirmRemoveRole(role: Role) {
  if (deletingRoleId.value || isSystemRole(role)) return
  roleToDelete.value = role
}

async function removeRole() {
  const role = roleToDelete.value
  if (!role || deletingRoleId.value || isSystemRole(role)) return

  deletingRoleId.value = role.id
  error.value = ''

  try {
    await roleService.remove(role.id)
    await loadData()

    toast.success('Role excluída com sucesso.')
    roleToDelete.value = null
  } catch (cause) {
    handleApiError(cause, undefined, 'Não foi possível excluir a role.')
  } finally {
    deletingRoleId.value = null
  }
}

onMounted(loadData)
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
              Roles
            </h1>

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

          <div
            class="flex flex-wrap items-center justify-between gap-3 border-b border-line px-4 py-4 sm:px-5"
          >
            <label
              class="flex h-[38px] w-full max-w-[380px] items-center gap-2 rounded-xl border border-line bg-paper px-3"
            >
              <Search
                class="size-4 shrink-0 text-[#74747f]"
                aria-hidden="true"
              />

              <span class="sr-only">
                Buscar roles pelo nome
              </span>

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
              @click="loadData"
            >
              <RefreshCw
                class="size-4"
                :class="{ 'animate-spin': loading }"
                aria-hidden="true"
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
              @click="loadData"
            >
              Tentar novamente
            </button>
          </p>

          <p
            v-if="loading && !hasLoaded"
            class="px-5 py-14 text-center text-sm text-[#74747f]"
            role="status"
          >
            Carregando roles...
          </p>

          <div
            v-else-if="filteredRoles.length"
            class="grid gap-4 p-4 sm:grid-cols-2 sm:p-5 xl:grid-cols-3"
          >
            <article
              v-for="role in filteredRoles"
              :key="role.id"
              class="rounded-xl border border-line bg-white p-5 shadow-[0_2px_8px_#1a1a1c08]"
            >
              <div class="flex items-start justify-between gap-3">

                <span
                  class="flex size-10 items-center justify-center rounded-xl bg-[#f5ede3] text-[#a67039]"
                >
                  <ShieldCheck
                    class="size-[19px]"
                    :stroke-width="1.7"
                    aria-hidden="true"
                  />
                </span>

                <div class="flex items-center gap-1">

                  <button
                    type="button"
                    class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line bg-white text-[#55555e] hover:bg-paper disabled:opacity-50"
                    :aria-label="`Editar ${role.name}`"
                    :disabled="Boolean(deletingRoleId)"
                    @click="openEdit(role)"
                  >
                    <Pencil
                      class="size-4"
                      aria-hidden="true"
                    />
                  </button>

                  <button
                    type="button"
                    class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line bg-white text-red-700 hover:bg-paper disabled:cursor-not-allowed disabled:opacity-40"
                    :aria-label="`Excluir ${role.name}`"
                    :title="isSystemRole(role)
                      ? 'Roles do sistema não podem ser excluídas'
                      : 'Excluir role'"
                    :disabled="isSystemRole(role) || Boolean(deletingRoleId)"
                    @click="confirmRemoveRole(role)"
                  >
                    <Trash2
                      class="size-4"
                      aria-hidden="true"
                    />
                  </button>

                </div>
              </div>

              <div class="mt-4">

                <div class="flex flex-wrap items-center gap-2">

                  <h2 class="text-[15px] font-semibold">
                    {{ role.name }}
                  </h2>

                  <span
                    v-if="isSystemRole(role)"
                    class="rounded-full bg-paper px-2 py-0.5 text-[10px] font-semibold text-[#74747f]"
                  >
                    Sistema
                  </span>

                </div>

                <p class="mt-1 min-h-10 text-[13px] leading-5 text-[#74747f]">
                  {{ role.description || 'Nenhuma descrição informada.' }}
                </p>

              </div>

              <footer
                class="mt-4 border-t border-line pt-3 text-xs text-[#74747f]"
              >
                <span class="inline-flex items-center gap-1.5">
                  <KeyRound
                    class="size-3.5"
                    aria-hidden="true"
                  />

                  {{ role.permissions.length }} permissões
                </span>

                <div
                  v-if="role.permissions.length"
                  class="mt-3 flex flex-wrap gap-1.5"
                >
                  <span
                    v-for="permission in role.permissions"
                    :key="permission.id"
                    class="rounded-full bg-paper px-2 py-1 text-[10px] font-medium text-[#55555e]"
                  >
                    {{ permission.name }}
                  </span>
                </div>
              </footer>

            </article>
          </div>

          <div
            v-else-if="hasLoaded && !error"
            class="px-5 py-14 text-center"
          >
            <p class="text-sm font-semibold">
              Nenhuma role encontrada.
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
              <Plus class="size-4" aria-hidden="true" />
              Criar primeira Role
            </button>
          </div>

        </section>
      </div>

      <!-- MODAL -->
      <div
        v-if="modalOpen"
        class="fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-black/40 p-4"
        @click.self="closeModal"
      >
        <section
          class="my-auto w-full max-w-[560px] rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6"
          role="dialog"
          aria-modal="true"
          aria-labelledby="role-form-title"
        >

          <header class="mb-5 flex items-start justify-between gap-4">

            <div>
              <p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">
                ACESSOS DA EQUIPE
              </p>

              <h2
                id="role-form-title"
                class="text-xl font-bold"
              >
                {{ editing ? 'Editar Role' : 'Nova Role' }}
              </h2>

              <p
                v-if="editing && isSystemRole(editing)"
                class="mt-1 text-xs text-[#74747f]"
              >
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

          <form
            class="space-y-5"
            @submit.prevent="saveRole"
            novalidate
          >

            <!-- NOME -->
            <label class="block text-[13px] font-semibold">

              Nome
              <span class="text-red-700">*</span>

              <select
                v-model="name"
                v-bind="nameProps"
                :disabled="Boolean(editing) || saving"
                class="mt-2 block h-10 w-full rounded-lg border bg-white px-3 text-sm font-normal outline-none focus:ring-2 disabled:bg-paper disabled:text-[#74747f]"
                :class="errors?.name ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15' : 'border-line focus:border-[#a67039]'"
              >
                <option
                  value=""
                  disabled
                >
                  Selecione uma Role
                </option>

                <option
                  v-for="roleName in ROLE_NAMES"
                  :key="roleName"
                  :value="roleName"
                >
                  {{ roleName }}
                </option>
              </select>
              <span v-if="errors?.name" class="mt-1 block text-xs font-medium text-red-500">
                  {{ errors.name }}
              </span>
            </label>

            <!-- DESCRIÇÃO -->
            <label class="block text-[13px] font-semibold">

              Descrição

              <textarea
                v-model="description"
                v-bind="descriptionProps"
                rows="3"
                maxlength="255"
                placeholder="Descreva o papel de acesso"
                :disabled="saving"
                class="mt-2 block w-full resize-y rounded-lg border px-3 py-2.5 text-sm font-normal outline-none focus:ring-2 disabled:bg-paper"
                :class="errors?.description ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15' : 'border-line focus:border-[#a67039]'"
              ></textarea>
              <span v-if="errors?.description" class="mt-1 block text-xs font-medium text-red-500">
                  {{ errors.description }}
              </span>
            </label>

            <!-- PERMISSÕES -->
            <div>

              <div class="mb-2 flex flex-wrap items-center justify-between gap-2">

                <div>
                  <p class="text-[13px] font-semibold">
                    Permissões
                    <span class="text-red-700">*</span>
                  </p>

                  <p class="mt-0.5 text-[11px] text-[#74747f]">
                    Selecione as permissões que esta Role poderá utilizar.
                  </p>
                </div>

                <div class="flex gap-2">

                  <button
                    type="button"
                    class="text-[11px] font-semibold text-[#a67039] hover:underline disabled:opacity-50"
                    :disabled="saving || !permissions.length"
                    @click="selectAllPermissions"
                  >
                    Selecionar todas
                  </button>

                  <button
                    type="button"
                    class="text-[11px] font-semibold text-[#74747f] hover:underline disabled:opacity-50"
                    :disabled="saving || !(permissionIds?.length)"
                    @click="clearPermissions"
                  >
                    Limpar
                  </button>

                </div>
              </div>

              <div
                v-if="permissions.length"
                class="max-h-[260px] overflow-y-auto rounded-xl border"
                :class="errors?.permissionIds ? 'border-red-500' : 'border-line'"
              >

                <label
                  v-for="permission in permissions"
                  :key="permission.id"
                  class="flex cursor-pointer items-start gap-3 border-b border-line px-3 py-3 last:border-b-0 hover:bg-paper"
                >

                  <input
                    type="checkbox"
                    :checked="isPermissionSelected(permission.id)"
                    :disabled="saving"
                    class="mt-0.5 size-4 accent-[#a67039]"
                    @change="togglePermission(permission.id)"
                  />

                  <span class="min-w-0">
                    <span class="block text-[12px] font-semibold">
                      {{ permission.name }}
                    </span>

                    <span
                      v-if="permission.description"
                      class="mt-0.5 block text-[11px] leading-4 text-[#74747f]"
                    >
                      {{ permission.description }}
                    </span>
                  </span>

                </label>

              </div>

              <p
                v-else
                class="rounded-lg border border-line px-3 py-4 text-center text-[12px] text-[#74747f]"
              >
                Nenhuma permissão disponível.
              </p>

              <span v-if="errors?.permissionIds" class="mt-1 block text-xs font-medium text-red-500">
                  {{ errors.permissionIds }}
              </span>

              <p class="mt-2 text-[11px] text-[#74747f]">
                {{ permissionIds?.length || 0 }} permissão(ões) selecionada(s)
              </p>

            </div>

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
                {{ saving
                  ? 'Salvando...'
                  : editing
                    ? 'Salvar alterações'
                    : 'Criar Role'
                }}
              </button>

            </div>

          </form>
        </section>
      </div>

      <ConfirmDeleteModal
        :open="Boolean(roleToDelete)"
        title="Excluir Role"
        :description="`A role &quot;${roleToDelete?.name}&quot; só pode ser excluída se não estiver atribuída a nenhum usuário. Tem certeza que deseja continuar?`"
        :loading="Boolean(deletingRoleId)"
        @close="roleToDelete = null"
        @confirm="removeRole"
      />
    </main>
  </DashboardLayout>
</template>