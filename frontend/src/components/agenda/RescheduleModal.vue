<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { CalendarX, Clock, RefreshCw, X } from '@lucide/vue'
import { toast } from 'vue-sonner'
import agendaService from '../../services/agendaService'
import type { AvailableSlot, Scheduling } from '../../types/agenda'
import {
  addDays,
  formatPrice,
  formatSchedulingDateTime,
  isoTimePart,
  parseIsoDate,
  toIsoDate,
} from '../../utils/agenda'
import { agendaErrorMessage } from '../../utils/agendaErrors'

const props = defineProps<{
  scheduling: Scheduling | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'rescheduled', updated: Scheduling): void
}>()

const MAX_DAYS_AHEAD = 60
const today = toIsoDate(new Date())
const maxDate = toIsoDate(addDays(new Date(), MAX_DAYS_AHEAD))
const quickDates = Array.from({ length: 7 }, (_, index) => addDays(new Date(), index))

const date = ref(today)
const slots = ref<AvailableSlot[]>([])
const loadingSlots = ref(false)
const slotsError = ref('')
const hasLoadedSlots = ref(false)
let slotsRequest = 0

const selectedSlot = ref<AvailableSlot | null>(null)
const submitting = ref(false)
const submitError = ref('')

const dateLabel = computed(() =>
  date.value
    ? parseIsoDate(date.value).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })
    : ''
)

async function loadSlots() {
  if (!props.scheduling || !date.value) {
    slots.value = []
    hasLoadedSlots.value = false
    return
  }

  if (date.value < today || date.value > maxDate) {
    slots.value = []
    slotsError.value = `Escolha uma data entre hoje e os próximos ${MAX_DAYS_AHEAD} dias.`
    return
  }

  const current = ++slotsRequest
  loadingSlots.value = true
  slotsError.value = ''
  selectedSlot.value = null
  submitError.value = ''

  try {
    const result = await agendaService.listAvailableSlots(
      props.scheduling.barberId,
      props.scheduling.serviceId,
      date.value
    )
    if (current === slotsRequest) {
      slots.value = result
    }
  } catch (cause) {
    if (current === slotsRequest) {
      slots.value = []
      slotsError.value = agendaErrorMessage(cause, 'Não foi possível consultar os horários disponíveis.')
    }
  } finally {
    if (current === slotsRequest) {
      loadingSlots.value = false
      hasLoadedSlots.value = true
    }
  }
}

async function handleReschedule() {
  if (!props.scheduling || !selectedSlot.value || submitting.value) return

  submitting.value = true
  submitError.value = ''

  try {
    const updated = await agendaService.rescheduleScheduling(props.scheduling.id, {
      newStartAt: selectedSlot.value.startAt,
    })
    toast.success('Agendamento remarcado com sucesso!')
    emit('rescheduled', updated)
    emit('close')
  } catch (cause) {
    const message = agendaErrorMessage(
      cause,
      'Não foi possível remarcar o agendamento. O horário pode ter sido reservado por outra pessoa.'
    )
    submitError.value = message
    // Atualiza a lista de horários para refletir a nova situação da agenda
    await loadSlots()
  } finally {
    submitting.value = false
  }
}

watch(
  () => props.scheduling,
  (newVal) => {
    if (newVal) {
      // Começa sugerindo a data atual ou a data de amanhã se for hoje
      date.value = today
      void loadSlots()
    }
  },
  { immediate: true }
)

watch(date, () => {
  void loadSlots()
})
</script>

<template>
  <div
    v-if="scheduling"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
    role="dialog"
    aria-modal="true"
    aria-labelledby="reschedule-modal-title"
    @click.self="emit('close')"
  >
    <div class="relative max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6">
      <div class="flex items-start justify-between gap-4 border-b border-line pb-4">
        <div>
          <p class="text-[11px] font-semibold tracking-wider text-[#a67039] uppercase">Remarcação Atômica</p>
          <h2 id="reschedule-modal-title" class="text-lg font-bold text-ink">Remarcar Horário</h2>
          <p class="mt-0.5 text-xs text-[#74747f]">
            Serviço: <span class="font-semibold text-ink">{{ scheduling.serviceName }}</span> ({{ formatPrice(scheduling.finalPrice) }})
          </p>
        </div>
        <button
          type="button"
          class="rounded-lg p-1.5 text-stone-400 hover:bg-stone-100 hover:text-stone-700 transition"
          aria-label="Fechar modal"
          @click="emit('close')"
        >
          <X class="size-5" />
        </button>
      </div>

      <!-- Detalhes do horário atual -->
      <div class="my-4 rounded-xl bg-paper p-3 text-xs text-[#55555e]">
        <p>
          Horário atual:
          <strong class="text-ink">{{ formatSchedulingDateTime(scheduling.startAt) }}</strong>
          até às {{ isoTimePart(scheduling.endAt) }}
        </p>
        <p v-if="scheduling.durationTime" class="mt-1 flex items-center gap-1 text-[11px] text-[#74747f]">
          <Clock class="size-3" /> Duração: {{ scheduling.durationTime }} minutos
        </p>
      </div>

      <!-- Seleção de Nova Data -->
      <div>
        <label class="mb-2 block text-xs font-semibold text-ink uppercase tracking-wider">Escolha a Nova Data</label>
        <div class="mb-3 flex gap-2 overflow-x-auto pb-1" role="group" aria-label="Dias rápidos">
          <button
            v-for="day in quickDates"
            :key="toIsoDate(day)"
            type="button"
            class="flex min-w-[54px] flex-col items-center rounded-xl border px-2 py-1.5 text-[11px] transition"
            :class="date === toIsoDate(day) ? 'border-ink bg-ink text-white shadow-xs' : 'border-line hover:bg-paper'"
            :aria-pressed="date === toIsoDate(day)"
            @click="date = toIsoDate(day)"
          >
            <span class="capitalize">{{ day.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '') }}</span>
            <span class="text-sm font-bold">{{ String(day.getDate()).padStart(2, '0') }}</span>
          </button>
        </div>

        <div class="flex items-center gap-3">
          <input
            id="reschedule-date"
            v-model="date"
            type="date"
            :min="today"
            :max="maxDate"
            class="field-input max-w-[180px] text-xs"
          />
          <button
            type="button"
            class="button button-secondary button-sm text-xs"
            :disabled="loadingSlots"
            @click="loadSlots"
          >
            <RefreshCw class="size-3" :class="{ 'animate-spin': loadingSlots }" /> Atualizar
          </button>
        </div>
      </div>

      <!-- Seleção de Horários Livres -->
      <div class="mt-5">
        <div class="mb-2 flex items-center justify-between">
          <label class="text-xs font-semibold text-ink uppercase tracking-wider">
            Horários Livres ({{ dateLabel }})
          </label>
        </div>

        <div v-if="slotsError" class="rounded-lg bg-red-50 p-3 text-xs text-red-800" role="alert">
          {{ slotsError }}
        </div>
        <div v-else-if="loadingSlots" class="py-8 text-center text-xs text-[#74747f]" role="status">
          Buscando horários disponíveis...
        </div>
        <div
          v-else-if="slots.length"
          class="grid grid-cols-[repeat(auto-fill,minmax(72px,1fr))] gap-2"
          role="radiogroup"
          aria-label="Novos horários"
        >
          <button
            v-for="slot in slots"
            :key="slot.startAt"
            type="button"
            role="radio"
            class="rounded-xl border py-2 text-xs font-semibold transition"
            :class="selectedSlot?.startAt === slot.startAt ? 'border-ink bg-ink text-white shadow-xs' : 'border-line hover:bg-paper'"
            :aria-checked="selectedSlot?.startAt === slot.startAt"
            @click="selectedSlot = slot; submitError = ''"
          >
            {{ isoTimePart(slot.startAt) }}
          </button>
        </div>
        <div v-else-if="hasLoadedSlots" class="py-8 text-center text-xs text-[#74747f]" role="status">
          <CalendarX class="mx-auto mb-2 size-7 text-[#a67039]" :stroke-width="1.5" />
          <p class="font-semibold text-ink">Nenhum horário livre nesta data.</p>
          <p class="mt-0.5">Tente selecionar outro dia para remarcar.</p>
        </div>
      </div>

      <!-- Erro da Remarcação (Ex: 409 conflito concorrente) -->
      <div v-if="submitError" class="mt-4 rounded-lg bg-red-50 p-3 text-xs font-medium text-red-800" role="alert">
        {{ submitError }}
      </div>

      <!-- Rodapé com Ações -->
      <div class="mt-6 flex flex-wrap items-center justify-end gap-3 border-t border-line pt-4">
        <button
          type="button"
          class="button button-secondary button-sm"
          :disabled="submitting"
          @click="emit('close')"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="button button-primary button-sm"
          :disabled="!selectedSlot || submitting"
          @click="handleReschedule"
        >
          {{ submitting ? 'Remarcando...' : 'Confirmar Remarcação' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped src="../stock/stock-ui.css"></style>

