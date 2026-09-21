<script setup lang="ts">
import type { Component } from 'vue'
import { Check, Clock, Heart, Scissors, Sparkles } from '@lucide/vue'
import { aboutDiffs, type AboutDiff } from '../../data/home'

const icons: Record<AboutDiff['icon'], Component> = {
  check: Check,
  sparkles: Sparkles,
  clock: Clock,
  heart: Heart,
}
</script>

<template>
  <section id="sobre" class="bg-paper py-[88px]">
    <div class="mx-auto grid w-full max-w-[1180px] items-center gap-[60px] px-7 max-lg:grid-cols-1 lg:grid-cols-[1.05fr_0.95fr]">
      <div>
        <h2
          class="mb-[22px] font-display text-[clamp(1.6rem,3vw,2.1rem)] font-extrabold tracking-[-0.02em] text-ink"
        >
          Sobre a Barbearia
        </h2>
        <p class="max-w-[480px] text-base leading-[1.75] text-text-soft">
          Há mais de dez anos a Barbearia Ferro atende em Valença com um compromisso simples: técnica
          apurada, navalha bem afiada e atenção a cada detalhe do corte. Nascemos da paixão pelo
          ofício tradicional da barbearia e crescemos sem abrir mão dele.
        </p>
        <p class="mt-3.5 max-w-[480px] text-base leading-[1.75] text-text-soft">
          Hoje somos uma equipe de profissionais especializados, num ambiente pensado para que cada
          visita seja tranquila, precisa e sem pressa — do primeiro corte ao acabamento final.
        </p>
        <div class="mt-[30px] grid grid-cols-1 gap-x-[26px] gap-y-[18px] sm:grid-cols-2">
          <div v-for="diff in aboutDiffs" :key="diff.label" class="flex items-start gap-3">
            <span
              class="flex size-[34px] shrink-0 items-center justify-center rounded-[9px] bg-gold-pale text-gold"
            >
              <component :is="icons[diff.icon]" class="size-[17px]" :stroke-width="2" />
            </span>
            <span class="text-[0.9rem] leading-[1.4] font-semibold text-ink">{{ diff.label }}</span>
          </div>
        </div>
      </div>

      <div
        class="relative order-first flex aspect-16/10 items-center justify-center overflow-hidden rounded-lg lg:order-0 lg:aspect-4/5"
        style="
          background:
            radial-gradient(circle at 25% 20%, rgba(192, 138, 85, 0.35), transparent 55%),
            linear-gradient(155deg, #232326 0%, #1a1a1c 60%, #100f10 100%);
        "
      >
        <div class="absolute size-[78%] rounded-full border border-gold-light/28" />
        <div class="absolute size-[56%] rounded-full border border-gold-light/28" />
        <div
          class="flex size-24 items-center justify-center rounded-3xl border border-on-dark/14 bg-on-dark/6 text-gold-light"
        >
          <Scissors class="size-11" :stroke-width="1.6" />
        </div>
        <div
          class="absolute right-6 bottom-6 left-6 rounded-xl border border-on-dark/12 bg-ink/55 px-4 py-3.5 text-on-dark backdrop-blur-[6px]"
        >
          <b class="block text-[0.85rem] font-bold">Ambiente e conceito</b>
          <span class="text-[0.78rem] text-on-dark-soft">
            Barbearia tradicional, cadeira de precisão e navalha em cada acabamento.
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
