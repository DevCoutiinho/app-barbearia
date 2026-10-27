import api from './api'
import type { ApiResponse } from '../types/api'
import type { CreateProductPayload, MovementPayload, MovementType, Product, StockMovement } from '../types/stock'
import { toDateInput } from '../utils/stock'

export interface StockApi {
  listProducts(): Promise<Product[]>
  createProduct(payload: CreateProductPayload): Promise<Product>
  deleteProduct(id: string): Promise<void>
  registerEntry(productId: string, payload: MovementPayload): Promise<StockMovement>
  registerExit(productId: string, payload: MovementPayload): Promise<StockMovement>
  listMovements(productId: string): Promise<StockMovement[]>
}

/** Aceita respostas no envelope `ApiResponse` ou o corpo puro. */
function unwrap<T>(body: ApiResponse<T> | T): T {
  if (body && typeof body === 'object' && 'status' in body && 'data' in body) return (body as ApiResponse<T>).data as T
  return body as T
}

const productPath = (id: string) => `/products/${encodeURIComponent(id)}`

export const httpStockApi: StockApi = {
  async listProducts() {
    const response = await api.get<ApiResponse<Product[]> | Product[]>('/products')
    return unwrap(response.data)
  },

  async createProduct(payload) {
    const response = await api.post<ApiResponse<Product> | Product>('/products', payload)
    return unwrap(response.data)
  },

  async deleteProduct(id) {
    await api.delete(productPath(id))
  },

  async registerEntry(productId, payload) {
    const response = await api.post<ApiResponse<StockMovement> | StockMovement>(`${productPath(productId)}/entries`, payload)
    return unwrap(response.data)
  },

  async registerExit(productId, payload) {
    const response = await api.post<ApiResponse<StockMovement> | StockMovement>(`${productPath(productId)}/exits`, payload)
    return unwrap(response.data)
  },

  async listMovements(productId) {
    const response = await api.get<ApiResponse<StockMovement[]> | StockMovement[]>(`${productPath(productId)}/movements`)
    return unwrap(response.data)
  },
}

// ====================
// Mock em memória (usado enquanto o back-end não expõe esse contrato)
// ====================
export function createMockStockApi(latency = 350): StockApi {
  const daysAgo = (days: number) => toDateInput(new Date(Date.now() - days * 24 * 60 * 60 * 1000))
  const system = { id: 'mock-user', name: 'Administrador' }
  let sequence = 0
  const nextId = () => `mock-${Date.now().toString(36)}-${(sequence++).toString(36)}`
  const wait = () => new Promise(resolve => setTimeout(resolve, latency))
  const clone = <T>(value: T): T => structuredClone(value)

  const products: Product[] = [
    { id: 'p-pomada', name: 'Pomada modeladora', category: 'Cabelo', unit: 'UN', quantity: 0, minimum: 6, updatedAt: daysAgo(1) },
    { id: 'p-oleo', name: 'Óleo para barba', category: 'Barba', unit: 'ML', quantity: 180, minimum: 500, updatedAt: daysAgo(2) },
    { id: 'p-lamina', name: 'Lâmina descartável', category: 'Descartáveis', unit: 'UN', quantity: 14, minimum: 40, updatedAt: daysAgo(0) },
    { id: 'p-shampoo', name: 'Shampoo antiqueda', category: 'Cabelo', unit: 'L', quantity: 8, minimum: 3, updatedAt: daysAgo(5) },
    { id: 'p-toalha', name: 'Toalha de papel', category: 'Higiene', unit: 'UN', quantity: 220, minimum: 100, updatedAt: daysAgo(3) },
    { id: 'p-cera', name: 'Cera depilatória', category: 'Barba', unit: 'G', quantity: 900, minimum: 400, updatedAt: daysAgo(9) },
  ]

  const movement = (product: Product, type: MovementType, quantity: number, days: number, note: string | null = null): StockMovement => ({
    id: nextId(), productId: product.id, productName: product.name, type, quantity, date: daysAgo(days), note, user: system, createdAt: new Date(Date.now() - days * 86_400_000).toISOString(),
  })
  const [pomada, oleo, lamina, shampoo, toalha, cera] = products
  const movements: StockMovement[] = [
    movement(pomada, 'ENTRY', 12, 20, 'Compra mensal'), movement(pomada, 'EXIT', 7, 6), movement(pomada, 'EXIT', 5, 1),
    movement(oleo, 'ENTRY', 600, 15), movement(oleo, 'EXIT', 420, 2),
    movement(lamina, 'ENTRY', 100, 25), movement(lamina, 'EXIT', 50, 10), movement(lamina, 'EXIT', 36, 0),
    movement(shampoo, 'ENTRY', 10, 12), movement(shampoo, 'EXIT', 2, 5),
    movement(toalha, 'ENTRY', 300, 18), movement(toalha, 'EXIT', 80, 3),
    movement(cera, 'ENTRY', 900, 9, 'Fornecedor novo'),
  ]

  function findProduct(id: string): Product {
    const product = products.find(item => item.id === id)
    if (!product) throw new Error('Produto não encontrado.')
    return product
  }

  function register(productId: string, type: MovementType, payload: MovementPayload): StockMovement {
    const product = findProduct(productId)
    if (type === 'EXIT' && payload.quantity > product.quantity) throw new Error('Estoque insuficiente para essa baixa.')
    product.quantity = Math.round((product.quantity + (type === 'ENTRY' ? payload.quantity : -payload.quantity)) * 100) / 100
    product.updatedAt = new Date().toISOString()
    const created: StockMovement = {
      id: nextId(), productId, productName: product.name, type, quantity: payload.quantity, date: payload.date,
      note: payload.note ?? null, user: payload.user, createdAt: new Date().toISOString(),
    }
    movements.push(created)
    return created
  }

  return {
    async listProducts() { await wait(); return clone(products) },

    async createProduct(payload) {
      await wait()
      if (products.some(product => product.name.toLowerCase() === payload.name.toLowerCase())) throw new Error('Já existe um produto com esse nome.')
      const product: Product = {
        id: nextId(), name: payload.name, category: payload.category, unit: payload.unit,
        quantity: payload.initialQuantity, minimum: payload.minimum, updatedAt: new Date().toISOString(),
      }
      products.push(product)
      return clone(product)
    },

    async deleteProduct(id) {
      await wait()
      products.splice(products.indexOf(findProduct(id)), 1)
    },

    async registerEntry(productId, payload) { await wait(); return clone(register(productId, 'ENTRY', payload)) },
    async registerExit(productId, payload) { await wait(); return clone(register(productId, 'EXIT', payload)) },

    async listMovements(productId) {
      await wait()
      findProduct(productId)
      return clone(movements.filter(item => item.productId === productId))
    },
  }
}

// Defina VITE_STOCK_API=http no .env para usar o back-end real.
const stockService: StockApi = import.meta.env?.VITE_STOCK_API === 'http' ? httpStockApi : createMockStockApi()

export default stockService
