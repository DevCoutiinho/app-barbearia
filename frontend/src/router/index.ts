import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/auth/Login.vue";
import Register from "../views/auth/Register.vue";
import Home from "../views/Home.vue";
import { useAuthStore } from "../stores/auth";
import { stockAccess } from "../composables/useStockPermissions";
import { isBarber } from "../utils/agenda";

const router = createRouter({
    history: createWebHistory(),

    routes: [
        {
            path: '/dashboard',
            name: 'dashboard',
            component: () => import('../views/Dashboard.vue')
        },
        {
            path: '/users',
            name: 'users',
            component: () => import('../views/UsersManager.vue')
        },
        {
            path: '/services',
            name: 'services',
            component: () => import('../views/ServicesManager.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
                // As operações exigem a role BARBER; a própria tela avisa quem não a tem.
            }
        },
        {
            path: '/estoque',
            name: 'stock',
            component: () => import('../views/StockManager.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
                // Sem permissão de leitura, nem carrega a tela.
                if (!stockAccess(auth.user).canView) return { name: 'dashboard' }
            }
        },
        {
            path: '/agenda',
            name: 'agenda',
            component: () => import('../views/BarberAgenda.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
                // A API só expõe a agenda do próprio barbeiro (/barbers/me), que exige a role BARBER
                // (inclusive para um administrador que também seja barbeiro).
                if (!isBarber(auth.user)) return { name: 'dashboard' }
            }
        },
        {
            path: '/agendamentos',
            name: 'barber-appointments',
            component: () => import('../views/BarberAppointments.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
                if (!isBarber(auth.user)) return { name: 'dashboard' }
            }
        },
        {
            path: '/historico-atendimentos',
            name: 'barber-history',
            component: () => import('../views/BarberAppointmentHistory.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
                if (!isBarber(auth.user)) return { name: 'dashboard' }
            }
        },
        {
            path: '/agendar',
            name: 'booking',
            component: () => import('../views/ClientBooking.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
            }
        },
        {
            path: '/meus-agendamentos',
            name: 'my-appointments',
            component: () => import('../views/MyAppointments.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
            }
        },
        {
            path: '/catalogo',
            name: 'catalog',
            component: () => import('../views/ServiceCatalog.vue'),
            beforeEnter: () => {
                const auth = useAuthStore()
                if (!auth.initialized) auth.restoreSession()
                if (!auth.isAuthenticated) return { name: 'login' }
            }
        },
        {
            path: '/roles',
            name: 'roles',
            component: () => import('../views/Roles.vue')
        },
        {
            path: '/permissoes',
            name: 'permissoes',
            component: () => import('../views/Permissions.vue')
        },
        {
            path: "/",
            name: "home",
            component: Home
        },
        {
            path: '/register',
            name: 'register',
            component: Register
        },
        {
            path: "/login",
            name: "login",
            component: Login
        }
    ],
    scrollBehavior(to) {
        if (to.hash) {
            return { el: to.hash, behavior: 'smooth' }
        }
        return { top: 0 }
    },
});

export default router;
