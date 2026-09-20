<script setup lang="ts">
import { ref, type Component } from 'vue'
import { Check, ChevronDown, Clock, Scissors, Sparkles, SprayCan } from '@lucide/vue'
import { barbers, services, type ServiceIcon } from '../../data/home'

const openIds = ref<string[]>([])

function toggle(id: string) {
  openIds.value = openIds.value.includes(id)
    ? openIds.value.filter((item) => item !== id)
    : [...openIds.value, id]
}

function isOpen(id: string) {
  return openIds.value.includes(id)
}

const serviceIcons: Record<ServiceIcon, Component> = {
  cut: Scissors,
  beard: SprayCan,
  combo: Scissors,
  brow: Sparkles,
  platinum: Sparkles,
}
</script>

<template>
  <section id="servicos" class="bg-white pt-[88px] pb-24">
    <div class="mx-auto w-full max-w-[1180px] px-7">
      <div class="mb-11 max-w-[620px]">
        <h2
          class="font-display text-[clamp(1.6rem,3vw,2.1rem)] font-extrabold tracking-[-0.02em] text-ink"
        >
          Serviços e Barbeiros
        </h2>
        <p class="mt-3.5 text-base leading-[1.6] text-text-soft">
          Conheça nossos barbeiros e, logo abaixo, cada serviço com seus detalhes.
        </p>
      </div>

      <div
        class="mb-[52px] grid grid-cols-1 gap-[18px] border-b border-line pb-[52px] sm:grid-cols-2 lg:grid-cols-4"
      >
        <div
          v-for="barber in barbers"
          :key="barber.name"
          class="flex items-center gap-3.5 rounded-md border border-line bg-paper p-[18px] transition hover:-translate-y-0.5 hover:border-[#d8d3cb] hover:shadow-[0_14px_30px_-20px_rgba(0,0,0,0.22)]"
        >
          <span
            class="flex size-[52px] shrink-0 items-center justify-center rounded-xl bg-ink font-display text-base font-extrabold text-gold-light"
          >
            {{ barber.initials }}
          </span>
          <span class="min-w-0 leading-[1.3]">
            <b class="block text-[0.95rem] font-bold text-ink">{{ barber.name }}</b>
            <span class="text-[0.8rem] text-text-soft">{{ barber.specialty }}</span>
          </span>
        </div>
      </div>

      <div class="grid grid-cols-1 items-start gap-[22px] sm:grid-cols-2 lg:grid-cols-3">
        <article
          v-for="service in services"
          :key="service.id"
          class="overflow-hidden rounded-lg border border-line bg-paper transition hover:border-[#d8d3cb] hover:shadow-[0_14px_30px_-20px_rgba(0,0,0,0.25)]"
        >
          <div
            class="relative flex h-[132px] items-center justify-center"
            style="background: linear-gradient(150deg, #1a1a1c 0%, #100f10 100%)"
          >
            <div
              class="pointer-events-none absolute inset-0"
              style="background: radial-gradient(circle at 75% 15%, rgba(192, 138, 85, 0.32), transparent 60%)"
            />
            <component
              :is="serviceIcons[service.icon]"
              class="relative z-1 size-[38px] text-gold-light"
              :stroke-width="1.8"
            />
          </div>
          <div class="px-[22px] pt-[22px] pb-6">
            <h3 class="font-display text-[1.08rem] font-bold tracking-[-0.02em] text-ink">
              {{ service.title }}
            </h3>
            <p class="mt-2 text-[0.9rem] leading-[1.55] text-text-soft">{{ service.summary }}</p>
            <button
              type="button"
              class="mt-[18px] flex items-center gap-2 text-[0.88rem] font-bold text-gold"
              @click="toggle(service.id)"
            >
              Ver informações
              <ChevronDown
                class="size-[15px] transition-transform duration-250"
                :class="{ 'rotate-180': isOpen(service.id) }"
                :stroke-width="2"
              />
            </button>
            <div
              class="grid transition-[grid-template-rows] duration-320"
              :class="isOpen(service.id) ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
            >
              <div class="overflow-hidden">
                <div class="mt-[18px] border-t border-line px-0 pt-[18px]">
                  <p class="text-[0.89rem] leading-[1.65] text-text-soft">
                    {{ service.fullDescription }}
                  </p>
                  <ul class="mt-3.5 flex flex-col gap-[7px]">
                    <li
                      v-for="item in service.includes"
                      :key="item"
                      class="flex items-start gap-2 text-[0.87rem] text-ink"
                    >
                      <Check class="mt-[3px] size-3.5 shrink-0 text-gold" :stroke-width="2" />
                      {{ item }}
                    </li>
                  </ul>
                  <span class="mt-3.5 inline-flex items-center gap-1.5 text-[0.8rem] font-semibold text-text-soft">
                    <Clock class="size-3.5" :stroke-width="2" />
                    Duração aproximada: {{ service.duration }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
