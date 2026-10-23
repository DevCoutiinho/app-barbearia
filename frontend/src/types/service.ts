export const SERVICE_TECHNIQUE_VALUES = [
  'MACHINE',
  'SCISSORS',
  'RAZOR',
  'MIXED',
  'TWEEZERS',
  'WAX',
] as const

export type ServiceTechnique = typeof SERVICE_TECHNIQUE_VALUES[number]

export const SERVICE_TECHNIQUES: ReadonlyArray<{ value: ServiceTechnique; label: string }> = [
  { value: 'MACHINE', label: 'Máquina' },
  { value: 'SCISSORS', label: 'Tesoura' },
  { value: 'RAZOR', label: 'Navalha' },
  { value: 'MIXED', label: 'Mista' },
  { value: 'TWEEZERS', label: 'Pinça' },
  { value: 'WAX', label: 'Cera' },
]

export interface ServiceCategory {
  id: string
  name: string
  description: string | null
  active: boolean
}

export interface BarberServiceImage {
  id: string
  url: string
}

export interface BarberService {
  id: string
  name: string
  technique: ServiceTechnique
  price: number
  active: boolean
  // A unidade de durationTime ainda precisa ser confirmada com o backend.
  durationTime: number
  category: Pick<ServiceCategory, 'id' | 'name'>
  images: BarberServiceImage[]
}

export interface ServiceFilters {
  name?: string
  technique?: ServiceTechnique
  active?: boolean
  minPrice?: number
  maxPrice?: number
  categoryId?: string
}

export interface ServiceListParams {
  page?: number
  size?: number
  sort?: string | string[]
  filters?: ServiceFilters
}

export interface CreateServicePayload {
  name: string
  technique: ServiceTechnique
  price: number
  active: boolean
  durationTime: number
  categoryId: string
}

export type UpdateServicePayload = Omit<CreateServicePayload, 'categoryId'> & {
  categoryId?: string
}
