<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { ArrowLeft, KeyRound } from '@lucide/vue'
</script>

<template>
  <main class="min-h-screen bg-paper text-ink">
    <div class="mx-auto max-w-[1200px] px-4 py-7 sm:px-[26px] lg:py-9">
      <RouterLink to="/dashboard" class="mb-5 inline-flex items-center gap-2 text-xs font-medium text-[#74747f] hover:text-ink">
        <ArrowLeft class="size-4" aria-hidden="true" />
        Voltar ao dashboard
      </RouterLink>

      <header class="mb-7">
        <p class="mb-2 text-[11px] font-semibold tracking-[1.5px] text-[#a67039]">ADMINISTRAÇÃO</p>
        <h1 class="text-[27px] leading-9 font-bold tracking-[-1px]">Permissões</h1>
        <p class="mt-1 max-w-[610px] text-[14px] leading-[21px] text-[#74747f]">
          Consulte a disponibilidade da gestão de permissões do sistema.
        </p>
      </header>

      <section class="rounded-2xl border border-line bg-white p-5 sm:p-7" aria-labelledby="permissions-unavailable-title">
        <span class="flex size-11 items-center justify-center rounded-xl bg-[#f5ede3] text-[#a67039]">
          <KeyRound class="size-5" :stroke-width="1.7" aria-hidden="true" />
        </span>
        <h2 id="permissions-unavailable-title" class="mt-4 text-base font-semibold">Gestão de permissões indisponível</h2>
        <p class="mt-2 max-w-2xl text-[13px] leading-5 text-[#74747f]">
          O backend ainda não disponibiliza endpoints para listar ou gerenciar permissões, nem para consultar ou alterar
          as permissões associadas a cada Role. Nenhuma operação ou dado fictício é exibido nesta tela.
        </p>
        <RouterLink
          to="/roles"
          class="mt-5 inline-flex min-h-[38px] items-center rounded-lg border border-line bg-white px-4 text-[13px] font-semibold hover:bg-paper"
        >
          Gerenciar Roles
        </RouterLink>
      </section>
    </div>
  </main>
</template>
