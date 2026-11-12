<script setup lang="ts">
import { CalendarOff, Trash2 } from '@lucide/vue'
import type { ScheduleBlock } from '../../types/agenda'
import { formatBlockPeriod, isFullDayBlock } from '../../utils/agenda'

defineProps<{
  blocks: ScheduleBlock[]
  busy: boolean
}>()
const emit = defineEmits<{ (e: 'remove', block: ScheduleBlock): void }>()
</script>

<template>
  <ul class="divide-y divide-line" aria-label="Bloqueios cadastrados">
    <li v-for="block in blocks" :key="block.id" class="flex items-center gap-3 px-4 py-3 sm:px-5">
      <span class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-red-50 text-red-700">
        <CalendarOff class="size-4" :stroke-width="1.8" aria-hidden="true" />
      </span>
      <div class="min-w-0 flex-1">
        <p class="flex flex-wrap items-center gap-2 text-[13px] font-semibold">
          {{ block.reasonLabel }}
          <span class="rounded-full bg-paper px-2 py-0.5 text-[11px] font-medium text-[#55555e]">{{ isFullDayBlock(block) ? 'Dia inteiro' : 'Horário' }}</span>
        </p>
        <p class="text-[12px] text-[#74747f] first-letter:uppercase">{{ formatBlockPeriod(block) }}</p>
        <p v-if="block.description" class="mt-0.5 break-words text-[12px] text-[#55555e]">{{ block.description }}</p>
      </div>
      <button
        type="button"
        class="button button-secondary button-sm"
        :disabled="busy"
        :aria-label="`Excluir bloqueio: ${block.reasonLabel}, ${formatBlockPeriod(block)}`"
        @click="emit('remove', block)"
      >
        <Trash2 class="size-3.5" aria-hidden="true" /><span class="hidden sm:inline">Excluir</span>
      </button>
    </li>
  </ul>
</template>

<style scoped src="../stock/stock-ui.css"></style>
