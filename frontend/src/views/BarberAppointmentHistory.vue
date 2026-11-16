<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  Calendar,
  CalendarCheck,
  CalendarX,
  CheckCircle2,
  Clock,
  DollarSign,
  History,
  RefreshCw,
  Search,
  User,
  X,
} from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import agendaService from '../services/agendaService'
import type { BarberAppointmentHistory, SchedulingStatus } from '../types/agenda'
import { SCHEDULING_STATUS_CONFIG } from '../utils/agenda'
import { agendaErrorMessage } from '../utils/agendaErrors'

const items = ref<BarberAppointmentHistory[]>([])
const loading = ref(false)
const error = ref('')

// Filtros
const startDate = ref('')
const endDate = ref('')
const clientName = ref('')
const selectedStatus = ref<SchedulingStatus | ''>('')

const money = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

const formatDate = (iso: string) => {
  if (!iso) return ''
  const date = new Date(iso)
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    const filters: Record<string, any> = {}
    if (startDate.value) filters.startDate = startDate.value
    if (endDate.value) filters.endDate = endDate.value
    if (clientName.value.trim()) filters.clientName = clientName.value.trim()
    if (selectedStatus.value) filters.status = selectedStatus.value

    const response = await agendaService.listBarberHistory(filters)
    // Se a resposta for paginada (Page do Spring), extrai o content
    if (response && Array.isArray(response.content)) {
      items.value = response.content
    } else if (Array.isArray(response)) {
      items.value = response
    } else {
      items.value = []
    }
  } catch (cause) {
    error.value = agendaErrorMessage(cause, 'Não foi possível carregar o histórico de atendimentos.')
  } finally {
    loading.value = false
  }
}

function clearFilters() {
  startDate.value = ''
  endDate.value = ''
  clientName.value = ''
  selectedStatus.value = ''
  void loadHistory()
}

// Métricas de resumo do período filtrado
const totalAppointments = computed(() => items.value.length)
const totalRevenue = computed(() =>
  items.value
    .filter(i => i.status === 'COMPLETED')
    .reduce((sum, i) => sum + (Number(i.price) || 0), 0),
)
const completedCount = computed(() => items.value.filter(i => i.status === 'COMPLETED').length)
const canceledCount = computed(() => items.value.filter(i => i.status === 'CANCELED').length)

// Recarrega ao alterar filtros de datas ou status
watch([startDate, endDate, selectedStatus], () => {
  void loadHistory()
})

// Debounce simples para a busca por nome de cliente
let searchTimeout: ReturnType<typeof setTimeout>
function onSearchInput() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    void loadHistory()
  }, 400)
}

onMounted(loadHistory)
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-12 text-ink min-h-[calc(100vh-62px)]" aria-labelledby="history-title">
      <div class="mx-auto max-w-[1280px] px-4 py-7 sm:px-[26px] lg:py-9">
        <!-- Cabeçalho -->
        <header class="mb-7 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.5px] text-[#a67039] uppercase">Atendimentos</p>
            <h1 id="history-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Histórico de Atendimentos</h1>
            <p class="mt-1 text-[14px] text-[#74747f]">
              Consulte e filtre todos os agendamentos atribuídos a você na barbearia.
            </p>
          </div>
          <button
            type="button"
            class="button button-secondary button-sm"
            :disabled="loading"
            @click="loadHistory"
          >
            <RefreshCw class="size-3.5" :class="{ 'animate-spin': loading }" aria-hidden="true" />
            Atualizar
          </button>
        </header>

        <!-- Cards de resumo rápido -->
        <div class="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Atendimentos Filtrados</span>
              <Calendar class="size-4 text-[#a67039]" />
            </div>
            <p class="mt-2 text-2xl font-bold text-ink">{{ totalAppointments }}</p>
            <span class="text-[11px] text-[#74747f]">No período selecionado</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Faturamento Estimado</span>
              <DollarSign class="size-4 text-emerald-600" />
            </div>
            <p class="mt-2 text-2xl font-bold text-emerald-700">{{ money(totalRevenue) }}</p>
            <span class="text-[11px] text-[#74747f]">Confirmados e concluídos</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Concluídos</span>
              <CheckCircle2 class="size-4 text-blue-600" />
            </div>
            <p class="mt-2 text-2xl font-bold text-ink">{{ completedCount }}</p>
            <span class="text-[11px] text-[#74747f]">Serviços já realizados</span>
          </div>

          <div class="rounded-2xl border border-line bg-white p-4 shadow-2xs">
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold text-[#74747f]">Cancelados</span>
              <CalendarX class="size-4 text-red-500" />
            </div>
            <p class="mt-2 text-2xl font-bold text-ink">{{ canceledCount }}</p>
            <span class="text-[11px] text-[#74747f]">Horários liberados</span>
          </div>
        </div>

        <!-- Barra de Filtros -->
        <div class="mb-6 rounded-2xl border border-line bg-white p-4 sm:p-5 shadow-2xs">
          <div class="mb-3 flex items-center justify-between">
            <h2 class="text-sm font-bold text-ink">Filtros de Busca</h2>
            <button
              v-if="startDate || endDate || clientName || selectedStatus"
              type="button"
              class="inline-flex items-center gap-1 text-xs font-semibold text-[#a67039] hover:underline"
              @click="clearFilters"
            >
              <X class="size-3.5" /> Limpar filtros
            </button>
          </div>

          <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <!-- Data Início -->
            <label class="block text-xs font-medium text-[#505055]">
              Data Inicial
              <input
                v-model="startDate"
                type="date"
                class="mt-1 block w-full rounded-xl border border-line bg-paper px-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
              />
            </label>

            <!-- Data Fim -->
            <label class="block text-xs font-medium text-[#505055]">
              Data Final
              <input
                v-model="endDate"
                type="date"
                class="mt-1 block w-full rounded-xl border border-line bg-paper px-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
              />
            </label>

            <!-- Nome do Cliente -->
            <label class="block text-xs font-medium text-[#505055]">
              Nome do Cliente
              <div class="relative mt-1">
                <input
                  v-model="clientName"
                  type="text"
                  placeholder="Buscar por cliente..."
                  class="block w-full rounded-xl border border-line bg-paper pl-8 pr-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
                  @input="onSearchInput"
                />
                <Search class="pointer-events-none absolute left-2.5 top-2.5 size-3.5 text-[#74747f]" />
              </div>
            </label>

            <!-- Status -->
            <label class="block text-xs font-medium text-[#505055]">
              Status
              <select
                v-model="selectedStatus"
                class="mt-1 block w-full rounded-xl border border-line bg-paper px-3 py-2 text-xs text-ink outline-none focus:border-[#a67039]"
              >
                <option value="">Todos (Finalizados e Cancelados)</option>
                <option value="COMPLETED">Finalizado</option>
                <option value="CANCELED">Cancelado</option>
              </select>
            </label>
          </div>
        </div>

        <!-- Estado de Erro -->
        <div v-if="error" class="mb-6 rounded-xl bg-red-50 p-4 text-xs text-red-800" role="alert">
          <p>{{ error }}</p>
          <button type="button" class="mt-2 font-semibold underline" @click="loadHistory">Tentar novamente</button>
        </div>

        <!-- Tabela de Histórico -->
        <div class="rounded-2xl border border-line bg-white shadow-2xs overflow-hidden">
          <div class="relative overflow-x-auto">
            <table class="w-full text-left text-xs whitespace-nowrap">
              <thead class="border-b border-line bg-paper/60 text-[11px] font-semibold text-[#74747f] uppercase tracking-wider">
                <tr>
                  <th scope="col" class="px-5 py-3.5">Cliente</th>
                  <th scope="col" class="px-5 py-3.5">Serviço</th>
                  <th scope="col" class="px-5 py-3.5">Data e Horário</th>
                  <th scope="col" class="px-5 py-3.5">Duração</th>
                  <th scope="col" class="px-5 py-3.5">Valor (Preço)</th>
                  <th scope="col" class="px-5 py-3.5">Status</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-line/60">
                <tr v-if="loading">
                  <td colspan="6" class="px-5 py-14 text-center text-[#74747f]">
                    <div class="flex flex-col items-center justify-center gap-2">
                      <RefreshCw class="size-5 animate-spin text-[#a67039]" />
                      <span>Carregando histórico de atendimentos...</span>
                    </div>
                  </td>
                </tr>

                <tr v-else-if="!items.length">
                  <td colspan="6" class="px-5 py-14 text-center text-[#74747f]">
                    <History class="mx-auto mb-2 size-7 text-[#a67039]/60" />
                    <p class="font-semibold text-ink">Nenhum atendimento encontrado</p>
                    <p class="mt-0.5 text-xs text-[#74747f]">
                      Ajuste os filtros de data, status ou nome de cliente acima.
                    </p>
                  </td>
                </tr>

                <tr
                  v-for="item in items"
                  :key="item.id"
                  class="hover:bg-paper/40 transition-colors"
                >
                  <!-- Nome do Cliente -->
                  <td class="px-5 py-4 font-semibold text-ink">
                    <div class="flex items-center gap-2">
                      <span class="flex size-7 items-center justify-center rounded-lg bg-[#45454d] text-[10px] font-bold text-white">
                        {{ item.clientName ? item.clientName.slice(0, 2).toUpperCase() : 'CL' }}
                      </span>
                      <span>{{ item.clientName }}</span>
                    </div>
                  </td>

                  <!-- Serviço -->
                  <td class="px-5 py-4 text-[#505055]">
                    {{ item.serviceName }}
                  </td>

                  <!-- Data e Horário -->
                  <td class="px-5 py-4 text-ink font-medium">
                    {{ formatDate(item.scheduledAt) }}
                  </td>

                  <!-- Duração -->
                  <td class="px-5 py-4 text-[#74747f]">
                    <span class="inline-flex items-center gap-1">
                      <Clock class="size-3" />
                      {{ item.durationTime }} min
                    </span>
                  </td>

                  <!-- Valor (Preço) -->
                  <td class="px-5 py-4 font-semibold text-ink">
                    {{ money(Number(item.price) || 0) }}
                  </td>

                  <!-- Status -->
                  <td class="px-5 py-4">
                    <span
                      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold"
                      :class="SCHEDULING_STATUS_CONFIG[item.status]?.badgeClass ?? 'bg-gray-100 text-gray-700'"
                    >
                      <span class="size-1.5 rounded-full bg-current"></span>
                      {{ SCHEDULING_STATUS_CONFIG[item.status]?.label ?? item.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>

