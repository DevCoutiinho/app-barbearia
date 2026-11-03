import * as z from 'zod'
import { STOCK_UNIT_VALUES, type MovementType } from '../types/stock'

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

const baseProductFields = {
  name: z.string({ required_error: 'Informe o nome do produto.' })
    .trim()
    .min(1, 'Informe o nome do produto.')
    .max(100, 'O nome deve ter no máximo 100 caracteres.'),
  description: z.string()
    .trim()
    .max(255, 'A descrição deve ter no máximo 255 caracteres.')
    .optional()
    .transform(value => value ?? ''),
  unitMeasurement: z.string({ required_error: 'Selecione uma unidade.' })
    .trim()
    .min(1, 'Selecione uma unidade.')
    .pipe(z.enum(STOCK_UNIT_VALUES, { errorMap: () => ({ message: 'Selecione uma unidade válida.' }) })),
  minimumQuantity: quantitySchema('o estoque mínimo', { allowZero: true }),
  priceCost: quantitySchema('o preço de custo', { allowZero: true }),
}

export const createProductSchema = z.object({
  ...baseProductFields,
  initialQuantity: quantitySchema('a quantidade inicial', { allowZero: true }),
  active: z.boolean({ required_error: 'Informe o status do produto.' }),
})

export const updateProductSchema = z.object({
  ...baseProductFields,
})

export const productSchema = createProductSchema

/** `maxQuantity` limita o consumo ao estoque atual; em consumo `userId` é obrigatório. */
export function createMovementSchema(type: MovementType = 'ENTRY', maxQuantity?: number) {
  if (type === 'CONSUMPTION') {
    return z.object({
      userId: z.string({ required_error: 'Selecione um usuário.' })
        .trim()
        .min(1, 'Selecione um usuário.')
        .uuid('Selecione um usuário válido.'),
      quantity: quantitySchema('a quantidade', { allowZero: false, max: maxQuantity }),
    })
  }

  return z.object({
    userId: z.string().optional(),
    quantity: quantitySchema('a quantidade', { allowZero: false }),
  })
}

export type CreateProductInput = z.input<typeof createProductSchema>
export type CreateProductData = z.output<typeof createProductSchema>
export type UpdateProductInput = z.input<typeof updateProductSchema>
export type UpdateProductData = z.output<typeof updateProductSchema>
export type ProductInput = CreateProductInput
export type ProductData = CreateProductData
export type MovementInput = { userId?: string; quantity: string }
export type MovementData = { userId?: string; quantity: number }
