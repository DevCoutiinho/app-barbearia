<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import {
  AlertCircle, ArrowLeft, ArrowRight, CalendarDays, Check,
  CheckCircle2, Clock, Loader2, RefreshCw, Scissors, User,
} from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import { useSchedulingStore } from '../stores/scheduling'
import type { AvailableSlot, BarberSummary, ServiceCatalogItem } from '../types/scheduling'

const store = useSchedulingStore()
const router = useRouter()

// Etapa atual: 1=serviço, 2=barbeiro, 3=data/horário, 4=resumo
const step = ref(1)

const money = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

const formatDate = (dateStr: string) => {
  const date = new Date(`${dateStr}T12:00:00`)
  return new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
  }).format(date)
}

const formatTime = (timeStr: string) => timeStr.slice(0, 5) // "HH:mm"

const today = new Date().toISOString().slice(0, 10)

// Carrega catálogo e barbeiros ao montar
onMounted(async () => {
  await Promise.all([store.fetchCatalog(), store.fetchBarbers()])
})

// Recarrega slots ao mudar barbeiro ou data
watch(
  () => [store.selection.barber?.id, store.selection.date],
  ([barberId, date]) => {
    if (barberId && date && store.selection.service) {
      store.fetchSlots()
    }
  },
)

function goBack() {
  if (step.value > 1) step.value--
}

function chooseService(service: ServiceCatalogItem) {
  store.selectService(service)
  step.value = 2
}

function chooseBarber(barber: BarberSummary) {
  store.selectBarber(barber)
  step.value = 3
}

function chooseSlot(slot: AvailableSlot) {
  store.selectSlot(slot)
}

function goToSummary() {
  if (!store.selection.slot) {
    toast.error('Selecione um horário antes de continuar.')
    return
  }
  step.value = 4
}

async function confirm() {
  const result = await store.confirmBooking()

  if (result.success) {
    toast.success(result.message)
    router.push({ name: 'my-schedulings' })
    return
  }

  if (result.conflict) {
    toast.error(result.message)
    step.value = 3
    return
  }

  toast.error(result.message)
}

const stepLabels = ['Serviço', 'Barbeiro', 'Data e horário', 'Confirmação']
</script>

<template>
  <DashboardLayout>
    <div class="px-4 pt-[26px] pb-9 sm:px-[26px]">
      <!-- Cabeçalho -->
      <div class="mb-6">
        <h1 class="text-[24px] font-bold tracking-[-0.8px]">Agendar horário</h1>
        <p class="mt-1 text-sm text-[#74747f]">Escolha o serviço, barbeiro e horário desejados.</p>
      </div>

      <!-- Indicador de etapas -->
      <nav aria-label="Etapas do agendamento" class="mb-8">
        <ol class="flex items-center gap-0">
          <li
            v-for="(label, index) in stepLabels"
            :key="label"
            class="flex items-center"
          >
            <div class="flex flex-col items-center">
              <span
                class="flex size-8 items-center justify-center rounded-full text-xs font-bold transition-colors"
                :class="
                  index + 1 < step
                    ? 'bg-[#1a1a1c] text-white'
                    : index + 1 === step
                      ? 'bg-[#a67039] text-white'
                      : 'bg-[#ededeb] text-[#74747f]'
                "
                :aria-current="index + 1 === step ? 'step' : undefined"
              >
                <Check v-if="index + 1 < step" class="size-4" aria-hidden="true" />
                <span v-else>{{ index + 1 }}</span>
              </span>
              <span
                class="mt-1 hidden text-[10px] sm:block"
                :class="index + 1 === step ? 'font-semibold text-[#a67039]' : 'text-[#74747f]'"
              >{{ label }}</span>
            </div>
            <div
              v-if="index < stepLabels.length - 1"
              class="mx-2 h-px w-8 sm:w-14"
              :class="index + 1 < step ? 'bg-[#1a1a1c]' : 'bg-[#ededeb]'"
              aria-hidden="true"
            />
          </li>
        </ol>
      </nav>

      <!-- ETAPA 1: Escolher serviço -->
      <section v-if="step === 1" aria-labelledby="step-service-title">
        <h2 id="step-service-title" class="mb-4 text-[15px] font-semibold">Escolha o serviço</h2>

        <!-- Aviso de endpoint ausente -->
        <div
          v-if="store.catalogError?.includes('404') || store.catalogError?.includes('não encontrado')"
          class="mb-4 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
          role="alert"
        >
          <AlertCircle class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <div>
            <p class="font-semibold">Catálogo indisponível</p>
            <p class="mt-0.5 text-xs">
              O endpoint de catálogo de serviços para clientes ainda não foi implementado no backend.
              Aguardando o desenvolvedor responsável.
            </p>
          </div>
        </div>

        <div v-if="store.loadingCatalog" class="flex items-center gap-2 text-sm text-[#74747f]">
          <Loader2 class="size-4 animate-spin" aria-hidden="true" />
          Carregando serviços...
        </div>

        <div
          v-else-if="store.catalogError && !store.catalogError.includes('404')"
          class="flex flex-col items-start gap-3"
        >
          <p class="text-sm text-red-600">{{ store.catalogError }}</p>
          <button type="button" class="action-button text-sm" @click="store.fetchCatalog">
            <RefreshCw class="size-4" aria-hidden="true" />Tentar novamente
          </button>
        </div>

        <p
          v-else-if="!store.loadingCatalog && store.catalog.length === 0 && !store.catalogError"
          class="text-sm text-[#74747f]"
        >
          Nenhum serviço disponível no momento.
        </p>

        <ul v-else class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" role="list">
          <li v-for="service in store.catalog" :key="service.id">
            <button
              type="button"
              class="service-card w-full text-left"
              :class="store.selection.service?.id === service.id ? 'ring-2 ring-[#a67039]' : ''"
              :aria-pressed="store.selection.service?.id === service.id"
              @click="chooseService(service)"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="truncate font-semibold text-[14px]">{{ service.name }}</p>
                  <p class="mt-0.5 text-xs text-[#74747f]">{{ service.technique }}</p>
                </div>
                <CheckCircle2
                  v-if="store.selection.service?.id === service.id"
                  class="size-5 shrink-0 text-[#a67039]"
                  aria-hidden="true"
                />
              </div>
              <div class="mt-3 flex items-center justify-between gap-2 text-sm">
                <span class="font-bold text-[#a67039]">{{ money(service.price) }}</span>
                <span class="flex items-center gap-1 text-xs text-[#74747f]">
                  <Clock class="size-3.5" aria-hidden="true" />
                  {{ service.durationTime }} min
                </span>
              </div>
            </button>
          </li>
        </ul>
      </section>

      <!-- ETAPA 2: Escolher barbeiro -->
      <section v-if="step === 2" aria-labelledby="step-barber-title">
        <h2 id="step-barber-title" class="mb-4 text-[15px] font-semibold">Escolha o barbeiro</h2>

        <!-- Aviso de endpoint ausente -->
        <div
          v-if="store.barbersError?.includes('404') || store.barbersError?.includes('não encontrado')"
          class="mb-4 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800"
          role="alert"
        >
          <AlertCircle class="mt-0.5 size-4 shrink-0" aria-hidden="true" />
          <div>
            <p class="font-semibold">Lista de barbeiros indisponível</p>
            <p class="mt-0.5 text-xs">
              O endpoint de listagem de barbeiros para clientes ainda não foi implementado no backend.
              Aguardando o desenvolvedor responsável.
            </p>
          </div>
        </div>

        <div v-if="store.loadingBarbers" class="flex items-center gap-2 text-sm text-[#74747f]">
          <Loader2 class="size-4 animate-spin" aria-hidden="true" />
          Carregando barbeiros...
        </div>

        <div
          v-else-if="store.barbersError && !store.barbersError.includes('404')"
          class="flex flex-col items-start gap-3"
        >
          <p class="text-sm text-red-600">{{ store.barbersError }}</p>
          <button type="button" class="action-button text-sm" @click="store.fetchBarbers">
            <RefreshCw class="size-4" aria-hidden="true" />Tentar novamente
          </button>
        </div>

        <p
          v-else-if="!store.loadingBarbers && store.barbers.length === 0 && !store.barbersError"
          class="text-sm text-[#74747f]"
        >
          Nenhum barbeiro disponível no momento.
        </p>

        <ul v-else class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" role="list">
          <li v-for="barber in store.barbers" :key="barber.id">
            <button
              type="button"
              class="service-card w-full text-left"
              :class="store.selection.barber?.id === barber.id ? 'ring-2 ring-[#a67039]' : ''"
              :aria-pressed="store.selection.barber?.id === barber.id"
              @click="chooseBarber(barber)"
            >
              <div class="flex items-center gap-3">
                <span
                  class="flex size-10 shrink-0 items-center justify-center rounded-full bg-[#1a1a1c] text-sm font-bold text-white"
                  aria-hidden="true"
                >
                  {{ barber.name.trim().split(/\s+/).map(p => p[0]).slice(0, 2).join('') }}
                </span>
                <div class="min-w-0">
                  <p class="truncate font-semibold text-[14px]">{{ barber.name }}</p>
                  <p v-if="barber.bio" class="mt-0.5 line-clamp-2 text-xs text-[#74747f]">{{ barber.bio }}</p>
                </div>
                <CheckCircle2
                  v-if="store.selection.barber?.id === barber.id"
                  class="ml-auto size-5 shrink-0 text-[#a67039]"
                  aria-hidden="true"
                />
              </div>
            </button>
          </li>
        </ul>
      </section>

      <!-- ETAPA 3: Escolher data e horário -->
      <section v-if="step === 3" aria-labelledby="step-datetime-title">
        <h2 id="step-datetime-title" class="mb-4 text-[15px] font-semibold">Escolha a data e o horário</h2>

        <div class="mb-5">
          <label for="booking-date" class="mb-1.5 block text-sm font-medium">Data</label>
          <input
            id="booking-date"
            type="date"
            :min="today"
            :value="store.selection.date ?? ''"
            class="rounded-xl border border-line bg-white px-3 py-2 text-sm text-ink focus:outline-none focus:ring-2 focus:ring-[#a67039]"
            @change="store.selectDate(($event.target as HTMLInputElement).value)"
          />
        </div>

        <div v-if="store.selection.date">
          <p class="mb-3 text-sm font-medium">
            Horários disponíveis em
            <span class="text-[#a67039]">{{ formatDate(store.selection.date) }}</span>
          </p>

          <div v-if="store.loadingSlots" class="flex items-center gap-2 text-sm text-[#74747f]">
            <Loader2 class="size-4 animate-spin" aria-hidden="true" />
            Consultando horários...
          </div>

          <div v-else-if="store.slotsError" class="flex flex-col items-start gap-3">
            <p class="text-sm text-red-600">{{ store.slotsError }}</p>
            <button type="button" class="action-button text-sm" @click="store.fetchSlots">
              <RefreshCw class="size-4" aria-hidden="true" />Tentar novamente
            </button>
          </div>

          <p v-else-if="store.slots.length === 0" class="text-sm text-[#74747f]">
            Nenhum horário disponível para esta data. Tente outro dia.
          </p>

          <div v-else>
            <ul class="flex flex-wrap gap-2" role="list" aria-label="Horários disponíveis">
              <li v-for="slot in store.slots" :key="slot.startTime">
                <button
                  type="button"
                  class="slot-button"
                  :class="
                    store.selection.slot?.startTime === slot.startTime
                      ? 'bg-[#1a1a1c] text-white'
                      : 'bg-white text-ink hover:bg-paper'
                  "
                  :aria-pressed="store.selection.slot?.startTime === slot.startTime"
                  @click="chooseSlot(slot)"
                >
                  {{ formatTime(slot.startTime) }}
                </button>
              </li>
            </ul>
          </div>
        </div>

        <p v-else class="text-sm text-[#74747f]">Selecione uma data para ver os horários disponíveis.</p>
      </section>

      <!-- ETAPA 4: Resumo e confirmação -->
      <section v-if="step === 4" aria-labelledby="step-summary-title">
        <h2 id="step-summary-title" class="mb-4 text-[15px] font-semibold">Resumo do agendamento</h2>

        <div class="max-w-md rounded-2xl border border-line bg-white p-5">
          <dl class="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 text-sm">
            <dt class="flex items-center gap-1.5 text-[#74747f]">
              <Scissors class="size-3.5" aria-hidden="true" />Serviço
            </dt>
            <dd class="font-semibold">{{ store.selection.service?.name }}</dd>

            <dt class="flex items-center gap-1.5 text-[#74747f]">
              <User class="size-3.5" aria-hidden="true" />Barbeiro
            </dt>
            <dd class="font-semibold">{{ store.selection.barber?.name }}</dd>

            <dt class="flex items-center gap-1.5 text-[#74747f]">
              <CalendarDays class="size-3.5" aria-hidden="true" />Data
            </dt>
            <dd class="font-semibold capitalize">
              {{ store.selection.date ? formatDate(store.selection.date) : '—' }}
            </dd>

            <dt class="flex items-center gap-1.5 text-[#74747f]">
              <Clock class="size-3.5" aria-hidden="true" />Horário
            </dt>
            <dd class="font-semibold">
              {{ store.selection.slot ? formatTime(store.selection.slot.startTime) : '—' }}
            </dd>

            <dt class="text-[#74747f]">Preço</dt>
            <dd class="font-bold text-[#a67039]">
              {{ store.selection.service ? money(store.selection.service.price) : '—' }}
            </dd>
          </dl>
        </div>

        <p class="mt-3 text-xs text-[#74747f]">
          O status inicial do agendamento será <strong>Pendente</strong>, conforme definido pelo sistema.
        </p>
      </section>

      <!-- Navegação entre etapas -->
      <div class="mt-8 flex items-center justify-between gap-3">
        <button
          v-if="step > 1"
          type="button"
          class="action-button"
          @click="goBack"
        >
          <ArrowLeft class="size-4" aria-hidden="true" />Voltar
        </button>
        <div v-else />

        <!-- Avançar etapas 1 e 2 (seleção automática ao clicar no card) -->
        <!-- Etapa 3: avançar para resumo -->
        <button
          v-if="step === 3"
          type="button"
          class="action-button border-ink bg-ink text-white hover:bg-[#2c2c2f] disabled:opacity-40"
          :disabled="!store.selection.slot"
          @click="goToSummary"
        >
          Ver resumo<ArrowRight class="size-4" aria-hidden="true" />
        </button>

        <!-- Etapa 4: confirmar -->
        <button
          v-if="step === 4"
          type="button"
          class="action-button border-[#a67039] bg-[#a67039] text-white hover:bg-[#8f5e2e] disabled:opacity-40"
          :disabled="store.submitting"
          @click="confirm"
        >
          <Loader2 v-if="store.submitting" class="size-4 animate-spin" aria-hidden="true" />
          <Check v-else class="size-4" aria-hidden="true" />
          {{ store.submitting ? 'Confirmando...' : 'Confirmar agendamento' }}
        </button>
      </div>
    </div>
  </DashboardLayout>
</template>

<style scoped>
@layer components {
.service-card {
  border: 1px solid var(--color-line);
  border-radius: 14px;
  background: white;
  padding: 14px;
  transition: border-color 150ms, box-shadow 150ms;
}
.service-card:hover { border-color: #a67039; }
.service-card:focus-visible { outline: 2px solid #a67039; outline-offset: 3px; }
.slot-button {
  border: 1px solid var(--color-line);
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  transition: background-color 150ms;
}
.slot-button:focus-visible { outline: 2px solid #a67039; outline-offset: 3px; }
.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid var(--color-line);
  border-radius: 11px;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  background: white;
  transition: background-color 150ms;
}
.action-button:hover { background-color: #ededeb; }
.action-button:focus-visible { outline: 2px solid #a67039; outline-offset: 3px; }
}
</style>
