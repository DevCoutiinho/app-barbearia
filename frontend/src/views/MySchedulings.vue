<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { CalendarPlus, Clock, Loader2, RefreshCw, Scissors, User } from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import { useSchedulingStore } from '../stores/scheduling'
import { SCHEDULING_STATUS_LABELS } from '../types/scheduling'
import type { SchedulingStatus } from '../types/scheduling'

const store = useSchedulingStore()

const money = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)

const formatDateTime = (instant: string) => {
  const date = new Date(instant)
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
    timeZone: 'America/Sao_Paulo',
  }).format(date)
}

const statusClasses: Record<SchedulingStatus, string> = {
  PENDING: 'bg-[#fbf0db] text-[#b57d19]',
  SCHEDULED: 'bg-[#e5f2ec] text-[#28835e]',
  COMPLETED: 'bg-[#ededeb] text-[#505055]',
  CANCELED: 'bg-red-50 text-red-700',
}

onMounted(() => store.fetchMySchedulings())
</script>

<template>
  <DashboardLayout>
    <div class="px-4 pt-[26px] pb-9 sm:px-[26px]">
      <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 class="text-[24px] font-bold tracking-[-0.8px]">Meus agendamentos</h1>
          <p class="mt-1 text-sm text-[#74747f]">Histórico e próximos atendimentos.</p>
        </div>
        <div class="flex gap-2">
          <button
            type="button"
            class="action-button"
            :disabled="store.loadingSchedulings"
            @click="store.fetchMySchedulings"
          >
            <Loader2 v-if="store.loadingSchedulings" class="size-4 animate-spin" aria-hidden="true" />
            <RefreshCw v-else class="size-4" aria-hidden="true" />
            Atualizar
          </button>
          <RouterLink :to="{ name: 'booking' }" class="action-button border-ink bg-ink text-white hover:bg-[#2c2c2f]">
            <CalendarPlus class="size-4" aria-hidden="true" />
            Novo agendamento
          </RouterLink>
        </div>
      </div>

      <!-- Carregando -->
      <div v-if="store.loadingSchedulings" class="flex items-center gap-2 text-sm text-[#74747f]">
        <Loader2 class="size-4 animate-spin" aria-hidden="true" />
        Carregando agendamentos...
      </div>

      <!-- Erro -->
      <div v-else-if="store.schedulingsError" class="flex flex-col items-start gap-3">
        <p class="text-sm text-red-600">{{ store.schedulingsError }}</p>
        <button type="button" class="action-button text-sm" @click="store.fetchMySchedulings">
          <RefreshCw class="size-4" aria-hidden="true" />Tentar novamente
        </button>
      </div>

      <!-- Vazio -->
      <div
        v-else-if="store.mySchedulings.length === 0"
        class="flex flex-col items-center gap-4 rounded-2xl border border-dashed border-line bg-white py-16 text-center"
      >
        <CalendarPlus class="size-10 text-[#74747f]" aria-hidden="true" />
        <div>
          <p class="font-semibold">Nenhum agendamento encontrado</p>
          <p class="mt-1 text-sm text-[#74747f]">Que tal agendar seu próximo atendimento?</p>
        </div>
        <RouterLink :to="{ name: 'booking' }" class="action-button border-ink bg-ink text-white hover:bg-[#2c2c2f]">
          <CalendarPlus class="size-4" aria-hidden="true" />Agendar agora
        </RouterLink>
      </div>

      <!-- Lista -->
      <ul v-else class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3" role="list">
        <li
          v-for="item in store.mySchedulings"
          :key="item.id"
          class="rounded-2xl border border-line bg-white p-4"
        >
          <div class="mb-3 flex items-start justify-between gap-2">
            <span
              class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold"
              :class="statusClasses[item.status]"
            >
              <span class="size-1.5 rounded-full bg-current" aria-hidden="true" />
              {{ SCHEDULING_STATUS_LABELS[item.status] }}
            </span>
            <span class="text-xs text-[#74747f]">{{ formatDateTime(item.scheduledAt) }}</span>
          </div>

          <dl class="space-y-1.5 text-sm">
            <div class="flex items-center gap-2">
              <dt class="sr-only">Serviço</dt>
              <Scissors class="size-3.5 shrink-0 text-[#74747f]" aria-hidden="true" />
              <dd class="font-semibold">{{ item.serviceName }}</dd>
            </div>
            <div class="flex items-center gap-2">
              <dt class="sr-only">Barbeiro</dt>
              <User class="size-3.5 shrink-0 text-[#74747f]" aria-hidden="true" />
              <dd class="text-[#55555e]">{{ item.barberName }}</dd>
            </div>
            <div class="flex items-center gap-2">
              <dt class="sr-only">Duração</dt>
              <Clock class="size-3.5 shrink-0 text-[#74747f]" aria-hidden="true" />
              <dd class="text-[#55555e]">{{ item.durationTime }} min</dd>
            </div>
          </dl>

          <div class="mt-3 border-t border-line pt-3">
            <span class="text-sm font-bold text-[#a67039]">{{ money(item.finalPrice) }}</span>
          </div>
        </li>
      </ul>
    </div>
  </DashboardLayout>
</template>

<style scoped>
@layer components {
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
  text-decoration: none;
  color: inherit;
}
.action-button:hover { background-color: #ededeb; }
.action-button:focus-visible { outline: 2px solid #a67039; outline-offset: 3px; }
.action-button:disabled { opacity: 0.4; cursor: not-allowed; }
}
</style>
