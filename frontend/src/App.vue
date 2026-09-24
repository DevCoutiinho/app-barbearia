<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { RouterView } from "vue-router";
import { Toaster } from 'vue-sonner';
import 'vue-sonner/style.css';
import { useAuthStore } from './stores/auth';
import { setupAuthInterceptors } from './services/api';

const auth = useAuthStore();
auth.restoreSession();
const removeAuthInterceptors = setupAuthInterceptors(auth);

function handleStorage(event: StorageEvent) {
    if (event.key === 'barbershop.accessToken' || event.key === null) auth.restoreSession();
}

function handleFocus() {
    auth.restoreSession();
}

onMounted(() => {
    window.addEventListener('storage', handleStorage);
    window.addEventListener('focus', handleFocus);
});

onUnmounted(() => {
    window.removeEventListener('storage', handleStorage);
    window.removeEventListener('focus', handleFocus);
    removeAuthInterceptors();
});
</script>

<template>
    <Toaster 
        position="top-right" 
        :duration="4000" 
        rich-colors 
        close-button
    />
    <RouterView />
</template>
