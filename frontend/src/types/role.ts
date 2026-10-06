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

export interface RoleCreate {
  name: string
  description: string
  permissionIds: string[]
}

export interface RoleUpdate {
  description: string
  permissionIds: string[]
}