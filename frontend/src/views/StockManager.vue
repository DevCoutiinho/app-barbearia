<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { AlertTriangle, Eye, History, Package, PackageX, Plus, RefreshCw, TrendingDown } from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import ConfirmDeleteModal from '../components/ConfirmDeleteModal.vue'
import StatCard from '../components/dashboard/StatCard.vue'
import StockAlertBanner from '../components/stock/StockAlertBanner.vue'
import StockItemsTable from '../components/stock/StockItemsTable.vue'
import StockMovementsTable from '../components/stock/StockMovementsTable.vue'
import ProductFormDialog from '../components/stock/ProductFormDialog.vue'
import MovementFormDialog from '../components/stock/MovementFormDialog.vue'
import ProductHistoryDialog from '../components/stock/ProductHistoryDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useStockPermissions } from '../composables/useStockPermissions'
import stockService from '../services/stockService'
import type { ProductData, MovementData } from '../schemas/stock.schema'
import type { MovementType, Product, StockMovement, StockUnit } from '../types/stock'
import { countRecentExits, sortByCriticality, stockStatus } from '../utils/stock'
import { stockErrorMessage } from '../utils/stockErrors'

type Tab = 'items' | 'history'

const auth = useAuthStore()
const { canView, canManage } = useStockPermissions()

const products = ref<Product[]>([])
const movements = ref<StockMovement[]>([])
const loading = ref(false)
const hasLoaded = ref(false)
const error = ref('')
const loadingMovements = ref(false)
const movementsError = ref('')
const tab = ref<Tab>('items')
const tableSection = ref<HTMLElement | null>(null)

const productFormOpen = ref(false)
const movementDialog = ref<{ type: MovementType; product: Product } | null>(null)
const historyProduct = ref<Product | null>(null)
const deleteTarget = ref<Product | null>(null)
const deleting = ref(false)
const busy = computed(() => loading.value || deleting.value)

const sortedProducts = computed(() => sortByCriticality(products.value))
const lowStock = computed(() => sortedProducts.value.filter(product => stockStatus(product) !== 'OK'))
const summary = computed(() => {
  const statuses = products.value.map(stockStatus)
  return {
    total: products.value.length,
    categories: new Set(products.value.map(product => product.category)).size,
    low: statuses.filter(status => status === 'LOW').length,
    out: statuses.filter(status => status === 'OUT').length,
    recentExits: countRecentExits(movements.value),
  }
})
const recentMovements = computed(() => [...movements.value].sort((a, b) => b.date.localeCompare(a.date) || b.createdAt.localeCompare(a.createdAt)))
const units = computed(() => Object.fromEntries(products.value.map(product => [product.id, product.unit])) as Record<string, StockUnit>)
// O JWT local traz o id do usuário em `sub`; `id` fica como fallback para outros emissores.
const responsible = computed(() => auth.user ? { id: auth.user.id ?? (auth.user as { sub?: string }).sub ?? '', name: auth.user.name } : null)

async function loadProducts() {
  if (loading.value || !canView.value) return
  loading.value = true
  error.value = ''
  try {
    const result = await stockService.listProducts()
    if (!Array.isArray(result)) throw new Error('Resposta de produtos inválida.')
    products.value = result
  } catch (cause) {
    error.value = stockErrorMessage(cause, 'Não foi possível carregar o estoque.')
    toast.error(error.value)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

/** O contrato só expõe histórico por produto; o histórico geral agrega todos. */
async function loadMovements() {
  if (!canView.value || !products.value.length) {
    movements.value = []
    return
  }
  loadingMovements.value = true
  movementsError.value = ''
  try {
    const lists = await Promise.all(products.value.map(product => stockService.listMovements(product.id)))
    movements.value = lists.flat()
  } catch (cause) {
    movementsError.value = stockErrorMessage(cause, 'Não foi possível carregar o histórico de uso.')
  } finally {
    loadingMovements.value = false
  }
}

async function refresh() {
  await loadProducts()
  if (!error.value) await loadMovements()
}

async function showHistoryTab() {
  tab.value = 'history'
  await nextTick()
  tableSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ====================
// Ações restritas (o guarda fica na ação, não só no botão)
// ====================
function denyIfReadOnly(): boolean {
  if (canManage.value) return false
  toast.error('Seu perfil tem acesso somente de leitura ao estoque.')
  return true
}

function openProductForm() {
  if (denyIfReadOnly() || busy.value) return
  productFormOpen.value = true
}

function openMovement(type: MovementType, product: Product) {
  if (denyIfReadOnly() || busy.value) return
  movementDialog.value = { type, product }
}

function openDelete(product: Product) {
  if (denyIfReadOnly() || busy.value) return
  deleteTarget.value = product
}

async function createProduct(data: ProductData) {
  if (denyIfReadOnly()) return
  try {
    await stockService.createProduct(data)
  } catch (cause) {
    const message = stockErrorMessage(cause, 'Não foi possível cadastrar o produto.')
    toast.error(message)
    throw new Error(message)
  }
  toast.success(`${data.name} adicionado ao estoque.`)
  productFormOpen.value = false
  await refresh()
}

async function registerMovement(data: MovementData) {
  const dialog = movementDialog.value
  if (denyIfReadOnly() || !dialog) return
  if (!responsible.value) throw new Error('Sessão inválida. Faça login novamente.')
  const payload = { ...data, user: responsible.value }
  const isEntry = dialog.type === 'ENTRY'
  try {
    if (isEntry) await stockService.registerEntry(dialog.product.id, payload)
    else await stockService.registerExit(dialog.product.id, payload)
  } catch (cause) {
    const message = stockErrorMessage(cause, `Não foi possível registrar a ${isEntry ? 'entrada' : 'baixa'}.`)
    toast.error(message)
    throw new Error(message)
  }
  toast.success(isEntry ? `Entrada registrada em ${dialog.product.name}.` : `Baixa registrada em ${dialog.product.name}.`)
  movementDialog.value = null
  await refresh()
}

async function confirmDelete() {
  const product = deleteTarget.value
  if (denyIfReadOnly() || !product || deleting.value) return
  deleting.value = true
  try {
    await stockService.deleteProduct(product.id)
    toast.success(`${product.name} foi excluído do estoque.`)
    deleteTarget.value = null
    await refresh()
  } catch (cause) {
    toast.error(stockErrorMessage(cause, 'Não foi possível excluir o produto.'))
  } finally {
    deleting.value = false
  }
}

watch(canManage, allowed => {
  // Perdeu a permissão com a tela aberta: fecha qualquer modal de escrita.
  if (allowed) return
  productFormOpen.value = false
  movementDialog.value = null
  deleteTarget.value = null
})

watch(canView, allowed => { if (allowed && !hasLoaded.value) void refresh() })

onMounted(() => { void refresh() })
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-8 text-ink" aria-labelledby="stock-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
          <div>
            <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">GESTÃO DE ESTOQUE</p>
            <div class="flex flex-wrap items-center gap-2.5">
              <h1 id="stock-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Estoque</h1>
              <span v-if="canView && !canManage" class="inline-flex items-center gap-1 rounded-full bg-paper-deep px-2.5 py-1 text-[11px] font-semibold text-[#55555e]"><Eye class="size-3" aria-hidden="true" />Somente leitura</span>
            </div>
            <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">Acompanhe produtos, níveis mínimos e o histórico de entradas e baixas.</p>
          </div>
          <div v-if="canView" class="flex flex-wrap gap-2">
            <button type="button" class="button button-secondary" aria-label="Atualizar estoque" :disabled="busy" @click="refresh"><RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading }" aria-hidden="true" /></button>
            <button type="button" class="button button-secondary" @click="showHistoryTab"><History class="size-4" aria-hidden="true" />Histórico de uso</button>
            <button v-if="canManage" type="button" class="button button-primary" :disabled="busy" @click="openProductForm"><Plus class="size-4" aria-hidden="true" />Adicionar item</button>
          </div>
        </header>

        <p v-if="!canView" class="rounded-xl border border-line bg-white p-5 text-sm" role="alert">
          {{ auth.isAuthenticated ? 'Você não tem acesso ao estoque.' : 'Faça login para acessar o estoque.' }}
        </p>
        <template v-else>
          <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard label="Total de itens" :value="hasLoaded ? summary.total : '—'" :description="`${summary.categories} ${summary.categories === 1 ? 'categoria' : 'categorias'}`" :icon="Package" />
            <StatCard label="Estoque baixo" :value="hasLoaded ? summary.low : '—'" description="Abaixo do mínimo, ainda com saldo" :icon="AlertTriangle" accent />
            <StatCard label="Produtos esgotados" :value="hasLoaded ? summary.out : '—'" description="Sem unidades disponíveis" :icon="PackageX" />
            <StatCard label="Baixas nos 7 dias" :value="hasLoaded && !loadingMovements ? summary.recentExits : '—'" description="Saídas registradas" :icon="TrendingDown" />
          </div>

          <StockAlertBanner v-if="!loading" :products="lowStock" />

          <div ref="tableSection" class="scroll-mt-20 overflow-hidden rounded-2xl border border-line bg-white" :aria-busy="loading">
            <div class="flex gap-1 border-b border-line px-3 pt-2 sm:px-4" role="tablist" aria-label="Visualização do estoque">
              <button v-for="option in ([{ id: 'items', label: 'Itens', count: products.length }, { id: 'history', label: 'Histórico de uso', count: movements.length }] as const)" :id="`stock-tab-${option.id}`" :key="option.id"
                type="button" role="tab" :aria-selected="tab === option.id" :aria-controls="`stock-panel-${option.id}`" :tabindex="tab === option.id ? 0 : -1"
                class="-mb-px flex items-center gap-2 border-b-2 px-3 py-3 text-[13px] font-semibold"
                :class="tab === option.id ? 'border-ink text-ink' : 'border-transparent text-[#74747f] hover:text-ink'"
                @click="tab = option.id" @keydown.right.left.prevent="tab = tab === 'items' ? 'history' : 'items'">
                {{ option.label }}<span v-if="hasLoaded" class="rounded-full bg-paper px-1.5 text-[11px] text-[#55555e]">{{ option.count }}</span>
              </button>
            </div>

            <div v-if="error" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
              <p>{{ error }}</p>
              <button type="button" class="mt-2 font-semibold underline" :disabled="loading" @click="refresh">Tentar novamente</button>
            </div>
            <p v-else-if="loading && !hasLoaded" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando estoque...</p>

            <div v-else-if="tab === 'items'" id="stock-panel-items" role="tabpanel" aria-labelledby="stock-tab-items">
              <StockItemsTable v-if="sortedProducts.length" :products="sortedProducts" :can-manage="canManage" :busy="busy"
                @entry="openMovement('ENTRY', $event)" @exit="openMovement('EXIT', $event)" @history="historyProduct = $event" @delete="openDelete" />
              <div v-else class="px-5 py-14 text-center" role="status">
                <Package class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhum item no estoque.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">{{ canManage ? 'Adicione o primeiro produto para começar o controle.' : 'Os produtos cadastrados pelo administrador aparecerão aqui.' }}</p>
                <button v-if="canManage" type="button" class="button button-primary mt-4" @click="openProductForm"><Plus class="size-4" aria-hidden="true" />Adicionar item</button>
              </div>
            </div>

            <div v-else id="stock-panel-history" role="tabpanel" aria-labelledby="stock-tab-history">
              <p v-if="loadingMovements" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando histórico...</p>
              <div v-else-if="movementsError" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
                <p>{{ movementsError }}</p>
                <button type="button" class="mt-2 font-semibold underline" @click="loadMovements">Tentar novamente</button>
              </div>
              <StockMovementsTable v-else-if="recentMovements.length" :movements="recentMovements" :units="units" show-product caption="Histórico de uso do estoque, das movimentações mais recentes para as mais antigas" />
              <div v-else class="px-5 py-14 text-center" role="status">
                <History class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhuma movimentação registrada.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">Entradas e baixas aparecerão aqui.</p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <ProductHistoryDialog :open="Boolean(historyProduct)" :product="historyProduct" @close="historyProduct = null" />

    <!-- Modais de escrita nem são montados para perfis somente leitura. -->
    <template v-if="canManage">
      <ProductFormDialog :open="productFormOpen" :submit="createProduct" @close="productFormOpen = false" />
      <MovementFormDialog :open="Boolean(movementDialog)" :type="movementDialog?.type ?? 'ENTRY'" :product="movementDialog?.product ?? null"
        :responsible="responsible?.name ?? '—'" :submit="registerMovement" @close="movementDialog = null" />
      <ConfirmDeleteModal :open="Boolean(deleteTarget)" title="Excluir item" :loading="deleting"
        :description="`Deseja realmente excluir ${deleteTarget?.name ?? 'este item'} do estoque? Essa ação não pode ser desfeita.`"
        @close="deleting || (deleteTarget = null)" @confirm="confirmDelete" />
    </template>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>
