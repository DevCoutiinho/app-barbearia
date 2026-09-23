import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import {createPinia} from 'pinia'
import router from './router'
import GoogleSignInPlugin from 'vue3-google-signin'
import { useAuthStore } from './stores/auth'
import api from './services/api'

const app = createApp(App)

app.use(createPinia())
const auth = useAuthStore()
auth.restoreSession()
window.addEventListener('storage', event => {
    if (event.key === 'barbershop.accessToken' || event.key === null) auth.restoreSession()
})
window.addEventListener('focus', () => auth.restoreSession())
api.interceptors.request.use(config => {
    if (auth.accessToken && !config.url?.startsWith('/auth/')) {
        config.headers.Authorization = `Bearer ${auth.accessToken}`
    }
    return config
})
api.interceptors.response.use(response => response, error => {
    if (error.response?.status === 401 && auth.accessToken
        && error.config?.headers?.Authorization === `Bearer ${auth.accessToken}`) auth.logout()
    return Promise.reject(error)
})
app.use(router)
app.use(GoogleSignInPlugin, {
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID
})

app.mount('#app')
