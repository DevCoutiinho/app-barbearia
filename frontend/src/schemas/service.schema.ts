import * as z from 'zod'
import { SERVICE_TECHNIQUE_VALUES } from '../types/service'

const priceSchema = z.string({ required_error: 'Informe o preço do serviço.' })
  .trim()
  .superRefine((value, context) => {
    let message: string | undefined
    const price = Number(value.replace(',', '.'))

    if (!value) message = 'Informe o preço do serviço.'
    else if (!/^-?(?:\d+(?:[.,]\d+)?|[.,]\d+)$/.test(value) || !Number.isFinite(price)) {
      message = 'Informe um preço válido.'
    } else if (price <= 0) message = 'O preço deve ser maior que zero.'
    else if (price > 9999.99) message = 'O preço máximo permitido é R$ 9.999,99.'
    else if ((value.split(/[.,]/)[1]?.length ?? 0) > 2) message = 'Use no máximo duas casas decimais no preço.'

    if (message) context.addIssue({ code: z.ZodIssueCode.custom, message })
  })
  .transform(value => Number(value.replace(',', '.')))

const durationSchema = z.string({ required_error: 'Informe a duração do serviço.' })
  .trim()
  .superRefine((value, context) => {
    let message: string | undefined
    const duration = Number(value)

    if (!value) message = 'Informe a duração do serviço.'
    else if (!/^-?\d+$/.test(value) || !Number.isInteger(duration)) message = 'A duração deve ser um número inteiro.'
    else if (duration <= 0) message = 'A duração deve ser maior que zero.'
    else if (duration > 2147483647) message = 'A duração informada é muito alta.'

    if (message) context.addIssue({ code: z.ZodIssueCode.custom, message })
  })
  .transform(value => Number(value))

const categorySchema = z.string({ required_error: 'Selecione uma categoria.' })
  .trim()
  .min(1, 'Selecione uma categoria.')
  .uuid('Selecione uma categoria válida.')

const serviceFields = {
  name: z.string({ required_error: 'Informe o nome do serviço.' })
    .trim()
    .min(1, 'Informe o nome do serviço.')
    .max(255, 'O nome deve ter no máximo 255 caracteres.'),
  technique: z.string({ required_error: 'Selecione uma técnica.' })
    .trim()
    .min(1, 'Selecione uma técnica.')
    .pipe(z.enum(SERVICE_TECHNIQUE_VALUES, { errorMap: () => ({ message: 'Selecione uma técnica válida.' }) })),
  price: priceSchema,
  active: z.boolean({ required_error: 'Informe o status do serviço.' }),
  durationTime: durationSchema,
}

export const createServiceSchema = z.object({
  ...serviceFields,
  categoryId: categorySchema,
})

export const updateServiceSchema = z.object({
  ...serviceFields,
  categoryId: z.string().optional()
    .transform(value => value?.trim() || undefined)
    .pipe(z.string().uuid('Selecione uma categoria válida.').optional()),
})

export type CreateServiceInput = z.input<typeof createServiceSchema>
export type UpdateServiceInput = z.input<typeof updateServiceSchema>
export type CreateServiceData = z.output<typeof createServiceSchema>
export type UpdateServiceData = z.output<typeof updateServiceSchema>
