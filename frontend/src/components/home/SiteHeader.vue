<script setup lang="ts">
import { computed, ref } from 'vue'
import { useWindowScroll } from '@vueuse/core'
import { Menu, X } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import { navLinks } from '../../data/home'

const { y } = useWindowScroll()
const scrolled = computed(() => y.value > 40)
const menuOpen = ref(false)

function closeMenu() {
  menuOpen.value = false
}
</script>

<template>
  <header
    class="fixed inset-x-0 top-0 z-[100] border-b pt-[env(safe-area-inset-top,0px)] transition-[background,border-color] duration-[350ms]"
    :class="
      scrolled
        ? 'border-line bg-paper/92 backdrop-blur-[10px]'
        : 'border-transparent bg-transparent'
    "
  >
    <div class="mx-auto flex h-[78px] w-full max-w-[1180px] items-center justify-between px-7">
      <a href="#home" class="shrink-0" @click="closeMenu">
        <BrandLogo :dark="!scrolled" />
      </a>

      <nav class="hidden items-center gap-[30px] min-[861px]:flex">
        <a
          v-for="link in navLinks"
          :key="link.href"
          :href="link.href"
          class="relative py-1 text-[0.92rem] font-medium transition-colors duration-[350ms] hover:text-gold-light"
          :class="scrolled ? 'text-text-soft hover:text-gold' : 'text-on-dark-soft'"
        >
          {{ link.label }}
        </a>
      </nav>

      <div class="flex items-center gap-3">
        <a
          href="#contato"
          class="hidden rounded-lg border px-[18px] py-[9px] text-[0.87rem] font-semibold transition-colors duration-[350ms] min-[861px]:inline-flex"
          :class="
            scrolled
              ? 'border-line text-ink hover:border-ink'
              : 'border-on-dark/28 text-on-dark hover:border-on-dark/60'
          "
        >
          Entrar
        </a>
        <a
          href="#contato"
          class="hidden rounded-lg px-5 py-[9px] text-[0.87rem] font-semibold transition-colors duration-[350ms] min-[861px]:inline-flex"
          :class="
            scrolled
              ? 'bg-ink text-white hover:bg-ink-soft'
              : 'bg-on-dark text-ink hover:bg-[#e5e3df]'
          "
        >
          Criar conta
        </a>
        <button
          type="button"
          class="flex size-10 items-center justify-center rounded-lg min-[861px]:hidden"
          :class="scrolled ? 'text-ink' : 'text-on-dark'"
          :aria-label="menuOpen ? 'Fechar menu' : 'Abrir menu'"
          @click="menuOpen = !menuOpen"
        >
          <X v-if="menuOpen" class="size-6" :stroke-width="2" />
          <Menu v-else class="size-6" :stroke-width="2" />
        </button>
      </div>
    </div>

    <div
      v-if="menuOpen"
      class="fixed inset-x-0 bottom-0 top-[78px] z-[99] flex flex-col gap-1 bg-ink px-7 pt-2.5 pb-7 min-[861px]:hidden"
    >
      <a
        v-for="link in navLinks"
        :key="link.href"
        :href="link.href"
        class="border-b border-on-dark/10 py-4 text-[1.1rem] font-semibold text-on-dark"
        @click="closeMenu"
      >
        {{ link.label }}
      </a>
      <a
        href="#contato"
        class="mt-[18px] inline-flex w-full items-center justify-center rounded-[10px] border border-on-dark/28 px-[26px] py-[13px] font-semibold text-on-dark"
        @click="closeMenu"
      >
        Entrar
      </a>
      <a
        href="#contato"
        class="inline-flex w-full items-center justify-center rounded-[10px] bg-on-dark px-[26px] py-[13px] font-semibold text-ink"
        @click="closeMenu"
      >
        Criar conta
      </a>
    </div>
  </header>
</template>
