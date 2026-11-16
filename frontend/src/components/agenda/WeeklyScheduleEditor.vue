<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Info, Plus, RotateCcw, Save, Trash2 } from '@lucide/vue'
import { weeklyScheduleSchema, type WeeklyScheduleInput } from '../../schemas/agenda.schema'
import { DAY_LABELS, type WeeklySchedule } from '../../types/agenda'
import { fromMinutes, toMinutes } from '../../utils/agenda'
import { agendaErrorMessage } from '../../utils/agendaErrors'

const props = defineProps<{
  schedule: WeeklySchedule
  submit: (schedule: WeeklySchedule) => Promise<void>
}>()

type EditorDays = WeeklyScheduleInput['days']

const DEFAULT_INTERVAL = { start: '09:00', end: '18:00' }

const toEditor = (schedule: WeeklySchedule): EditorDays => schedule.days.map(day => ({
  dayOfWeek: day.dayOfWeek,
  enabled: day.intervals.length > 0,
  intervals: day.intervals.map(range => ({ ...range })),
}))

const days = ref<EditorDays>(toEditor(props.schedule))
const dayErrors = ref<Record<number, string>>({})
const submitError = ref('')
const saving = ref(false)

// Compara só o que vai para a API: intervalos de dia desmarcado não contam.
const snapshot = (value: EditorDays) => JSON.stringify(value.map(day => (day.enabled ? day.intervals : [])))
const dirty = computed(() => snapshot(days.value) !== snapshot(toEditor(props.schedule)))
const workingDays = computed(() => days.value.filter(day => day.enabled).length)

watch(() => props.schedule, schedule => {
  days.value = toEditor(schedule)
  dayErrors.value = {}
  submitError.value = ''
})

function clearDayError(index: number) {
  if (!dayErrors.value[index]) return
  const { [index]: _, ...rest } = dayErrors.value
  dayErrors.value = rest
}

function toggleDay(index: number) {
  const day = days.value[index]
  day.enabled = !day.enabled
  if (day.enabled && !day.intervals.length) {
    // Reaproveita o horário do último dia configurado para agilizar o preenchimento.
    const reference = [...days.value].reverse().find(item => item.enabled && item.intervals.length)
    day.intervals = (reference?.intervals ?? [DEFAULT_INTERVAL]).map(range => ({ ...range }))
  }
  clearDayError(index)
}

function addInterval(index: number) {
  const intervals = days.value[index].intervals
  const lastEnd = intervals.length ? toMinutes(intervals[intervals.length - 1].end) : 9 * 60
  const start = Math.min(lastEnd + 60, 22 * 60)
  intervals.push({ start: fromMinutes(start), end: fromMinutes(Math.min(start + 4 * 60, 23 * 60 + 59)) })
  clearDayError(index)
}

function removeInterval(dayIndex: number, intervalIndex: number) {
  const day = days.value[dayIndex]
  day.intervals.splice(intervalIndex, 1)
  if (!day.intervals.length) day.enabled = false
  clearDayError(dayIndex)
}

function discard() {
  days.value = toEditor(props.schedule)
  dayErrors.value = {}
  submitError.value = ''
}

async function save() {
  if (saving.value) return
  submitError.value = ''
  const result = weeklyScheduleSchema.safeParse({ days: days.value })
  if (!result.success) {
    dayErrors.value = Object.fromEntries(result.error.issues.map(issue => [Number(issue.path[1]), issue.message]))
    submitError.value = 'Corrija os horários destacados antes de salvar.'
    return
  }
  dayErrors.value = {}
  saving.value = true
  try {
    await props.submit(result.data)
  } catch (cause) {
    submitError.value = agendaErrorMessage(cause, 'Não foi possível salvar o expediente. Tente novamente.')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <form class="p-4 sm:p-5" novalidate @submit.prevent="save">
    <p class="mb-4 flex items-start gap-2 rounded-lg bg-paper p-3 text-[13px] leading-5 text-[#55555e]">
      <Info class="mt-0.5 size-4 shrink-0 text-[#a67039]" aria-hidden="true" />
      Para criar uma pausa, divida o dia em dois períodos (ex.: 09:00–12:00 e 13:00–18:00). Alterar o expediente não muda atendimentos já marcados: a nova grade vale para os próximos agendamentos.
    </p>

    <div v-if="submitError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ submitError }}</div>

    <fieldset :disabled="saving" class="min-w-0">
      <legend class="sr-only">Dias e horários de trabalho</legend>
      <ul class="divide-y divide-line rounded-xl border border-line">
        <li v-for="(day, dayIndex) in days" :key="day.dayOfWeek" class="flex flex-col gap-3 p-3 sm:flex-row sm:items-start sm:p-4">
          <label class="flex w-full shrink-0 cursor-pointer items-center gap-2.5 sm:w-44 sm:pt-2">
            <input
              type="checkbox"
              class="size-4 accent-[#1a1a1c]"
              :checked="day.enabled"
              :aria-describedby="dayErrors[dayIndex] ? `day-error-${dayIndex}` : undefined"
              @change="toggleDay(dayIndex)"
            />
            <span class="text-[13px] font-semibold">{{ DAY_LABELS[day.dayOfWeek].long }}</span>
          </label>

          <div class="min-w-0 flex-1">
            <p v-if="!day.enabled" class="py-2 text-[13px] text-[#74747f]">Folga</p>
            <template v-else>
              <div class="flex flex-wrap items-center gap-2">
                <div v-for="(interval, intervalIndex) in day.intervals" :key="intervalIndex" class="flex items-center gap-1.5 rounded-xl border border-line bg-paper px-2 py-1">
                  <label class="sr-only" :for="`day-${dayIndex}-start-${intervalIndex}`">Início do período {{ intervalIndex + 1 }} de {{ DAY_LABELS[day.dayOfWeek].long }}</label>
                  <input
                    :id="`day-${dayIndex}-start-${intervalIndex}`"
                    v-model="interval.start"
                    type="time"
                    step="300"
                    required
                    class="time-input"
                    :aria-invalid="Boolean(dayErrors[dayIndex])"
                    @input="clearDayError(dayIndex)"
                  />
                  <span class="text-[12px] text-[#74747f]">até</span>
                  <label class="sr-only" :for="`day-${dayIndex}-end-${intervalIndex}`">Término do período {{ intervalIndex + 1 }} de {{ DAY_LABELS[day.dayOfWeek].long }}</label>
                  <input
                    :id="`day-${dayIndex}-end-${intervalIndex}`"
                    v-model="interval.end"
                    type="time"
                    step="300"
                    required
                    class="time-input"
                    :aria-invalid="Boolean(dayErrors[dayIndex])"
                    @input="clearDayError(dayIndex)"
                  />
                  <button
                    type="button"
                    class="icon-button !size-7 border-transparent !bg-transparent"
                    :aria-label="`Remover período ${intervalIndex + 1} de ${DAY_LABELS[day.dayOfWeek].long}`"
                    @click="removeInterval(dayIndex, intervalIndex)"
                  >
                    <Trash2 class="size-3.5" aria-hidden="true" />
                  </button>
                </div>
                <button type="button" class="button button-secondary button-sm" @click="addInterval(dayIndex)">
                  <Plus class="size-3.5" aria-hidden="true" />Período
                </button>
              </div>
            </template>
            <p v-if="dayErrors[dayIndex]" :id="`day-error-${dayIndex}`" class="field-error">{{ dayErrors[dayIndex] }}</p>
          </div>
        </li>
      </ul>
    </fieldset>

    <div class="mt-5 flex flex-wrap items-center justify-between gap-3">
      <p class="text-[12px] text-[#74747f]" role="status">
        {{ workingDays ? `${workingDays} ${workingDays === 1 ? 'dia' : 'dias'} de trabalho por semana` : 'Nenhum dia de trabalho marcado' }}{{ dirty ? ' · alterações não salvas' : '' }}
      </p>
      <div class="flex flex-wrap gap-2">
        <button type="button" class="button button-secondary" :disabled="saving || !dirty" @click="discard">
          <RotateCcw class="size-4" aria-hidden="true" />Descartar
        </button>
        <button type="submit" class="button button-primary" :disabled="saving || !dirty">
          <Save class="size-4" aria-hidden="true" />{{ saving ? 'Salvando...' : 'Salvar expediente' }}
        </button>
      </div>
    </div>
  </form>
</template>

<style scoped src="../stock/stock-ui.css"></style>
<style scoped>
.time-input { height: 32px; border: 0; background: transparent; color: var(--color-ink); font-size: 13px; }
.time-input[aria-invalid="true"] { color: #b91c1c; }
</style>
