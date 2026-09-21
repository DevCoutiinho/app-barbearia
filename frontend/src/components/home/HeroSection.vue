<script setup lang="ts">
import { heroStats } from '../../data/home'
</script>

<template>
  <section id="home" class="bg-ink pt-[168px] pb-[88px]">
    <div class="mx-auto w-full max-w-[1180px] px-7">
      <div class="max-w-[640px]">
        <h1
          class="font-display text-[clamp(2rem,4.6vw,2.9rem)] leading-[1.12] font-extrabold tracking-[-0.02em] text-on-dark"
        >
          A cadeira cheia começa na agenda organizada.
        </h1>
        <p class="mt-5 max-w-[520px] text-[1.05rem] leading-[1.6] text-on-dark-soft">
          Escolha o barbeiro, veja o horário livre e confirme em menos de um minuto. Sem ligação, sem
          espera.
        </p>
        <div class="mt-[30px] flex flex-wrap gap-3.5">
          <a
            href="#contato"
            class="inline-flex items-center justify-center rounded-[10px] bg-gold px-[26px] py-[13px] text-[0.95rem] font-semibold whitespace-nowrap text-white transition hover:bg-gold-light hover:shadow-[0_8px_20px_-8px_rgba(156,108,61,0.6)] active:translate-y-px"
          >
            Agendar horário
          </a>
          <a
            href="#servicos"
            class="inline-flex items-center justify-center rounded-[10px] border border-on-dark/28 px-[26px] py-[13px] text-[0.95rem] font-semibold whitespace-nowrap text-on-dark transition hover:border-on-dark/60 active:translate-y-px"
          >
            Ver barbeiros
          </a>
        </div>
        <div class="mt-[52px] flex flex-wrap gap-11">
          <div v-for="stat in heroStats" :key="stat.label">
            <b class="block font-display text-[1.7rem] font-extrabold text-gold-light">{{
              stat.value
            }}</b>
            <span class="text-[0.86rem] text-on-dark-soft">{{ stat.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
