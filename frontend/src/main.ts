import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import {createPinia} from 'pinia'
import router from './router'
import GoogleSignInPlugin from 'vue3-google-signin'

const app = createApp(App)

app.use(router)
app.use(createPinia())
app.use(GoogleSignInPlugin, {
    clientId: import.meta.env.VITE_GOOGLE_CLIENT_ID
})

app.mount('#app')
