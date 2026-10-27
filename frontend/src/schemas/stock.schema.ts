import * as z from 'zod'
import { PRODUCT_CATEGORIES, STOCK_UNIT_VALUES } from '../types/stock'
import { toDateInput } from '../utils/stock'

function quantitySchema(label: string, { allowZero, max }: { allowZero: boolean; max?: number }) {
  return z.string({ required_error: `Informe ${label}.` })
    .trim()
    .superRefine((value, context) => {
      let message: string | undefined
      const quantity = Number(value.replace(',', '.'))

      if (!value) message = `Informe ${label}.`
      else if (!/^-?(?:\d+(?:[.,]\d+)?|[.,]\d+)$/.test(value) || !Number.isFinite(quantity)) message = 'Informe um número válido.'
      else if (allowZero ? quantity < 0 : quantity <= 0) message = allowZero ? 'O valor não pode ser negativo.' : 'A quantidade deve ser maior que zero.'
      else if ((value.split(/[.,]/)[1]?.length ?? 0) > 2) message = 'Use no máximo duas casas decimais.'
      else if (quantity > 99_999_999.99) message = 'O valor informado é muito alto.'
      else if (max !== undefined && quantity > max) message = `A quantidade não pode ultrapassar o estoque atual (${max.toLocaleString('pt-BR')}).`

      if (message) context.addIssue({ code: z.ZodIssueCode.custom, message })
    })
    .transform(value => Number(value.replace(',', '.')))
}

export const productSchema = z.object({
  name: z.string({ required_error: 'Informe o nome do produto.' })
    .trim()
    .min(1, 'Informe o nome do produto.')
    .max(100, 'O nome deve ter no máximo 100 caracteres.'),
  category: z.string({ required_error: 'Selecione uma categoria.' })
    .trim()
    .min(1, 'Selecione uma categoria.')
    .pipe(z.enum(PRODUCT_CATEGORIES, { errorMap: () => ({ message: 'Selecione uma categoria válida.' }) })),
  unit: z.string({ required_error: 'Selecione uma unidade.' })
    .trim()
    .min(1, 'Selecione uma unidade.')
    .pipe(z.enum(STOCK_UNIT_VALUES, { errorMap: () => ({ message: 'Selecione uma unidade válida.' }) })),
  initialQuantity: quantitySchema('a quantidade inicial', { allowZero: true }),
  minimum: quantitySchema('o estoque mínimo', { allowZero: true }),
})

/** `maxQuantity` limita a baixa ao estoque atual; entradas não têm limite. */
export function createMovementSchema(maxQuantity?: number, today = toDateInput()) {
  return z.object({
    quantity: quantitySchema('a quantidade', { allowZero: false, max: maxQuantity }),
    date: z.string({ required_error: 'Informe a data da movimentação.' })
      .trim()
      .regex(/^\d{4}-\d{2}-\d{2}$/, 'Informe uma data válida.')
      .refine(value => value <= today, 'A data não pode estar no futuro.'),
    note: z.string().trim().max(255, 'A observação deve ter no máximo 255 caracteres.').optional()
      .transform(value => value || undefined),
  })
}

export type ProductInput = z.input<typeof productSchema>
export type ProductData = z.output<typeof productSchema>
export type MovementInput = z.input<ReturnType<typeof createMovementSchema>>
export type MovementData = z.output<ReturnType<typeof createMovementSchema>>
