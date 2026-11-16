<script setup lang="ts">
import { ArrowDownToLine, ArrowUpFromLine, History, Pencil, Power } from '@lucide/vue'
import StockLevelBar from './StockLevelBar.vue'
import StockStatusBadge from './StockStatusBadge.vue'
import type { Product } from '../../types/stock'
import { STOCK_UNITS } from '../../types/stock'
import { formatCurrency, formatDate, formatQuantity, stockStatus } from '../../utils/stock'

defineProps<{
  products: Product[]
  canUpdate: boolean
  busy: boolean
}>()

const emit = defineEmits<{
  (e: 'entry', product: Product): void
  (e: 'consumption', product: Product): void
  (e: 'history', product: Product): void
  (e: 'edit', product: Product): void
  (e: 'toggle-status', product: Product): void
}>()

const unitLabel = (unit: Product['unitMeasurement']) => STOCK_UNITS.find(option => option.value === unit)?.label ?? unit
</script>

<template>
  <div class="overflow-x-auto">
    <table class="w-full text-left text-[13px]" :class="canUpdate ? 'min-w-[1160px]' : 'min-w-[920px]'">
      <caption class="sr-only">Itens do estoque com descrição, quantidade, mínimo, unidade, preço de custo, nível, status e ações</caption>
      <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
        <tr>
          <th scope="col" class="px-5 py-3 font-semibold">Produto</th>
          <th scope="col" class="px-5 py-3 font-semibold">Descrição</th>
          <th scope="col" class="px-5 py-3 font-semibold">Quantidade</th>
          <th scope="col" class="px-5 py-3 font-semibold">Mínimo</th>
          <th scope="col" class="px-5 py-3 font-semibold">Unidade</th>
          <th scope="col" class="px-5 py-3 font-semibold">Preço de custo</th>
          <th scope="col" class="px-5 py-3 font-semibold">Nível</th>
          <th scope="col" class="px-5 py-3 font-semibold">Status</th>
          <th scope="col" class="px-5 py-3 font-semibold">Atualizado</th>
          <th scope="col" class="px-5 py-3 font-semibold">Ações</th>
        </tr>
      </thead>
      <tbody class="divide-y divide-line/70">
        <tr v-for="product in products" :key="product.id" class="hover:bg-paper/40">
          <td class="max-w-[200px] break-words px-5 py-4 font-semibold">{{ product.name }}</td>
          <td class="max-w-[220px] break-words px-5 py-4 text-[#55555e]">{{ product.description || '—' }}</td>
          <td class="px-5 py-4"><StockLevelBar :product="product" /></td>
          <td class="whitespace-nowrap px-5 py-4 text-[#55555e]">{{ formatQuantity(product.minimumQuantity) }}</td>
          <td class="whitespace-nowrap px-5 py-4 text-[#55555e]">{{ unitLabel(product.unitMeasurement) }}</td>
          <td class="whitespace-nowrap px-5 py-4 font-semibold">{{ formatCurrency(product.priceCost) }}</td>
          <td class="px-5 py-4"><StockStatusBadge :status="stockStatus(product)" /></td>
          <td class="px-5 py-4">
            <span
              class="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-1 text-[11px] font-semibold"
              :class="product.active ? 'bg-emerald-50 text-emerald-800' : 'bg-paper text-[#55555e]'"
            >
              <span class="size-1.5 rounded-full" :class="product.active ? 'bg-emerald-600' : 'bg-[#74747f]'" aria-hidden="true"></span>
              {{ product.active ? 'Ativo' : 'Inativo' }}
            </span>
          </td>
          <td class="whitespace-nowrap px-5 py-4 text-[#55555e]">{{ formatDate(product.updatedAt) }}</td>
          <td class="px-5 py-4">
            <div class="flex items-center gap-1.5">
              <template v-if="canUpdate">
                <button
                  type="button"
                  class="button button-secondary button-sm"
                  :aria-label="`Dar entrada em ${product.name}`"
                  :disabled="busy"
                  @click="emit('entry', product)"
                >
                  <ArrowDownToLine class="size-3.5" aria-hidden="true" />Entrada
                </button>
                <button
                  type="button"
                  class="button button-secondary button-sm"
                  :aria-label="`Registrar consumo em ${product.name}`"
                  :disabled="busy || product.currentQuantity <= 0"
                  :title="product.currentQuantity <= 0 ? 'Produto esgotado' : undefined"
                  @click="emit('consumption', product)"
                >
                  <ArrowUpFromLine class="size-3.5" aria-hidden="true" />Consumo
                </button>
              </template>
              <button
                type="button"
                class="icon-button"
                :aria-label="`Ver histórico de ${product.name}`"
                title="Histórico de movimentações"
                :disabled="busy"
                @click="emit('history', product)"
              >
                <History class="size-4" aria-hidden="true" />
              </button>
              <template v-if="canUpdate">
                <button
                  type="button"
                  class="icon-button"
                  :aria-label="`Editar ${product.name}`"
                  title="Editar produto"
                  :disabled="busy"
                  @click="emit('edit', product)"
                >
                  <Pencil class="size-4" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  class="icon-button"
                  :aria-label="`${product.active ? 'Inativar' : 'Reativar'} ${product.name}`"
                  :title="product.active ? 'Inativar produto' : 'Reativar produto'"
                  :disabled="busy"
                  @click="emit('toggle-status', product)"
                >
                  <Power class="size-4" aria-hidden="true" />
                </button>
              </template>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped src="./stock-ui.css"></style>
