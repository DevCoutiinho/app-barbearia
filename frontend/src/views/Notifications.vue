<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { toast } from 'vue-sonner'
import {
  Bell,
  CalendarCheck,
  Check,
  CircleAlert,
  CircleX,
  Clock,
  Loader2,
  Package,
  RefreshCw,
} from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import notificationService from '../services/notificationService'
import type { NotificationItem, NotificationType } from '../types/notification'

const notifications = ref<NotificationItem[]>([])
const loading = ref(false)
const filter = ref<'ALL' | 'UNREAD' | 'CANCELED'>('ALL')
const markingId = ref<string | null>(null)

async function loadNotifications() {
  loading.value = true
  try {
    const data = await notificationService.listMyNotifications()
    notifications.value = data
  } catch (err: any) {
    const msg = err?.response?.data?.message || 'Erro ao carregar notificações.'
    toast.error(msg)
  } finally {
    loading.value = false
  }
}

async function handleMarkAsRead(item: NotificationItem) {
  if (item.read || markingId.value) return
  markingId.value = item.id
  try {
    await notificationService.markAsRead(item.id)
    item.read = true
    toast.success('Notificação marcada como lida.')
  } catch (err: any) {
    const msg = err?.response?.data?.message || 'Erro ao atualizar notificação.'
    toast.error(msg)
  } finally {
    markingId.value = null
  }
}

async function markAllAsRead() {
  const unreadItems = notifications.value.filter(n => !n.read)
  if (!unreadItems.length) return
  for (const item of unreadItems) {
    try {
      await notificationService.markAsRead(item.id)
      item.read = true
    } catch {
      // Ignora erros individuais ao marcar em lote
    }
  }
  toast.success('Todas as notificações foram marcadas como lidas.')
}

const filteredNotifications = computed(() => {
  if (filter.value === 'UNREAD') {
    return notifications.value.filter(n => !n.read)
  }
  if (filter.value === 'CANCELED') {
    return notifications.value.filter(n => n.type === 'APPOINTMENT_CANCELED')
  }
  return notifications.value
})

const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)

function formatDate(iso: string): string {
  if (!iso) return ''
  try {
    const date = new Date(iso)
    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date)
  } catch {
    return iso
  }
}

function getTypeBadge(type: NotificationType) {
  switch (type) {
    case 'APPOINTMENT_CANCELED':
      return {
        label: 'Cancelamento',
        class: 'bg-red-50 text-red-700 border-red-200',
        icon: CircleX,
      }
    case 'NEW_APPOINTMENT':
      return {
        label: 'Novo Agendamento',
        class: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        icon: CalendarCheck,
      }
    case 'APPOINTMENT_REMINDER':
      return {
        label: 'Lembrete',
        class: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: Clock,
      }
    case 'LOW_STOCK':
      return {
        label: 'Estoque Baixo',
        class: 'bg-orange-50 text-orange-700 border-orange-200',
        icon: Package,
      }
    default:
      return {
        label: 'Sistema',
        class: 'bg-gray-100 text-gray-700 border-gray-200',
        icon: Bell,
      }
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-12 text-ink min-h-[calc(100vh-62px)]" aria-labelledby="notifications-title">
      <div class="mx-auto max-w-[960px] px-4 py-7 sm:px-6 lg:py-9">
        <!-- Cabeçalho -->
        <header class="mb-6 flex flex-wrap items-center justify-between gap-4">
          <div>
            <div class="flex items-center gap-2.5">
              <h1 id="notifications-title" class="text-2xl font-bold tracking-tight text-ink sm:text-[28px]">
                Notificações
              </h1>
              <span
                v-if="unreadCount > 0"
                class="inline-flex items-center rounded-full bg-[#a67039] px-2.5 py-0.5 text-xs font-bold text-white"
              >
                {{ unreadCount }} não lida{{ unreadCount > 1 ? 's' : '' }}
              </span>
            </div>
            <p class="mt-1 text-xs text-[#74747f] sm:text-sm">
              Acompanhe avisos de cancelamentos, agendamentos e alertas do sistema.
            </p>
          </div>

          <div class="flex items-center gap-2">
            <button
              v-if="unreadCount > 0"
              type="button"
              class="inline-flex items-center gap-1.5 rounded-xl border border-line bg-white px-3 py-2 text-xs font-semibold text-[#55555e] hover:bg-paper transition-colors"
              @click="markAllAsRead"
            >
              <Check class="size-4 text-emerald-600" />
              Marcar todas como lidas
            </button>

            <button
              type="button"
              class="inline-flex size-9 items-center justify-center rounded-xl border border-line bg-white text-[#55555e] hover:bg-paper transition-colors disabled:opacity-50"
              :disabled="loading"
              title="Atualizar notificações"
              @click="loadNotifications"
            >
              <RefreshCw class="size-4" :class="{ 'animate-spin': loading }" />
            </button>
          </div>
        </header>

        <!-- Filtros rápidos -->
        <div class="mb-5 flex items-center gap-2 border-b border-line pb-3">
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 text-xs font-medium transition-colors"
            :class="filter === 'ALL' ? 'bg-ink font-semibold text-white' : 'text-[#74747f] hover:bg-white hover:text-ink'"
            @click="filter = 'ALL'"
          >
            Todas ({{ notifications.length }})
          </button>
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 text-xs font-medium transition-colors"
            :class="filter === 'UNREAD' ? 'bg-ink font-semibold text-white' : 'text-[#74747f] hover:bg-white hover:text-ink'"
            @click="filter = 'UNREAD'"
          >
            Não lidas ({{ unreadCount }})
          </button>
          <button
            type="button"
            class="rounded-lg px-3 py-1.5 text-xs font-medium transition-colors"
            :class="filter === 'CANCELED' ? 'bg-ink font-semibold text-white' : 'text-[#74747f] hover:bg-white hover:text-ink'"
            @click="filter = 'CANCELED'"
          >
            Cancelamentos
          </button>
        </div>

        <!-- Estado de Carregamento -->
        <div v-if="loading && !notifications.length" class="flex flex-col items-center justify-center py-20 text-[#74747f]">
          <Loader2 class="size-8 animate-spin text-[#a67039]" />
          <p class="mt-3 text-sm">Carregando notificações...</p>
        </div>

        <!-- Lista de Notificações -->
        <div v-else-if="filteredNotifications.length" class="space-y-3">
          <article
            v-for="item in filteredNotifications"
            :key="item.id"
            class="relative flex flex-col sm:flex-row sm:items-start justify-between gap-4 rounded-2xl border p-4 sm:p-5 transition-all"
            :class="item.read ? 'border-line/80 bg-white/60' : 'border-[#e0c9aa] bg-white shadow-sm ring-1 ring-[#e0c9aa]/40'"
          >
            <div class="flex items-start gap-3.5">
              <!-- Ícone do tipo -->
              <span
                class="flex size-10 shrink-0 items-center justify-center rounded-xl border text-sm"
                :class="getTypeBadge(item.type).class"
              >
                <component :is="getTypeBadge(item.type).icon" class="size-5" />
              </span>

              <!-- Conteúdo -->
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <h2 class="text-sm font-bold text-ink sm:text-[15px]">
                    {{ item.title }}
                  </h2>
                  <span
                    class="inline-flex items-center rounded-md border px-2 py-0.5 text-[10px] font-semibold"
                    :class="getTypeBadge(item.type).class"
                  >
                    {{ getTypeBadge(item.type).label }}
                  </span>
                  <span
                    v-if="!item.read"
                    class="inline-flex items-center gap-1 rounded-full bg-[#fcf4e8] px-2 py-0.5 text-[10px] font-bold text-[#a67039]"
                  >
                    <span class="size-1.5 rounded-full bg-[#a67039]"></span>
                    Nova
                  </span>
                </div>

                <p class="mt-1 text-xs text-[#404048] leading-relaxed sm:text-[13px]">
                  {{ item.message }}
                </p>

                <p class="mt-2 flex items-center gap-1 text-[11px] text-[#74747f]">
                  <Clock class="size-3" />
                  <span>{{ formatDate(item.createdAt) }}</span>
                </p>
              </div>
            </div>

            <!-- Botão Marcar como Lida -->
            <div class="flex shrink-0 items-center justify-end">
              <button
                v-if="!item.read"
                type="button"
                class="inline-flex items-center gap-1.5 rounded-lg border border-line bg-paper px-3 py-1.5 text-xs font-semibold text-[#55555e] hover:bg-white active:scale-95 transition-all disabled:opacity-50"
                :disabled="markingId === item.id"
                title="Marcar como lida"
                @click="handleMarkAsRead(item)"
              >
                <Loader2 v-if="markingId === item.id" class="size-3.5 animate-spin" />
                <Check v-else class="size-3.5 text-emerald-600" />
                Marcar como lida
              </button>
            </div>
          </article>
        </div>

        <!-- Empty State -->
        <div v-else class="rounded-2xl border border-line bg-white py-16 text-center text-sm text-[#74747f]">
          <div class="flex flex-col items-center justify-center">
            <Bell class="size-10 text-[#d8c3a9] mb-3" />
            <p class="text-base font-semibold text-ink">Nenhuma notificação encontrada.</p>
            <p class="mt-1 text-xs text-[#74747f]">
              {{ filter === 'UNREAD' ? 'Você leu todas as notificações!' : 'Você não possui notificações no momento.' }}
            </p>
            <button
              v-if="filter !== 'ALL'"
              type="button"
              class="mt-3 text-xs font-semibold text-[#a67039] hover:underline"
              @click="filter = 'ALL'"
            >
              Ver todas as notificações
            </button>
          </div>
        </div>
      </div>
    </section>
  </DashboardLayout>
</template>
