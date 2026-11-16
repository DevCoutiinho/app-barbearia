<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  CalendarPlus,
  Clock,
  Phone,
  RefreshCw,
  Scissors,
  Search,
  Sparkles,
  User,
} from '@lucide/vue'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import agendaService from '../services/agendaService'
import type { CatalogService } from '../types/agenda'
import { formatPrice } from '../utils/agenda'
import { agendaErrorMessage } from '../utils/agendaErrors'

const router = useRouter()

const catalog = ref<CatalogService[]>([])
const loading = ref(false)
const error = ref('')
const search = ref('')

const techniqueLabels: Record<string, string> = {
  MACHINE: 'Máquina',
  SCISSORS: 'Tesoura',
  RAZOR: 'Navalha',
  MIXED: 'Misto',
  TWEEZERS: 'Pinça',
  WAX: 'Cera',
}

async function loadCatalog() {
  loading.value = true
  error.value = ''
  try {
    catalog.value = await agendaService.listCatalog()
  } catch (cause) {
    error.value = agendaErrorMessage(cause, 'Não foi possível carregar o catálogo de serviços.')
  } finally {
    loading.value = false
  }
}

const filteredServices = computed(() => {
  const query = search.value.trim().toLowerCase()
  if (!query) return catalog.value

  return catalog.value.filter((service) => {
    const matchName = service.name.toLowerCase().includes(query)
    const matchBarber = service.barber.name.toLowerCase().includes(query)
    const matchTechnique = (techniqueLabels[service.technique] ?? service.technique)
      .toLowerCase()
      .includes(query)
    return matchName || matchBarber || matchTechnique
  })
})

function selectService(service: CatalogService) {
  router.push({
    path: '/agendar',
    query: { serviceId: service.id },
  })
}

function getInitials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .map((part) => part[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}

onMounted(loadCatalog)
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-12 text-ink" aria-labelledby="catalog-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <!-- Cabeçalho -->
        <div class="mb-7 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="mb-1 text-[11px] font-semibold tracking-[1.5px] text-[#a67039] uppercase">Catálogo</p>
            <h1 id="catalog-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Serviços Disponíveis</h1>
            <p class="mt-1 text-[14px] text-[#74747f]">
              Escolha o procedimento ideal e agende diretamente com o barbeiro especialista.
            </p>
          </div>
          <button
            type="button"
            class="button button-secondary button-sm"
            :disabled="loading"
            @click="loadCatalog"
          >
            <RefreshCw class="size-3.5" :class="{ 'animate-spin': loading }" /> Atualizar
          </button>
        </div>

        <!-- Barra de Busca -->
        <div class="mb-6 flex max-w-md items-center gap-2 rounded-xl border border-line bg-white px-3.5 py-2.5 shadow-2xs">
          <Search class="size-4 shrink-0 text-[#74747f]" />
          <input
            v-model="search"
            type="search"
            placeholder="Buscar por serviço, barbeiro ou técnica..."
            class="w-full bg-transparent text-xs text-ink placeholder:text-[#9e9ea7] focus:outline-none"
          />
        </div>

        <!-- Estado de Erro -->
        <div v-if="error" class="rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
          <p>{{ error }}</p>
          <button type="button" class="mt-2 font-semibold underline" @click="loadCatalog">Tentar novamente</button>
        </div>

        <!-- Estado de Carregamento -->
        <div
          v-else-if="loading"
          class="rounded-2xl border border-line bg-white px-5 py-16 text-center text-sm text-[#74747f]"
          role="status"
        >
          <RefreshCw class="mx-auto mb-3 size-6 animate-spin text-[#a67039]" />
          Carregando catálogo de serviços...
        </div>

        <!-- Estado EMPTY: Nenhum serviço retornado pelo backend -->
        <div
          v-else-if="!catalog.length"
          class="rounded-2xl border border-line bg-white px-5 py-16 text-center"
          role="status"
        >
          <div class="mx-auto mb-4 flex size-14 items-center justify-center rounded-2xl bg-amber-50 text-[#a67039]">
            <Scissors class="size-7" :stroke-width="1.5" />
          </div>
          <h2 class="text-base font-bold text-ink">Nenhum serviço disponível no momento</h2>
          <p class="mx-auto mt-1 max-w-md text-xs text-[#74747f]">
            Os barbeiros ainda não cadastraram serviços ativos no catálogo da barbearia. Por favor, volte mais tarde ou entre em contato conosco.
          </p>
          <button
            type="button"
            class="button button-secondary button-sm mt-5"
            @click="loadCatalog"
          >
            Tentar Novamente
          </button>
        </div>

        <!-- Estado EMPTY de Busca: Nenhum serviço com o filtro informado -->
        <div
          v-else-if="!filteredServices.length"
          class="rounded-2xl border border-line bg-white px-5 py-12 text-center"
          role="status"
        >
          <Search class="mx-auto mb-3 size-8 text-[#9e9ea7]" />
          <p class="text-sm font-semibold text-ink">Nenhum serviço encontrado para "{{ search }}"</p>
          <p class="mt-0.5 text-xs text-[#74747f]">Tente outros termos de busca.</p>
          <button type="button" class="mt-3 text-xs font-semibold text-[#a67039] underline" @click="search = ''">
            Limpar busca
          </button>
        </div>

        <!-- Grid de Cards do Catálogo -->
        <div v-else class="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          <article
            v-for="service in filteredServices"
            :key="service.id"
            class="flex flex-col justify-between rounded-2xl border border-line bg-white p-5 shadow-xs transition hover:border-ink hover:shadow-md"
          >
            <div>
              <!-- Cabeçalho do Card -->
              <div class="flex items-start justify-between gap-3">
                <span
                  v-if="service.technique"
                  class="inline-flex items-center gap-1 rounded-md border border-stone-200 bg-paper px-2 py-0.5 text-[10px] font-semibold text-[#55555e] uppercase tracking-wider"
                >
                  <Sparkles class="size-2.5 text-[#a67039]" />
                  {{ techniqueLabels[service.technique] ?? service.technique }}
                </span>
                <span class="text-base font-bold text-ink">
                  {{ formatPrice(service.price) }}
                </span>
              </div>

              <!-- Título do Serviço -->
              <h3 class="mt-2.5 text-lg font-bold text-ink leading-snug">
                {{ service.name }}
              </h3>

              <!-- Duração -->
              <div class="mt-2 flex items-center gap-1.5 text-xs text-[#74747f]">
                <Clock class="size-3.5 text-[#a67039]" />
                <span>{{ service.durationTime }} minutos de atendimento</span>
              </div>

              <!-- Barbeiro Responsável -->
              <div class="mt-4 flex items-center gap-3 rounded-xl bg-paper p-3 border border-line/50">
                <div
                  v-if="service.barber.avatar"
                  class="size-9 shrink-0 overflow-hidden rounded-full border border-line"
                >
                  <img
                    :src="service.barber.avatar"
                    :alt="service.barber.name"
                    class="size-full object-cover"
                  />
                </div>
                <div
                  v-else
                  class="flex size-9 shrink-0 items-center justify-center rounded-full bg-ink text-xs font-bold text-white shadow-2xs"
                >
                  {{ getInitials(service.barber.name) }}
                </div>
                <div class="min-w-0 flex-1 text-xs">
                  <p class="truncate font-semibold text-ink">{{ service.barber.name }}</p>
                  <p v-if="service.barber.telephone" class="mt-0.5 flex items-center gap-1 text-[11px] text-[#74747f]">
                    <Phone class="size-2.5" /> {{ service.barber.telephone }}
                  </p>
                  <p v-else class="text-[11px] text-[#74747f]">Barbeiro Profissional</p>
                </div>
              </div>
            </div>

            <!-- Botão de Ação para Pré-selecionar e Agendar -->
            <div class="mt-5 border-t border-line pt-4">
              <button
                type="button"
                class="button button-primary w-full justify-center text-xs font-semibold gap-1.5"
                @click="selectService(service)"
              >
                <CalendarPlus class="size-3.5" /> Agendar este serviço
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>

