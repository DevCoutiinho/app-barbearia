<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { CalendarCheck, CalendarX, Clock, Info, RefreshCw, Scissors } from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import { useAuthStore } from '../stores/auth'
import agendaService from '../services/agendaService'
import type { AvailableSlot, CatalogService, Scheduling } from '../types/agenda'
import { addDays, formatPrice, isBarber, isoTimePart, parseIsoDate, toIsoDate } from '../utils/agenda'
import { agendaErrorMessage } from '../utils/agendaErrors'

// Mesmo limite de `api.scheduling.max-days-ahead` no servidor, que também valida.
const MAX_DAYS_AHEAD = 60

const auth = useAuthStore()
const today = toIsoDate(new Date())
const maxDate = toIsoDate(addDays(new Date(), MAX_DAYS_AHEAD))
const quickDates = Array.from({ length: 7 }, (_, index) => addDays(new Date(), index))

const catalog = ref<CatalogService[]>([])
const loadingCatalog = ref(false)
const catalogError = ref('')

const serviceId = ref('')
const date = ref(today)
const slots = ref<AvailableSlot[]>([])
const loadingSlots = ref(false)
const slotsError = ref('')
const hasLoadedSlots = ref(false)
let slotsRequest = 0

const selectedSlot = ref<AvailableSlot | null>(null)
const booking = ref(false)
const bookingError = ref('')
const confirmed = ref<Scheduling | null>(null)

const selectedService = computed(() => catalog.value.find(service => service.id === serviceId.value) ?? null)
const servicesByBarber = computed(() => {
  const groups = new Map<string, { barber: CatalogService['barber']; services: CatalogService[] }>()
  catalog.value.forEach(service => {
    const group = groups.get(service.barber.id) ?? { barber: service.barber, services: [] }
    group.services.push(service)
    groups.set(service.barber.id, group)
  })
  return [...groups.values()]
})
const dateLabel = computed(() => date.value
  ? parseIsoDate(date.value).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' })
  : '')

async function loadCatalog() {
  loadingCatalog.value = true
  catalogError.value = ''
  try {
    catalog.value = await agendaService.listCatalog()
    if (!selectedService.value) serviceId.value = catalog.value[0]?.id ?? ''
  } catch (cause) {
    catalogError.value = agendaErrorMessage(cause, 'Não foi possível carregar os serviços.')
  } finally {
    loadingCatalog.value = false
  }
}

async function loadSlots() {
  const service = selectedService.value
  selectedSlot.value = null
  bookingError.value = ''
  if (!service || !date.value) {
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
  try {
    const result = await agendaService.listAvailableSlots(service.barber.id, service.id, date.value)
    if (current === slotsRequest) slots.value = result
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

async function confirmBooking() {
  const service = selectedService.value
  const slot = selectedSlot.value
  if (!service || !slot || booking.value) return
  booking.value = true
  bookingError.value = ''
  try {
    confirmed.value = await agendaService.createScheduling({ serviceId: service.id, startAt: slot.startAt })
    toast.success('Agendamento confirmado.')
    await loadSlots()
  } catch (cause) {
    const message = agendaErrorMessage(cause, 'Não foi possível confirmar o agendamento. Tente novamente.')
    // Horário pode ter sido ocupado ou bloqueado depois da consulta: atualiza a lista e mantém o aviso.
    await loadSlots()
    bookingError.value = message
  } finally {
    booking.value = false
  }
}

watch([serviceId, date], () => {
  confirmed.value = null
  void loadSlots()
})

onMounted(loadCatalog)
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-8 text-ink" aria-labelledby="booking-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <header class="mb-7">
          <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">AGENDAMENTO</p>
          <h1 id="booking-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Agendar horário</h1>
          <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">Escolha o serviço, o dia e um dos horários livres do barbeiro.</p>
        </header>

        <p v-if="isBarber(auth.user)" class="mb-5 flex items-start gap-2 rounded-xl border border-line bg-white p-4 text-[13px] text-[#55555e]" role="status">
          <Info class="mt-0.5 size-4 shrink-0 text-[#a67039]" aria-hidden="true" />Você está vendo os horários como um cliente vê. Folgas, bloqueios e horários ocupados não aparecem.
        </p>

        <div v-if="catalogError" class="rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
          <p>{{ catalogError }}</p>
          <button type="button" class="mt-2 font-semibold underline" :disabled="loadingCatalog" @click="loadCatalog">Tentar novamente</button>
        </div>
        <p v-else-if="loadingCatalog" class="rounded-2xl border border-line bg-white px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando serviços...</p>
        <div v-else-if="!catalog.length" class="rounded-2xl border border-line bg-white px-5 py-14 text-center" role="status">
          <Scissors class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
          <p class="text-sm font-semibold">Nenhum serviço disponível para agendamento.</p>
        </div>

        <div v-else class="grid gap-5 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)]">
          <fieldset class="min-w-0 rounded-2xl border border-line bg-white p-4 sm:p-5">
            <legend class="sr-only">Serviço</legend>
            <h2 class="mb-3 text-[15px] font-bold">1. Serviço</h2>
            <div v-for="group in servicesByBarber" :key="group.barber.id" class="mb-4 last:mb-0">
              <p class="mb-2 text-[12px] font-semibold text-[#74747f]">com {{ group.barber.name }}</p>
              <div class="grid gap-2">
                <label
                  v-for="service in group.services"
                  :key="service.id"
                  class="flex cursor-pointer items-center justify-between gap-3 rounded-xl border p-3 text-[13px]"
                  :class="serviceId === service.id ? 'border-ink bg-paper' : 'border-line hover:bg-paper'"
                >
                  <span class="flex items-center gap-2.5">
                    <input v-model="serviceId" type="radio" name="booking-service" :value="service.id" class="accent-[#1a1a1c]" />
                    <span>
                      <span class="block font-semibold">{{ service.name }}</span>
                      <span class="flex items-center gap-1 text-[12px] text-[#74747f]"><Clock class="size-3" aria-hidden="true" />{{ service.durationTime }} min</span>
                    </span>
                  </span>
                  <span class="font-semibold">{{ formatPrice(service.price) }}</span>
                </label>
              </div>
            </div>
          </fieldset>

          <div class="min-w-0 rounded-2xl border border-line bg-white p-4 sm:p-5">
            <h2 class="mb-3 text-[15px] font-bold">2. Dia e horário</h2>
            <div class="mb-3 flex gap-2 overflow-x-auto pb-1" role="group" aria-label="Próximos dias">
              <button
                v-for="day in quickDates"
                :key="toIsoDate(day)"
                type="button"
                class="flex min-w-[56px] flex-col items-center rounded-xl border px-2 py-2 text-[12px]"
                :class="date === toIsoDate(day) ? 'border-ink bg-ink text-white' : 'border-line hover:bg-paper'"
                :aria-pressed="date === toIsoDate(day)"
                @click="date = toIsoDate(day)"
              >
                <span class="capitalize">{{ day.toLocaleDateString('pt-BR', { weekday: 'short' }).replace('.', '') }}</span>
                <span class="text-[15px] font-bold">{{ String(day.getDate()).padStart(2, '0') }}</span>
              </button>
            </div>
            <label class="field-label max-w-[240px]" for="booking-date">Outra data
              <input id="booking-date" v-model="date" type="date" :min="today" :max="maxDate" class="field-input" />
            </label>

            <div class="mt-5 flex items-center justify-between gap-3">
              <p class="text-[13px] font-semibold first-letter:uppercase">{{ dateLabel }}</p>
              <button type="button" class="button button-secondary button-sm" :disabled="loadingSlots || !selectedService" @click="loadSlots">
                <RefreshCw class="size-3.5" :class="{ 'animate-spin motion-reduce:animate-none': loadingSlots }" aria-hidden="true" />Atualizar horários
              </button>
            </div>

            <div class="mt-3" :aria-busy="loadingSlots">
              <div v-if="slotsError" class="rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
                <p>{{ slotsError }}</p>
                <button type="button" class="mt-2 font-semibold underline" :disabled="loadingSlots" @click="loadSlots">Tentar novamente</button>
              </div>
              <p v-else-if="loadingSlots" class="py-10 text-center text-sm text-[#74747f]" role="status">Consultando horários disponíveis...</p>
              <div v-else-if="slots.length" class="grid grid-cols-[repeat(auto-fill,minmax(76px,1fr))] gap-2" role="radiogroup" aria-label="Horários disponíveis">
                <button
                  v-for="slot in slots"
                  :key="slot.startAt"
                  type="button"
                  role="radio"
                  class="rounded-xl border py-2 text-[13px] font-semibold"
                  :class="selectedSlot?.startAt === slot.startAt ? 'border-ink bg-ink text-white' : 'border-line hover:bg-paper'"
                  :aria-checked="selectedSlot?.startAt === slot.startAt"
                  @click="selectedSlot = slot; bookingError = ''"
                >{{ isoTimePart(slot.startAt) }}</button>
              </div>
              <div v-else-if="hasLoadedSlots" class="py-10 text-center" role="status">
                <CalendarX class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhum horário disponível neste dia.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">O barbeiro pode estar de folga, com a agenda bloqueada ou sem horários livres. Tente outra data.</p>
              </div>
            </div>

            <div v-if="bookingError" class="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ bookingError }}</div>
            <div v-if="confirmed" class="mt-4 flex items-start gap-2 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900" role="status">
              <CalendarCheck class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
              <p>{{ confirmed.serviceName }} confirmado para {{ parseIsoDate(confirmed.startAt.slice(0, 10)).toLocaleDateString('pt-BR') }}, às {{ isoTimePart(confirmed.startAt) }} ({{ formatPrice(confirmed.finalPrice) }}).</p>
            </div>

            <div v-if="selectedSlot && selectedService" class="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-4">
              <p class="text-[13px] text-[#55555e]">
                <strong class="text-ink">{{ selectedService.name }}</strong> com {{ selectedService.barber.name }} · {{ isoTimePart(selectedSlot.startAt) }} às {{ isoTimePart(selectedSlot.endAt) }}
              </p>
              <button type="button" class="button button-primary" :disabled="booking" @click="confirmBooking">
                {{ booking ? 'Confirmando...' : 'Confirmar agendamento' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>
