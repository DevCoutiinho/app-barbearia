export interface Role {
  id: string
  name: string
  description: string | null
  permissionsCount: number
  usersCount: number
}
