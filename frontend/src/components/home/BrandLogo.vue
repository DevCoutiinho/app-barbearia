<script setup lang="ts">
import { Scissors } from '@lucide/vue'

defineProps<{
  dark?: boolean
}>()
</script>

<template>
  <span
    class="flex items-center gap-2.5 font-display text-[1.05rem] font-extrabold transition-colors duration-[350ms]"
    :class="dark ? 'text-on-dark' : 'text-ink'"
  >
    <span class="flex size-[34px] shrink-0 items-center justify-center rounded-[9px] bg-ink">
      <Scissors class="size-[18px] text-gold-light" :stroke-width="2" />
    </span>
    Barbearia Ferro
  </span>
</template>
