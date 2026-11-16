<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import StockDialog from '../stock/StockDialog.vue'
import { createBlockSchema, type BlockData, type BlockInput } from '../../schemas/agenda.schema'
import { BLOCK_REASONS, type BlockReason } from '../../types/agenda'
import { BLOCK_CONFLICT_MESSAGE, agendaErrorMessage } from '../../utils/agendaErrors'

const props = defineProps<{
  open: boolean
  today: string
  initialDate?: string
  submit: (data: BlockData) => Promise<void>
}>()
const emit = defineEmits<{ (e: 'close'): void }>()

// Folga e feriado costumam valer o dia todo; os demais motivos, um período.
const FULL_DAY_REASONS: readonly BlockReason[] = ['DAY_OFF', 'HOLIDAY']

const emptyForm = (): BlockInput => ({
  date: props.initialDate && props.initialDate >= props.today ? props.initialDate : props.today,
  fullDay: true,
  startTime: '',
  endTime: '',
  reason: 'DAY_OFF',
  description: '',
})

const validationSchema = computed(() => toTypedSchema(createBlockSchema(props.today)))
const { defineField, errors, handleSubmit, resetForm, setFieldError, isSubmitting } = useForm<BlockInput>({
  validationSchema,
  initialValues: emptyForm(),
})
const [date, dateProps] = defineField('date')
const [fullDay] = defineField('fullDay')
const [startTime, startTimeProps] = defineField('startTime')
const [endTime, endTimeProps] = defineField('endTime')
const [reason, reasonProps] = defineField('reason')
const [description, descriptionProps] = defineField('description')

const submitError = ref('')
const busy = computed(() => isSubmitting.value)

watch(() => props.open, open => {
  if (!open) return
  resetForm({ values: emptyForm() })
  submitError.value = ''
})

watch(reason, value => {
  if (value === 'LUNCH' && fullDay.value) {
    fullDay.value = false
    if (!startTime.value && !endTime.value) {
      startTime.value = '12:00'
      endTime.value = '13:00'
    }
  } else if (FULL_DAY_REASONS.includes(value as BlockReason)) {
    fullDay.value = true
  }
})

const onSubmit = handleSubmit(async values => {
  submitError.value = ''
  try {
    await props.submit(values as unknown as BlockData)
  } catch (cause) {
    submitError.value = agendaErrorMessage(cause, 'Não foi possível cadastrar o bloqueio. Tente novamente.', BLOCK_CONFLICT_MESSAGE)
    // Erros de campo vindos do servidor (ex.: data obrigatória) também aparecem no campo.
    const fieldErrors = (cause as { response?: { data?: { errors?: { field: string; message: string }[] } } })?.response?.data?.errors ?? []
    fieldErrors.forEach(error => {
      if (error.field in emptyForm()) setFieldError(error.field as keyof BlockInput, error.message)
    })
  }
})
</script>

<template>
  <StockDialog :open="open" eyebrow="AGENDA" title="Novo bloqueio" :busy="busy" @close="emit('close')">
    <template #subtitle>
      <p class="mt-1 text-[13px] text-[#74747f]">Folgas, feriados, almoço ou saídas deixam de aparecer como horários livres para os clientes.</p>
    </template>
    <form novalidate @submit.prevent="onSubmit">
      <div v-if="submitError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ submitError }}</div>
      <fieldset :disabled="busy" class="grid min-w-0 gap-4 sm:grid-cols-2">
        <legend class="sr-only">Dados do bloqueio</legend>
        <div>
          <label for="block-reason" class="field-label">Motivo</label>
          <select id="block-reason" v-model="reason" v-bind="reasonProps" class="field-input" :aria-invalid="Boolean(errors.reason)" :aria-describedby="errors.reason ? 'block-reason-error' : undefined">
            <option v-for="option in BLOCK_REASONS" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
          <p v-if="errors.reason" id="block-reason-error" class="field-error">{{ errors.reason }}</p>
        </div>
        <div>
          <label for="block-date" class="field-label">Data</label>
          <input id="block-date" v-model="date" v-bind="dateProps" type="date" :min="today" class="field-input" :aria-invalid="Boolean(errors.date)" :aria-describedby="errors.date ? 'block-date-error' : undefined" />
          <p v-if="errors.date" id="block-date-error" class="field-error">{{ errors.date }}</p>
        </div>

        <div class="sm:col-span-2">
          <span class="field-label">Período</span>
          <div class="mt-1.5 grid grid-cols-2 gap-1 rounded-xl bg-paper p-1" role="radiogroup" aria-label="Período do bloqueio">
            <label v-for="option in [{ value: true, label: 'Dia inteiro' }, { value: false, label: 'Horário específico' }]" :key="String(option.value)" class="period-option" :class="{ 'period-option-active': fullDay === option.value }">
              <input v-model="fullDay" type="radio" name="block-period" class="sr-only" :value="option.value" />{{ option.label }}
            </label>
          </div>
        </div>

        <template v-if="!fullDay">
          <div>
            <label for="block-start" class="field-label">Início</label>
            <input id="block-start" v-model="startTime" v-bind="startTimeProps" type="time" step="300" class="field-input" :aria-invalid="Boolean(errors.startTime)" :aria-describedby="errors.startTime ? 'block-start-error' : undefined" />
            <p v-if="errors.startTime" id="block-start-error" class="field-error">{{ errors.startTime }}</p>
          </div>
          <div>
            <label for="block-end" class="field-label">Término</label>
            <input id="block-end" v-model="endTime" v-bind="endTimeProps" type="time" step="300" class="field-input" :aria-invalid="Boolean(errors.endTime)" :aria-describedby="errors.endTime ? 'block-end-error' : undefined" />
            <p v-if="errors.endTime" id="block-end-error" class="field-error">{{ errors.endTime }}</p>
          </div>
        </template>

        <div class="sm:col-span-2">
          <label for="block-description" class="field-label">Observação <span class="font-normal text-[#74747f]">(opcional)</span></label>
          <textarea id="block-description" v-model="description" v-bind="descriptionProps" maxlength="255" rows="2" class="field-input" placeholder="Ex.: consulta médica" :aria-invalid="Boolean(errors.description)" :aria-describedby="errors.description ? 'block-description-error' : undefined"></textarea>
          <p v-if="errors.description" id="block-description-error" class="field-error">{{ errors.description }}</p>
        </div>
      </fieldset>
      <div class="mt-6 flex flex-wrap justify-end gap-2">
        <button type="button" class="button button-secondary" :disabled="busy" @click="emit('close')">Cancelar</button>
        <button type="submit" class="button button-primary" :disabled="busy">{{ busy ? 'Salvando...' : 'Bloquear agenda' }}</button>
      </div>
    </form>
  </StockDialog>
</template>

<style scoped src="../stock/stock-ui.css"></style>
<style scoped>
.period-option { display: flex; min-height: 34px; cursor: pointer; align-items: center; justify-content: center; border-radius: 9px; font-size: 13px; font-weight: 600; color: #55555e; }
.period-option-active { background: white; color: var(--color-ink); box-shadow: 0 1px 2px rgb(0 0 0 / 8%); }
.period-option:focus-within { outline: 2px solid #a67039; outline-offset: 2px; }
</style>
