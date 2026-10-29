<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { isAxiosError } from 'axios'
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import { ChevronLeft, ChevronRight, Pencil, Plus, Power, RefreshCw, Scissors, Search, X } from '@lucide/vue'
import { toast } from 'vue-sonner'
import DashboardLayout from '../layouts/DashboardLayout.vue'
import { useAuthStore } from '../stores/auth'
import serviceService from '../services/serviceService'
import { createServiceSchema, updateServiceSchema, type UpdateServiceInput } from '../schemas/service.schema'
import { SERVICE_TECHNIQUES, type BarberService, type ServiceCategory, type ServiceFilters, type ServiceTechnique, type UpdateServicePayload } from '../types/service'
import type { PageMetadata } from '../types/user'
import { handleApiError } from '../utils/errorHandler'

const auth = useAuthStore()
const canManageServices = computed(() => auth.user?.roles?.includes('BARBER') ?? false)
const services = ref<BarberService[]>([])
const categories = ref<ServiceCategory[]>([])
const loading = ref(false)
const hasLoaded = ref(false)
const error = ref('')
const loadingCategories = ref(false)
const categoriesError = ref('')
const saving = ref(false)
const changingStatus = ref(false)
const busy = computed(() => loading.value || saving.value || changingStatus.value)

const nameFilter = ref('')
const statusFilter = ref<'' | 'true' | 'false'>('')
const categoryFilter = ref('')
const techniqueFilter = ref<'' | ServiceTechnique>('')
const minPriceFilter = ref('')
const maxPriceFilter = ref('')
const appliedFilters = ref<ServiceFilters>({})
const pageSize = ref(10)
const requestedPage = ref(0)
const page = ref<PageMetadata>({ size: 10, number: 0, totalElements: 0, totalPages: 0 })
const pageSizes = [10, 20, 50]
const hasAppliedFilters = computed(() => Object.keys(appliedFilters.value).length > 0)
const firstItem = computed(() => services.value.length ? page.value.number * page.value.size + 1 : 0)
const lastItem = computed(() => services.value.length ? firstItem.value + services.value.length - 1 : 0)
const money = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

const editorDialog = ref<HTMLDialogElement | null>(null)
const createButton = ref<HTMLButtonElement | null>(null)
const editorOpen = ref(false)
const editing = ref<BarberService | null>(null)
const saveError = ref('')
const imageInput = ref<HTMLInputElement | null>(null)
const image = ref<File | undefined>()
const imageError = ref('')
let editorTrigger: HTMLElement | null = null
const statusDialog = ref<HTMLDialogElement | null>(null)
const statusTarget = ref<BarberService | null>(null)
const pendingUpdate = ref<UpdateServicePayload | null>(null)
const statusError = ref('')
let statusTrigger: HTMLElement | null = null

const validationSchema = computed(() => toTypedSchema(editing.value ? updateServiceSchema : createServiceSchema))
const { defineField, errors, handleSubmit, resetForm, setFieldError, isSubmitting } = useForm({
  validationSchema,
  initialValues: emptyForm(),
})
const [name, nameProps] = defineField('name')
const [categoryId, categoryProps] = defineField('categoryId')
const [technique, techniqueProps] = defineField('technique')
const [price, priceProps] = defineField('price')
const [durationTime, durationProps] = defineField('durationTime')
const [active, activeProps] = defineField('active')
const editorBusy = computed(() => saving.value || isSubmitting.value || changingStatus.value)
const missingCurrentCategory = computed(() => editing.value && !categories.value.some(category => category.id === editing.value?.category.id))
const cannotCreate = computed(() => !editing.value && (loadingCategories.value || Boolean(categoriesError.value) || !categories.value.length))

function emptyForm(): UpdateServiceInput {
  return { name: '', categoryId: '', technique: '', price: '', durationTime: '', active: true }
}

function techniqueLabel(value: ServiceTechnique): string {
  return SERVICE_TECHNIQUES.find(technique => technique.value === value)?.label ?? value
}

function restoreFocus(trigger: HTMLElement | null) {
  if (trigger?.isConnected && trigger.getClientRects().length && !trigger.matches(':disabled')) trigger.focus()
  else createButton.value?.focus()
}

function requestMessage(cause: unknown, fallback: string): string {
  if (!isAxiosError(cause)) return fallback
  if (cause.response?.status === 403) return 'Você não tem acesso ao gerenciamento de serviços.'
  if (cause.response?.status === 401) return 'Sua sessão expirou. Faça login novamente.'
  if (!cause.response) return 'Não foi possível conectar ao servidor. Tente novamente.'
  return fallback
}

async function loadServices(targetPage = requestedPage.value) {
  if (loading.value || !canManageServices.value) return
  loading.value = true
  error.value = ''
  requestedPage.value = targetPage
  try {
    const fetchPage = async (number: number) => {
      const response = await serviceService.list({ page: number, size: pageSize.value, filters: { ...appliedFilters.value } })
      const result = response.data?.[0]
      if (!result || !Array.isArray(result.content) || !result.page) throw new Error('Resposta de serviços inválida.')
      return result
    }
    let result = await fetchPage(targetPage)
    const lastPage = Math.max(result.page.totalPages - 1, 0)
    // Recupera uma página válida quando os filtros ou registros reduzem a listagem.
    if (result.page.number > lastPage) result = await fetchPage(lastPage)
    services.value = result.content
    page.value = result.page
    requestedPage.value = result.page.number
  } catch (cause) {
    error.value = requestMessage(cause, 'Não foi possível carregar os serviços.')
    handleApiError(cause, undefined, error.value)
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

async function loadCategories() {
  if (loadingCategories.value || !canManageServices.value) return
  loadingCategories.value = true
  categoriesError.value = ''
  try {
    const response = await serviceService.listCategories()
    if (!Array.isArray(response.data)) throw new Error('Resposta de categorias inválida.')
    categories.value = response.data
  } catch (cause) {
    categoriesError.value = requestMessage(cause, 'Não foi possível carregar as categorias.')
    handleApiError(cause, undefined, categoriesError.value)
  } finally {
    loadingCategories.value = false
  }
}

async function applyFilters() {
  if (busy.value) return
  const filters: ServiceFilters = {}
  if (nameFilter.value.trim()) filters.name = nameFilter.value.trim()
  if (statusFilter.value !== '') filters.active = statusFilter.value === 'true'
  if (categoryFilter.value) filters.categoryId = categoryFilter.value
  if (techniqueFilter.value) filters.technique = techniqueFilter.value
  const min = parseFloat(minPriceFilter.value.replace(',', '.'))
  const max = parseFloat(maxPriceFilter.value.replace(',', '.'))
  if (!isNaN(min) && min >= 0) filters.minPrice = min
  if (!isNaN(max) && max >= 0) filters.maxPrice = max
  appliedFilters.value = filters
  await loadServices(0)
}

async function clearFilters() {
  if (busy.value) return
  nameFilter.value = ''
  statusFilter.value = ''
  categoryFilter.value = ''
  techniqueFilter.value = ''
  minPriceFilter.value = ''
  maxPriceFilter.value = ''
  appliedFilters.value = {}
  await loadServices(0)
}

async function changePage(target: number) {
  if (busy.value || target < 0 || target >= page.value.totalPages) return
  await loadServices(target)
}

async function changePageSize(event: Event) {
  if (busy.value) return
  const size = Number((event.target as HTMLSelectElement).value)
  if (!pageSizes.includes(size)) return
  pageSize.value = size
  await loadServices(0)
}

function clearImage() {
  image.value = undefined
  imageError.value = ''
  if (imageInput.value) imageInput.value.value = ''
}

function selectImage(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  image.value = undefined
  imageError.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type) || !/\.(jpe?g|png)$/i.test(file.name)) {
    imageError.value = 'Selecione uma imagem JPG, JPEG ou PNG.'
  } else if (file.size > 10 * 1024 * 1024) {
    imageError.value = 'A imagem deve ter no máximo 10 MB.'
  } else {
    image.value = file
  }
}

async function openEditor(service?: BarberService) {
  if (!canManageServices.value || busy.value || editorOpen.value || statusTarget.value) return
  editorTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
  editing.value = service ?? null
  saveError.value = ''
  clearImage()
  resetForm({ values: service ? {
    name: service.name,
    categoryId: service.category.id,
    technique: service.technique,
    price: String(service.price),
    durationTime: String(service.durationTime),
    active: service.active,
  } : emptyForm() })
  editorOpen.value = true
  await nextTick()
  editorDialog.value?.showModal()
  editorDialog.value?.querySelector<HTMLInputElement>('#service-name')?.focus()
}

function finishEditor() {
  editorDialog.value?.close()
  editorOpen.value = false
  editing.value = null
  clearImage()
  resetForm({ values: emptyForm() })
  restoreFocus(editorTrigger)
}

function closeEditor() {
  if (editorBusy.value) return
  finishEditor()
}

const submitService = handleSubmit(async values => {
  if (busy.value || !canManageServices.value || !editorOpen.value || imageError.value || cannotCreate.value) return
  const service = editing.value
  if (service?.active && !values.active) {
    await openStatusConfirmation(service, values)
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    if (service) {
      await serviceService.update(service.id, values)
    } else {
      if (!values.categoryId) {
        setFieldError('categoryId', 'Selecione uma categoria.')
        return
      }
      await serviceService.create({ ...values, categoryId: values.categoryId }, image.value)
    }
    toast.success(service ? 'Serviço atualizado com sucesso.' : 'Serviço cadastrado com sucesso.')
    finishEditor()
    await loadServices(service ? requestedPage.value : 0)
  } catch (cause) {
    saveError.value = requestMessage(cause, 'Não foi possível salvar o serviço. Confira os dados e tente novamente.')
    handleApiError(cause, setFieldError, saveError.value)
  } finally {
    saving.value = false
    await nextTick()
    if (!editorOpen.value) restoreFocus(editorTrigger)
  }
})

function saveService() {
  if (editorBusy.value || busy.value || imageError.value) return
  void submitService()
}

async function openStatusConfirmation(service: BarberService, update?: UpdateServicePayload) {
  if (!canManageServices.value || busy.value || (editorOpen.value && !update) || statusTarget.value) return
  statusTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
  statusTarget.value = service
  pendingUpdate.value = update ?? null
  statusError.value = ''
  await nextTick()
  statusDialog.value?.showModal()
  statusDialog.value?.querySelector<HTMLButtonElement>('[data-cancel]')?.focus()
}

function closeStatusConfirmation() {
  if (changingStatus.value) return
  statusDialog.value?.close()
  statusTarget.value = null
  pendingUpdate.value = null
  restoreFocus(statusTrigger)
}

async function changeStatus() {
  const service = statusTarget.value
  if (!service || busy.value || !canManageServices.value) return
  const update = pendingUpdate.value
  changingStatus.value = true
  statusError.value = ''
  try {
    if (update) await serviceService.update(service.id, update)
    else await serviceService.changeStatus(service, !service.active)
    statusDialog.value?.close()
    statusTarget.value = null
    pendingUpdate.value = null
    if (update) finishEditor()
    toast.success(update ? 'Serviço atualizado e inativado com sucesso.' : service.active ? 'Serviço inativado com sucesso.' : 'Serviço reativado com sucesso.')
    await loadServices()
  } catch (cause) {
    statusError.value = requestMessage(cause, 'Não foi possível alterar o status do serviço. Tente novamente.')
    handleApiError(cause, update ? setFieldError : undefined, statusError.value)
  } finally {
    changingStatus.value = false
    await nextTick()
    if (!statusTarget.value) restoreFocus(statusTrigger)
  }
}

watch(editorBusy, async (current, previous) => {
  if (previous && !current && editorOpen.value && !statusTarget.value) {
    // Aguarda os campos serem habilitados antes de focar o primeiro erro.
    await nextTick()
    editorDialog.value?.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus()
  }
})

watch(canManageServices, allowed => {
  if (allowed) {
    void loadServices(0)
    void loadCategories()
  } else {
    editorDialog.value?.close()
    statusDialog.value?.close()
    editorOpen.value = false
    statusTarget.value = null
  }
})

onMounted(() => {
  if (canManageServices.value) {
    void loadServices(0)
    void loadCategories()
  }
})
</script>

<template>
  <DashboardLayout>
    <section class="bg-paper pb-8 text-ink" aria-labelledby="services-title">
      <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
        <header class="mb-7 flex flex-wrap items-end justify-between gap-5">
          <div>
            <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">MEU TRABALHO</p>
            <h1 id="services-title" class="text-[27px] leading-9 font-bold tracking-[-1px]">Gerenciamento de Serviços</h1>
            <p class="mt-1 text-[14px] leading-[21px] text-[#74747f]">Cadastre e mantenha seus serviços, preços e disponibilidade.</p>
          </div>
          <div v-if="canManageServices" class="flex flex-wrap gap-2">
            <button type="button" class="button button-secondary" :disabled="busy" @click="loadServices()">
              <RefreshCw class="size-4" :class="{ 'animate-spin motion-reduce:animate-none': loading }" aria-hidden="true" />Atualizar
            </button>
            <button ref="createButton" type="button" class="button button-primary" :disabled="busy" @click="openEditor()">
              <Plus class="size-4" aria-hidden="true" />Novo serviço
            </button>
          </div>
        </header>

        <p v-if="!canManageServices" class="rounded-xl border border-line bg-white p-5 text-sm" role="alert">
          {{ auth.isAuthenticated ? 'O gerenciamento de serviços está disponível apenas para barbeiros.' : 'Faça login como barbeiro para gerenciar os serviços.' }}
        </p>
        <div v-else class="overflow-hidden rounded-2xl border border-line bg-white" :aria-busy="loading">
          <form class="border-b border-line p-4 sm:p-5" @submit.prevent="applyFilters">
            <fieldset :disabled="busy" class="grid min-w-0 gap-3 sm:grid-cols-2 xl:grid-cols-3">
              <legend class="sr-only">Filtros de serviços</legend>
              <label class="field-label" for="service-filter-name">Nome
                <input id="service-filter-name" v-model="nameFilter" type="search" class="field-input" placeholder="Buscar pelo nome" />
              </label>
              <label class="field-label" for="service-filter-category">Categoria
                <select id="service-filter-category" v-model="categoryFilter" class="field-input" :disabled="loadingCategories">
                  <option value="">Todas</option>
                  <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}{{ category.active ? '' : ' (inativa)' }}</option>
                </select>
              </label>
              <label class="field-label" for="service-filter-technique">Técnica
                <select id="service-filter-technique" v-model="techniqueFilter" class="field-input">
                  <option value="">Todas</option>
                  <option v-for="option in SERVICE_TECHNIQUES" :key="option.value" :value="option.value">{{ option.label }}</option>
                </select>
              </label>
              <label class="field-label" for="service-filter-status">Status
                <select id="service-filter-status" v-model="statusFilter" class="field-input">
                  <option value="">Todos</option><option value="true">Ativos</option><option value="false">Inativos</option>
                </select>
              </label>
              <label class="field-label" for="service-filter-min-price">Preço mínimo (R$)
                <input id="service-filter-min-price" v-model="minPriceFilter" type="text" inputmode="decimal" class="field-input" placeholder="0,00" />
              </label>
              <label class="field-label" for="service-filter-max-price">Preço máximo (R$)
                <input id="service-filter-max-price" v-model="maxPriceFilter" type="text" inputmode="decimal" class="field-input" placeholder="9999,99" />
              </label>
              <div class="flex flex-wrap gap-2 sm:col-span-2 xl:col-span-3">
                <button type="submit" class="button button-primary"><Search class="size-4" aria-hidden="true" />Filtrar</button>
                <button type="button" class="button button-secondary" @click="clearFilters"><X class="size-4" aria-hidden="true" />Limpar</button>
              </div>
            </fieldset>
          </form>
          <p v-if="loadingCategories" class="px-5 pt-4 text-xs text-[#74747f]" role="status">Carregando categorias...</p>
          <div v-if="categoriesError" class="m-4 rounded-lg bg-amber-50 p-3 text-[13px] text-amber-900 sm:m-5" role="alert">
            <p>{{ categoriesError }}</p>
            <button type="button" class="mt-2 font-semibold underline" :disabled="loadingCategories" @click="loadCategories">Tentar carregar categorias novamente</button>
          </div>
          <div v-if="error" class="m-4 rounded-lg bg-red-50 p-4 text-[13px] text-red-800 sm:m-5" role="alert">
            <p>{{ error }}</p>
            <button type="button" class="mt-2 font-semibold underline" :disabled="loading" @click="loadServices()">Tentar novamente</button>
          </div>
          <p v-if="loading" class="px-5 py-14 text-center text-sm text-[#74747f]" role="status">Carregando serviços...</p>
          <div v-else-if="hasLoaded && !error && services.length" class="overflow-x-auto">
            <table class="w-full min-w-[900px] text-left text-[13px]">
              <caption class="sr-only">Seus serviços, categorias, técnicas, preços, durações, status e ações</caption>
              <thead class="border-b border-line bg-paper text-[11px] text-[#74747f]">
                <tr>
                  <th scope="col" class="px-5 py-3 font-semibold">Nome</th><th scope="col" class="px-5 py-3 font-semibold">Categoria</th>
                  <th scope="col" class="px-5 py-3 font-semibold">Técnica</th><th scope="col" class="px-5 py-3 font-semibold">Preço</th>
                  <th scope="col" class="px-5 py-3 font-semibold">Duração</th><th scope="col" class="px-5 py-3 font-semibold">Status</th>
                  <th scope="col" class="px-5 py-3 font-semibold">Ações</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-line/70">
                <tr v-for="service in services" :key="service.id" class="hover:bg-paper/40">
                  <td class="max-w-[240px] break-words px-5 py-4 font-semibold">{{ service.name }}</td>
                  <td class="max-w-[180px] break-words px-5 py-4 text-[#55555e]">{{ service.category.name }}</td>
                  <td class="px-5 py-4 text-[#55555e]">{{ techniqueLabel(service.technique) }}</td>
                  <td class="whitespace-nowrap px-5 py-4 font-semibold">{{ money.format(service.price) }}</td>
                  <td class="px-5 py-4 text-[#55555e]">{{ service.durationTime }}</td>
                  <td class="px-5 py-4"><span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold" :class="service.active ? 'bg-emerald-50 text-emerald-800' : 'bg-paper text-[#55555e]'">
                    <span class="size-1.5 rounded-full" :class="service.active ? 'bg-emerald-600' : 'bg-[#74747f]'" aria-hidden="true"></span>{{ service.active ? 'Ativo' : 'Inativo' }}
                  </span></td>
                  <td class="px-5 py-4"><div class="flex gap-2">
                    <button type="button" class="button button-secondary" :aria-label="`Editar ${service.name}`" :disabled="busy" @click="openEditor(service)"><Pencil class="size-4" aria-hidden="true" />Editar</button>
                    <button type="button" class="button button-secondary" :aria-label="`${service.active ? 'Inativar' : 'Reativar'} ${service.name}`" :disabled="busy" @click="openStatusConfirmation(service)"><Power class="size-4" aria-hidden="true" />{{ service.active ? 'Inativar' : 'Reativar' }}</button>
                  </div></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else-if="hasLoaded && !error" class="px-5 py-14 text-center" role="status">
            <Scissors class="mx-auto mb-3 size-8 text-[#a67039]" :stroke-width="1.5" aria-hidden="true" />
            <p class="text-sm font-semibold">Nenhum serviço encontrado.</p>
            <p class="mt-1 text-[13px] text-[#74747f]">{{ hasAppliedFilters ? 'Tente ajustar ou limpar os filtros da busca.' : 'Cadastre seu primeiro serviço para começar.' }}</p>
            <button v-if="hasAppliedFilters" type="button" class="mt-4 text-[13px] font-semibold underline" @click="clearFilters">Limpar filtros</button>
          </div>
          <footer v-if="hasLoaded && !error && !loading" class="flex flex-wrap items-center justify-between gap-4 border-t border-line p-4 text-[12px] sm:px-5">
            <p class="text-[#74747f]" role="status">{{ firstItem }}–{{ lastItem }} de {{ page.totalElements }} serviços</p>
            <div class="flex flex-wrap items-center gap-4">
              <label class="flex items-center gap-2 text-[#55555e]">Por página
                <select :value="pageSize" :disabled="busy" class="h-9 rounded-lg border border-line bg-white px-2" @change="changePageSize"><option v-for="size in pageSizes" :key="size" :value="size">{{ size }}</option></select>
              </label>
              <nav class="flex items-center gap-2" aria-label="Paginação de serviços">
                <button type="button" class="button button-secondary size-9 !p-0" aria-label="Página anterior" :disabled="busy || page.number === 0 || !page.totalPages" @click="changePage(page.number - 1)"><ChevronLeft class="size-4" aria-hidden="true" /></button>
                <span>{{ page.totalPages ? `Página ${page.number + 1} de ${page.totalPages}` : 'Sem páginas' }}</span>
                <button type="button" class="button button-secondary size-9 !p-0" aria-label="Próxima página" :disabled="busy || page.number + 1 >= page.totalPages" @click="changePage(page.number + 1)"><ChevronRight class="size-4" aria-hidden="true" /></button>
              </nav>
            </div>
          </footer>
        </div>
      </div>
    </section>

    <dialog ref="editorDialog" class="service-dialog max-w-[600px]" aria-labelledby="service-editor-title" @cancel.prevent="closeEditor">
      <header class="mb-5 flex items-start justify-between gap-4">
        <div><p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">MEUS SERVIÇOS</p><h2 id="service-editor-title" class="text-xl font-bold">{{ editing ? 'Editar serviço' : 'Novo serviço' }}</h2></div>
        <button type="button" class="button button-secondary size-9 !p-0" aria-label="Fechar formulário" :disabled="editorBusy" @click="closeEditor"><X class="size-4" aria-hidden="true" /></button>
      </header>
      <form novalidate @submit.prevent="saveService">
        <div v-if="saveError" class="mb-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ saveError }}</div>
        <fieldset :disabled="editorBusy" class="grid min-w-0 gap-4 sm:grid-cols-2">
          <legend class="sr-only">Dados do serviço</legend>
          <div class="sm:col-span-2">
            <label for="service-name" class="field-label">Nome</label>
            <input id="service-name" v-model="name" v-bind="nameProps" name="name" type="text" maxlength="255" class="field-input" :aria-invalid="Boolean(errors.name)" :aria-describedby="errors.name ? 'service-name-error' : undefined" />
            <p v-if="errors.name" id="service-name-error" class="field-error">{{ errors.name }}</p>
          </div>
          <div>
            <label for="service-category" class="field-label">Categoria</label>
            <select id="service-category" v-model="categoryId" v-bind="categoryProps" name="categoryId" class="field-input" :disabled="loadingCategories" :aria-invalid="Boolean(errors.categoryId)" :aria-describedby="errors.categoryId ? 'service-category-error' : undefined">
              <option value="">{{ editing ? 'Manter categoria atual' : 'Selecione uma categoria' }}</option>
              <option v-if="missingCurrentCategory && editing" :value="editing.category.id">{{ editing.category.name }} (atual)</option>
              <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}{{ category.active ? '' : ' (inativa)' }}</option>
            </select>
            <p v-if="errors.categoryId" id="service-category-error" class="field-error">{{ errors.categoryId }}</p>
          </div>
          <div>
            <label for="service-technique" class="field-label">Técnica</label>
            <select id="service-technique" v-model="technique" v-bind="techniqueProps" name="technique" class="field-input" :aria-invalid="Boolean(errors.technique)" :aria-describedby="errors.technique ? 'service-technique-error' : undefined">
              <option value="">Selecione uma técnica</option><option v-for="option in SERVICE_TECHNIQUES" :key="option.value" :value="option.value">{{ option.label }}</option>
            </select>
            <p v-if="errors.technique" id="service-technique-error" class="field-error">{{ errors.technique }}</p>
          </div>
          <div>
            <label for="service-price" class="field-label">Preço (R$)</label>
            <input id="service-price" v-model="price" v-bind="priceProps" name="price" type="text" inputmode="decimal" placeholder="50,00" class="field-input" :aria-invalid="Boolean(errors.price)" :aria-describedby="errors.price ? 'service-price-error' : undefined" />
            <p v-if="errors.price" id="service-price-error" class="field-error">{{ errors.price }}</p>
          </div>
          <div>
            <label for="service-duration" class="field-label">Duração</label>
            <input id="service-duration" v-model="durationTime" v-bind="durationProps" name="durationTime" type="text" inputmode="numeric" class="field-input" :aria-invalid="Boolean(errors.durationTime)" :aria-describedby="errors.durationTime ? 'service-duration-error' : undefined" />
            <p v-if="errors.durationTime" id="service-duration-error" class="field-error">{{ errors.durationTime }}</p>
          </div>
          <div>
            <label for="service-active" class="field-label">Status</label>
            <select id="service-active" v-model="active" v-bind="activeProps" name="active" class="field-input" :aria-invalid="Boolean(errors.active)"><option :value="true">Ativo</option><option :value="false">Inativo</option></select>
            <p v-if="errors.active" class="field-error">{{ errors.active }}</p>
          </div>
          <div v-if="!editing" class="sm:col-span-2">
            <label for="service-image" class="field-label">Imagem (opcional)</label>
            <input id="service-image" ref="imageInput" type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" class="field-input !h-auto py-2" :aria-invalid="Boolean(imageError)" :aria-describedby="imageError ? 'service-image-error' : 'service-image-help'" @change="selectImage" />
            <p id="service-image-help" class="mt-1.5 text-xs text-[#74747f]">JPG, JPEG ou PNG, até 10 MB.</p>
            <p v-if="imageError" id="service-image-error" class="field-error">{{ imageError }}</p>
            <button v-if="image || imageError" type="button" class="mt-2 text-xs font-semibold underline" @click="clearImage">Remover imagem</button>
          </div>
        </fieldset>
        <p v-if="loadingCategories" class="mt-4 text-sm text-[#74747f]" role="status">Carregando categorias...</p>
        <div v-else-if="categoriesError" class="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900" role="alert">
          <p>{{ categoriesError }}</p><button type="button" class="mt-2 font-semibold underline" :disabled="editorBusy" @click="loadCategories">Tentar novamente</button>
        </div>
        <p v-else-if="!categories.length && !editing" class="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900" role="status">Nenhuma categoria disponível. É necessário cadastrar uma categoria antes de criar um serviço.</p>
        <div class="mt-6 flex justify-end gap-2">
          <button type="button" class="button button-secondary" :disabled="editorBusy" @click="closeEditor">Cancelar</button>
          <button type="submit" class="button button-primary" :disabled="editorBusy || cannotCreate || Boolean(imageError) || !canManageServices">{{ editorBusy ? 'Salvando...' : 'Salvar serviço' }}</button>
        </div>
      </form>
    </dialog>

    <dialog ref="statusDialog" class="service-dialog max-w-[440px]" aria-labelledby="service-status-title" aria-describedby="service-status-description" @cancel.prevent="closeStatusConfirmation">
      <h2 id="service-status-title" class="text-xl font-bold">{{ statusTarget?.active ? 'Inativar serviço' : 'Reativar serviço' }}</h2>
      <p id="service-status-description" class="mt-3 text-sm leading-6 text-[#55555e]">Deseja realmente {{ statusTarget?.active ? 'inativar' : 'reativar' }} o serviço <strong class="break-words">{{ statusTarget?.name }}</strong>?</p>
      <p v-if="statusTarget?.active" class="mt-2 text-sm leading-6 text-[#74747f]">O serviço continuará disponível no histórico administrativo.</p>
      <p v-if="statusError" class="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800" role="alert">{{ statusError }}</p>
      <div class="mt-6 flex justify-end gap-2">
        <button type="button" data-cancel class="button button-secondary" :disabled="changingStatus" @click="closeStatusConfirmation">Cancelar</button>
        <button type="button" class="button button-primary" :disabled="busy || !canManageServices" @click="changeStatus">{{ changingStatus ? 'Alterando status...' : statusTarget?.active ? 'Inativar serviço' : 'Reativar serviço' }}</button>
      </div>
    </dialog>
  </DashboardLayout>
</template>

<style scoped>
.field-label { display: block; color: #55555e; font-size: 12px; font-weight: 600; }
.field-input { display: block; width: 100%; min-width: 0; height: 40px; margin-top: 6px; padding: 0 12px; border: 1px solid var(--color-line); border-radius: 12px; background: var(--color-paper); color: var(--color-ink); font-size: 13px; font-weight: 400; }
.field-input[aria-invalid="true"] { border-color: #b91c1c; }
.field-error { margin-top: 6px; color: #b91c1c; font-size: 12px; }
.button { display: inline-flex; min-height: 38px; align-items: center; justify-content: center; gap: 8px; border: 1px solid transparent; border-radius: 12px; padding: 8px 14px; font-size: 13px; font-weight: 600; }
.button-primary { background: var(--color-ink); color: white; }
.button-primary:hover:not(:disabled) { background: var(--color-ink-soft); }
.button-secondary { border-color: var(--color-line); background: white; color: #55555e; }
.button-secondary:hover:not(:disabled) { background: var(--color-paper); }
.service-dialog { margin: auto; width: calc(100% - 32px); max-height: calc(100dvh - 32px); overflow-y: auto; border: 1px solid var(--color-line); border-radius: 16px; padding: 24px; background: white; color: var(--color-ink); box-shadow: 0 25px 50px -12px rgb(0 0 0 / 25%); }
.service-dialog::backdrop { background: rgb(0 0 0 / 40%); }
button:not(:disabled) { cursor: pointer; }
button:disabled, input:disabled, select:disabled { cursor: not-allowed; opacity: .6; }
button, input, select { outline-offset: 3px; }
button:focus-visible, input:focus-visible, select:focus-visible { outline: 2px solid #a67039; }
</style>
