<script setup lang="ts">
import { AlertTriangle } from '@lucide/vue'
import type { Product } from '../../types/stock'
import { formatQuantity } from '../../utils/stock'

defineProps<{ products: Product[] }>()
</script>

<template>
  <div v-if="products.length" class="mb-6 flex gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-amber-900 sm:p-5" role="alert">
    <AlertTriangle class="mt-0.5 size-5 shrink-0 text-amber-600" aria-hidden="true" />
    <div class="min-w-0">
      <p class="text-[14px] font-semibold">{{ products.length === 1 ? '1 produto abaixo do mínimo' : `${products.length} produtos abaixo do mínimo` }}</p>
      <ul class="mt-2 flex flex-wrap gap-2 text-[12px]">
        <li v-for="product in products" :key="product.id" class="rounded-full border border-amber-200 bg-white/70 px-2.5 py-1">
          <span class="font-semibold">{{ product.name }}</span>
          <span class="text-amber-800"> · {{ formatQuantity(product.quantity, product.unit) }} de {{ formatQuantity(product.minimum, product.unit) }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>
