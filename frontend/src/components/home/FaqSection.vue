<script setup lang="ts">
import { ref } from 'vue'
import { ChevronDown } from '@lucide/vue'
import { faqItems } from '../../data/home'

const openIds = ref<string[]>([])

function toggle(id: string) {
  openIds.value = openIds.value.includes(id)
    ? openIds.value.filter((item) => item !== id)
    : [...openIds.value, id]
}

function isOpen(id: string) {
  return openIds.value.includes(id)
}
</script>

<template>
  <section id="faq" class="bg-paper pt-[88px] pb-24">
    <div class="mx-auto w-full max-w-[1180px] px-7">
      <div class="mb-11 max-w-[620px]">
        <h2
          class="font-display text-[clamp(1.6rem,3vw,2.1rem)] font-extrabold tracking-[-0.02em] text-ink"
        >
          Perguntas frequentes
        </h2>
      </div>
      <div class="mx-auto flex max-w-[920px] flex-col gap-3.5">
        <div
          v-for="item in faqItems"
          :key="item.id"
          class="overflow-hidden rounded-md border bg-white transition duration-220"
          :class="
            isOpen(item.id)
              ? 'border-gold shadow-[0_16px_34px_-22px_rgba(156,108,61,0.45)]'
              : 'border-line hover:border-[#d8d3cb]'
          "
        >
          <button
            type="button"
            class="flex w-full items-center justify-between gap-[18px] px-6 py-[22px] text-left text-[1.02rem] font-bold text-ink"
            @click="toggle(item.id)"
          >
            {{ item.question }}
            <span
              class="flex size-[34px] shrink-0 items-center justify-center rounded-full border transition-colors duration-250"
              :class="isOpen(item.id) ? 'border-gold bg-gold' : 'border-line bg-paper'"
            >
              <ChevronDown
                class="size-[15px] transition-transform duration-280"
                :class="isOpen(item.id) ? 'rotate-180 text-white' : 'text-text-soft'"
                :stroke-width="2"
              />
            </span>
          </button>
          <div
            class="grid transition-[grid-template-rows] duration-320"
            :class="isOpen(item.id) ? 'grid-rows-[1fr]' : 'grid-rows-[0fr]'"
          >
            <div class="overflow-hidden">
              <p class="mx-6 border-t border-line pt-[18px] pb-6 text-[0.95rem] leading-[1.7] text-text-soft">
                {{ item.answer }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
