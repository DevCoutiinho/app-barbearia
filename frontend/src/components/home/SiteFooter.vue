<script setup lang="ts">
import { Clock, Mail, MapPin, MessageCircle, Phone } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import { contact } from '../../data/home'
</script>

<template>
  <footer id="contato" class="bg-paper pt-[72px] pb-[30px]">
    <div class="mx-auto w-full max-w-[1180px] px-7">
      <div
        class="mb-16 flex flex-wrap items-center justify-between gap-6 rounded-lg border border-line bg-white px-9 py-[34px] max-md:flex-col max-md:items-start"
      >
        <div>
          <h3 class="font-display text-[1.3rem] font-extrabold tracking-[-0.02em] text-ink">
            Quer conhecer a Barbearia Ferro?
          </h3>
          <p class="mt-1.5 text-[0.92rem] text-text-soft">
            Fale com a gente pelo telefone ou WhatsApp e tire suas dúvidas.
          </p>
        </div>
        <a
          :href="contact.phoneHref"
          class="inline-flex items-center justify-center rounded-[10px] bg-ink px-[26px] py-[13px] text-[0.95rem] font-semibold text-white transition hover:bg-ink-soft active:translate-y-px"
        >
          Entrar em contato
        </a>
      </div>

      <div class="grid grid-cols-1 gap-10 md:grid-cols-[1.3fr_1fr_1fr]">
        <div>
          <BrandLogo />
          <p class="mt-4 max-w-[320px] text-[0.9rem] leading-[1.6] text-text-soft">
            Tradição, técnica e cuidado em cada detalhe. Conheça nossos serviços e profissionais.
          </p>
          <div class="mt-5 flex gap-2.5">
            <a
              href="#"
              aria-label="Instagram"
              class="flex size-9 items-center justify-center rounded-[9px] border border-line bg-white text-ink transition hover:bg-ink hover:text-gold-light"
            >
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="5" />
                <circle cx="12" cy="12" r="4" />
                <circle cx="17.5" cy="6.5" r="1" />
              </svg>
            </a>
            <a
              href="#"
              aria-label="Facebook"
              class="flex size-9 items-center justify-center rounded-[9px] border border-line bg-white text-ink transition hover:bg-ink hover:text-gold-light"
            >
              <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z" />
              </svg>
            </a>
            <a
              href="#"
              aria-label="WhatsApp"
              class="flex size-9 items-center justify-center rounded-[9px] border border-line bg-white text-ink transition hover:bg-ink hover:text-gold-light"
            >
              <MessageCircle class="size-4" :stroke-width="2" />
            </a>
          </div>
        </div>

        <div>
          <h4 class="mb-4 text-[0.82rem] font-bold text-ink">Contato</h4>
          <ul class="flex flex-col gap-[13px]">
            <li class="flex items-start gap-2.5 text-[0.87rem] leading-[1.4] text-text-soft">
              <MapPin class="mt-0.5 size-[15px] shrink-0 text-gold" :stroke-width="2" />
              {{ contact.address }}
            </li>
            <li class="flex items-start gap-2.5 text-[0.87rem] leading-[1.4] text-text-soft">
              <Phone class="mt-0.5 size-[15px] shrink-0 text-gold" :stroke-width="2" />
              {{ contact.phoneDisplay }}
            </li>
            <li class="flex items-start gap-2.5 text-[0.87rem] leading-[1.4] text-text-soft">
              <Mail class="mt-0.5 size-[15px] shrink-0 text-gold" :stroke-width="2" />
              {{ contact.email }}
            </li>
          </ul>
        </div>

        <div>
          <h4 class="mb-4 text-[0.82rem] font-bold text-ink">Horário de funcionamento</h4>
          <ul class="flex flex-col gap-[13px]">
            <li
              v-for="line in contact.hours"
              :key="line"
              class="flex items-start gap-2.5 text-[0.87rem] leading-[1.4] text-text-soft"
            >
              <Clock class="mt-0.5 size-[15px] shrink-0 text-gold" :stroke-width="2" />
              {{ line }}
            </li>
          </ul>
        </div>
      </div>

      <div class="mt-14 border-t border-line pt-[22px] text-[0.8rem] text-text-soft">
        © 2026 Barbearia Ferro. Todos os direitos reservados.
      </div>
    </div>
  </footer>
</template>
