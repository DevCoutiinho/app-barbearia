<script setup lang="ts">
import SiteHeader from '../components/home/SiteHeader.vue'
import HeroSection from '../components/home/HeroSection.vue'
import AboutSection from '../components/home/AboutSection.vue'
import ServicesSection from '../components/home/ServicesSection.vue'
import TestimonialsSection from '../components/home/TestimonialsSection.vue'
import FaqSection from '../components/home/FaqSection.vue'
import SiteFooter from '../components/home/SiteFooter.vue'
</script>

<template>
  <div class="max-w-[100vw] overflow-x-hidden">
    <SiteHeader />
    <main>
      <HeroSection />
      <AboutSection />
      <ServicesSection />
      <TestimonialsSection />
      <FaqSection />
    </main>
    <SiteFooter />
  </div>
</template>
