import api from './api'
import type { ApiResponse } from '../types/api'
import type { PageMetadata, PaginatedResponse } from '../types/user'
import type {
  CreateProductPayload,
  EntryMovementPayload,
  MovementListParams,
  MovementType,
  OutputMovementPayload,
  Product,
  ProductFilters,
  ProductListParams,
  StockMovement,
  UpdateProductPayload,
} from '../types/stock'

const PRODUCTS_BASE_PATH = '/admin/stock/products'
const MOVEMENTS_BASE_PATH = '/admin/stock/movements/product'

const productPath = (id: string) => `${PRODUCTS_BASE_PATH}/${encodeURIComponent(id)}`
const movementProductPath = (productId: string) => `${MOVEMENTS_BASE_PATH}/${encodeURIComponent(productId)}`

type RawMovement = Record<string, unknown>

function normalizeMovement(raw: RawMovement, fallbackProductId: string): StockMovement {
  const rawType = String(raw.type ?? raw.movementType ?? 'ENTRY').toUpperCase()
  const type: MovementType = rawType === 'CONSUMPTION' || rawType === 'EXIT' || rawType === 'OUTPUT' ? 'CONSUMPTION' : 'ENTRY'
  const createdAt = String(raw.createdAt ?? raw.date ?? raw.movementDate ?? new Date().toISOString())
  const date = String(raw.date ?? raw.movementDate ?? createdAt)

  const rawProduct = raw.product && typeof raw.product === 'object' ? (raw.product as Record<string, unknown>) : null
  const productId = String(raw.productId ?? rawProduct?.id ?? fallbackProductId)
  const productName = String(raw.productName ?? rawProduct?.name ?? '')

  const rawUser = raw.user && typeof raw.user === 'object' ? (raw.user as Record<string, unknown>) : null
  const userId = raw.userId ?? rawUser?.id
  const userName = raw.userName ?? raw.responsibleName ?? rawUser?.name
  const user = userId || userName
    ? { id: String(userId ?? ''), name: String(userName ?? userId ?? '—') }
    : null

  return {
    id: String(raw.id ?? `${productId}-${createdAt}-${Math.random()}`),
    productId,
    productName,
    type,
    quantity: Number(raw.quantity ?? 0),
    date,
    createdAt,
    user,
  }
}

function extractMovementsPage(body: unknown, productId: string): PaginatedResponse<StockMovement> {
  const envelope = body && typeof body === 'object' && 'data' in body ? (body as ApiResponse<unknown>).data : body

  if (Array.isArray(envelope)) {
    const first = envelope[0]
    if (first && typeof first === 'object' && 'content' in first && Array.isArray((first as PaginatedResponse<RawMovement>).content)) {
      const paginated = first as PaginatedResponse<RawMovement>
      const content = paginated.content.map(item => normalizeMovement(item, productId))
      const page: PageMetadata = paginated.page ?? {
        size: content.length || 10,
        number: 0,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
      }
      return { content, page }
    }

    const content = (envelope as RawMovement[]).map(item => normalizeMovement(item, productId))
    return {
      content,
      page: {
        size: content.length || 10,
        number: 0,
        totalElements: content.length,
        totalPages: content.length ? 1 : 0,
      },
    }
  }

  if (envelope && typeof envelope === 'object' && 'content' in envelope && Array.isArray((envelope as PaginatedResponse<RawMovement>).content)) {
    const paginated = envelope as PaginatedResponse<RawMovement>
    const content = paginated.content.map(item => normalizeMovement(item, productId))
    const page: PageMetadata = paginated.page ?? {
      size: content.length || 10,
      number: 0,
      totalElements: content.length,
      totalPages: content.length ? 1 : 0,
    }
    return { content, page }
  }

  return {
    content: [],
    page: { size: 10, number: 0, totalElements: 0, totalPages: 0 },
  }
}

const stockService = {
  async listProducts({ page = 0, size = 10, sort = 'name,ASC', filters = {} }: ProductListParams = {}): Promise<ApiResponse<PaginatedResponse<Product>[]>> {
    const params: ProductFilters & Pick<ProductListParams, 'page' | 'size' | 'sort'> = {
      page,
      size,
      sort,
    }

    if (filters.name?.trim()) params.name = filters.name.trim()
    if (filters.description?.trim()) params.description = filters.description.trim()
    if (filters.unitMeasurement) params.unitMeasurement = filters.unitMeasurement
    if (filters.priceCost !== undefined) params.priceCost = filters.priceCost
    if (filters.active !== undefined) params.active = filters.active

    const response = await api.get<ApiResponse<PaginatedResponse<Product>[]>>(PRODUCTS_BASE_PATH, {
      params,
      paramsSerializer: { indexes: null },
    })
    return response.data
  },

  async createProduct(payload: CreateProductPayload): Promise<ApiResponse<Product>> {
    const response = await api.post<ApiResponse<Product>>(PRODUCTS_BASE_PATH, {
      name: payload.name,
      description: payload.description ?? '',
      minimumQuantity: payload.minimumQuantity,
      initialQuantity: payload.initialQuantity,
      unitMeasurement: payload.unitMeasurement,
      priceCost: payload.priceCost,
      active: payload.active,
    })
    return response.data
  },

  async updateProduct(id: string, payload: UpdateProductPayload): Promise<ApiResponse<Product>> {
    const response = await api.put<ApiResponse<Product>>(productPath(id), {
      name: payload.name,
      description: payload.description ?? '',
      minimumQuantity: payload.minimumQuantity,
      unitMeasurement: payload.unitMeasurement,
      priceCost: payload.priceCost,
    })
    return response.data
  },

  async activateProduct(id: string): Promise<ApiResponse<void>> {
    const response = await api.patch<ApiResponse<void>>(`${productPath(id)}/activate`)
    return response.data
  },

  async deactivateProduct(id: string): Promise<ApiResponse<void>> {
    const response = await api.patch<ApiResponse<void>>(`${productPath(id)}/deactivate`)
    return response.data
  },

  async registerEntry(productId: string, payload: EntryMovementPayload): Promise<ApiResponse<StockMovement>> {
    const response = await api.post<ApiResponse<StockMovement>>(`${movementProductPath(productId)}/entry`, {
      quantity: payload.quantity,
    })
    return response.data
  },

  async registerOutput(productId: string, payload: OutputMovementPayload): Promise<ApiResponse<StockMovement>> {
    const response = await api.post<ApiResponse<StockMovement>>(`${movementProductPath(productId)}/output`, {
      userId: payload.userId,
      quantity: payload.quantity,
    })
    return response.data
  },

  async listMovements(productId: string, { page = 0, size = 10, sort }: MovementListParams = {}): Promise<PaginatedResponse<StockMovement>> {
    const params: MovementListParams = { page, size }
    if (sort) params.sort = sort
    const response = await api.get(movementProductPath(productId), {
      params,
      paramsSerializer: { indexes: null },
    })
    return extractMovementsPage(response.data, productId)
  },
}

export default stockService
