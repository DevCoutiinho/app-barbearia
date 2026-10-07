export interface Permission {
  id: string
  name: string
  description: string | null
}

export interface Role {
  id: string
  name: string
  description: string | null
  permissions: Permission[]
}
