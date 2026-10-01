<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { onKeyStroke, useMediaQuery } from '@vueuse/core'
import { toast } from 'vue-sonner'
import {
  ArrowDownToLine, Bell, CalendarDays, CalendarPlus, CircleX, ClipboardList,
  Eye, History, Home, KeyRound, Layers, LayoutDashboard, LogOut, Menu, Package, Pencil, Plus,
  RefreshCw, Scissors, Search, Settings, ShieldCheck, Trash2, TrendingUp, UserRound,
  Users, UserRoundCog, X,
} from '@lucide/vue'
import StatCard from '../components/dashboard/StatCard.vue'
import { appointments, dashboardDate, weeklyRevenue, type Appointment, type AppointmentStatus } from '../data/dashboard'
import { barbers, services } from '../data/home'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const selectedDate = ref(dashboardDate)
const search = ref('')
const status = ref<AppointmentStatus | ''>('')
const showAll = ref(false)
const sidebarOpen = ref(false)
const mobileMenuButton = ref<HTMLButtonElement | null>(null)
const sidebar = ref<HTMLElement | null>(null)
const appointmentDialog = ref<HTMLDialogElement | null>(null)
const selectedAppointment = ref<Appointment | null>(null)
const desktop = useMediaQuery('(min-width: 1024px)')
const money = (value: number) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const initials = (name: string) => name.trim().split(/\s+/).map(part => part[0]).slice(0, 2).join('')
const userInitials = computed(() => auth.user ? initials(auth.user.name) : 'BF')
const roleLabel = computed(() => {
  const roles = auth.user?.roles || []
  if (roles.includes('ADMIN')) return 'Administrador'
  if (roles.includes('BARBEIRO') || roles.includes('BARBER')) return 'Barbeiro'
  return 'Cliente'
})
const dailyAppointments = computed(() => appointments.filter(item => item.date === selectedDate.value))
const completed = computed(() => dailyAppointments.value.filter(item => item.status === 'Concluído'))
const revenue = computed(() => completed.value.reduce((sum, item) => sum + item.price, 0))
const upcoming = computed(() => dailyAppointments.value.filter(item => item.status === 'Confirmado' || item.status === 'Pendente'))
const filteredAppointments = computed(() => (showAll.value ? dailyAppointments.value : upcoming.value).filter(item =>
  (!status.value || item.status === status.value)
  && normalize(`${item.client} ${item.barber} ${item.service}`).includes(normalize(search.value.trim())),
))
const formattedDate = computed(() => {
  if (!selectedDate.value) return 'Selecione uma data'
  const date = new Date(`${selectedDate.value}T12:00:00`)
  return new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' }).format(date)
})
const activeBarbers = computed(() => new Set(dailyAppointments.value.filter(item => item.status !== 'Cancelado').map(item => item.barber)).size)
const canceled = computed(() => dailyAppointments.value.filter(item => item.status === 'Cancelado').length)
const statusClasses: Record<AppointmentStatus, string> = {
  Confirmado: 'bg-[#e5f2ec] text-[#28835e]',
  Concluído: 'bg-[#ededeb] text-[#505055]',
  Pendente: 'bg-[#fbf0db] text-[#b57d19]',
  Cancelado: 'bg-red-50 text-red-700',
}
const navigation = computed(() => {
  const roles = auth.user?.roles || []
  const isAdmin = roles.includes('ADMIN')
  const isBarber = roles.includes('BARBEIRO') || roles.includes('BARBER')

  if (isAdmin) {
    return [
      { title: '', items: [
        { label: 'Dashboard', icon: LayoutDashboard, target: '#dashboard-content' },
      ] },
      { title: 'Administração', items: [
        { label: 'Roles', icon: ShieldCheck, target: '' },
        { label: 'Permissões', icon: KeyRound, target: '' },
        { label: 'Usuários', icon: Users, target: '' },
        { label: 'Estoque', icon: Package, target: '' },
        { label: 'Serviços', icon: Scissors, target: '#servicos' },
        { label: 'Configurações', icon: Settings, target: '' },
      ] },
      { title: 'Conta', items: [
        { label: 'Notificações', icon: Bell, target: '' },
        { label: 'Perfil', icon: UserRound, target: '' },
      ] },
    ]
  }

  if (isBarber) {
    return [
      { title: 'Meu dia', items: [
        { label: 'Dashboard', icon: LayoutDashboard, target: '#dashboard-content' },
        { label: 'Minha agenda', icon: CalendarDays, target: '#agenda' },
        { label: 'Agendamentos', icon: ClipboardList, target: '#agenda' },
      ] },
      { title: 'Trabalho', items: [
        { label: 'Meus serviços', icon: Scissors, target: '#servicos' },
        { label: 'Estoque', icon: Package, target: '' },
        { label: 'Histórico', icon: History, target: '' },
      ] },
      { title: 'Conta', items: [
        { label: 'Notificações', icon: Bell, target: '' },
        { label: 'Perfil', icon: UserRound, target: '' },
      ] },
    ]
  }

  return [
    { title: '', items: [
      { label: 'Início', icon: Home, target: '#dashboard-content' },
      { label: 'Agendar', icon: CalendarPlus, target: '#agenda' },
      { label: 'Meus agendamentos', icon: ClipboardList, target: '#agenda' },
      { label: 'Histórico', icon: History, target: '' },
    ] },
    { title: 'Explorar', items: [
      { label: 'Barbeiros', icon: Users, target: '#equipe' },
      { label: 'Serviços', icon: Scissors, target: '#servicos' },
    ] },
    { title: 'Conta', items: [
      { label: 'Notificações', icon: Bell, target: '' },
      { label: 'Perfil', icon: UserRound, target: '' },
    ] },
  ]
})

const weeklyAppointments = computed(() => {
  const selected = new Date(`${selectedDate.value || dashboardDate}T12:00:00`)
  selected.setDate(selected.getDate() - (selected.getDay() + 6) % 7)
  return ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'].map((label, index) => {
    const date = new Date(selected)
    date.setDate(date.getDate() + index)
    const day = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
    const items = appointments.filter(item => item.date === day)
    return { label, total: items.length, canceled: items.filter(item => item.status === 'Cancelado').length }
  })
})
const weekCount = computed(() => weeklyAppointments.value.reduce((sum, day) => sum + day.total, 0))
const chartMax = computed(() => Math.max(4, ...weeklyAppointments.value.map(day => day.total)))
const chartTicks = computed(() => Array.from({ length: 5 }, (_, index) => Math.round(chartMax.value * (4 - index) / 4)))
const revenueMax = Math.max(1, ...weeklyRevenue.map(day => day.value))
const revenuePoints = weeklyRevenue.map((day, index) => `${32 + index * 56},${174 - day.value / revenueMax * 132}`).join(' ')
const servicePerformance = computed(() => services.map(service => ({
  name: service.title,
  count: completed.value.filter(item => normalize(item.service) === normalize(service.title)).length,
})))
const maxServiceCount = computed(() => Math.max(1, ...servicePerformance.value.map(item => item.count)))
const barberPerformance = computed(() => barbers.map(barber => ({
  name: barber.name,
  count: completed.value.filter(item => item.barber === barber.name).length,
})))
const maxBarberCount = computed(() => Math.max(1, ...barberPerformance.value.map(item => item.count)))

function unavailable(feature: string) {
  toast.info(`${feature} ainda não está disponível.`)
}

async function openSidebar() {
  sidebarOpen.value = true
  await nextTick()
  sidebar.value?.querySelector<HTMLElement>('a')?.focus()
}

function closeSidebar() {
  sidebarOpen.value = false
  mobileMenuButton.value?.focus()
}

function trapSidebarFocus(event: KeyboardEvent) {
  if (!sidebarOpen.value || desktop.value || event.key !== 'Tab') return
  const items = sidebar.value?.querySelectorAll<HTMLElement>('a[href], button:not(:disabled)')
  const first = items?.[0]
  const last = items?.[items.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}

onKeyStroke('Escape', () => { if (sidebarOpen.value) closeSidebar() })
watch(desktop, value => { if (value) sidebarOpen.value = false })
watch(search, value => { if (value.trim()) showAll.value = true })

async function viewAppointment(item: Appointment) {
  selectedAppointment.value = item
  await nextTick()
  appointmentDialog.value?.showModal()
}

async function logout() {
  auth.logout()
  await router.replace({ name: 'home' })
}

function refresh() {
  search.value = ''
  status.value = ''
  selectedDate.value = dashboardDate
  showAll.value = false
  toast.info('Exibindo os dados de demonstração disponíveis.')
}

function exportAppointments() {
  const rows = [['Data', 'Horário', 'Cliente', 'Serviço', 'Barbeiro', 'Valor (R$)', 'Status'],
    ...filteredAppointments.value.map(item => [item.date, item.time, item.client, item.service, item.barber, item.price.toFixed(2).replace('.', ','), item.status])]
  const csv = '\uFEFF' + rows.map(row => row.map(cell => `"${cell.replaceAll('"', '""')}"`).join(';')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a')
  link.href = url
  link.download = `agendamentos-${selectedDate.value}.csv`
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
</script>

<template>
  <div class="dashboard min-h-screen bg-paper text-ink">
    <a href="#dashboard-content" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-lg focus:bg-white focus:p-3">Ir para o conteúdo</a>
    <div v-if="sidebarOpen" class="fixed inset-0 z-40 bg-black/30 lg:hidden" aria-hidden="true" @click="closeSidebar"></div>
    <aside
      id="dashboard-sidebar" ref="sidebar"
      class="fixed inset-y-0 left-0 z-50 w-[244px] flex-col border-r border-line bg-white"
      :class="sidebarOpen ? 'flex' : 'hidden lg:flex'"
      :role="sidebarOpen && !desktop ? 'dialog' : undefined"
      :aria-modal="sidebarOpen && !desktop ? true : undefined"
      aria-label="Menu principal" @keydown="trapSidebarFocus"
    >
      <div class="flex h-[72px] shrink-0 items-center justify-between px-4">
        <RouterLink to="/" class="flex items-center gap-2.5" aria-label="Barbearia Ferro — início">
          <span class="flex size-[34px] items-center justify-center rounded-[11px] bg-ink text-white"><Scissors class="size-[18px]" :stroke-width="1.7" aria-hidden="true" /></span>
          <span><span class="block text-[15px] leading-[18px] font-bold tracking-[-0.6px]">Barbearia Ferro</span><span class="block text-[11px] leading-4 text-[#505055]">Sistema de gestão</span></span>
        </RouterLink>
        <button type="button" class="icon-button lg:hidden" aria-label="Fechar menu" @click="closeSidebar"><X class="size-4" /></button>
      </div>
      <nav aria-label="Navegação do dashboard" class="min-h-0 flex-1 overflow-y-auto px-[10px] pb-4">
        <div v-for="group in navigation" :key="group.title" class="mt-[18px]">
          <p v-if="group.title" class="mb-1 px-[11px] text-[11px] leading-4 font-medium text-[#74747f]">{{ group.title }}</p>
          <template v-for="item in group.items" :key="item.label">
            <a v-if="item.target" :href="item.target" class="nav-item" :class="['Dashboard', 'Início'].includes(item.label) ? 'bg-ink font-semibold text-white' : 'text-[#303039] hover:bg-paper'" :aria-current="['Dashboard', 'Início'].includes(item.label) ? 'page' : undefined" @click="sidebarOpen = false">
              <component :is="item.icon" class="size-[17px] shrink-0" :class="['Dashboard', 'Início'].includes(item.label) ? 'text-[#a67039]' : 'text-[#74747f]'" :stroke-width="1.7" aria-hidden="true" />{{ item.label }}
            </a>
            <button v-else type="button" class="nav-item w-full text-[#303039] hover:bg-paper" :title="`${item.label} ainda não está disponível`" @click="unavailable(item.label)">
              <component :is="item.icon" class="size-[17px] shrink-0 text-[#74747f]" :stroke-width="1.7" aria-hidden="true" />
              <div class="flex flex-1 justify-between items-center">
                <span>{{ item.label }}</span>
                <span v-if="item.label === 'Notificações'" class="flex h-5 items-center justify-center rounded-full bg-[#a67039] px-1.5 text-[10px] font-bold text-white">3</span>
              </div>
            </button>
          </template>
        </div>
      </nav>
      <div class="shrink-0 border-t border-line px-4 pt-[18px] pb-3">
        <div class="flex items-center gap-2.5"><span class="avatar">{{ userInitials }}</span><div class="min-w-0"><p class="truncate text-[13px] leading-[18px] font-semibold">{{ auth.user?.name ?? 'Barbearia Ferro' }}</p><p class="text-[11px] text-[#74747f]">{{ auth.user ? roleLabel : 'Demonstração' }}</p></div></div>
        <button v-if="auth.isAuthenticated" type="button" class="mt-3 flex w-full items-center gap-3 rounded-lg px-2 py-2 text-xs hover:bg-paper" @click="logout"><LogOut class="size-4 text-[#74747f]" aria-hidden="true" />Sair</button>
        <RouterLink v-else to="/" class="mt-3 flex items-center gap-3 rounded-lg px-2 py-2 text-xs hover:bg-paper"><LogOut class="size-4 text-[#74747f]" aria-hidden="true" />Voltar ao site</RouterLink>
      </div>
    </aside>

    <div class="lg:ml-[244px]" :inert="sidebarOpen && !desktop">
      <header class="sticky top-0 z-30 flex h-[62px] items-center justify-between gap-3 border-b border-line bg-white/95 px-4 backdrop-blur-sm lg:px-[18px]">
        <button ref="mobileMenuButton" type="button" class="icon-button shrink-0 lg:hidden" aria-label="Abrir menu" :aria-expanded="sidebarOpen" aria-controls="dashboard-sidebar" @click="openSidebar"><Menu class="size-[18px]" /></button>
        <label class="flex h-[38px] min-w-0 flex-1 items-center gap-2 rounded-xl border border-line bg-paper px-3 sm:max-w-[380px]">
          <Search class="size-4 shrink-0 text-[#74747f]" aria-hidden="true" /><span class="sr-only">Buscar cliente, agendamento ou serviço</span>
          <input v-model="search" type="search" placeholder="Buscar cliente, agendamento ou serviço" class="w-full min-w-0 bg-transparent text-[13px] text-ink outline-none placeholder:text-[#74747f]" />
        </label>
        <div class="flex shrink-0 items-center gap-2 sm:gap-3">
          <span class="hidden items-center gap-1.5 rounded-full border border-[#e7d3ba] bg-[#f5ede3] px-3 py-1.5 text-[11px] font-medium text-[#945d28] sm:inline-flex"><span class="size-1.5 rounded-full bg-[#a67039]"></span>Demonstração</span>
          <button type="button" class="icon-button hidden size-9 sm:flex" aria-label="Notificações" @click="unavailable('Notificações')"><Bell class="size-[18px]" :stroke-width="1.6" /></button>
          <span class="avatar" :aria-label="auth.user?.name ?? 'Barbearia Ferro'">{{ userInitials }}</span>
        </div>
      </header>

      <main id="dashboard-content" class="px-4 pt-[26px] pb-9 sm:px-[26px]">
        <div class="mb-[22px] flex flex-wrap items-end justify-between gap-5">
          <div>
            <h1 class="text-[27px] leading-9 font-bold tracking-[-1px]">Bom dia<span v-if="auth.firstName">, {{ auth.firstName }}</span></h1>
            <p class="mt-1 max-w-[610px] text-[14px] leading-[21px] text-[#74747f]"><span class="inline-block first-letter:uppercase">{{ formattedDate }}</span>. A casa tem {{ dailyAppointments.length }} atendimentos marcados na data selecionada.</p>
          </div>
          <div class="flex flex-wrap items-center gap-2.5">
            <button type="button" class="action-button" @click="refresh"><RefreshCw class="size-4" :stroke-width="1.6" aria-hidden="true" />Atualizar</button>
            <button type="button" class="action-button border-ink bg-ink text-white hover:bg-ink-soft" @click="unavailable('Novo agendamento')"><Plus class="size-4" aria-hidden="true" />Novo agendamento</button>
          </div>
        </div>

        <section aria-label="Resumo da barbearia" class="grid gap-[18px] sm:grid-cols-2 xl:grid-cols-4">
          <StatCard label="Agendamentos hoje" :value="dailyAppointments.length" description="Na data selecionada" :icon="CalendarDays" accent />
          <StatCard label="Agendamentos na semana" :value="weekCount" description="Na semana selecionada" :icon="Layers" />
          <StatCard label="Clientes cadastrados" value="—" description="Ainda sem dados de cadastro" :icon="Users" />
          <StatCard label="Barbeiros ativos" :value="`${activeBarbers} de ${barbers.length}`" description="Com agendamentos na data" :icon="UserRoundCog" />
          <StatCard label="Serviços no catálogo" :value="services.length" :icon="Scissors" />
          <StatCard label="Itens em estoque" value="—" description="Ainda sem dados de estoque" :icon="Package" />
          <StatCard label="Cancelamentos" :value="canceled" description="Na data selecionada" :icon="CircleX" />
          <StatCard label="Faturamento estimado" :value="money(revenue)" description="Atendimentos concluídos na data" :icon="TrendingUp" accent />
        </section>

        <div class="mt-[22px] grid items-start gap-[18px] sm:grid-cols-2 xl:grid-cols-4">
          <section class="dashboard-panel" aria-labelledby="appointments-chart-title">
            <div class="panel-heading"><h2 id="appointments-chart-title">Agendamentos por dia</h2><p>Semana selecionada, incluindo cancelamentos</p></div>
            <div class="flex h-[272px] items-center px-5 py-7">
              <svg viewBox="0 0 340 206" class="w-full" role="img" :aria-label="weeklyAppointments.map(day => `${day.label}: ${day.total} agendamentos, ${day.canceled} cancelamentos`).join('; ')">
                <g v-for="(tick, index) in chartTicks" :key="index"><line x1="28" :y1="30 + index * 36" x2="332" :y2="30 + index * 36" stroke="#eeeeec" /><text x="22" :y="33 + index * 36" text-anchor="end">{{ tick }}</text></g>
                <g v-for="(day, index) in weeklyAppointments" :key="day.label">
                  <rect :x="36 + index * 43" :y="174 - day.total / chartMax * 144" width="13" :height="day.total / chartMax * 144" rx="3" fill="#1a1a1c" />
                  <rect :x="51 + index * 43" :y="174 - day.canceled / chartMax * 144" width="13" :height="day.canceled / chartMax * 144" rx="3" fill="#e5d3b9" />
                  <text :x="49 + index * 43" y="189" text-anchor="middle">{{ day.label }}</text>
                </g>
              </svg>
            </div>
          </section>

          <section class="dashboard-panel" aria-labelledby="revenue-title">
            <div class="panel-heading"><h2 id="revenue-title">Movimento na semana</h2><p>Faturamento disponível, em reais</p></div>
            <div class="flex h-[272px] items-center px-5 py-7">
              <svg viewBox="0 0 340 206" class="w-full" role="img" :aria-label="weeklyRevenue.map(day => `${day.label}: ${money(day.value)}`).join('; ')">
                <defs><linearGradient id="revenue-fill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#a67039" stop-opacity="0.28" /><stop offset="100%" stop-color="#a67039" stop-opacity="0.02" /></linearGradient></defs>
                <g v-for="index in 5" :key="index"><line x1="30" :y1="30 + (index - 1) * 36" x2="320" :y2="30 + (index - 1) * 36" stroke="#eeeeec" /><text x="24" :y="33 + (index - 1) * 36" text-anchor="end">{{ Math.round(revenueMax * (5 - index) / 4) }}</text></g>
                <polygon :points="`32,174 ${revenuePoints} 312,174`" fill="url(#revenue-fill)" />
                <polyline :points="revenuePoints" fill="none" stroke="#a67039" stroke-width="1.5" />
                <g v-for="(day, index) in weeklyRevenue" :key="day.label"><circle :cx="32 + index * 56" :cy="174 - day.value / revenueMax * 132" r="2.2" fill="#a67039" /><text :x="32 + index * 56" y="189" text-anchor="middle">{{ day.label }}</text></g>
              </svg>
            </div>
          </section>

          <section id="servicos" class="dashboard-panel scroll-mt-20" aria-labelledby="services-title">
            <div class="panel-heading"><h2 id="services-title">Serviços mais realizados</h2><p>Atendimentos concluídos na data</p></div>
            <ul class="space-y-[14px] p-5"><li v-for="(item, index) in servicePerformance" :key="item.name"><div class="mb-1 flex items-center justify-between gap-2 text-[12px]"><span class="font-semibold">{{ item.name }}</span><span class="shrink-0 text-[#74747f]">{{ item.count }} atendimentos</span></div><div class="h-2 overflow-hidden rounded-full bg-[#ededeb]"><div class="h-full rounded-full" :class="index === 0 ? 'bg-[#a67039]' : 'bg-ink'" :style="{ width: `${item.count / maxServiceCount * 100}%` }"></div></div></li></ul>
          </section>

          <section id="equipe" class="dashboard-panel scroll-mt-20" aria-labelledby="team-title">
            <div class="panel-heading"><h2 id="team-title">Desempenho dos barbeiros</h2><p>Atendimentos concluídos na data</p></div>
            <ul class="space-y-[14px] p-5"><li v-for="(item, index) in barberPerformance" :key="item.name"><div class="mb-1 flex items-center justify-between gap-2 text-[12px]"><span class="font-semibold">{{ item.name.split(' ')[0] }}</span><span class="text-[#74747f]">{{ item.count }} atendimentos</span></div><div class="h-2 overflow-hidden rounded-full bg-[#ededeb]"><div class="h-full rounded-full" :class="index === 0 ? 'bg-[#a67039]' : 'bg-ink'" :style="{ width: `${item.count / maxBarberCount * 100}%` }"></div></div></li></ul>
          </section>
        </div>

        <section id="agenda" class="dashboard-panel mt-[18px] scroll-mt-20" aria-labelledby="agenda-title">
          <div class="flex items-center justify-between gap-3 px-5 py-[17px]"><div><h2 id="agenda-title" class="text-[14px] font-semibold tracking-[-0.3px]">{{ showAll ? 'Todos os agendamentos' : 'Próximos agendamentos' }}</h2><p class="mt-0.5 text-xs text-[#74747f]">{{ showAll ? 'Na data selecionada' : 'Atendimentos aguardando na data selecionada' }}</p></div><button type="button" class="action-button px-3 py-1.5 text-xs" :aria-expanded="showAll" @click="showAll = !showAll">{{ showAll ? 'Ver próximos' : 'Ver todos' }}</button></div>
          <div v-if="showAll" class="flex flex-wrap items-center gap-3 border-t border-line bg-paper/50 px-4 py-3">
            <label class="flex items-center gap-2 text-xs text-[#74747f]">Data<input v-model="selectedDate" type="date" class="rounded-lg border border-line bg-white px-2 py-1.5 text-ink" /></label>
            <label class="flex items-center gap-2 text-xs text-[#74747f]">Status<select v-model="status" class="rounded-lg border border-line bg-white px-2 py-2 text-ink"><option value="">Todos os status</option><option v-for="(_, itemStatus) in statusClasses" :key="itemStatus" :value="itemStatus">{{ itemStatus }}</option></select></label>
            <button type="button" class="action-button ml-auto px-3 py-1.5 text-xs disabled:opacity-40" :disabled="!filteredAppointments.length" @click="exportAppointments"><ArrowDownToLine class="size-4" aria-hidden="true" />Exportar CSV</button>
          </div>
          <div class="relative overflow-x-auto">
            <table class="w-full min-w-[920px] whitespace-nowrap text-left text-[13px]">
              <caption class="sr-only">Agendamentos em {{ selectedDate }}</caption>
              <thead class="border-y border-line bg-paper text-[11px] text-[#74747f]"><tr><th scope="col">Cliente</th><th scope="col">Barbeiro</th><th scope="col">Serviço</th><th scope="col">Horário</th><th scope="col">Valor</th><th scope="col">Status</th><th scope="col"><span class="sr-only">Ações</span></th></tr></thead>
              <tbody class="divide-y divide-line/70">
                <tr v-for="item in filteredAppointments" :key="item.id" class="hover:bg-paper/40">
                  <td><div class="flex items-center gap-3"><span class="avatar bg-[#45454d]">{{ initials(item.client) }}</span><div><p class="font-semibold">{{ item.client }}</p><p class="text-xs text-[#74747f]">#{{ item.id }}</p></div></div></td>
                  <td class="text-[#55555e]">{{ item.barber }}</td><td class="text-[#55555e]">{{ item.service }}</td><td class="font-semibold">{{ item.time }}</td><td class="text-[#55555e]">{{ money(item.price) }}</td>
                  <td><span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] leading-[15px] font-semibold" :class="statusClasses[item.status]"><span class="size-1.5 rounded-full bg-current"></span>{{ item.status }}</span></td>
                  <td><div class="flex items-center gap-1.5"><button type="button" class="icon-button" :aria-label="`Ver agendamento de ${item.client}`" @click="viewAppointment(item)"><Eye class="size-[14px]" /></button><button type="button" class="icon-button" :aria-label="`Editar agendamento de ${item.client}`" @click="unavailable('Edição de agendamentos')"><Pencil class="size-[14px]" /></button><button type="button" class="icon-button text-red-600" :aria-label="`Excluir agendamento de ${item.client}`" @click="unavailable('Exclusão de agendamentos')"><Trash2 class="size-[14px]" /></button></div></td>
                </tr>
                <tr v-if="!filteredAppointments.length"><td colspan="7" class="h-32 text-center text-sm text-[#74747f]">Nenhum agendamento encontrado para os filtros selecionados.</td></tr>
              </tbody>
            </table>
          </div>
        </section>
        <p class="mt-4 text-[11px] text-[#74747f]">Dados de demonstração. Os indicadores sem informação aparecem como —.</p>
      </main>
    </div>

    <dialog ref="appointmentDialog" class="m-auto w-[calc(100%-32px)] max-w-md rounded-2xl border border-line bg-white p-6 text-ink shadow-xl backdrop:bg-black/30" aria-labelledby="appointment-detail-title">
      <div class="flex items-center justify-between gap-3"><h2 id="appointment-detail-title" class="text-lg font-semibold">Detalhes do agendamento</h2><button type="button" class="icon-button" aria-label="Fechar detalhes" @click="appointmentDialog?.close()"><X class="size-4" /></button></div>
      <dl v-if="selectedAppointment" class="mt-5 grid grid-cols-[auto_1fr] gap-x-5 gap-y-3 text-sm"><dt class="text-[#74747f]">Cliente</dt><dd>{{ selectedAppointment.client }}</dd><dt class="text-[#74747f]">Barbeiro</dt><dd>{{ selectedAppointment.barber }}</dd><dt class="text-[#74747f]">Serviço</dt><dd>{{ selectedAppointment.service }}</dd><dt class="text-[#74747f]">Horário</dt><dd>{{ selectedAppointment.time }}</dd><dt class="text-[#74747f]">Valor</dt><dd>{{ money(selectedAppointment.price) }}</dd><dt class="text-[#74747f]">Status</dt><dd>{{ selectedAppointment.status }}</dd></dl>
    </dialog>
  </div>
</template>

<style scoped>
@layer components {
.dashboard { font-family: 'Plus Jakarta Sans', 'Inter', 'Segoe UI', sans-serif; }
.nav-item { display: flex; align-items: center; gap: 12px; min-height: 36px; margin-bottom: 1px; border-radius: 11px; padding: 8px 11px; font-size: 13px; text-align: left; }
.avatar { display: flex; width: 34px; height: 34px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 11px; background-color: #1a1a1c; color: white; font-size: 11px; font-weight: 700; }
td .avatar { background-color: #45454d; }
.dashboard-panel { overflow: hidden; border: 1px solid var(--color-line); border-radius: 16px; background: white; }
.panel-heading { min-height: 70px; padding: 16px 20px; border-bottom: 1px solid #eeeeec; }
.panel-heading h2 { font-size: 14px; font-weight: 600; letter-spacing: -0.3px; }
.panel-heading p { margin-top: 2px; font-size: 12px; color: #74747f; }
.action-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; border: 1px solid var(--color-line); border-radius: 11px; padding: 10px 16px; font-size: 13px; font-weight: 600; transition: background-color 150ms; }
.action-button:hover { background-color: #ededeb; }
.action-button.bg-ink { border-color: #1a1a1c; }
.action-button.bg-ink:hover { background-color: #2c2c2f; }
.icon-button { display: inline-flex; width: 30px; height: 30px; align-items: center; justify-content: center; border: 1px solid var(--color-line); border-radius: 9px; background: white; color: #55555e; }
.icon-button:hover { background: var(--color-paper); }
.icon-button.text-red-600 { color: #dc2626; }
svg text { fill: #74747f; font-size: 7px; font-family: 'Inter', sans-serif; }
th { padding: 11px 16px; font-weight: 600; }
td { height: 61px; padding: 12px 16px; }
button, a, input, select { outline-offset: 3px; }
button { cursor: pointer; }
button:focus-visible, a:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #a67039; }
label:focus-within { border-color: #a67039; }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto; transition: none; } }
}
</style>
