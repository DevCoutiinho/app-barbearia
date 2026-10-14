import type { Role } from './role'

// Resumo da role atribuída a um usuário.
export type UserRole = Pick<Role, 'id' | 'name'> & {
  name: 'USER' | 'ADMIN' | 'BARBER'
}

// Dados retornados pela listagem administrativa de usuários.
export interface User {
  id: string
  name: string
  email: string
  avatar: string | null
  active: boolean
  roles: UserRole[]
}

// Metadados retornados dentro do campo page.
export interface PageMetadata {
  size: number
  number: number
  totalElements: number
  totalPages: number
}

// Estrutura da página serializada pelo Spring Data em modo VIA_DTO.
export interface PaginatedResponse<T> {
  content: T[]
  page: PageMetadata
}

// Filtros opcionais aceitos pelo backend.
export interface UserFilters {
  name?: string
  email?: string
  active?: boolean
}

// Payload utilizado para substituir as roles do usuário.
export interface UpdateUserRolesPayload {
  roleIds: string[]
}
