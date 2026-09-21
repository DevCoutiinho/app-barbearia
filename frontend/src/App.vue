<script setup lang="ts">
import { RouterView } from "vue-router";
import { Toaster } from 'vue-sonner';
import 'vue-sonner/style.css';
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
