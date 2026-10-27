<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import StockDialog from './StockDialog.vue'
import { createMovementSchema, type MovementData, type MovementInput } from '../../schemas/stock.schema'
import type { MovementType, Product } from '../../types/stock'
import { formatQuantity, toDateInput, unitSymbol } from '../../utils/stock'

const props = defineProps<{
  open: boolean
  type: MovementType
  product: Product | null
  responsible: string
  submit: (data: MovementData) => Promise<void>
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

const isEntry = computed(() => props.type === 'ENTRY')
const today = ref(toDateInput())
// Baixas não podem ultrapassar o estoque atual.
const validationSchema = computed(() => toTypedSchema(createMovementSchema(isEntry.value ? undefined : props.product?.quantity ?? 0, today.value)))
const emptyForm = (): MovementInput => ({ quantity: '', date: today.value, note: '' })

const { defineField, errors, handleSubmit, resetForm, isSubmitting } = useForm({ validationSchema, initialValues: emptyForm() })
const [quantity, quantityProps] = defineField('quantity')
const [date, dateProps] = defineField('date')
const [note, noteProps] = defineField('note')
const submitError = ref('')
const busy = computed(() => isSubmitting.value)
const outOfStock = computed(() => !isEntry.value && (props.product?.quantity ?? 0) <= 0)

watch(() => props.open, open => {
  if (!open) return
  today.value = toDateInput()
  resetForm({ values: emptyForm() })
  submitError.value = ''
})

const onSubmit = handleSubmit(async values => {
  submitError.value = ''
  try {
    await props.submit(values)
  } catch (cause) {
    submitError.value = cause instanceof Error && cause.message ? cause.message : 'Não foi possível registrar a movimentação.'
  }
})
</script>

<template>
  <StockDialog :open="open" :title="isEntry ? 'Dar entrada' : 'Dar baixa'" :busy="busy" @close="emit('close')">
    <template #subtitle>
      <p v-if="product" class="mt-1 text-[13px] text-[#74747f]">
        <span class="font-semibold text-ink">{{ product.name }}</span> · em estoque: {{ formatQuantity(product.quantity, product.unit) }}
      </p>
    </template>
    <form novalidate @submit.prevent="onSubmit">
      <div v-if="submitError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ submitError }}</div>
      <p v-if="outOfStock" class="mb-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900" role="status">Este produto está esgotado. Registre uma entrada antes de dar baixa.</p>
      <fieldset :disabled="busy || outOfStock" class="grid min-w-0 gap-4 sm:grid-cols-2">
        <legend class="sr-only">Dados da movimentação</legend>
        <div>
          <label for="movement-quantity" class="field-label">Quantidade{{ product ? ` (${unitSymbol(product.unit)})` : '' }}</label>
          <input id="movement-quantity" v-model="quantity" v-bind="quantityProps" type="text" inputmode="decimal" class="field-input" :aria-invalid="Boolean(errors.quantity)" :aria-describedby="errors.quantity ? 'movement-quantity-error' : undefined" />
          <p v-if="errors.quantity" id="movement-quantity-error" class="field-error">{{ errors.quantity }}</p>
        </div>
        <div>
          <label for="movement-date" class="field-label">Data da movimentação</label>
          <input id="movement-date" v-model="date" v-bind="dateProps" type="date" :max="today" class="field-input" :aria-invalid="Boolean(errors.date)" :aria-describedby="errors.date ? 'movement-date-error' : undefined" />
          <p v-if="errors.date" id="movement-date-error" class="field-error">{{ errors.date }}</p>
        </div>
        <div v-if="isEntry" class="sm:col-span-2">
          <label for="movement-note" class="field-label">Observação (opcional)</label>
          <textarea id="movement-note" v-model="note" v-bind="noteProps" maxlength="255" class="field-input" placeholder="Ex.: compra com o fornecedor X" :aria-invalid="Boolean(errors.note)" :aria-describedby="errors.note ? 'movement-note-error' : undefined"></textarea>
          <p v-if="errors.note" id="movement-note-error" class="field-error">{{ errors.note }}</p>
        </div>
      </fieldset>
      <p class="mt-4 text-xs text-[#74747f]">Responsável: <span class="font-semibold text-[#55555e]">{{ responsible }}</span></p>
      <div class="mt-6 flex flex-wrap justify-end gap-2">
        <button type="button" class="button button-secondary" :disabled="busy" @click="emit('close')">Cancelar</button>
        <button type="submit" class="button button-primary" :disabled="busy || outOfStock">{{ busy ? 'Salvando...' : isEntry ? 'Registrar entrada' : 'Registrar baixa' }}</button>
      </div>
    </form>
  </StockDialog>
</template>

<style scoped src="./stock-ui.css"></style>
