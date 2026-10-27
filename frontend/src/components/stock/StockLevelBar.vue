<script setup lang="ts">
import { computed } from 'vue'
import type { Product } from '../../types/stock'
import { formatQuantity, stockLevelPercent, stockStatus } from '../../utils/stock'

const props = defineProps<{ product: Product }>()

const percent = computed(() => stockLevelPercent(props.product))
const barColor = computed(() => ({ OUT: 'bg-red-600', LOW: 'bg-amber-500', OK: 'bg-emerald-600' })[stockStatus(props.product)])
</script>

<template>
  <div class="min-w-[120px]">
    <p class="font-semibold">{{ formatQuantity(product.quantity) }}</p>
    <div class="mt-1.5 h-1.5 w-full max-w-[140px] overflow-hidden rounded-full bg-paper-deep" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100" :aria-label="`Nível de estoque de ${product.name}`">
      <div class="h-full rounded-full transition-[width] motion-reduce:transition-none" :class="barColor" :style="{ width: `${percent}%` }"></div>
    </div>
  </div>
</template>
