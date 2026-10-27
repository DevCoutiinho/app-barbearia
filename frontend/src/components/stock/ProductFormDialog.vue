<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import StockDialog from './StockDialog.vue'
import { productSchema, type ProductData, type ProductInput } from '../../schemas/stock.schema'
import { PRODUCT_CATEGORIES, STOCK_UNITS } from '../../types/stock'
import { unitSymbol } from '../../utils/stock'

const props = defineProps<{
  open: boolean
  submit: (data: ProductData) => Promise<void>
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

const emptyForm = (): ProductInput => ({ name: '', category: '', unit: '', initialQuantity: '0', minimum: '' })

const { defineField, errors, handleSubmit, resetForm, isSubmitting } = useForm({
  validationSchema: toTypedSchema(productSchema),
  initialValues: emptyForm(),
})
const [name, nameProps] = defineField('name')
const [category, categoryProps] = defineField('category')
const [unit, unitProps] = defineField('unit')
const [initialQuantity, initialQuantityProps] = defineField('initialQuantity')
const [minimum, minimumProps] = defineField('minimum')
const submitError = ref('')
const busy = computed(() => isSubmitting.value)

watch(() => props.open, open => {
  if (open) {
    resetForm({ values: emptyForm() })
    submitError.value = ''
  }
})

const onSubmit = handleSubmit(async values => {
  submitError.value = ''
  try {
    await props.submit(values)
  } catch (cause) {
    submitError.value = cause instanceof Error && cause.message ? cause.message : 'Não foi possível cadastrar o produto.'
  }
})
</script>

<template>
  <StockDialog :open="open" title="Adicionar item" :busy="busy" width-class="max-w-[600px]" @close="emit('close')">
    <form novalidate @submit.prevent="onSubmit">
      <div v-if="submitError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ submitError }}</div>
      <fieldset :disabled="busy" class="grid min-w-0 gap-4 sm:grid-cols-2">
        <legend class="sr-only">Dados do produto</legend>
        <div class="sm:col-span-2">
          <label for="product-name" class="field-label">Nome</label>
          <input id="product-name" v-model="name" v-bind="nameProps" type="text" maxlength="100" class="field-input" placeholder="Ex.: Pomada modeladora" :aria-invalid="Boolean(errors.name)" :aria-describedby="errors.name ? 'product-name-error' : undefined" />
          <p v-if="errors.name" id="product-name-error" class="field-error">{{ errors.name }}</p>
        </div>
        <div>
          <label for="product-category" class="field-label">Categoria</label>
          <select id="product-category" v-model="category" v-bind="categoryProps" class="field-input" :aria-invalid="Boolean(errors.category)" :aria-describedby="errors.category ? 'product-category-error' : undefined">
            <option value="">Selecione uma categoria</option>
            <option v-for="option in PRODUCT_CATEGORIES" :key="option" :value="option">{{ option }}</option>
          </select>
          <p v-if="errors.category" id="product-category-error" class="field-error">{{ errors.category }}</p>
        </div>
        <div>
          <label for="product-unit" class="field-label">Unidade</label>
          <select id="product-unit" v-model="unit" v-bind="unitProps" class="field-input" :aria-invalid="Boolean(errors.unit)" :aria-describedby="errors.unit ? 'product-unit-error' : undefined">
            <option value="">Selecione uma unidade</option>
            <option v-for="option in STOCK_UNITS" :key="option.value" :value="option.value">{{ option.label }} ({{ unitSymbol(option.value) }})</option>
          </select>
          <p v-if="errors.unit" id="product-unit-error" class="field-error">{{ errors.unit }}</p>
        </div>
        <div>
          <label for="product-initial" class="field-label">Quantidade inicial</label>
          <input id="product-initial" v-model="initialQuantity" v-bind="initialQuantityProps" type="text" inputmode="decimal" class="field-input" :aria-invalid="Boolean(errors.initialQuantity)" :aria-describedby="errors.initialQuantity ? 'product-initial-error' : undefined" />
          <p v-if="errors.initialQuantity" id="product-initial-error" class="field-error">{{ errors.initialQuantity }}</p>
        </div>
        <div>
          <label for="product-minimum" class="field-label">Estoque mínimo</label>
          <input id="product-minimum" v-model="minimum" v-bind="minimumProps" type="text" inputmode="decimal" class="field-input" placeholder="Ex.: 5" :aria-invalid="Boolean(errors.minimum)" :aria-describedby="errors.minimum ? 'product-minimum-error' : 'product-minimum-help'" />
          <p v-if="errors.minimum" id="product-minimum-error" class="field-error">{{ errors.minimum }}</p>
          <p v-else id="product-minimum-help" class="mt-1.5 text-xs text-[#74747f]">Abaixo desse valor o item entra em alerta.</p>
        </div>
      </fieldset>
      <div class="mt-6 flex flex-wrap justify-end gap-2">
        <button type="button" class="button button-secondary" :disabled="busy" @click="emit('close')">Cancelar</button>
        <button type="submit" class="button button-primary" :disabled="busy">{{ busy ? 'Salvando...' : 'Salvar item' }}</button>
      </div>
    </form>
  </StockDialog>
</template>

<style scoped src="./stock-ui.css"></style>
