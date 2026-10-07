<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { onKeyStroke, useMediaQuery } from '@vueuse/core'
import { toast } from 'vue-sonner'
import {
  Bell, CalendarDays, CalendarPlus, ClipboardList,
  History, Home, KeyRound, LayoutDashboard, LogOut, Menu, Package, Scissors, Search, Settings, ShieldCheck, UserRound,
  Users, X,
} from '@lucide/vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const search = ref('')
const sidebarOpen = ref(false)
const mobileMenuButton = ref<HTMLButtonElement | null>(null)
const sidebar = ref<HTMLElement | null>(null)
const desktop = useMediaQuery('(min-width: 1024px)')

const normalize = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const initials = (name: string) => name.trim().split(/\s+/).map(part => part[0]).slice(0, 2).join('')
const userInitials = computed(() => auth.user ? initials(auth.user.name) : 'BF')
const roleLabel = computed(() => {
  const roles = auth.user?.roles || []
  if (roles.includes('ADMIN')) return 'Administrador'
  if (roles.includes('BARBEIRO') || roles.includes('BARBER')) return 'Barbeiro'
  return 'Cliente'
})

const navigation = computed(() => {
  const roles = auth.user?.roles || []
  const isAdmin = roles.includes('ADMIN')
  const isBarber = roles.includes('BARBEIRO') || roles.includes('BARBER')

  if (isAdmin) {
    return [
      { title: '', items: [
        { label: 'Dashboard', icon: LayoutDashboard, target: '/dashboard' },
      ] },
      { title: 'Administração', items: [
        { label: 'Roles', icon: ShieldCheck, target: '/roles' },
        { label: 'Permissões', icon: KeyRound, target: '/permissoes' },
        { label: 'Usuários', icon: Users, target: '' },
        { label: 'Estoque', icon: Package, target: '' },
        { label: 'Serviços', icon: Scissors, target: '/dashboard#servicos' },
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
        { label: 'Dashboard', icon: LayoutDashboard, target: '/dashboard' },
        { label: 'Minha agenda', icon: CalendarDays, target: '/dashboard#agenda' },
        { label: 'Agendamentos', icon: ClipboardList, target: '/dashboard#agenda' },
      ] },
      { title: 'Trabalho', items: [
        { label: 'Meus serviços', icon: Scissors, target: '/dashboard#servicos' },
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
      { label: 'Início', icon: Home, target: '/dashboard' },
      { label: 'Agendar', icon: CalendarPlus, target: '/dashboard#agenda' },
      { label: 'Meus agendamentos', icon: ClipboardList, target: '/dashboard#agenda' },
      { label: 'Histórico', icon: History, target: '' },
    ] },
    { title: 'Explorar', items: [
      { label: 'Barbeiros', icon: Users, target: '/dashboard#equipe' },
      { label: 'Serviços', icon: Scissors, target: '/dashboard#servicos' },
    ] },
    { title: 'Conta', items: [
      { label: 'Notificações', icon: Bell, target: '' },
      { label: 'Perfil', icon: UserRound, target: '' },
    ] },
  ]
})

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

async function logout() {
  auth.logout()
  await router.replace({ name: 'home' })
}
</script>

<template>
  <div class="dashboard min-h-screen bg-paper text-ink">
    <a href="#main-content" class="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-lg focus:bg-white focus:p-3">Ir para o conteúdo</a>
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
            <RouterLink v-if="item.target && item.target !== '/dashboard' && item.target.startsWith('/') && !item.target.includes('#')" :to="item.target" class="nav-item text-[#303039] hover:bg-paper" active-class="bg-ink font-semibold text-white" @click="sidebarOpen = false">
              <component :is="item.icon" class="size-[17px] shrink-0 text-[#74747f]" :stroke-width="1.7" aria-hidden="true" />{{ item.label }}
            </RouterLink>
            <a v-else-if="item.target" :href="item.target" class="nav-item" :class="['Dashboard', 'Início'].includes(item.label) && $route.path === '/dashboard' ? 'bg-ink font-semibold text-white' : 'text-[#303039] hover:bg-paper'" :aria-current="['Dashboard', 'Início'].includes(item.label) && $route.path === '/dashboard' ? 'page' : undefined" @click="sidebarOpen = false">
              <component :is="item.icon" class="size-[17px] shrink-0" :class="['Dashboard', 'Início'].includes(item.label) && $route.path === '/dashboard' ? 'text-[#a67039]' : 'text-[#74747f]'" :stroke-width="1.7" aria-hidden="true" />{{ item.label }}
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
        <div class="flex items-center gap-2.5"><span class="avatar">{{ userInitials }}</span><div class="min-w-0"><p class="truncate text-[13px] leading-[18px] font-semibold">{{ auth.user?.name}}</p><p class="text-[11px] text-[#74747f]">{{ auth.user ? roleLabel : '' }}</p></div></div>
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
          <button type="button" class="icon-button hidden size-9 sm:flex" aria-label="Notificações" @click="unavailable('Notificações')"><Bell class="size-[18px]" :stroke-width="1.6" /></button>
          <span class="avatar" :aria-label="auth.user?.name ?? 'Barbearia Ferro'">{{ userInitials }}</span>
        </div>
      </header>

      <main id="main-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
@layer components {
.dashboard { font-family: 'Plus Jakarta Sans', 'Inter', 'Segoe UI', sans-serif; }
.nav-item { display: flex; align-items: center; gap: 12px; min-height: 36px; margin-bottom: 1px; border-radius: 11px; padding: 8px 11px; font-size: 13px; text-align: left; }
.avatar { display: flex; width: 34px; height: 34px; flex-shrink: 0; align-items: center; justify-content: center; border-radius: 11px; background-color: #1a1a1c; color: white; font-size: 11px; font-weight: 700; }
.icon-button { display: inline-flex; width: 30px; height: 30px; align-items: center; justify-content: center; border: 1px solid var(--color-line); border-radius: 9px; background: white; color: #55555e; }
.icon-button:hover { background: var(--color-paper); }
button, a, input, select { outline-offset: 3px; }
button { cursor: pointer; }
button:focus-visible, a:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #a67039; }
label:focus-within { border-color: #a67039; }
@media (prefers-reduced-motion: reduce) { * { scroll-behavior: auto; transition: none; } }
}
</style>
