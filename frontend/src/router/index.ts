import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/auth/Login.vue";
import Register from "../views/auth/Register.vue";
import Home from "../views/Home.vue";
import { useAuthStore } from "../stores/auth";
import { stockAccess } from "../composables/useStockPermissions";

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
            component: () => import('../views/ServicesManager.vue')
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
