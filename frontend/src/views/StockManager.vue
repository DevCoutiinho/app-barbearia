<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  ChevronLeft,
  ChevronRight,
  Eye,
  History,
  Package,
  Plus,
  RefreshCw,
  Search,
  X,
} from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import StockAlertBanner from '../components/stock/StockAlertBanner.vue'
import StockItemsTable from '../components/stock/StockItemsTable.vue'
import StockMovementsTable from '../components/stock/StockMovementsTable.vue'
import StockStatusBadge from '../components/stock/StockStatusBadge.vue'
import ProductFormDialog from '../components/stock/ProductFormDialog.vue'
import MovementFormDialog from '../components/stock/MovementFormDialog.vue'
import ProductHistoryDialog from '../components/stock/ProductHistoryDialog.vue'
import StockDialog from '../components/stock/StockDialog.vue'
import { useAuthStore } from '../stores/auth'
import { useStockPermissions } from '../composables/useStockPermissions'
import stockService from '../services/stockService'
import type { CreateProductData, MovementData, UpdateProductData } from '../schemas/stock.schema'
import {
  STOCK_UNITS,
  type MovementType,
  type Product,
  type ProductFilters,
  type StockMovement,
  type StockUnit,
} from '../types/stock'
import type { PageMetadata } from '../types/user'
import { formatQuantity, stockStatus, unitSymbol } from '../utils/stock'
import { stockErrorMessage } from '../utils/stockErrors'
import { handleApiError } from '../utils/errorHandler'

type Tab = 'items' | 'history'

const auth = useAuthStore()
const { canView, canCreate, canUpdate, canManage } = useStockPermissions()

const tab = ref<Tab>('items')
const tableSection = ref<HTMLElement | null>(null)

const products = ref<Product[]>([])
const loading = ref(false)
const hasLoaded = ref(false)
const error = ref('')

const nameFilter = ref('')
const descriptionFilter = ref('')
const unitFilter = ref<'' | StockUnit>('')
const priceCostFilter = ref('')
const activeFilter = ref<'' | 'true' | 'false'>('')
const sortFilter = ref('name,ASC')
const appliedFilters = ref<ProductFilters>({})
const appliedSort = ref('name,ASC')

const pageSize = ref(10)
const requestedPage = ref(0)
const page = ref<PageMetadata>({ size: 10, number: 0, totalElements: 0, totalPages: 0 })
const pageSizes = [10, 20, 50]

const sortOptions = [
  { value: 'name,ASC', label: 'Nome (A–Z)' },
  { value: 'name,DESC', label: 'Nome (Z–A)' },
  { value: 'priceCost,ASC', label: 'Preço de custo (menor)' },
  { value: 'priceCost,DESC', label: 'Preço de custo (maior)' },
  { value: 'updatedAt,DESC', label: 'Atualizado recentemente' },
]

/* ── Histórico na aba ── */
const selectedHistoryProductId = ref('')
const tabMovements = ref<StockMovement[]>([])
const tabMovementsPage = ref<PageMetadata>({ size: 10, number: 0, totalElements: 0, totalPages: 0 })
const loadingTabMovements = ref(false)
const tabMovementsError = ref('')
let tabMovementsRequest = 0

/* ── Modais e Drawer ── */
const productFormOpen = ref(false)
const editingProduct = ref<Product | null>(null)
const movementDialog = ref<{ type: MovementType; product: Product } | null>(null)
const historyProduct = ref<Product | null>(null)
const historyRefreshKey = ref(0)
const statusTarget = ref<Product | null>(null)
const changingStatus = ref(false)
const statusError = ref('')

const busy = computed(() => loading.value || changingStatus.value)
const hasAppliedFilters = computed(() =>
  Object.keys(appliedFilters.value).length > 0 || appliedSort.value !== 'name,ASC',
)
const firstItem = computed(() => products.value.length ? page.value.number * page.value.size + 1 : 0)
const lastItem = computed(() => products.value.length ? firstItem.value + products.value.length - 1 : 0)
const lowStock = computed(() => products.value.filter(product => product.active && stockStatus(product) !== 'OK'))

const selectedHistoryProduct = computed(() =>
  products.value.find(product => product.id === selectedHistoryProductId.value) ?? null,
)
const units = computed(() =>
  Object.fromEntries(products.value.map(product => [product.id, product.unitMeasurement])) as Record<string, StockUnit>,
)

async function loadProducts(targetPage = requestedPage.value) {
  if (loading.value || !canView.value) return
  loading.value = true
  error.value = ''
  requestedPage.value = targetPage
  try {
    const fetchPage = async (pageNumber: number) => {
      const response = await stockService.listProducts({
        page: pageNumber,
        size: pageSize.value,
        sort: appliedSort.value,
        filters: { ...appliedFilters.value },
      })
      const result = response.data?.[0]
      if (!result || !Array.isArray(result.content) || !result.page) {
        throw new Error('Resposta de produtos inválida.')
      }
      return result
    }

    let result = await fetchPage(targetPage)
    const lastPage = Math.max(result.page.totalPages - 1, 0)
    if (result.page.number > lastPage) {
      result = await fetchPage(lastPage)
    }

    products.value = result.content
    page.value = result.page
    requestedPage.value = result.page.number

    if (historyProduct.value) {
      const updated = result.content.find(item => item.id === historyProduct.value?.id)
      if (updated) historyProduct.value = updated
    }
    if (!selectedHistoryProductId.value && result.content.length) {
      selectedHistoryProductId.value = result.content[0].id
    } else if (selectedHistoryProductId.value && !result.content.some(item => item.id === selectedHistoryProductId.value)) {
      selectedHistoryProductId.value = result.content[0]?.id ?? ''
    }
  } catch (cause) {
    error.value = stockErrorMessage(cause, 'Não foi possível carregar os produtos do estoque.')
    handleApiError(cause, undefined, error.value)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

async function loadTabMovements(targetPage = 0) {
  const productId = selectedHistoryProductId.value
  if (!canView.value || !productId) {
    tabMovements.value = []
    tabMovementsPage.value = { size: 10, number: 0, totalElements: 0, totalPages: 0 }
    return
  }
  const current = ++tabMovementsRequest
  loadingTabMovements.value = true
  tabMovementsError.value = ''
  try {
    const result = await stockService.listMovements(productId, { page: targetPage, size: 10 })
    if (current === tabMovementsRequest) {
      tabMovements.value = result.content
      tabMovementsPage.value = result.page
    }
  } catch (cause) {
    if (current === tabMovementsRequest) {
      tabMovementsError.value = stockErrorMessage(cause, 'Não foi possível carregar o histórico de movimentações.')
    }
  } finally {
    if (current === tabMovementsRequest) loadingTabMovements.value = false
  }
}

async function refresh() {
  await loadProducts()
  if (tab.value === 'history' && selectedHistoryProductId.value) {
    await loadTabMovements(tabMovementsPage.value.number)
  }
}

async function showHistoryTab(product?: Product) {
  if (product) selectedHistoryProductId.value = product.id
  else if (!selectedHistoryProductId.value && products.value.length) {
    selectedHistoryProductId.value = products.value[0].id
  }
  tab.value = 'history'
  await nextTick()
  tableSection.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function applyFilters() {
  if (busy.value) return
  const filters: ProductFilters = {}
  if (nameFilter.value.trim()) filters.name = nameFilter.value.trim()
  if (descriptionFilter.value.trim()) filters.description = descriptionFilter.value.trim()
  if (unitFilter.value) filters.unitMeasurement = unitFilter.value
  if (activeFilter.value !== '') filters.active = activeFilter.value === 'true'
  if (priceCostFilter.value.trim()) {
    const parsedPrice = Number(priceCostFilter.value.trim().replace(',', '.'))
    if (Number.isFinite(parsedPrice) && parsedPrice >= 0) {
      filters.priceCost = parsedPrice
    }
  }
  appliedFilters.value = filters
  appliedSort.value = sortFilter.value || 'name,ASC'
  await loadProducts(0)
}

async function clearFilters() {
  if (busy.value) return
  nameFilter.value = ''
  descriptionFilter.value = ''
  unitFilter.value = ''
  priceCostFilter.value = ''
  activeFilter.value = ''
  sortFilter.value = 'name,ASC'
  appliedFilters.value = {}
  appliedSort.value = 'name,ASC'
  await loadProducts(0)
}

async function changePage(target: number) {
  if (busy.value || target < 0 || target >= page.value.totalPages) return
  await loadProducts(target)
}

async function changePageSize(event: Event) {
  if (busy.value) return
  const size = Number((event.target as HTMLSelectElement).value)
  if (!pageSizes.includes(size)) return
  pageSize.value = size
  await loadProducts(0)
}

function changeTabMovementsPage(target: number) {
  if (loadingTabMovements.value || target < 0 || target >= tabMovementsPage.value.totalPages) return
  void loadTabMovements(target)
}

function openCreateProduct() {
  if (!canCreate.value) {
    toast.error('Você não tem permissão para criar produtos no estoque.')
    return
  }
  if (busy.value) return
  editingProduct.value = null
  productFormOpen.value = true
}

function openEditProduct(product: Product) {
  if (!canUpdate.value) {
    toast.error('Você não tem permissão para atualizar produtos no estoque.')
    return
  }
  if (busy.value) return
  editingProduct.value = product
  productFormOpen.value = true
}

function closeProductForm() {
  productFormOpen.value = false
  editingProduct.value = null
}

function openMovement(type: MovementType, product: Product) {
  if (!canUpdate.value) {
    toast.error('Você não tem permissão para registrar movimentações no estoque.')
    return
  }
  if (busy.value) return
  movementDialog.value = { type, product }
}

async function saveProduct(data: CreateProductData | UpdateProductData) {
  const target = editingProduct.value
  if (target) {
    if (!canUpdate.value) {
      throw new Error('Você não tem permissão para atualizar produtos no estoque.')
    }
    await stockService.updateProduct(target.id, {
      name: data.name,
      description: data.description,
      minimumQuantity: data.minimumQuantity,
      unitMeasurement: data.unitMeasurement,
      priceCost: data.priceCost,
    })
    toast.success('Produto atualizado com sucesso.')
    closeProductForm()
    await loadProducts(requestedPage.value)
  } else {
    if (!canCreate.value) {
      throw new Error('Você não tem permissão para cadastrar produtos no estoque.')
    }
    const createPayload = data as CreateProductData
    await stockService.createProduct({
      name: createPayload.name,
      description: createPayload.description,
      minimumQuantity: createPayload.minimumQuantity,
      initialQuantity: createPayload.initialQuantity,
      unitMeasurement: createPayload.unitMeasurement,
      priceCost: createPayload.priceCost,
      active: createPayload.active,
    })
    toast.success(`${createPayload.name} adicionado ao estoque.`)
    closeProductForm()
    await loadProducts(0)
  }
}

async function registerMovement(data: MovementData) {
  const dialog = movementDialog.value
  if (!dialog) return
  if (!canUpdate.value) {
    throw new Error('Você não tem permissão para registrar movimentações no estoque.')
  }
  const isEntry = dialog.type === 'ENTRY'
  if (isEntry) {
    await stockService.registerEntry(dialog.product.id, {
      quantity: data.quantity,
    })
    toast.success(`Entrada registrada em ${dialog.product.name}.`)
  } else {
    if (!data.userId) {
      throw new Error('Selecione o usuário atribuído ao consumo.')
    }
    await stockService.registerOutput(dialog.product.id, {
      userId: data.userId,
      quantity: data.quantity,
    })
    toast.success(`Consumo registrado em ${dialog.product.name}.`)
  }
  movementDialog.value = null
  historyRefreshKey.value++
  await loadProducts(requestedPage.value)
  if (tab.value === 'history' && selectedHistoryProductId.value === dialog.product.id) {
    await loadTabMovements(0)
  }
}

function openStatusConfirmation(product: Product) {
  if (!canUpdate.value) {
    toast.error('Você não tem permissão para alterar o status de produtos.')
    return
  }
  if (busy.value) return
  statusTarget.value = product
  statusError.value = ''
}

function closeStatusConfirmation() {
  if (changingStatus.value) return
  statusTarget.value = null
  statusError.value = ''
}

async function confirmStatusChange() {
  const product = statusTarget.value
  if (!product || changingStatus.value || !canUpdate.value) return
  changingStatus.value = true
  statusError.value = ''
  try {
    if (product.active) {
      await stockService.deactivateProduct(product.id)
      toast.success('Produto inativado com sucesso.')
    } else {
      await stockService.activateProduct(product.id)
      toast.success('Produto ativado com sucesso.')
    }
    statusTarget.value = null
    await loadProducts(requestedPage.value)
  } catch (cause) {
    statusError.value = stockErrorMessage(cause, 'Não foi possível alterar o status do produto.')
    handleApiError(cause, undefined, statusError.value)
  } finally {
    changingStatus.value = false
  }
}

watch([tab, selectedHistoryProductId], ([currentTab, productId]) => {
  if (currentTab === 'history' && productId) {
    void loadTabMovements(0)
  }
})

watch([canCreate, canUpdate], ([allowedCreate, allowedUpdate]) => {
  if (!allowedCreate && !editingProduct.value) productFormOpen.value = false
  if (!allowedUpdate && editingProduct.value) closeProductForm()
  if (!allowedUpdate) {
    movementDialog.value = null
    statusTarget.value = null
  }
})

watch(canView, allowed => {
  if (allowed && !hasLoaded.value) void loadProducts(0)
})

onMounted(() => {
  if (canView.value) void loadProducts(0)
})
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
            <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">Gerencie produtos, níveis mínimos e o histórico de entradas e consumos.</p>
          </div>
          <div v-if="canView" class="flex flex-wrap gap-2">
            <button type="button" class="button button-secondary" aria-label="Atualizar estoque" :disabled="busy" @click="refresh">
              <RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading || loadingTabMovements }" aria-hidden="true" />Atualizar
            </button>
            <button type="button" class="button button-secondary" @click="showHistoryTab()">
              <History class="size-4" aria-hidden="true" />Histórico de movimentações
            </button>
            <button v-if="canCreate" type="button" class="button button-primary" :disabled="busy" @click="openCreateProduct">
              <Plus class="size-4" aria-hidden="true" />Adicionar produto
            </button>
          </div>
        </header>

        <p v-if="!canView" class="rounded-xl border border-line bg-white p-5 text-sm" role="alert">
          {{ auth.isAuthenticated ? 'Você não tem permissão para acessar o estoque.' : 'Faça login para acessar o estoque.' }}
        </p>
        <template v-else>
          <StockAlertBanner v-if="!loading && hasLoaded" :products="lowStock" />

          <div ref="tableSection" class="scroll-mt-20 overflow-hidden rounded-2xl border border-line bg-white" :aria-busy="loading">
            <div class="flex gap-1 border-b border-line px-3 pt-2 sm:px-4" role="tablist" aria-label="Visualização do estoque">
              <button
                v-for="option in ([
                  { id: 'items', label: 'Produtos', count: page.totalElements },
                  { id: 'history', label: 'Histórico por produto', count: null },
                ] as const)"
                :id="`stock-tab-${option.id}`"
                :key="option.id"
                type="button"
                role="tab"
                :aria-selected="tab === option.id"
                :aria-controls="`stock-panel-${option.id}`"
                :tabindex="tab === option.id ? 0 : -1"
                class="-mb-px flex items-center gap-2 border-b-2 px-3 py-3 text-[13px] font-semibold"
                :class="tab === option.id ? 'border-ink text-ink' : 'border-transparent text-[#74747f] hover:text-ink'"
                @click="tab = option.id"
                @keydown.right.left.prevent="tab = tab === 'items' ? 'history' : 'items'"
              >
                {{ option.label }}
                <span v-if="hasLoaded && option.count !== null" class="rounded-full bg-paper px-1.5 text-[11px] text-[#55555e]">{{ option.count }}</span>
              </button>
            </div>

            <div v-if="tab === 'items'" id="stock-panel-items" role="tabpanel" aria-labelledby="stock-tab-items">
              <form class="border-b border-line p-4 sm:p-5" @submit.prevent="applyFilters">
                <fieldset :disabled="busy" class="grid min-w-0 gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  <legend class="sr-only">Filtros de produtos</legend>
                  <label class="field-label" for="product-filter-name">Nome
                    <input id="product-filter-name" v-model="nameFilter" type="search" class="field-input" placeholder="Buscar pelo nome" />
                  </label>
                  <label class="field-label" for="product-filter-description">Descrição
                    <input id="product-filter-description" v-model="descriptionFilter" type="search" class="field-input" placeholder="Buscar pela descrição" />
                  </label>
                  <label class="field-label" for="product-filter-unit">Unidade de medida
                    <select id="product-filter-unit" v-model="unitFilter" class="field-input">
                      <option value="">Todas</option>
                      <option v-for="option in STOCK_UNITS" :key="option.value" :value="option.value">{{ option.label }} ({{ unitSymbol(option.value) }})</option>
                    </select>
                  </label>
                  <label class="field-label" for="product-filter-price">Preço de custo (R$)
                    <input id="product-filter-price" v-model="priceCostFilter" type="text" inputmode="decimal" class="field-input" placeholder="0,00" />
                  </label>
                  <label class="field-label" for="product-filter-active">Status
                    <select id="product-filter-active" v-model="activeFilter" class="field-input">
                      <option value="">Todos</option>
                      <option value="true">Ativos</option>
                      <option value="false">Inativos</option>
                    </select>
                  </label>
                  <label class="field-label" for="product-filter-sort">Ordenação
                    <select id="product-filter-sort" v-model="sortFilter" class="field-input">
                      <option v-for="option in sortOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
                    </select>
                  </label>
                  <div class="flex flex-wrap gap-2 sm:col-span-2 xl:col-span-3">
                    <button type="submit" class="button button-primary"><Search class="size-4" aria-hidden="true" />Filtrar</button>
                    <button type="button" class="button button-secondary" @click="clearFilters"><X class="size-4" aria-hidden="true" />Limpar</button>
                  </div>
                </fieldset>
              </form>

              <div v-if="error" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
                <p>{{ error }}</p>
                <button type="button" class="mt-2 font-semibold underline" :disabled="loading" @click="loadProducts()">Tentar novamente</button>
              </div>
              <p v-else-if="loading" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando produtos...</p>
              <StockItemsTable
                v-else-if="hasLoaded && products.length"
                :products="products"
                :can-update="canUpdate"
                :busy="busy"
                @entry="openMovement('ENTRY', $event)"
                @consumption="openMovement('CONSUMPTION', $event)"
                @history="historyProduct = $event"
                @edit="openEditProduct"
                @toggle-status="openStatusConfirmation"
              />
              <div v-else-if="hasLoaded" class="px-5 py-14 text-center" role="status">
                <Package class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhum produto encontrado.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">
                  {{ hasAppliedFilters ? 'Tente ajustar ou limpar os filtros da busca.' : canCreate ? 'Adicione o primeiro produto para começar o controle.' : 'Os produtos cadastrados aparecerão aqui.' }}
                </p>
                <button v-if="hasAppliedFilters" type="button" class="mt-4 text-[13px] font-semibold underline" @click="clearFilters">Limpar filtros</button>
                <button v-else-if="canCreate" type="button" class="button button-primary mt-4" @click="openCreateProduct"><Plus class="size-4" aria-hidden="true" />Adicionar produto</button>
              </div>

              <footer v-if="hasLoaded && !error && !loading" class="flex flex-wrap items-center justify-between gap-4 border-t border-line p-4 text-[12px] sm:px-5">
                <p class="text-[#74747f]" role="status">{{ firstItem }}–{{ lastItem }} de {{ page.totalElements }} produtos</p>
                <div class="flex flex-wrap items-center gap-4">
                  <label class="flex items-center gap-2 text-[#55555e]">Por página
                    <select :value="pageSize" :disabled="busy" class="h-9 rounded-lg border border-line bg-white px-2" @change="changePageSize">
                      <option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option>
                    </select>
                  </label>
                  <nav class="flex items-center gap-2" aria-label="Paginação de produtos">
                    <button type="button" class="button button-secondary size-9 !p-0" aria-label="Página anterior" :disabled="busy || page.number === 0 || !page.totalPages" @click="changePage(page.number - 1)">
                      <ChevronLeft class="size-4" aria-hidden="true" />
                    </button>
                    <span>{{ page.totalPages ? `Página ${page.number + 1} de ${page.totalPages}` : 'Sem páginas' }}</span>
                    <button type="button" class="button button-secondary size-9 !p-0" aria-label="Próxima página" :disabled="busy || page.number + 1 >= page.totalPages" @click="changePage(page.number + 1)">
                      <ChevronRight class="size-4" aria-hidden="true" />
                    </button>
                  </nav>
                </div>
              </footer>
            </div>

            <div v-else id="stock-panel-history" role="tabpanel" aria-labelledby="stock-tab-history">
              <div class="flex flex-wrap items-end justify-between gap-4 border-b border-line p-4 sm:p-5">
                <label class="field-label w-full max-w-[380px]" for="history-product-select">Produto
                  <select id="history-product-select" v-model="selectedHistoryProductId" class="field-input" :disabled="!products.length || loadingTabMovements">
                    <option v-if="!products.length" value="">Nenhum produto disponível</option>
                    <option v-for="product in products" :key="product.id" :value="product.id">
                      {{ product.name }} ({{ formatQuantity(product.currentQuantity, product.unitMeasurement) }})
                    </option>
                  </select>
                </label>

                <div v-if="selectedHistoryProduct" class="flex flex-wrap items-center gap-3">
                  <div class="flex items-center gap-2 text-[13px] text-[#74747f]">
                    <StockStatusBadge :status="stockStatus(selectedHistoryProduct)" />
                    <span>Estoque atual: <strong class="text-ink">{{ formatQuantity(selectedHistoryProduct.currentQuantity, selectedHistoryProduct.unitMeasurement) }}</strong></span>
                  </div>
                  <div v-if="canUpdate" class="flex flex-wrap gap-2">
                    <button type="button" class="button button-secondary button-sm" :disabled="busy" @click="openMovement('ENTRY', selectedHistoryProduct)">
                      <ArrowDownToLine class="size-3.5" aria-hidden="true" />Dar entrada
                    </button>
                    <button
                      type="button"
                      class="button button-secondary button-sm"
                      :disabled="busy || selectedHistoryProduct.currentQuantity <= 0"
                      :title="selectedHistoryProduct.currentQuantity <= 0 ? 'Produto esgotado' : undefined"
                      @click="openMovement('CONSUMPTION', selectedHistoryProduct)"
                    >
                      <ArrowUpFromLine class="size-3.5" aria-hidden="true" />Registrar consumo
                    </button>
                  </div>
                </div>
              </div>

              <p v-if="loadingTabMovements" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando histórico de movimentações...</p>
              <div v-else-if="tabMovementsError" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
                <p>{{ tabMovementsError }}</p>
                <button type="button" class="mt-2 font-semibold underline" @click="loadTabMovements(tabMovementsPage.number)">Tentar novamente</button>
              </div>
              <template v-else-if="tabMovements.length">
                <StockMovementsTable
                  :movements="tabMovements"
                  :units="units"
                  :caption="`Histórico de movimentações de ${selectedHistoryProduct?.name ?? 'produto'}`"
                />
                <footer v-if="tabMovementsPage.totalPages > 1" class="flex flex-wrap items-center justify-between gap-4 border-t border-line p-4 text-[12px] sm:px-5">
                  <span class="text-[#74747f]">Total de {{ tabMovementsPage.totalElements }} movimentações</span>
                  <nav class="flex items-center gap-2" aria-label="Paginação de movimentações">
                    <button type="button" class="button button-secondary size-9 !p-0" aria-label="Página anterior" :disabled="loadingTabMovements || tabMovementsPage.number === 0" @click="changeTabMovementsPage(tabMovementsPage.number - 1)">
                      <ChevronLeft class="size-4" aria-hidden="true" />
                    </button>
                    <span>Página {{ tabMovementsPage.number + 1 }} de {{ tabMovementsPage.totalPages }}</span>
                    <button type="button" class="button button-secondary size-9 !p-0" aria-label="Próxima página" :disabled="loadingTabMovements || tabMovementsPage.number + 1 >= tabMovementsPage.totalPages" @click="changeTabMovementsPage(tabMovementsPage.number + 1)">
                      <ChevronRight class="size-4" aria-hidden="true" />
                    </button>
                  </nav>
                </footer>
              </template>
              <div v-else class="px-5 py-14 text-center" role="status">
                <History class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
                <p class="text-sm font-semibold">Nenhuma movimentação registrada.</p>
                <p class="mt-1 text-[13px] text-[#74747f]">
                  {{ selectedHistoryProduct ? `Entradas e consumos de ${selectedHistoryProduct.name} aparecerão aqui.` : 'Selecione um produto para consultar seu histórico.' }}
                </p>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <ProductHistoryDialog
      :open="Boolean(historyProduct)"
      :product="historyProduct"
      :can-update="canUpdate"
      :refresh-key="historyRefreshKey"
      @close="historyProduct = null"
      @entry="openMovement('ENTRY', $event)"
      @consumption="openMovement('CONSUMPTION', $event)"
    />

    <ProductFormDialog
      v-if="canCreate || canUpdate"
      :open="productFormOpen"
      :product="editingProduct"
      :submit="saveProduct"
      @close="closeProductForm"
    />

    <MovementFormDialog
      v-if="canUpdate"
      :open="Boolean(movementDialog)"
      :type="movementDialog?.type ?? 'ENTRY'"
      :product="movementDialog?.product ?? null"
      :submit="registerMovement"
      @close="movementDialog = null"
    />

    <StockDialog
      v-if="canUpdate"
      :open="Boolean(statusTarget)"
      :title="statusTarget?.active ? 'Inativar produto' : 'Reativar produto'"
      :busy="changingStatus"
      width-class="max-w-[440px]"
      @close="closeStatusConfirmation"
    >
      <p class="text-sm leading-6 text-[#55555e]">
        Deseja realmente {{ statusTarget?.active ? 'inativar' : 'reativar' }} o produto <strong class="break-words text-ink">{{ statusTarget?.name }}</strong>?
      </p>
      <p v-if="statusError" class="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ statusError }}</p>
      <div class="mt-6 flex justify-end gap-2">
        <button type="button" class="button button-secondary" :disabled="changingStatus" @click="closeStatusConfirmation">Cancelar</button>
        <button type="button" class="button button-primary" :disabled="changingStatus || !canUpdate" @click="confirmStatusChange">
          {{ changingStatus ? 'Alterando status...' : statusTarget?.active ? 'Inativar produto' : 'Reativar produto' }}
        </button>
      </div>
    </StockDialog>
  </DashboardLayout>
</template>

<style scoped src="../components/stock/stock-ui.css"></style>
