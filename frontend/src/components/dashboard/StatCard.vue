<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  description?: string
  icon: Component
  accent?: boolean
}>()
</script>

<template>
  <article class="min-h-[140px] rounded-[16px] border border-line bg-white p-5">
    <div class="flex items-start justify-between gap-3">
      <p class="text-[13px] font-medium text-[#74747f]">{{ label }}</p>
      <span class="flex size-8 shrink-0 items-center justify-center rounded-[10px]" :class="accent ? 'bg-[#f5ede3] text-[#a67039]' : 'bg-[#ededeb] text-[#505055]'">
        <component :is="icon" class="size-4" :stroke-width="1.7" aria-hidden="true" />
      </span>
    </div>
    <p class="mt-2 text-[28px] leading-9 font-bold tracking-[-1px]">{{ value }}</p>
    <p v-if="description" class="mt-0.5 text-xs leading-5 text-[#74747f]">{{ description }}</p>
  </article>
</template>
