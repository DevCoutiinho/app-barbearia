<script setup lang="ts">
import { computed, ref } from 'vue'

type UserRole = 'Administrador' | 'Barbeiro' | 'Cliente'
type UserStatus = 'Ativo' | 'Inativo'

interface User {
  id: number
  name: string
  email: string
  role: UserRole
  status: UserStatus
  createdAt: string
}

const users = ref<User[]>([
  {
    id: 1,
    name: 'Carlos Silva',
    email: 'carlos@email.com',
    role: 'Barbeiro',
    status: 'Ativo',
    createdAt: '18/09/2026',
  },
  {
    id: 2,
    name: 'João Santos',
    email: 'joao@email.com',
    role: 'Barbeiro',
    status: 'Ativo',
    createdAt: '15/09/2026',
  },
  {
    id: 3,
    name: 'Marcos Vieira',
    email: 'marcos@email.com',
    role: 'Cliente',
    status: 'Ativo',
    createdAt: '12/09/2026',
  },
  {
    id: 4,
    name: 'Ana Oliveira',
    email: 'ana@email.com',
    role: 'Cliente',
    status: 'Ativo',
    createdAt: '08/09/2026',
  },
  {
    id: 5,
    name: 'Rafael Lima',
    email: 'rafael@email.com',
    role: 'Barbeiro',
    status: 'Ativo',
    createdAt: '02/09/2026',
  },
  {
    id: 6,
    name: 'Administrador',
    email: 'admin@barbeariaferro.com.br',
    role: 'Administrador',
    status: 'Ativo',
    createdAt: '01/09/2026',
  },
])

const search = ref('')
const roleFilter = ref('')
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const selectedUser = ref<User | null>(null)

const editForm = ref({
  name: '',
  email: '',
  role: 'Cliente' as UserRole,
  status: 'Ativo' as UserStatus,
})

const filteredUsers = computed(() => {
  const term = search.value.toLowerCase().trim()

  return users.value.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(term) ||
      user.email.toLowerCase().includes(term)

    const matchesRole =
      roleFilter.value === '' || user.role === roleFilter.value

    return matchesSearch && matchesRole
  })
})

function initials(name: string) {
  return name
    .split(' ')
    .slice(0, 2)
    .map((part) => part.charAt(0))
    .join('')
    .toUpperCase()
}

function openEdit(user: User) {
  selectedUser.value = user

  editForm.value = {
    name: user.name,
    email: user.email,
    role: user.role,
    status: user.status,
  }

  showEditModal.value = true
}

function saveUser() {
  if (!selectedUser.value) return

  const index = users.value.findIndex(
    (user) => user.id === selectedUser.value?.id,
  )

  if (index !== -1) {
    users.value[index] = {
      ...users.value[index],
      ...editForm.value,
    }
  }

  showEditModal.value = false
}

function openDelete(user: User) {
  selectedUser.value = user
  showDeleteModal.value = true
}

function deleteUser() {
  if (!selectedUser.value) return

  users.value = users.value.filter(
    (user) => user.id !== selectedUser.value?.id,
  )

  showDeleteModal.value = false
  selectedUser.value = null
}
</script>

<template>
  <div class="users-page">
    <!-- Cabeçalho -->
    <header class="admin-header">
      <div class="header-container">
        <div class="brand">
          <div class="brand-icon">✂</div>
          <span>Barbearia Ferro</span>
        </div>

        <div class="admin-profile">
          <div>
            <strong>Administrador</strong>
            <span>Painel administrativo</span>
          </div>

          <div class="avatar">AD</div>
        </div>
      </div>
    </header>

    <main class="container">
      <!-- Breadcrumb -->
      <div class="breadcrumb">
        Administração / <strong>Usuários</strong>
      </div>

      <!-- Título -->
      <section class="page-heading">
        <div>
          <h1>Gerenciador de usuários</h1>

          <p>
            Visualize, edite e gerencie os usuários cadastrados no sistema.
          </p>
        </div>

        <div class="users-count">
          <strong>{{ users.length }}</strong>
          <span>usuários cadastrados</span>
        </div>
      </section>

      <!-- Filtros -->
      <section class="filters">
        <div class="search-box">
          <span>⌕</span>

          <input
            v-model="search"
            type="text"
            placeholder="Buscar por nome ou e-mail"
          />
        </div>

        <select v-model="roleFilter">
          <option value="">Todos os perfis</option>
          <option value="Administrador">Administrador</option>
          <option value="Barbeiro">Barbeiro</option>
          <option value="Cliente">Cliente</option>
        </select>
      </section>

      <!-- Tabela -->
      <section class="table-card">
        <table>
          <thead>
            <tr>
              <th>Usuário</th>
              <th>Perfil</th>
              <th>Status</th>
              <th>Cadastro</th>
              <th class="actions-title">Ações</th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="user in filteredUsers"
              :key="user.id"
            >
              <td>
                <div class="user-info">
                  <div class="user-avatar">
                    {{ initials(user.name) }}
                  </div>

                  <div>
                    <strong>{{ user.name }}</strong>
                    <span>{{ user.email }}</span>
                  </div>
                </div>
              </td>

              <td>
                <span class="role-badge">
                  {{ user.role }}
                </span>
              </td>

              <td>
                <span
                  class="status"
                  :class="{ inactive: user.status === 'Inativo' }"
                >
                  <span class="status-dot"></span>
                  {{ user.status }}
                </span>
              </td>

              <td class="date">
                {{ user.createdAt }}
              </td>

              <td>
                <div class="actions">
                  <button
                    class="edit-button"
                    @click="openEdit(user)"
                  >
                    Editar
                  </button>

                  <button
                    class="delete-button"
                    @click="openDelete(user)"
                  >
                    Excluir
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div
          v-if="filteredUsers.length === 0"
          class="empty-state"
        >
          Nenhum usuário encontrado.
        </div>
      </section>
    </main>

    <!-- MODAL EDITAR -->
    <div
      v-if="showEditModal"
      class="modal-overlay"
      @click.self="showEditModal = false"
    >
      <div class="modal">
        <div class="modal-header">
          <div>
            <h2>Editar usuário</h2>
            <p>Atualize as informações do usuário.</p>
          </div>

          <button
            class="close-button"
            @click="showEditModal = false"
          >
            ×
          </button>
        </div>

        <form @submit.prevent="saveUser">
          <div class="form-group">
            <label>Nome</label>

            <input
              v-model="editForm.name"
              required
              type="text"
            />
          </div>

          <div class="form-group">
            <label>E-mail</label>

            <input
              v-model="editForm.email"
              required
              type="email"
            />
          </div>

          <div class="form-grid">
            <div class="form-group">
              <label>Perfil</label>

              <select v-model="editForm.role">
                <option value="Administrador">Administrador</option>
                <option value="Barbeiro">Barbeiro</option>
                <option value="Cliente">Cliente</option>
              </select>
            </div>

            <div class="form-group">
              <label>Status</label>

              <select v-model="editForm.status">
                <option value="Ativo">Ativo</option>
                <option value="Inativo">Inativo</option>
              </select>
            </div>
          </div>

          <div class="modal-actions">
            <button
              type="button"
              class="cancel-button"
              @click="showEditModal = false"
            >
              Cancelar
            </button>

            <button
              type="submit"
              class="save-button"
            >
              Salvar alterações
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- MODAL EXCLUIR -->
    <div
      v-if="showDeleteModal"
      class="modal-overlay"
      @click.self="showDeleteModal = false"
    >
      <div class="modal delete-modal">
        <div class="delete-icon">!</div>

        <h2>Excluir usuário?</h2>

        <p>
          Você está prestes a excluir
          <strong>{{ selectedUser?.name }}</strong>.
        </p>

        <p class="warning">
          Esta ação não poderá ser desfeita.
        </p>

        <div class="modal-actions">
          <button
            class="cancel-button"
            @click="showDeleteModal = false"
          >
            Cancelar
          </button>

          <button
            class="confirm-delete"
            @click="deleteUser"
          >
            Excluir usuário
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.users-page {
  min-height: 100vh;
  background: #f8f8f7;
  color: #171717;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e5;
}

.header-container,
.container {
  max-width: 1100px;
  margin: 0 auto;
  padding-left: 24px;
  padding-right: 24px;
}

.header-container {
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 17px;
}

.brand-icon,
.avatar,
.user-avatar {
  background: #181818;
  color: #e1b94f;
}

.brand-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 8px;
}

.admin-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: right;
}

.admin-profile div:first-child {
  display: flex;
  flex-direction: column;
}

.admin-profile strong {
  font-size: 13px;
}

.admin-profile span {
  color: #777;
  font-size: 12px;
}

.avatar {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
}

.container {
  padding-top: 38px;
  padding-bottom: 60px;
}

.breadcrumb {
  color: #777;
  font-size: 13px;
  margin-bottom: 22px;
}

.breadcrumb strong {
  color: #181818;
}

.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 30px;
}

.page-heading h1 {
  margin: 0;
  font-size: 32px;
  letter-spacing: -1px;
}

.page-heading p {
  margin-top: 8px;
  color: #6d6d6d;
}

.users-count {
  background: #fff;
  border: 1px solid #e3e3df;
  border-radius: 12px;
  padding: 12px 18px;
  display: flex;
  gap: 6px;
  align-items: center;
  white-space: nowrap;
}

.users-count strong {
  color: #c69b2b;
  font-size: 18px;
}

.users-count span {
  color: #666;
  font-size: 13px;
}

.filters {
  margin-top: 32px;
  display: flex;
  gap: 12px;
}

.search-box {
  flex: 1;
  position: relative;
}

.search-box span {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #777;
}

.search-box input,
.filters select,
.form-group input,
.form-group select {
  border: 1px solid #deded9;
  background: #fff;
  border-radius: 10px;
  outline: none;
  font-size: 14px;
}

.search-box input {
  width: 100%;
  height: 46px;
  padding: 0 16px 0 43px;
  box-sizing: border-box;
}

.filters select {
  min-width: 190px;
  padding: 0 14px;
}

.search-box input:focus,
.filters select:focus,
.form-group input:focus,
.form-group select:focus {
  border-color: #d3ad49;
  box-shadow: 0 0 0 3px rgba(211, 173, 73, 0.12);
}

.table-card {
  margin-top: 20px;
  background: #fff;
  border: 1px solid #e2e2df;
  border-radius: 14px;
  overflow: hidden;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background: #f4f4f2;
  color: #777;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  padding: 14px 20px;
}

td {
  padding: 17px 20px;
  border-top: 1px solid #eeeeeb;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
}

.user-info div:last-child {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-info span,
.date {
  color: #777;
  font-size: 13px;
}

.role-badge {
  background: #f4f1e8;
  color: #82651d;
  border-radius: 20px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 600;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
}

.status-dot {
  width: 7px;
  height: 7px;
  background: #3e9c5c;
  border-radius: 50%;
}

.status.inactive .status-dot {
  background: #999;
}

.actions-title {
  text-align: right;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.actions button {
  height: 36px;
  padding: 0 13px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}

.edit-button {
  border: 1px solid #ddd;
  background: #fff;
}

.edit-button:hover {
  border-color: #c9a23e;
}

.delete-button {
  border: 1px solid #ead5d5;
  background: #fff;
  color: #a83d3d;
}

.delete-button:hover {
  background: #fff5f5;
}

.empty-state {
  padding: 50px;
  text-align: center;
  color: #777;
}

/* Modal */

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.modal {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border-radius: 16px;
  padding: 26px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.modal h2 {
  margin: 0;
  font-size: 21px;
}

.modal-header p,
.delete-modal p {
  color: #777;
  font-size: 14px;
}

.close-button {
  border: 0;
  background: transparent;
  font-size: 26px;
  cursor: pointer;
}

.form-group {
  margin-top: 18px;
}

.form-group label {
  display: block;
  margin-bottom: 7px;
  font-size: 13px;
  font-weight: 600;
}

.form-group input,
.form-group select {
  width: 100%;
  height: 44px;
  padding: 0 12px;
  box-sizing: border-box;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.modal-actions {
  margin-top: 26px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.modal-actions button {
  min-height: 42px;
  border-radius: 9px;
  padding: 0 18px;
  font-weight: 600;
  cursor: pointer;
}

.cancel-button {
  background: #fff;
  border: 1px solid #ddd;
}

.save-button {
  background: #d5ad45;
  color: #171717;
  border: 1px solid #d5ad45;
}

.delete-modal {
  text-align: center;
  max-width: 430px;
}

.delete-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 18px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fff1f1;
  color: #b54141;
  font-size: 22px;
  font-weight: 700;
}

.delete-modal .modal-actions {
  justify-content: center;
}

.warning {
  font-size: 12px !important;
}

.confirm-delete {
  background: #a83d3d;
  border: 1px solid #a83d3d;
  color: #fff;
}

@media (max-width: 760px) {
  .admin-profile div:first-child {
    display: none;
  }

  .page-heading {
    flex-direction: column;
  }

  .filters {
    flex-direction: column;
  }

  .filters select {
    height: 46px;
  }

  .table-card {
    overflow-x: auto;
  }

  table {
    min-width: 800px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>