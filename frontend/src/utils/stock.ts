import type { MovementType, Product, StockMovement, StockStatus, StockUnit } from '../types/stock'

const STATUS_PRIORITY: Record<StockStatus, number> = { OUT: 0, LOW: 1, OK: 2 }
const DAY_MS = 24 * 60 * 60 * 1000

export const STATUS_LABELS: Record<StockStatus, string> = { OUT: 'Esgotado', LOW: 'Baixo', OK: 'Em dia' }
export const MOVEMENT_LABELS: Record<MovementType, string> = { ENTRY: 'Entrada', EXIT: 'Baixa' }

export function stockStatus(product: Pick<Product, 'quantity' | 'minimum'>): StockStatus {
  if (product.quantity <= 0) return 'OUT'
  if (product.quantity < product.minimum) return 'LOW'
  return 'OK'
}

/** Itens críticos primeiro (Esgotado > Baixo > Em dia), depois por nome. */
export function sortByCriticality(products: readonly Product[]): Product[] {
  return [...products].sort((a, b) =>
    STATUS_PRIORITY[stockStatus(a)] - STATUS_PRIORITY[stockStatus(b)] || a.name.localeCompare(b.name, 'pt-BR'))
}

/** Percentual da barra: o dobro do mínimo é considerado estoque cheio. */
export function stockLevelPercent(product: Pick<Product, 'quantity' | 'minimum'>): number {
  if (product.quantity <= 0) return 0
  if (product.minimum <= 0) return 100
  return Math.min(100, Math.round(product.quantity / (product.minimum * 2) * 100))
}

/** Movimentações em ordem cronológica (mais antigas primeiro). */
export function sortChronologically(movements: readonly StockMovement[]): StockMovement[] {
  return [...movements].sort((a, b) => a.date.localeCompare(b.date) || a.createdAt.localeCompare(b.createdAt))
}

export function countRecentExits(movements: readonly StockMovement[], days = 7, now = new Date()): number {
  const start = toDateInput(new Date(now.getTime() - (days - 1) * DAY_MS))
  const end = toDateInput(now)
  return movements.filter(movement => movement.type === 'EXIT' && movement.date >= start && movement.date <= end).length
}

/** Data local no formato aceito por <input type="date">. */
export function toDateInput(date = new Date()): string {
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 10)
}

const quantityFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })

// "L" maiúsculo para não confundir litro com o número 1.
const UNIT_SYMBOLS: Record<StockUnit, string> = { UN: 'un', G: 'g', ML: 'ml', L: 'L' }

export function unitSymbol(unit: StockUnit): string {
  return UNIT_SYMBOLS[unit] ?? unit
}

export function formatQuantity(value: number, unit?: StockUnit): string {
  return unit ? `${quantityFormat.format(value)} ${unitSymbol(unit)}` : quantityFormat.format(value)
}

export function formatDate(value: string): string {
  // Datas puras (YYYY-MM-DD) são locais; evita o deslocamento de fuso do construtor.
  const date = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value)
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}
