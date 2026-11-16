<script setup lang="ts">
import { ArrowDownLeft, ArrowUpRight } from '@lucide/vue'
import type { MovementType } from '../../types/stock'
import { MOVEMENT_LABELS } from '../../utils/stock'

defineProps<{ type: MovementType }>()
</script>

<template>
  <span class="inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="type === 'ENTRY' ? 'bg-emerald-50 text-emerald-800' : 'bg-red-50 text-red-800'">
    <component :is="type === 'ENTRY' ? ArrowDownLeft : ArrowUpRight" class="size-3" :stroke-width="2" aria-hidden="true" />{{ MOVEMENT_LABELS[type] }}
  </span>
</template>
