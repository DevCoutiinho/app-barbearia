<script setup lang="ts">
import { computed } from 'vue'
import type { StockStatus } from '../../types/stock'
import { STATUS_LABELS } from '../../utils/stock'

const props = defineProps<{ status: StockStatus }>()

const tone = computed(() => ({
  OUT: { badge: 'bg-red-50 text-red-800', dot: 'bg-red-600' },
  LOW: { badge: 'bg-amber-50 text-amber-900', dot: 'bg-amber-500' },
  OK: { badge: 'bg-emerald-50 text-emerald-800', dot: 'bg-emerald-600' },
})[props.status])
</script>

<template>
  <span class="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="tone.badge">
    <span class="size-1.5 rounded-full" :class="tone.dot" aria-hidden="true"></span>{{ STATUS_LABELS[status] }}
  </span>
</template>
