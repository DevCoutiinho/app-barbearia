<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import StockDialog from './StockDialog.vue'
import { createMovementSchema, type MovementData, type MovementInput } from '../../schemas/stock.schema'
import userService from '../../services/userService'
import type { MovementType, Product } from '../../types/stock'
import type { User } from '../../types/user'
import { formatQuantity, unitSymbol } from '../../utils/stock'
import { handleApiError } from '../../utils/errorHandler'
import { stockErrorMessage } from '../../utils/stockErrors'

const props = defineProps<{
  open: boolean
  type: MovementType
  product: Product | null
  submit: (data: MovementData) => Promise<void>
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

const isEntry = computed(() => props.type === 'ENTRY')
const validationSchema = computed(() =>
  toTypedSchema(createMovementSchema(props.type, isEntry.value ? undefined : props.product?.currentQuantity ?? 0)),
)
const emptyForm = (): MovementInput => ({ userId: '', quantity: '' })

const { defineField, errors, handleSubmit, resetForm, setFieldError, isSubmitting } = useForm<MovementInput>({
  validationSchema,
  initialValues: emptyForm(),
})
const [userId, userIdProps] = defineField('userId')
const [quantity, quantityProps] = defineField('quantity')

const users = ref<User[]>([])
const loadingUsers = ref(false)
const usersError = ref('')
const submitError = ref('')
const busy = computed(() => isSubmitting.value)
const outOfStock = computed(() => !isEntry.value && (props.product?.currentQuantity ?? 0) <= 0)
let usersRequest = 0

async function loadUsers() {
  if (isEntry.value) return
  const current = ++usersRequest
  loadingUsers.value = true
  usersError.value = ''
  try {
    const response = await userService.list({ page: 0, size: 100, filters: { active: true } })
    const result = response.data?.[0]
    if (current !== usersRequest) return
    if (!result || !Array.isArray(result.content)) {
      throw new Error('Resposta de usuários inválida.')
    }
    users.value = result.content
  } catch (cause) {
    if (current === usersRequest) {
      usersError.value = stockErrorMessage(cause, 'Não foi possível carregar a lista de usuários.')
    }
  } finally {
    if (current === usersRequest) loadingUsers.value = false
  }
}

watch(() => [props.open, props.type] as const, ([open, type]) => {
  if (!open) return
  resetForm({ values: emptyForm() })
  submitError.value = ''
  if (type === 'CONSUMPTION') {
    void loadUsers()
  }
})

const onSubmit = handleSubmit(async values => {
  submitError.value = ''
  try {
    await props.submit(values as unknown as MovementData)
  } catch (cause) {
    const fallback = isEntry.value
      ? 'Não foi possível registrar a entrada.'
      : 'Não foi possível registrar o consumo.'
    submitError.value = stockErrorMessage(cause, fallback)
    handleApiError(cause, setFieldError, submitError.value)
  }
})
</script>

<template>
  <StockDialog :open="open" :title="isEntry ? 'Registrar entrada' : 'Registrar consumo'" :busy="busy" @close="emit('close')">
    <template #subtitle>
      <p v-if="product" class="mt-1 text-[13px] text-[#74747f]">
        <span class="font-semibold text-ink">{{ product.name }}</span> · em estoque: {{ formatQuantity(product.currentQuantity, product.unitMeasurement) }}
      </p>
    </template>
    <form novalidate @submit.prevent="onSubmit">
      <div v-if="submitError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ submitError }}</div>
      <p v-if="outOfStock" class="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900" role="status">Este produto está esgotado. Registre uma entrada antes de registrar consumo.</p>
      <div v-if="!isEntry && usersError" class="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900" role="alert">
        <p>{{ usersError }}</p>
        <button type="button" class="mt-2 font-semibold underline" :disabled="loadingUsers" @click="loadUsers">Tentar carregar usuários novamente</button>
      </div>
      <fieldset :disabled="busy || outOfStock" class="grid min-w-0 gap-4">
        <legend class="sr-only">Dados da movimentação</legend>
        <div v-if="!isEntry">
          <label for="movement-user" class="field-label">Usuário atribuído</label>
          <select
            id="movement-user"
            v-model="userId"
            v-bind="userIdProps"
            class="field-input"
            :disabled="loadingUsers"
            :aria-invalid="Boolean(errors.userId)"
            :aria-describedby="errors.userId ? 'movement-user-error' : undefined"
          >
            <option value="">{{ loadingUsers ? 'Carregando usuários...' : 'Selecione um usuário' }}</option>
            <option v-for="user in users" :key="user.id" :value="user.id">
              {{ user.name }} ({{ user.email }})
            </option>
          </select>
          <p v-if="errors.userId" id="movement-user-error" class="field-error">{{ errors.userId }}</p>
        </div>
        <div>
          <label for="movement-quantity" class="field-label">Quantidade{{ product ? ` (${unitSymbol(product.unitMeasurement)})` : '' }}</label>
          <input id="movement-quantity" v-model="quantity" v-bind="quantityProps" type="text" inputmode="decimal" placeholder="Ex.: 10" class="field-input" :aria-invalid="Boolean(errors.quantity)" :aria-describedby="errors.quantity ? 'movement-quantity-error' : undefined" />
          <p v-if="errors.quantity" id="movement-quantity-error" class="field-error">{{ errors.quantity }}</p>
        </div>
      </fieldset>
      <div class="mt-6 flex flex-wrap justify-end gap-2">
        <button type="button" class="button button-secondary" :disabled="busy" @click="emit('close')">Cancelar</button>
        <button type="submit" class="button button-primary" :disabled="busy || outOfStock || (!isEntry && loadingUsers)">
          {{ busy ? 'Salvando...' : isEntry ? 'Registrar entrada' : 'Registrar consumo' }}
        </button>
      </div>
    </form>
  </StockDialog>
</template>

<style scoped src="./stock-ui.css"></style>
