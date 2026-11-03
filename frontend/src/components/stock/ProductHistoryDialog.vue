<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ArrowDownToLine, ArrowUpFromLine, ChevronLeft, ChevronRight, History } from '@lucide/vue'
import StockDialog from './StockDialog.vue'
import StockMovementsTable from './StockMovementsTable.vue'
import StockStatusBadge from './StockStatusBadge.vue'
import stockService from '../../services/stockService'
import type { Product, StockMovement } from '../../types/stock'
import type { PageMetadata } from '../../types/user'
import { formatQuantity, stockStatus } from '../../utils/stock'
import { stockErrorMessage } from '../../utils/stockErrors'

const props = withDefaults(defineProps<{
  open: boolean
  product: Product | null
  canUpdate?: boolean
  refreshKey?: number
}>(), { canUpdate: false, refreshKey: 0 })

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'entry', product: Product): void
  (e: 'consumption', product: Product): void
}>()

const movements = ref<StockMovement[]>([])
const page = ref<PageMetadata>({ size: 10, number: 0, totalElements: 0, totalPages: 0 })
const loading = ref(false)
const error = ref('')
const units = computed(() => props.product ? { [props.product.id]: props.product.unitMeasurement } : {})
let request = 0

async function load(targetPage = 0) {
  const product = props.product
  if (!product) return
  const current = ++request
  loading.value = true
  error.value = ''
  try {
    const result = await stockService.listMovements(product.id, { page: targetPage, size: 10 })
    if (current === request) {
      movements.value = result.content
      page.value = result.page
    }
  } catch (cause) {
    if (current === request) error.value = stockErrorMessage(cause, 'Não foi possível carregar o histórico do produto.')
  } finally {
    if (current === request) loading.value = false
  }
}

function changePage(targetPage: number) {
  if (loading.value || targetPage < 0 || targetPage >= page.value.totalPages) return
  void load(targetPage)
}

watch(() => [props.open, props.product?.id, props.refreshKey] as const, ([open]) => {
  if (open) void load(0)
  else movements.value = []
}, { immediate: true })
</script>

<template>
  <StockDialog :open="open" :title="product?.name ?? 'Histórico'" eyebrow="HISTÓRICO DO PRODUTO" drawer @close="emit('close')">
    <template #subtitle>
      <div v-if="product" class="mt-2 space-y-2.5">
        <div class="flex flex-wrap items-center gap-2 text-[13px] text-[#74747f]">
          <StockStatusBadge :status="stockStatus(product)" />
          <span>em estoque: <strong class="text-ink">{{ formatQuantity(product.currentQuantity, product.unitMeasurement) }}</strong></span>
          <span>· mínimo: {{ formatQuantity(product.minimumQuantity, product.unitMeasurement) }}</span>
        </div>
        <div v-if="canUpdate" class="flex flex-wrap gap-2 pt-1">
          <button type="button" class="button button-secondary button-sm" @click="emit('entry', product)">
            <ArrowDownToLine class="size-3.5" aria-hidden="true" />Dar entrada
          </button>
          <button
            type="button"
            class="button button-secondary button-sm"
            :disabled="product.currentQuantity <= 0"
            :title="product.currentQuantity <= 0 ? 'Produto esgotado' : undefined"
            @click="emit('consumption', product)"
          >
            <ArrowUpFromLine class="size-3.5" aria-hidden="true" />Registrar consumo
          </button>
        </div>
      </div>
    </template>
    <div class="-mx-6 border-t border-line" :aria-busy="loading">
      <p v-if="loading" class="px-6 py-12 text-center text-sm text-[#74747f]" role="status">Carregando histórico...</p>
      <div v-else-if="error" class="m-6 rounded-lg bg-red-50 p-4 text-[13px] text-red-800" role="alert">
        <p>{{ error }}</p>
        <button type="button" class="mt-2 font-semibold underline" @click="load(page.number)">Tentar novamente</button>
      </div>
      <template v-else-if="movements.length">
        <StockMovementsTable :movements="movements" :units="units" :caption="`Entradas e consumos de ${product?.name ?? 'produto'}`" />
        <footer v-if="page.totalPages > 1" class="flex items-center justify-between gap-3 border-t border-line px-6 py-3.5 text-[12px]">
          <span class="text-[#74747f]">Página {{ page.number + 1 }} de {{ page.totalPages }}</span>
          <div class="flex items-center gap-2">
            <button type="button" class="button button-secondary size-8 !p-0" aria-label="Página anterior" :disabled="loading || page.number === 0" @click="changePage(page.number - 1)">
              <ChevronLeft class="size-4" aria-hidden="true" />
            </button>
            <button type="button" class="button button-secondary size-8 !p-0" aria-label="Próxima página" :disabled="loading || page.number + 1 >= page.totalPages" @click="changePage(page.number + 1)">
              <ChevronRight class="size-4" aria-hidden="true" />
            </button>
          </div>
        </footer>
      </template>
      <div v-else class="px-6 py-12 text-center" role="status">
        <History class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
        <p class="text-sm font-semibold">Nenhuma movimentação registrada.</p>
        <p class="mt-1 text-[13px] text-[#74747f]">As entradas e consumos deste produto aparecerão aqui.</p>
      </div>
    </div>
  </StockDialog>
</template>

<style scoped src="./stock-ui.css"></style>
