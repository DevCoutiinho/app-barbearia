export const STOCK_UNIT_VALUES = ['UN', 'G', 'ML', 'L'] as const
export type StockUnit = typeof STOCK_UNIT_VALUES[number]

export const STOCK_UNITS: ReadonlyArray<{ value: StockUnit; label: string }> = [
  { value: 'UN', label: 'Unidade' },
  { value: 'G', label: 'Gramas' },
  { value: 'ML', label: 'Mililitros' },
  { value: 'L', label: 'Litros' },
]

export const PRODUCT_CATEGORIES = ['Cabelo', 'Barba', 'Higiene', 'Descartáveis', 'Ferramentas', 'Outros'] as const

export type StockStatus = 'OUT' | 'LOW' | 'OK'
export type MovementType = 'ENTRY' | 'EXIT'

export interface StockUser {
  id: string
  name: string
}

export interface Product {
  id: string
  name: string
  category: string
  unit: StockUnit
  quantity: number
  minimum: number
  updatedAt: string
}

export interface StockMovement {
  id: string
  productId: string
  productName: string
  type: MovementType
  quantity: number
  // Data informada pelo usuário (YYYY-MM-DD), não o instante do registro.
  date: string
  note: string | null
  user: StockUser
  createdAt: string
}

export interface CreateProductPayload {
  name: string
  category: string
  unit: StockUnit
  initialQuantity: number
  minimum: number
}

export interface MovementPayload {
  quantity: number
  date: string
  note?: string
  // Responsável vindo da sessão logada.
  user: StockUser
}
