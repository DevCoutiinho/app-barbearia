<script setup lang="ts">
import { ArrowDownToLine, ArrowUpFromLine, Eye, Trash2 } from '@lucide/vue'
import StockLevelBar from './StockLevelBar.vue'
import StockStatusBadge from './StockStatusBadge.vue'
import type { Product } from '../../types/stock'
import { STOCK_UNITS } from '../../types/stock'
import { formatDate, formatQuantity, stockStatus } from '../../utils/stock'

defineProps<{
  products: Product[]
  canManage: boolean
  busy: boolean
}>()

const emit = defineEmits<{
  (e: 'entry', product: Product): void
  (e: 'exit', product: Product): void
  (e: 'history', product: Product): void
  (e: 'delete', product: Product): void
}>()

const unitLabel = (unit: Product['unit']) => STOCK_UNITS.find(option => option.value === unit)?.label ?? unit
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-left text-[13px]" :class="canManage ? 'min-w-[1040px]' : 'min-w-[820px]'">
      <caption class="sr-only">Itens do estoque com categoria, quantidade, mínimo, unidade, status e ações</caption>
      <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
        <tr>
          <th scope="col" class="px-5 py-3 font-semibold">Produto</th>
          <th scope="col" class="px-5 py-3 font-semibold">Categoria</th>
          <th scope="col" class="px-5 py-3 font-semibold">Quantidade</th>
          <th scope="col" class="px-5 py-3 font-semibold">Mínimo</th>
          <th scope="col" class="px-5 py-3 font-semibold">Unidade</th>
          <th scope="col" class="px-5 py-3 font-semibold">Status</th>
          <th scope="col" class="px-5 py-3 font-semibold">Atualizado</th>
          <th scope="col" class="px-5 py-3 font-semibold">Ações</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-line/70">
        <tr v-for="product in products" :key="product.id" class="hover:bg-paper/40">
          <td class="max-w-[220px] break-words px-5 py-4 font-semibold">{{ product.name }}</td>
          <td class="px-5 py-4 text-[#55555e]">{{ product.category }}</td>
          <td class="px-5 py-4"><StockLevelBar :product="product" /></td>
          <td class="whitespace-nowrap px-5 py-4 text-[#55555e]">{{ formatQuantity(product.minimum) }}</td>
          <td class="px-5 py-4 text-[#55555e]">{{ unitLabel(product.unit) }}</td>
          <td class="px-5 py-4"><StockStatusBadge :status="stockStatus(product)" /></td>
          <td class="whitespace-nowrap px-5 py-4 text-[#55555e]">{{ formatDate(product.updatedAt) }}</td>
          <td class="px-5 py-4">
            <div class="flex items-center gap-2">
              <template v-if="canManage">
                <button type="button" class="button button-secondary button-sm" :aria-label="`Dar entrada em ${product.name}`" :disabled="busy" @click="emit('entry', product)"><ArrowDownToLine class="size-3.5" aria-hidden="true" />Dar entrada</button>
                <button type="button" class="button button-secondary button-sm" :aria-label="`Dar baixa em ${product.name}`" :disabled="busy || product.quantity <= 0" :title="product.quantity <= 0 ? 'Produto esgotado' : undefined" @click="emit('exit', product)"><ArrowUpFromLine class="size-3.5" aria-hidden="true" />Dar baixa</button>
              </template>
              <button type="button" class="icon-button" :aria-label="`Ver histórico de ${product.name}`" title="Ver histórico" :disabled="busy" @click="emit('history', product)"><Eye class="size-4" aria-hidden="true" /></button>
              <button v-if="canManage" type="button" class="icon-button hover:!text-red-700" :aria-label="`Excluir ${product.name}`" title="Excluir" :disabled="busy" @click="emit('delete', product)"><Trash2 class="size-4" aria-hidden="true" /></button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped src="./stock-ui.css"></style>
