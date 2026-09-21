import { createRouter, createWebHistory } from "vue-router";
import Login from "../views/auth/Login.vue";
import Register from "../views/auth/Register.vue";
import Home from "../views/Home.vue";

const router = createRouter({
    history: createWebHistory(),

    routes: [
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
    ]
});

export default router;