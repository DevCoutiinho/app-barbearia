<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AlertTriangle,
  Calendar,
  CalendarCheck,
  CalendarDays,
  Clock,
  History,
  Plus,
  RefreshCw,
  RotateCcw,
} from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import RescheduleModal from '../components/agenda/RescheduleModal.vue'
import agendaService from '../services/agendaService'
import type { Scheduling } from '../types/agenda'
import {
  formatPrice,
  formatSchedulingDateTime,
  isPastScheduling,
  isoTimePart,
  SCHEDULING_STATUS_CONFIG,
} from '../utils/agenda'
import { agendaErrorMessage } from '../utils/agendaErrors'

const router = useRouter()

const schedulings = ref<Scheduling[]>([])
const loading = ref(false)
const error = ref('')

// Controle de abas
const activeTab = ref<'upcoming' | 'past'>('upcoming')

// Estado do modal de remarcação
const schedulingToReschedule = ref<Scheduling | null>(null)

// Estado do modal de cancelamento
const schedulingToCancel = ref<Scheduling | null>(null)
const canceling = ref(false)
const cancelError = ref('')

async function loadSchedulings() {
  loading.value = true
  error.value = ''
  try {
    schedulings.value = await agendaService.listMySchedulings()
  } catch (cause) {
    error.value = agendaErrorMessage(cause, 'Não foi possível carregar seus agendamentos.')
  } finally {
    loading.value = false
  }
}

const upcomingSchedulings = computed(() => {
  return schedulings.value.filter(
    (s) => s.status !== 'CANCELED' && s.status !== 'COMPLETED' && !isPastScheduling(s.startAt)
  )
})

const pastSchedulings = computed(() => {
  return schedulings.value.filter(
    (s) => s.status === 'CANCELED' || s.status === 'COMPLETED' || isPastScheduling(s.startAt)
  )
})

function canModify(s: Scheduling): boolean {
  if (s.status === 'CANCELED' || s.status === 'COMPLETED') return false
  return !isPastScheduling(s.startAt)
}

function openReschedule(s: Scheduling) {
  if (!canModify(s)) {
    toast.error('Não é possível alterar ou cancelar um agendamento passado ou finalizado.')
    return
  }
  schedulingToReschedule.value = s
}

function openCancel(s: Scheduling) {
  if (!canModify(s)) {
    toast.error('Não é possível alterar ou cancelar um agendamento passado ou finalizado.')
    return
  }
  schedulingToCancel.value = s
  cancelError.value = ''
}

async function confirmCancel() {
  if (!schedulingToCancel.value || canceling.value) return
  canceling.value = true
  cancelError.value = ''

  try {
    const updated = await agendaService.cancelScheduling(schedulingToCancel.value.id)
    // Atualiza o item na lista local
    const index = schedulings.value.findIndex((s) => s.id === updated.id)
    if (index !== -1) {
      schedulings.value[index] = updated
    }
    toast.success('Agendamento cancelado com sucesso. O horário foi liberado.')
    schedulingToCancel.value = null
  } catch (cause) {
    cancelError.value = agendaErrorMessage(
      cause,
      'Não foi possível cancelar o agendamento. Pode ser que já tenha passado ou sido finalizado.'
    )
  } finally {
    canceling.value = false
  }
}

function handleRescheduled(updated: Scheduling) {
  const index = schedulings.value.findIndex((s) => s.id === updated.id)
  if (index !== -1) {
    schedulings.value[index] = updated
  }
}

function repeatService(s: Scheduling) {
  if (!s.serviceId) {
    toast.error('Identificador do serviço não encontrado.')
    return
  }
  router.push({
    path: '/agendar',
    query: { serviceId: s.serviceId }
  })
}

onMounted(loadSchedulings)
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-12 text-ink" aria-labelledby="my-appointments-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <!-- Cabeçalho -->
        <div class="mb-7 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.5px] text-[#a67039] uppercase">Área do Cliente</p>
            <h1 id="my-appointments-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Meus Agendamentos</h1>
            <p class="mt-1 text-[14px] text-[#74747f]">Acompanhe, remarque ou cancele seus atendimentos com autonomia.</p>
          </div>
          <div class="flex items-center gap-3">
            <button
              type="button"
              class="button button-secondary button-sm"
              :disabled="loading"
              @click="loadSchedulings"
            >
              <RefreshCw class="size-3.5" :class="{ 'animate-spin': loading }" /> Atualizar
            </button>
            <button
              type="button"
              class="button button-primary button-sm"
              @click="router.push('/agendar')"
            >
              <Plus class="size-4" /> Novo Agendamento
            </button>
          </div>
        </div>

        <!-- Abas -->
        <div class="mb-6 flex border-b border-line">
          <button
            type="button"
            class="flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-semibold transition"
            :class="activeTab === 'upcoming' ? 'border-ink text-ink font-bold' : 'border-transparent text-[#74747f] hover:text-ink'"
            @click="activeTab = 'upcoming'"
          >
            <CalendarCheck class="size-4" />
            Próximos ({{ upcomingSchedulings.length }})
          </button>
          <button
            type="button"
            class="flex items-center gap-2 border-b-2 px-5 py-3 text-sm font-semibold transition"
            :class="activeTab === 'past' ? 'border-ink text-ink font-bold' : 'border-transparent text-[#74747f] hover:text-ink'"
            @click="activeTab = 'past'"
          >
            <History class="size-4" />
            Histórico ({{ pastSchedulings.length }})
          </button>
        </div>

        <!-- Estado de Carregamento e Erro -->
        <div v-if="error" class="rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
          <p>{{ error }}</p>
          <button type="button" class="mt-2 font-semibold underline" @click="loadSchedulings">Tentar novamente</button>
        </div>

        <div v-else-if="loading" class="rounded-2xl border border-line bg-white px-5 py-16 text-center text-sm text-[#74747f]" role="status">
          <RefreshCw class="mx-auto mb-3 size-6 animate-spin text-[#a67039]" />
          Carregando seus agendamentos...
        </div>

        <!-- Lista Vazia -->
        <div
          v-else-if="activeTab === 'upcoming' ? !upcomingSchedulings.length : !pastSchedulings.length"
          class="rounded-2xl border border-line bg-white px-5 py-14 text-center"
        >
          <CalendarDays class="mx-auto mb-3 size-10 text-[#a67039]" :stroke-width="1.5" />
          <h2 class="text-base font-bold text-ink">
            {{ activeTab === 'upcoming' ? 'Você não possui nenhum agendamento próximo.' : 'Nenhum agendamento no histórico.' }}
          </h2>
          <p class="mt-1 text-sm text-[#74747f]">
            {{ activeTab === 'upcoming' ? 'Escolha um serviço e garanta o seu horário na barbearia.' : 'Seus atendimentos concluídos ou cancelados aparecerão aqui.' }}
          </p>
          <button
            v-if="activeTab === 'upcoming'"
            type="button"
            class="button button-primary button-sm mt-5"
            @click="router.push('/agendar')"
          >
            Agendar Agora
          </button>
        </div>

        <!-- Grid de Cards de Agendamentos -->
        <div v-else class="grid gap-4 md:grid-cols-2">
          <article
            v-for="s in (activeTab === 'upcoming' ? upcomingSchedulings : pastSchedulings)"
            :key="s.id"
            class="relative rounded-2xl border border-line bg-white p-5 shadow-xs transition hover:shadow-md"
          >
            <!-- Header do Card -->
            <div class="flex items-start justify-between gap-3">
              <div>
                <span
                  class="inline-flex items-center rounded-md border px-2 py-0.5 text-[11px] font-semibold tracking-wide uppercase"
                  :class="SCHEDULING_STATUS_CONFIG[s.status]?.badgeClass ?? 'bg-gray-100 text-gray-700'"
                >
                  {{ SCHEDULING_STATUS_CONFIG[s.status]?.label ?? s.status }}
                </span>
                <h3 class="mt-2 text-lg font-bold text-ink">{{ s.serviceName }}</h3>
              </div>
              <p class="text-base font-bold text-ink">{{ formatPrice(s.finalPrice) }}</p>
            </div>

            <!-- Dados de Horário e Duração -->
            <div class="mt-4 space-y-2 border-t border-line/60 pt-3 text-xs text-[#55555e]">
              <div class="flex items-center gap-2">
                <Calendar class="size-4 text-[#a67039]" />
                <span class="font-medium text-ink">{{ formatSchedulingDateTime(s.startAt) }}</span>
                <span>até às {{ isoTimePart(s.endAt) }}</span>
              </div>
              <div v-if="s.durationTime" class="flex items-center gap-2 text-[#74747f]">
                <Clock class="size-3.5" />
                <span>Duração do serviço: {{ s.durationTime }} minutos</span>
              </div>
            </div>

            <!-- Informações de Histórico/Remarcação -->
            <div
              v-if="s.rescheduleCount && s.rescheduleCount > 0"
              class="mt-3 rounded-lg bg-amber-50/70 p-2.5 text-[11px] text-amber-900 border border-amber-200/50"
            >
              <p class="font-semibold flex items-center gap-1.5">
                <History class="size-3 text-amber-700" />
                Remarcado {{ s.rescheduleCount }}x
              </p>
              <p v-if="s.previousStartAt" class="mt-0.5 text-amber-800/80">
                Horário anterior: {{ formatSchedulingDateTime(s.previousStartAt) }}
              </p>
            </div>

            <!-- Ações do Cliente -->
            <div v-if="canModify(s)" class="mt-5 flex items-center justify-end gap-2 border-t border-line pt-3">
              <button
                type="button"
                class="rounded-lg border border-red-200 bg-red-50/60 px-3 py-1.5 text-xs font-semibold text-red-700 hover:bg-red-100 transition"
                @click="openCancel(s)"
              >
                Cancelar
              </button>
              <button
                type="button"
                class="button button-secondary button-sm text-xs font-semibold"
                @click="openReschedule(s)"
              >
                Remarcar
              </button>
            </div>

            <!-- Ações no Histórico de Serviços (Repetir Serviço) -->
            <div
              v-else-if="activeTab === 'past'"
              class="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-line pt-3"
            >
              <span class="text-xs text-[#74747f]">
                {{ s.status === 'COMPLETED' ? 'Atendimento concluído' : 'Horário cancelado' }}
              </span>
              <button
                type="button"
                class="inline-flex items-center gap-1.5 rounded-xl border border-[#a67039] bg-[#fbf0db]/60 px-3.5 py-1.5 text-xs font-semibold text-[#945d28] hover:bg-[#fbf0db] hover:border-[#8f5d2b] transition-all shadow-2xs"
                title="Agendar este mesmo serviço novamente"
                @click="repeatService(s)"
              >
                <RotateCcw class="size-3.5" />
                Repetir serviço
              </button>
            </div>

            <div v-else-if="s.status === 'CANCELED'" class="mt-4 border-t border-line pt-2 text-right text-[11px] text-stone-400">
              Horário cancelado e liberado na agenda.
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- Modal de Remarcação Atômica -->
    <RescheduleModal
      :scheduling="schedulingToReschedule"
      @close="schedulingToReschedule = null"
      @rescheduled="handleRescheduled"
    />

    <!-- Modal de Confirmação de Cancelamento -->
    <div
      v-if="schedulingToCancel"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-labelledby="cancel-modal-title"
      @click.self="schedulingToCancel = null"
    >
      <div class="w-full max-w-md rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6">
        <div class="flex items-start gap-3">
          <div class="rounded-xl bg-red-100 p-2.5 text-red-700">
            <AlertTriangle class="size-6" />
          </div>
          <div>
            <h3 id="cancel-modal-title" class="text-base font-bold text-ink">Cancelar Agendamento?</h3>
            <p class="mt-1 text-xs text-[#74747f]">
              Tem certeza que deseja cancelar o agendamento de
              <strong class="text-ink">{{ schedulingToCancel.serviceName }}</strong> para
              {{ formatSchedulingDateTime(schedulingToCancel.startAt) }}?
            </p>
            <p class="mt-2 text-xs text-stone-500">
              O horário será liberado imediatamente para que outros clientes possam reservar.
            </p>
          </div>
        </div>

        <div v-if="cancelError" class="mt-4 rounded-lg bg-red-50 p-3 text-xs font-medium text-red-800" role="alert">
          {{ cancelError }}
        </div>

        <div class="mt-6 flex justify-end gap-3 border-t border-line pt-4">
          <button
            type="button"
            class="button button-secondary button-sm"
            :disabled="canceling"
            @click="schedulingToCancel = null"
          >
            Manter Horário
          </button>
          <button
            type="button"
            class="rounded-xl bg-red-600 px-4 py-2 text-xs font-semibold text-white shadow-xs hover:bg-red-700 disabled:opacity-50 transition"
            :disabled="canceling"
            @click="confirmCancel"
          >
            {{ canceling ? 'Cancelando...' : 'Confirmar Cancelamento' }}
          </button>
        </div>
      </div>
    </div>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>

