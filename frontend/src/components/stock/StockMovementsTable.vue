<script setup lang="ts">
import MovementTypeBadge from './MovementTypeBadge.vue'
import type { StockMovement, StockUnit } from '../../types/stock'
import { formatDate, formatQuantity } from '../../utils/stock'

withDefaults(defineProps<{
  movements: StockMovement[]
  showProduct?: boolean
  units?: Record<string, StockUnit>
  caption: string
}>(), { showProduct: false, units: () => ({}) })
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-left text-[13px]" :class="showProduct ? 'min-w-[760px]' : 'min-w-[440px]'">
      <caption class="sr-only">{{ caption }}</caption>
      <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
        <tr>
          <th scope="col" class="px-5 py-3 font-semibold">Data</th>
          <th v-if="showProduct" scope="col" class="px-5 py-3 font-semibold">Produto</th>
          <th scope="col" class="px-5 py-3 font-semibold">Tipo</th>
          <th scope="col" class="px-5 py-3 font-semibold">Quantidade</th>
          <th scope="col" class="px-5 py-3 font-semibold">Responsável</th>
          <th v-if="showProduct" scope="col" class="px-5 py-3 font-semibold">Observação</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-line/70">
        <tr v-for="movement in movements" :key="movement.id" class="align-top hover:bg-paper/40">
          <td class="whitespace-nowrap px-5 py-3.5 text-[#55555e]">{{ formatDate(movement.date) }}</td>
          <td v-if="showProduct" class="max-w-[220px] break-words px-5 py-3.5 font-semibold">{{ movement.productName }}</td>
          <td class="px-5 py-3.5"><MovementTypeBadge :type="movement.type" /></td>
          <td class="whitespace-nowrap px-5 py-3.5 font-semibold" :class="movement.type === 'ENTRY' ? 'text-emerald-800' : 'text-red-800'">
            {{ movement.type === 'ENTRY' ? '+' : '−' }}{{ formatQuantity(movement.quantity, units[movement.productId]) }}
          </td>
          <td class="px-5 py-3.5 text-[#55555e]">
            {{ movement.user.name }}
            <p v-if="!showProduct && movement.note" class="mt-1 text-xs text-[#74747f]">{{ movement.note }}</p>
          </td>
          <td v-if="showProduct" class="max-w-[220px] break-words px-5 py-3.5 text-[#74747f]">{{ movement.note || '—' }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
