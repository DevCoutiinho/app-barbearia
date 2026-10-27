<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { History } from '@lucide/vue'
import StockDialog from './StockDialog.vue'
import StockMovementsTable from './StockMovementsTable.vue'
import StockStatusBadge from './StockStatusBadge.vue'
import stockService from '../../services/stockService'
import type { Product, StockMovement } from '../../types/stock'
import { formatQuantity, sortChronologically, stockStatus } from '../../utils/stock'
import { stockErrorMessage } from '../../utils/stockErrors'

const props = defineProps<{ open: boolean; product: Product | null }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const movements = ref<StockMovement[]>([])
const loading = ref(false)
const error = ref('')
const units = computed(() => props.product ? { [props.product.id]: props.product.unit } : {})
let request = 0

async function load() {
  const product = props.product
  if (!product) return
  const current = ++request
  loading.value = true
  error.value = ''
  try {
    const result = await stockService.listMovements(product.id)
    // Ignora respostas de um produto aberto anteriormente.
    if (current === request) movements.value = sortChronologically(result)
  } catch (cause) {
    if (current === request) error.value = stockErrorMessage(cause, 'Não foi possível carregar o histórico do produto.')
  } finally {
    if (current === request) loading.value = false
  }
}

watch(() => [props.open, props.product?.id] as const, ([open]) => {
  movements.value = []
  if (open) void load()
}, { immediate: true })
</script>

<template>
  <StockDialog :open="open" :title="product?.name ?? 'Histórico'" eyebrow="HISTÓRICO DO PRODUTO" drawer @close="emit('close')">
    <template #subtitle>
      <div v-if="product" class="mt-2 flex flex-wrap items-center gap-2 text-[13px] text-[#74747f]">
        <StockStatusBadge :status="stockStatus(product)" />
        <span>{{ product.category }} · em estoque: <strong class="text-ink">{{ formatQuantity(product.quantity, product.unit) }}</strong></span>
      </div>
    </template>
    <div class="-mx-6 border-t border-line" :aria-busy="loading">
      <p v-if="loading" class="px-6 py-12 text-center text-sm text-[#74747f]" role="status">Carregando histórico...</p>
      <div v-else-if="error" class="m-6 rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
        <p>{{ error }}</p>
        <button type="button" class="mt-2 font-semibold underline" @click="load">Tentar novamente</button>
      </div>
      <StockMovementsTable v-else-if="movements.length" :movements="movements" :units="units" :caption="`Entradas e saídas de ${product?.name ?? 'produto'} em ordem cronológica`" />
      <div v-else class="px-6 py-12 text-center" role="status">
        <History class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
        <p class="text-sm font-semibold">Nenhuma movimentação registrada.</p>
        <p class="mt-1 text-[13px] text-[#74747f]">As entradas e baixas deste produto aparecerão aqui.</p>
      </div>
    </div>
  </StockDialog>
</template>
