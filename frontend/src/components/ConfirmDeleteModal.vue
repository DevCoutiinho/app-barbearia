<script setup lang="ts">
import { AlertTriangle, X } from '@lucide/vue'

defineProps<{
  open: boolean
  title: string
  description: string
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'confirm'): void
}>()
</script>

<template>
  <div
    v-if="open"
    class="fixed inset-0 z-[60] grid place-items-center overflow-y-auto bg-black/40 p-4"
    @click.self="emit('close')"
  >
    <section
      class="my-auto w-full max-w-[400px] rounded-2xl border border-line bg-white p-5 shadow-2xl sm:p-6"
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-modal-title"
    >
      <header class="mb-4 flex items-start justify-between gap-4">
        <div class="flex items-center gap-3">
          <span class="flex size-10 shrink-0 items-center justify-center rounded-full bg-red-100 text-red-600">
            <AlertTriangle class="size-5" aria-hidden="true" />
          </span>
          <h2 id="delete-modal-title" class="text-[17px] font-bold text-ink">
            {{ title }}
          </h2>
        </div>
        <button
          type="button"
          class="inline-flex size-[30px] items-center justify-center rounded-lg border border-line text-[#55555e] hover:bg-paper disabled:opacity-50"
          :disabled="loading"
          @click="emit('close')"
        >
          <X class="size-4" aria-hidden="true" />
        </button>
      </header>

      <p class="mb-6 text-[14px] leading-relaxed text-[#74747f]">
        {{ description }}
      </p>

      <div class="flex justify-end gap-2">
        <button
          type="button"
          class="inline-flex min-h-[38px] items-center justify-center rounded-xl border border-line px-4 text-[13px] font-semibold hover:bg-paper disabled:opacity-60"
          :disabled="loading"
          @click="emit('close')"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="inline-flex min-h-[38px] items-center justify-center rounded-lg bg-red-600 px-4 text-[13px] font-semibold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-60"
          :disabled="loading"
          @click="emit('confirm')"
        >
          {{ loading ? 'Excluindo...' : 'Sim, excluir' }}
        </button>
      </div>
    </section>
  </div>
</template>
