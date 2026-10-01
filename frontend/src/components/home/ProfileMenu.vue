<script setup lang="ts">
import { nextTick, ref, useId, watch } from 'vue'
import { onClickOutside, onKeyStroke } from '@vueuse/core'
import { ChevronDown, LayoutDashboard, LogOut, UserRound } from '@lucide/vue'
import { useAuthStore } from '../../stores/auth'

defineProps<{ scrolled: boolean }>()
const emit = defineEmits<{ open: [] }>()
const auth = useAuthStore()
const opened = ref(false)
const root = ref<HTMLElement | null>(null)
const trigger = ref<HTMLButtonElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const menuId = useId()

function close(restoreFocus = false) {
  opened.value = false
  if (restoreFocus) trigger.value?.focus()
}

async function open() {
  opened.value = true
  emit('open')
  await nextTick()
  menu.value?.querySelector<HTMLElement>('[role="menuitem"]:not(:disabled)')?.focus()
}

function toggle() {
  if (opened.value) close(true)
  else void open()
}

function onFocusOut(event: FocusEvent) {
  if (!root.value?.contains(event.relatedTarget as Node | null)) close()
}

function onMenuKeydown(event: KeyboardEvent) {
  if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const items = Array.from(menu.value?.querySelectorAll<HTMLElement>('[role="menuitem"]:not(:disabled)') ?? [])
  const current = items.indexOf(document.activeElement as HTMLElement)
  const index = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1
    : (current + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length
  items[index]?.focus()
}

function logout() {
  close(true)
  auth.logout()
  void nextTick(() => {
    const target = window.matchMedia('(min-width: 861px)').matches ? 'header-login' : 'header-mobile-menu'
    document.getElementById(target)?.focus()
  })
}

onClickOutside(root, () => close())
onKeyStroke('Escape', event => {
  if (!opened.value) return
  event.preventDefault()
  close(true)
})
watch(() => auth.isAuthenticated, () => close())
</script>

<template>
  <div ref="root" class="relative z-[101]" @focusout="onFocusOut">
    <button
      ref="trigger"
      type="button"
      class="inline-flex max-w-[140px] items-center gap-2 rounded-lg border px-2 py-[9px] text-[0.87rem] font-semibold transition-colors duration-[350ms] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gold min-[861px]:max-w-[200px] min-[861px]:px-[18px]"
      :class="scrolled ? 'border-line text-ink hover:border-ink' : 'border-on-dark/28 text-on-dark hover:border-on-dark/60'"
      aria-haspopup="menu"
      :aria-expanded="opened"
      :aria-controls="menuId"
      :aria-label="`Menu de ${auth.user?.name}`"
      @click="toggle"
      @keydown.down.prevent="open"
      @keydown.up.prevent="open"
    >
      <UserRound class="size-[18px] shrink-0" aria-hidden="true" />
      <span class="truncate max-[380px]:max-w-[48px]">{{ auth.firstName }}</span>
      <ChevronDown class="hidden size-4 shrink-0 transition-transform min-[381px]:block" :class="{ 'rotate-180': opened }" aria-hidden="true" />
    </button>

    <div
      v-if="opened"
      :id="menuId"
      ref="menu"
      role="menu"
      aria-label="Opções do perfil"
      class="absolute right-0 top-full mt-3 w-[230px] max-w-[calc(100vw-56px)] rounded-lg border border-line bg-paper p-2 text-ink shadow-lg"
      @keydown="onMenuKeydown"
    >
      <RouterLink to="/dashboard" role="menuitem" class="flex w-full items-center gap-2 rounded-lg px-3 py-3 text-left text-[0.87rem] font-semibold hover:bg-paper-deep focus-visible:outline-2 focus-visible:outline-gold" @click="close()">
        <LayoutDashboard class="size-[18px]" aria-hidden="true" /> Dashboard
      </RouterLink>
      <button type="button" role="menuitem" disabled aria-disabled="true" class="flex w-full items-center gap-2 rounded-lg px-3 py-3 text-left text-[0.87rem] font-semibold text-text-soft opacity-60">
        <UserRound class="size-[18px]" aria-hidden="true" /> Meu perfil
      </button>
      <p class="px-3 pb-3 text-xs text-text-soft">Meu perfil ainda não está disponível.</p>
      <div role="separator" class="mb-1 border-t border-line"></div>
      <button type="button" role="menuitem" class="flex w-full items-center gap-2 rounded-lg px-3 py-3 text-left text-[0.87rem] font-semibold hover:bg-paper-deep focus-visible:outline-2 focus-visible:outline-gold" @click="logout">
        <LogOut class="size-[18px]" aria-hidden="true" /> Sair
      </button>
    </div>
  </div>
</template>
