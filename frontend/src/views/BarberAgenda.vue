<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { CalendarClock, CalendarOff, ChevronLeft, ChevronRight, Info, Plus, RefreshCw } from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import AgendaWeekGrid from '../components/agenda/AgendaWeekGrid.vue'
import WeeklyScheduleEditor from '../components/agenda/WeeklyScheduleEditor.vue'
import BlockFormDialog from '../components/agenda/BlockFormDialog.vue'
import BlocksList from '../components/agenda/BlocksList.vue'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.vue'
import { useAuthStore } from '../stores/auth'
import agendaService from '../services/agendaService'
import type { BlockData } from '../schemas/agenda.schema'
import type { ScheduleBlock, WeeklySchedule } from '../types/agenda'
import {
  addDays, formatBlockPeriod, formatWeekRange, hasWorkingHours, isBarber, startOfWeek, toIsoDate,
} from '../utils/agenda'
import { agendaErrorMessage } from '../utils/agendaErrors'

type Tab = 'week' | 'schedule' | 'blocks'

// O servidor aceita no máximo 93 dias por consulta de bloqueios.
const UPCOMING_DAYS = 90

const auth = useAuthStore()
const canManage = computed(() => isBarber(auth.user))
const today = toIsoDate(new Date())

const tab = ref<Tab>('week')
const weekStart = ref(startOfWeek(new Date()))

const schedule = ref<WeeklySchedule | null>(null)
const loadingSchedule = ref(false)
const scheduleError = ref('')

const weekBlocks = ref<ScheduleBlock[]>([])
const loadingWeek = ref(false)
const weekError = ref('')
let weekRequest = 0

const upcomingBlocks = ref<ScheduleBlock[]>([])
const loadingUpcoming = ref(false)
const upcomingError = ref('')
const hasLoadedUpcoming = ref(false)

const blockFormOpen = ref(false)
const blockToDelete = ref<ScheduleBlock | null>(null)
const deleting = ref(false)

const loading = computed(() => loadingSchedule.value || loadingWeek.value || loadingUpcoming.value)
const isCurrentWeek = computed(() => toIsoDate(weekStart.value) === toIsoDate(startOfWeek(new Date())))
const scheduleConfigured = computed(() => hasWorkingHours(schedule.value))
const blockFormDate = computed(() => (isCurrentWeek.value ? today : toIsoDate(weekStart.value)))

async function loadSchedule() {
  loadingSchedule.value = true
  scheduleError.value = ''
  try {
    schedule.value = await agendaService.getWorkingHours()
  } catch (cause) {
    scheduleError.value = agendaErrorMessage(cause, 'Não foi possível carregar seu expediente.')
  } finally {
    loadingSchedule.value = false
  }
}

async function loadWeekBlocks() {
  const current = ++weekRequest
  loadingWeek.value = true
  weekError.value = ''
  try {
    const blocks = await agendaService.listBlocks({
      from: toIsoDate(weekStart.value),
      to: toIsoDate(addDays(weekStart.value, 6)),
    })
    if (current === weekRequest) weekBlocks.value = blocks
  } catch (cause) {
    if (current === weekRequest) weekError.value = agendaErrorMessage(cause, 'Não foi possível carregar os bloqueios da semana.')
  } finally {
    if (current === weekRequest) loadingWeek.value = false
  }
}

async function loadUpcomingBlocks() {
  loadingUpcoming.value = true
  upcomingError.value = ''
  try {
    upcomingBlocks.value = await agendaService.listBlocks({ from: today, to: toIsoDate(addDays(new Date(), UPCOMING_DAYS)) })
  } catch (cause) {
    upcomingError.value = agendaErrorMessage(cause, 'Não foi possível carregar seus bloqueios.')
  } finally {
    loadingUpcoming.value = false
    hasLoadedUpcoming.value = true
  }
}

async function refreshBlocks() {
  await Promise.all([loadWeekBlocks(), loadUpcomingBlocks()])
}

async function refresh() {
  if (loading.value) return
  await Promise.all([loadSchedule(), refreshBlocks()])
}

function changeWeek(offset: number) {
  weekStart.value = offset === 0 ? startOfWeek(new Date()) : addDays(weekStart.value, offset * 7)
  void loadWeekBlocks()
}

async function saveSchedule(data: WeeklySchedule) {
  // Erros sobem para o editor, que mostra a mensagem sem descartar o que foi digitado.
  schedule.value = await agendaService.saveWorkingHours(data)
  toast.success('Expediente salvo com sucesso.')
}

async function createBlock(data: BlockData) {
  const block = await agendaService.createBlock(data)
  blockFormOpen.value = false
  toast.success(`Bloqueio cadastrado: ${block.reasonLabel}, ${formatBlockPeriod(block)}.`)
  await refreshBlocks()
}

async function confirmDelete() {
  const block = blockToDelete.value
  if (!block || deleting.value) return
  deleting.value = true
  try {
    await agendaService.deleteBlock(block.id)
    blockToDelete.value = null
    toast.success('Bloqueio excluído. O horário voltou a ficar disponível.')
    await refreshBlocks()
  } catch (cause) {
    toast.error(agendaErrorMessage(cause, 'Não foi possível excluir o bloqueio. Tente novamente.'))
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  if (canManage.value) void refresh()
})
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-8 text-ink" aria-labelledby="agenda-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
          <div>
            <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">MINHA AGENDA</p>
            <h1 id="agenda-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Minha agenda</h1>
            <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">Seu expediente, pausas, folgas e bloqueios.</p>
          </div>
          <div v-if="canManage" class="flex flex-wrap gap-2">
            <button type="button" class="button button-secondary" aria-label="Atualizar agenda" :disabled="loading" @click="refresh">
              <RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading }" aria-hidden="true" />Atualizar
            </button>
            <button type="button" class="button button-primary" @click="blockFormOpen = true">
              <Plus class="size-4" aria-hidden="true" />Novo bloqueio
            </button>
          </div>
        </header>

        <p v-if="!canManage" class="rounded-xl border border-line bg-white p-5 text-sm" role="alert">
          {{ auth.isAuthenticated ? 'A agenda está disponível apenas para barbeiros.' : 'Faça login para acessar sua agenda.' }}
        </p>

        <template v-else>
          <div v-if="scheduleError" class="mb-5 rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
            <p>{{ scheduleError }}</p>
            <button type="button" class="mt-2 font-semibold underline" :disabled="loadingSchedule" @click="loadSchedule">Tentar novamente</button>
          </div>
          <div v-else-if="schedule && !scheduleConfigured && tab !== 'schedule'" class="mb-5 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-[13px] text-amber-900" role="status">
            <p class="flex items-center gap-2"><CalendarClock class="size-4 shrink-0" aria-hidden="true" />Você ainda não configurou seu expediente. Sem ele, os clientes não veem horários disponíveis.</p>
            <button type="button" class="button button-secondary button-sm" @click="tab = 'schedule'">Configurar expediente</button>
          </div>

          <div class="overflow-hidden rounded-2xl border border-line bg-white">
            <div class="flex gap-1 overflow-x-auto border-b border-line px-3 pt-2 sm:px-4" role="tablist" aria-label="Seções da agenda">
              <button
                v-for="option in ([
                  { id: 'week', label: 'Semana', count: null },
                  { id: 'schedule', label: 'Expediente', count: null },
                  { id: 'blocks', label: 'Bloqueios', count: hasLoadedUpcoming && !upcomingError ? upcomingBlocks.length : null },
                ] as const)"
                :id="`agenda-tab-${option.id}`"
                :key="option.id"
                type="button"
                role="tab"
                :aria-selected="tab === option.id"
                :aria-controls="`agenda-panel-${option.id}`"
                class="-mb-px flex items-center gap-2 border-b-2 px-3 py-3 text-[13px] font-semibold whitespace-nowrap"
                :class="tab === option.id ? 'border-ink text-ink' : 'border-transparent text-[#74747f] hover:text-ink'"
                @click="tab = option.id"
              >
                {{ option.label }}
                <span v-if="option.count !== null" class="rounded-full bg-paper px-1.5 text-[11px] text-[#55555e]">{{ option.count }}</span>
              </button>
            </div>

            <!-- Semana -->
            <div v-if="tab === 'week'" id="agenda-panel-week" role="tabpanel" aria-labelledby="agenda-tab-week" :aria-busy="loadingSchedule || loadingWeek">
              <div class="flex flex-wrap items-center justify-between gap-3 p-4 sm:px-5">
                <div>
                  <h2 class="text-[15px] font-bold first-letter:uppercase">{{ formatWeekRange(weekStart) }}</h2>
                  <p class="text-[12px] text-[#74747f]">Expediente, pausas e bloqueios da semana</p>
                </div>
                <nav class="flex items-center gap-2" aria-label="Navegar entre semanas">
                  <button type="button" class="button button-secondary size-9 !p-0" aria-label="Semana anterior" :disabled="loadingWeek" @click="changeWeek(-1)"><ChevronLeft class="size-4" aria-hidden="true" /></button>
                  <button type="button" class="button button-secondary button-sm" :disabled="loadingWeek || isCurrentWeek" @click="changeWeek(0)">Hoje</button>
                  <button type="button" class="button button-secondary size-9 !p-0" aria-label="Próxima semana" :disabled="loadingWeek" @click="changeWeek(1)"><ChevronRight class="size-4" aria-hidden="true" /></button>
                </nav>
              </div>

              <div v-if="weekError" class="mx-4 mb-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:mx-5" role="alert">
                <p>{{ weekError }}</p>
                <button type="button" class="mt-2 font-semibold underline" :disabled="loadingWeek" @click="loadWeekBlocks">Tentar novamente</button>
              </div>
              <p v-if="(loadingSchedule && !schedule) || (loadingWeek && !weekBlocks.length)" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando agenda...</p>
              <AgendaWeekGrid v-else-if="schedule" :week-start="weekStart" :schedule="schedule" :blocks="weekBlocks" :today="today" />

              <footer class="flex flex-wrap items-center justify-between gap-3 border-t border-line p-4 text-[12px] text-[#74747f] sm:px-5">
                <ul class="flex flex-wrap gap-4" aria-label="Legenda">
                  <li class="flex items-center gap-1.5"><span class="size-3 rounded-sm border border-line bg-white" aria-hidden="true"></span>Expediente</li>
                  <li class="flex items-center gap-1.5"><span class="size-3 rounded-sm bg-paper-deep" aria-hidden="true"></span>Intervalo / fora do expediente</li>
                  <li class="flex items-center gap-1.5"><span class="size-3 rounded-sm border-l-[3px] border-red-700 bg-red-50" aria-hidden="true"></span>Bloqueio</li>
                </ul>
                <p class="flex items-center gap-1.5"><Info class="size-3.5 shrink-0" aria-hidden="true" />Os agendamentos ainda não aparecem aqui: a API não disponibiliza a listagem.</p>
              </footer>
            </div>

            <!-- Expediente -->
            <div v-else-if="tab === 'schedule'" id="agenda-panel-schedule" role="tabpanel" aria-labelledby="agenda-tab-schedule">
              <p v-if="loadingSchedule && !schedule" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando expediente...</p>
              <WeeklyScheduleEditor v-else-if="schedule" :schedule="schedule" :submit="saveSchedule" />
              <p v-else-if="!scheduleError" class="px-5 py-14 text-center text-sm text-[#74747f]">Expediente indisponível.</p>
            </div>

            <!-- Bloqueios -->
            <div v-else id="agenda-panel-blocks" role="tabpanel" aria-labelledby="agenda-tab-blocks" :aria-busy="loadingUpcoming">
              <div class="flex flex-wrap items-center justify-between gap-3 border-b border-line p-4 sm:px-5">
                <div>
                  <h2 class="text-[15px] font-bold">Próximos bloqueios</h2>
                  <p class="text-[12px] text-[#74747f]">Hoje e os próximos {{ UPCOMING_DAYS }} dias</p>
                </div>
                <button type="button" class="button button-primary button-sm" @click="blockFormOpen = true"><Plus class="size-3.5" aria-hidden="true" />Novo bloqueio</button>
              </div>
              <div v-if="upcomingError" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
                <p>{{ upcomingError }}</p>
                <button type="button" class="mt-2 font-semibold underline" :disabled="loadingUpcoming" @click="loadUpcomingBlocks">Tentar novamente</button>
              </div>
              <p v-else-if="loadingUpcoming && !upcomingBlocks.length" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando bloqueios...</p>
              <BlocksList v-else-if="upcomingBlocks.length" :blocks="upcomingBlocks" :busy="deleting" @remove="blockToDelete = $event" />
              <div v-else-if="hasLoadedUpcoming" class="px-5 py-14 text-center" role="status">
                <CalendarOff class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhum bloqueio cadastrado.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">Cadastre folgas, feriados, almoço ou saídas para tirar esses horários da agenda.</p>
              </div>
              <p class="flex items-center gap-1.5 border-t border-line p-4 text-[12px] text-[#74747f] sm:px-5">
                <Info class="size-3.5 shrink-0" aria-hidden="true" />Para alterar um bloqueio, exclua e cadastre novamente: a API ainda não permite edição.
              </p>
            </div>
          </div>
        </template>
      </div>
    </section>

    <BlockFormDialog v-if="canManage" :open="blockFormOpen" :today="today" :initial-date="blockFormDate" :submit="createBlock" @close="blockFormOpen = false" />

    <ConfirmDeleteModal
      :open="Boolean(blockToDelete)"
      title="Excluir bloqueio"
      :description="blockToDelete ? `${blockToDelete.reasonLabel} — ${formatBlockPeriod(blockToDelete)}. O horário volta a ficar disponível para agendamento.` : ''"
      :loading="deleting"
      @close="deleting || (blockToDelete = null)"
      @confirm="confirmDelete"
    />
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>
