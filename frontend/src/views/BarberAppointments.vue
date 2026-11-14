<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import {
  ArrowDownToLine,
  Calendar,
  CalendarCheck,
  CalendarDays,
  Check,
  CheckCircle2,
  CircleX,
  Clock,
  DollarSign,
  Eye,
  Loader2,
  RefreshCw,
  Scissors,
  Search,
  User,
  X,
} from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import agendaService from '../services/agendaService'
import type { BarberAppointmentHistory, SchedulingStatus } from '../types/agenda'

const todayIso = new Date().toLocaleDateString('en-CA')
const selectedDate = ref(todayIso)
const selectedStatus = ref<SchedulingStatus | ''>('')
const search = ref('')

const appointments = ref<BarberAppointmentHistory[]>([])
const loading = ref(false)
const error = ref('')
const completingId = ref<string | null>(null)

const selectedAppointment = ref<BarberAppointmentHistory | null>(null)
const detailsDialog = ref<HTMLDialogElement | null>(null)

const money = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

const normalize = (value: string) =>
  value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

const initials = (name: string) =>
  name.trim().split(/\s+/).map(p => p[0]).slice(0, 2).join('')

function formatTimeRange(startIso: string, endIso?: string): string {
  if (!startIso) return '—'
  try {
    const start = new Date(startIso)
    const startTime = start.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
    if (endIso) {
      const end = new Date(endIso)
      const endTime = end.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
      return `${startTime} - ${endTime}`
    }
    return startTime
  } catch {
    return startIso
  }
}

const statusBadges: Record<SchedulingStatus, { label: string; class: string }> = {
  SCHEDULED: { label: 'Agendado', class: 'bg-[#e5f2ec] text-[#28835e]' },
  PENDING: { label: 'Pendente', class: 'bg-[#fbf0db] text-[#b57d19]' },
  COMPLETED: { label: 'Finalizado', class: 'bg-emerald-50 text-emerald-700 border border-emerald-200' },
  CANCELED: { label: 'Cancelado', class: 'bg-red-50 text-red-700' },
}

const formattedDate = computed(() => {
  if (!selectedDate.value) return 'Selecione uma data'
  const date = new Date(`${selectedDate.value}T12:00:00`)
  return new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(date)
})

// Filtra a lista completa em memória por status e busca de texto
const filteredAppointments = computed(() => {
  return appointments.value.filter(item => {
    const matchesStatus = !selectedStatus.value || item.status === selectedStatus.value
    const term = normalize(search.value.trim())
    const matchesSearch = !term || normalize(`${item.clientName} ${item.serviceName}`).includes(term)
    return matchesStatus && matchesSearch
  })
})

// Métricas de resumo do dia
const totalCount = computed(() => appointments.value.length)
const completedCount = computed(() => appointments.value.filter(i => i.status === 'COMPLETED').length)
const canceledCount = computed(() => appointments.value.filter(i => i.status === 'CANCELED').length)
const scheduledCount = computed(() => appointments.value.filter(i => i.status === 'SCHEDULED' || i.status === 'PENDING').length)
const totalRevenue = computed(() =>
  appointments.value
    .filter(i => i.status === 'COMPLETED')
    .reduce((sum, i) => sum + (Number(i.price) || 0), 0)
)

async function loadAppointments() {
  loading.value = true
  error.value = ''
  try {
    const data = await agendaService.listBarberDaily(selectedDate.value)
    appointments.value = data
  } catch (err: any) {
    const msg = err?.response?.data?.message || 'Erro ao carregar os agendamentos do dia.'
    error.value = msg
    toast.error(msg)
    appointments.value = []
  } finally {
    loading.value = false
  }
}

async function handleCompleteScheduling(item: BarberAppointmentHistory) {
  if (completingId.value) return
  const confirmed = window.confirm(`Deseja finalizar o atendimento de ${item.clientName}?`)
  if (!confirmed) return

  completingId.value = item.id
  try {
    await agendaService.completeScheduling(item.id)
    toast.success(`Atendimento de ${item.clientName} finalizado com sucesso!`)
    await loadAppointments()
  } catch (err: any) {
    const msg = err?.response?.data?.message || 'Não foi possível finalizar o atendimento.'
    toast.error(msg)
  } finally {
    completingId.value = null
  }
}

function openDetails(item: BarberAppointmentHistory) {
  selectedAppointment.value = item
  nextTick(() => {
    detailsDialog.value?.showModal()
  })
}

function selectToday() {
  selectedDate.value = todayIso
}

function exportAppointments() {
  const rows = [
    ['Data', 'Horário Início', 'Horário Fim', 'Cliente', 'Serviço', 'Duração (min)', 'Valor (R$)', 'Status'],
    ...filteredAppointments.value.map(item => [
      selectedDate.value,
      formatTimeRange(item.scheduledAt),
      item.endsAt ? formatTimeRange(item.endsAt) : '',
      item.clientName,
      item.serviceName,
      item.durationTime,
      Number(item.price || 0).toFixed(2).replace('.', ','),
      statusBadges[item.status]?.label || item.status,
    ]),
  ]
  const csv = '\uFEFF' + rows.map(row => row.map(cell => `"${String(cell).replaceAll('"', '""')}"`).join(';')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `agendamentos-completos-${selectedDate.value}.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

watch(selectedDate, () => {
  loadAppointments()
})

onMounted(() => {
  loadAppointments()
})
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-12 text-ink min-h-[calc(100vh-62px)]" aria-labelledby="appointments-title">
      <div class="mx-auto max-w-[1280px] px-4 py-7 sm:px-[26px] lg:py-9">
        <!-- Cabeçalho -->
        <header class="mb-7 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.5px] text-[#a67039] uppercase">Atendimentos</p>
            <h1 id="appointments-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">
              Agendamentos do Dia
            </h1>
            <p class="mt-1 text-[14px] text-[#74747f]">
              <span class="capitalize">{{ formattedDate }}</span> · Todos os horários agendados para você.
            </p>
          </div>

          <div class="flex flex-wrap items-center gap-2.5">
            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-xl border border-line bg-white px-4 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-paper transition-colors"
              :class="selectedDate === todayIso ? 'border-[#a67039] text-[#a67039] bg-[#fbf0db]/40' : ''"
              @click="selectToday"
            >
              <CalendarDays class="size-4" />
              Hoje
            </button>

            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-xl border border-line bg-white px-4 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-paper transition-colors"
              :disabled="loading"
              @click="loadAppointments"
            >
              <RefreshCw class="size-4" :class="{ 'animate-spin': loading }" aria-hidden="true" />
              Atualizar
            </button>

            <button
              type="button"
              class="inline-flex items-center gap-2 rounded-xl border border-line bg-white px-4 py-2 text-xs font-semibold text-ink shadow-2xs hover:bg-paper transition-colors disabled:opacity-40"
              :disabled="!filteredAppointments.length"
              @click="exportAppointments"
            >
              <ArrowDownToLine class="size-4" aria-hidden="true" />
              Exportar CSV
            </button>
          </div>
        </header>

        <!-- Cards de Resumo Rápido -->
        <div class="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Total do Dia</span>
              <Calendar class="size-4 text-[#a67039]" />
            </div>
            <p class="mt-2 text-2xl font-bold text-ink">{{ totalCount }}</p>
            <span class="text-[11px] text-[#74747f]">Atendimentos marcados</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Aguardando</span>
              <Clock class="size-4 text-[#b57d19]" />
            </div>
            <p class="mt-2 text-2xl font-bold text-[#b57d19]">{{ scheduledCount }}</p>
            <span class="text-[11px] text-[#74747f]">Agendados ou pendentes</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Concluídos</span>
              <CalendarCheck class="size-4 text-[#28835e]" />
            </div>
            <p class="mt-2 text-2xl font-bold text-[#28835e]">{{ completedCount }}</p>
            <span class="text-[11px] text-[#74747f]">Atendimentos finalizados</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Faturamento Estimado</span>
              <DollarSign class="size-4 text-[#a67039]" />
            </div>
            <p class="mt-2 text-2xl font-bold text-ink">{{ money(totalRevenue) }}</p>
            <span class="text-[11px] text-[#74747f]">Concluídos nesta data</span>
          </div>
        </div>

        <!-- Filtros de Busca -->
        <div class="mb-6 rounded-2xl border border-line bg-white p-4 shadow-2xs">
          <div class="grid gap-3 sm:grid-cols-3">
            <!-- Data -->
            <div>
              <label class="block text-xs font-semibold text-[#74747f] mb-1">Data</label>
              <input
                v-model="selectedDate"
                type="date"
                class="w-full rounded-xl border border-line bg-paper px-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
              />
            </div>

            <!-- Status -->
            <div>
              <label class="block text-xs font-semibold text-[#74747f] mb-1">Status</label>
              <select
                v-model="selectedStatus"
                class="w-full rounded-xl border border-line bg-paper px-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
              >
                <option value="">Todos os status</option>
                <option value="SCHEDULED">Agendado</option>
                <option value="PENDING">Pendente</option>
                <option value="COMPLETED">Finalizado</option>
                <option value="CANCELED">Cancelado</option>
              </select>
            </div>

            <!-- Busca por Cliente ou Serviço -->
            <div>
              <label class="block text-xs font-semibold text-[#74747f] mb-1">Buscar cliente ou serviço</label>
              <div class="relative">
                <Search class="absolute left-3 top-2.5 size-3.5 text-[#74747f]" />
                <input
                  v-model="search"
                  type="text"
                  placeholder="Nome do cliente ou serviço..."
                  class="w-full rounded-xl border border-line bg-paper pl-9 pr-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Tabela Completa de Agendamentos -->
        <div class="overflow-hidden rounded-2xl border border-line bg-white shadow-2xs">
          <!-- Loading -->
          <div v-if="loading" class="flex flex-col items-center justify-center py-20 text-center">
            <Loader2 class="size-8 animate-spin text-[#a67039]" />
            <p class="mt-3 text-sm font-medium text-ink">Carregando agendamentos...</p>
            <p class="mt-1 text-xs text-[#74747f]">Consultando os dados no servidor</p>
          </div>

          <!-- Erro -->
          <div v-else-if="error" class="p-8 text-center text-red-600">
            <p class="font-semibold">{{ error }}</p>
            <button
              type="button"
              class="mt-3 inline-flex items-center gap-2 rounded-xl bg-ink px-4 py-2 text-xs font-semibold text-white"
              @click="loadAppointments"
            >
              Tentar novamente
            </button>
          </div>

          <!-- Tabela -->
          <div v-else class="relative overflow-x-auto">
            <table class="w-full min-w-[860px] whitespace-nowrap text-left text-[13px]">
              <caption class="sr-only">Todos os agendamentos em {{ selectedDate }}</caption>
              <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
                <tr>
                  <th scope="col" class="py-3 px-5">Cliente</th>
                  <th scope="col" class="py-3 px-4">Horário</th>
                  <th scope="col" class="py-3 px-4">Serviço</th>
                  <th scope="col" class="py-3 px-4">Preço</th>
                  <th scope="col" class="py-3 px-4">Status</th>
                  <th scope="col" class="py-3 pr-6 text-right">Ações</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-line/70">
                <tr
                  v-for="item in filteredAppointments"
                  :key="item.id"
                  class="hover:bg-paper/40 transition-colors"
                >
                  <!-- Cliente -->
                  <td class="py-3.5 px-5">
                    <div class="flex items-center gap-3">
                      <span class="flex size-9 items-center justify-center rounded-xl bg-[#45454d] text-xs font-bold text-white">
                        {{ initials(item.clientName) }}
                      </span>
                      <div>
                        <p class="font-semibold text-ink">{{ item.clientName }}</p>
                        <p class="text-[11px] text-[#74747f]">#{{ item.id.slice(0, 8) }}</p>
                      </div>
                    </div>
                  </td>

                  <!-- Horário -->
                  <td class="py-3.5 px-4">
                    <div class="flex items-center gap-1.5 font-semibold text-ink">
                      <Clock class="size-3.5 text-[#a67039]" />
                      <span>{{ formatTimeRange(item.scheduledAt, item.endsAt) }}</span>
                    </div>
                  </td>

                  <!-- Serviço -->
                  <td class="py-3.5 px-4">
                    <div>
                      <p class="font-medium text-ink">{{ item.serviceName }}</p>
                      <p class="text-[11px] text-[#74747f]">{{ item.durationTime }} min</p>
                    </div>
                  </td>

                  <!-- Preço -->
                  <td class="py-3.5 px-4">
                    <span class="font-semibold text-ink">{{ money(item.price) }}</span>
                  </td>

                  <!-- Status -->
                  <td class="py-3.5 px-4">
                    <span
                      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] leading-[15px] font-semibold"
                      :class="statusBadges[item.status]?.class || 'bg-gray-100 text-gray-700'"
                    >
                      <span class="size-1.5 rounded-full bg-current"></span>
                      {{ statusBadges[item.status]?.label || item.status }}
                    </span>
                  </td>

                  <!-- Ações -->
                  <td class="py-3.5 pr-6 text-right">
                    <div class="inline-flex items-center justify-end gap-2">
                      <!-- Botão Finalizar Serviço -->
                      <button
                        v-if="item.status !== 'COMPLETED' && item.status !== 'CANCELED'"
                        type="button"
                        class="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm hover:bg-emerald-700 active:scale-95 transition-all disabled:opacity-50"
                        :disabled="completingId === item.id"
                        title="Finalizar este atendimento"
                        @click="handleCompleteScheduling(item)"
                      >
                        <Loader2 v-if="completingId === item.id" class="size-3.5 animate-spin" />
                        <Check v-else class="size-3.5" />
                        Finalizar serviço
                      </button>

                      <!-- Badge Concluído -->
                      <span
                        v-else-if="item.status === 'COMPLETED'"
                        class="inline-flex items-center gap-1 rounded-md bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700 border border-emerald-200"
                      >
                        <CheckCircle2 class="size-3.5 text-emerald-600" />
                        Concluído
                      </span>

                      <!-- Badge Cancelado -->
                      <span
                        v-else-if="item.status === 'CANCELED'"
                        class="inline-flex items-center gap-1 rounded-md bg-red-50 px-2 py-1 text-xs font-medium text-red-700"
                      >
                        <CircleX class="size-3.5 text-red-500" />
                        Cancelado
                      </span>

                      <!-- Botão Detalhes -->
                      <button
                        type="button"
                        class="inline-flex size-8 items-center justify-center rounded-lg border border-line bg-white text-[#55555e] hover:bg-paper transition-colors"
                        :aria-label="`Ver agendamento de ${item.clientName}`"
                        title="Ver detalhes"
                        @click="openDetails(item)"
                      >
                        <Eye class="size-3.5" />
                      </button>
                    </div>
                  </td>
                </tr>

                <!-- Empty State -->
                <tr v-if="!filteredAppointments.length">
                  <td colspan="6" class="py-16 text-center text-sm text-[#74747f]">
                    <div class="flex flex-col items-center justify-center">
                      <CalendarDays class="size-10 text-[#d8c3a9] mb-3" />
                      <p class="font-semibold text-ink text-base">Nenhum agendamento encontrado.</p>
                      <p class="text-xs text-[#74747f] mt-1">
                        Não há atendimentos para os filtros ou data selecionada.
                      </p>
                      <button
                        v-if="selectedDate !== todayIso || selectedStatus || search"
                        type="button"
                        class="mt-3 text-xs font-semibold text-[#a67039] hover:underline"
                        @click="selectedDate = todayIso; selectedStatus = ''; search = ''"
                      >
                        Limpar filtros e ver hoje
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>

    <!-- Modal de Detalhes -->
    <dialog
      ref="detailsDialog"
      class="m-auto w-[calc(100%-32px)] max-w-md rounded-2xl border border-line bg-white p-6 text-ink shadow-xl backdrop:bg-black/30"
      aria-labelledby="dialog-title"
    >
      <div class="flex items-center justify-between gap-3">
        <h2 id="dialog-title" class="text-lg font-semibold">Detalhes do agendamento</h2>
        <button
          type="button"
          class="inline-flex size-8 items-center justify-center rounded-lg border border-line bg-white text-[#55555e] hover:bg-paper"
          aria-label="Fechar detalhes"
          @click="detailsDialog?.close()"
        >
          <X class="size-4" />
        </button>
      </div>

      <dl v-if="selectedAppointment" class="mt-5 grid grid-cols-[auto_1fr] gap-x-5 gap-y-3 text-sm">
        <dt class="text-[#74747f]">Cliente</dt>
        <dd class="font-semibold text-ink">{{ selectedAppointment.clientName }}</dd>
        <dt class="text-[#74747f]">Serviço</dt>
        <dd>{{ selectedAppointment.serviceName }}</dd>
        <dt class="text-[#74747f]">Horário</dt>
        <dd>{{ formatTimeRange(selectedAppointment.scheduledAt, selectedAppointment.endsAt) }}</dd>
        <dt class="text-[#74747f]">Duração</dt>
        <dd>{{ selectedAppointment.durationTime }} minutos</dd>
        <dt class="text-[#74747f]">Preço</dt>
        <dd class="font-semibold text-ink">{{ money(selectedAppointment.price) }}</dd>
        <dt class="text-[#74747f]">Status</dt>
        <dd>
          <span
            class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold"
            :class="statusBadges[selectedAppointment.status]?.class || 'bg-gray-100 text-gray-700'"
          >
            {{ statusBadges[selectedAppointment.status]?.label || selectedAppointment.status }}
          </span>
        </dd>
      </dl>

      <div
        v-if="selectedAppointment && selectedAppointment.status !== 'COMPLETED' && selectedAppointment.status !== 'CANCELED'"
        class="mt-6 flex justify-end gap-2 border-t border-line pt-4"
      >
        <button
          type="button"
          class="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-xs font-semibold text-white shadow-sm hover:bg-emerald-700"
          :disabled="completingId === selectedAppointment.id"
          @click="handleCompleteScheduling(selectedAppointment); detailsDialog?.close()"
        >
          <Check class="size-4" />
          Finalizar serviço agora
        </button>
      </div>
    </dialog>
  </DashboardLayout>
</template>

