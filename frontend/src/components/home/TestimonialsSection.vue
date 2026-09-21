<script setup lang="ts">
import { Star } from '@lucide/vue'
import { testimonials } from '../../data/home'

const loop = [...testimonials, ...testimonials]
</script>

<template>
  <section id="depoimentos" class="bg-paper-deep pt-[88px] pb-24">
    <div class="mx-auto w-full max-w-[1180px] px-7">
      <div class="mb-11 max-w-[620px]">
        <h2
          class="font-display text-[clamp(1.6rem,3vw,2.1rem)] font-extrabold tracking-[-0.02em] text-ink"
        >
          Depoimentos
        </h2>
        <p class="mt-3.5 text-base leading-[1.6] text-text-soft">
          O que dizem os clientes que já passaram pela cadeira.
        </p>
      </div>
    </div>
    <div
      class="mt-2 [mask-image:linear-gradient(90deg,transparent,#000_6%,#000_94%,transparent)]"
    >
      <div class="animate-marquee flex w-max gap-5 hover:[animation-play-state:paused]">
        <article
          v-for="(item, index) in loop"
          :key="`${item.name}-${index}`"
          class="w-[340px] shrink-0 rounded-md border border-line bg-white p-6 max-sm:w-[82vw]"
          :aria-hidden="index >= testimonials.length"
        >
          <div class="flex gap-[3px] text-gold">
            <Star
              v-for="n in 5"
              :key="n"
              class="size-3.5"
              :fill="n <= item.rating ? 'currentColor' : 'none'"
              :stroke-width="2"
            />
          </div>
          <p class="mt-3.5 text-[0.93rem] leading-[1.6] text-ink">“{{ item.quote }}”</p>
          <div class="mt-[18px] flex items-center gap-2.5">
            <span
              class="flex size-[34px] items-center justify-center rounded-[9px] bg-ink font-display text-[0.74rem] font-extrabold text-gold-light"
            >
              {{ item.initials }}
            </span>
            <b class="text-[0.85rem] font-bold text-ink">{{ item.name }}</b>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>
