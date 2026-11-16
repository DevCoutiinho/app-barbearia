export const STOCK_UNIT_VALUES = ['UN', 'G', 'ML', 'L'] as const
export type StockUnit = typeof STOCK_UNIT_VALUES[number]

export const STOCK_UNITS: ReadonlyArray<{ value: StockUnit; label: string }> = [
  { value: 'UN', label: 'Unidade' },
  { value: 'G', label: 'Gramas' },
  { value: 'ML', label: 'Mililitros' },
  { value: 'L', label: 'Litros' },
]

export type StockStatus = 'OUT' | 'LOW' | 'OK'
export const MOVEMENT_TYPE_VALUES = ['CONSUMPTION', 'ENTRY'] as const
export type MovementType = typeof MOVEMENT_TYPE_VALUES[number]

export interface StockUser {
  id: string
  name: string
}

export interface Product {
  id: string
  name: string
  description: string | null
  minimumQuantity: number
  currentQuantity: number
  unitMeasurement: StockUnit
  priceCost: number
  active: boolean
  updatedAt: string
}

export interface ProductFilters {
  name?: string
  description?: string
  unitMeasurement?: StockUnit
  priceCost?: number
  active?: boolean
}

export interface ProductListParams {
  page?: number
  size?: number
  sort?: string | string[]
  filters?: ProductFilters
}

export interface CreateProductPayload {
  name: string
  description: string
  minimumQuantity: number
  initialQuantity: number
  unitMeasurement: StockUnit
  priceCost: number
  active: boolean
}

export interface UpdateProductPayload {
  name: string
  description: string
  minimumQuantity: number
  unitMeasurement: StockUnit
  priceCost: number
}

export interface EntryMovementPayload {
  quantity: number
}

export interface OutputMovementPayload {
  userId: string
  quantity: number
}

export interface StockMovement {
  id: string
  productId: string
  productName: string
  type: MovementType
  quantity: number
  date: string
  createdAt: string
  user: StockUser | null
}

export interface MovementListParams {
  page?: number
  size?: number
  sort?: string | string[]
}
