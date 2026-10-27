<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
import { X } from '@lucide/vue'

const props = withDefaults(defineProps<{
  open: boolean
  title: string
  eyebrow?: string
  busy?: boolean
  drawer?: boolean
  widthClass?: string
}>(), { eyebrow: 'ESTOQUE', widthClass: 'max-w-[520px]' })

const emit = defineEmits<{ (e: 'close'): void }>()

const dialog = ref<HTMLDialogElement | null>(null)
const titleId = useId()
let trigger: HTMLElement | null = null

watch(() => props.open, async open => {
  if (open) {
    trigger = document.activeElement instanceof HTMLElement ? document.activeElement : null
    await nextTick()
    if (!dialog.value?.open) dialog.value?.showModal()
    dialog.value?.querySelector<HTMLElement>('[autofocus], input, select, textarea, [data-initial-focus]')?.focus()
  } else {
    dialog.value?.close()
    if (trigger?.isConnected && trigger.getClientRects().length) trigger.focus()
  }
}, { immediate: true })

onBeforeUnmount(() => dialog.value?.close())

function requestClose() {
  if (!props.busy) emit('close')
}
</script>

<template>
  <dialog ref="dialog" class="stock-dialog" :class="[widthClass, { 'stock-drawer': drawer }]" :aria-labelledby="titleId" @cancel.prevent="requestClose">
    <header class="mb-5 flex items-start justify-between gap-4">
      <div class="min-w-0">
        <p class="mb-1 text-[11px] font-semibold tracking-[1.3px] text-[#a67039]">{{ eyebrow }}</p>
        <h2 :id="titleId" class="break-words text-xl font-bold">{{ title }}</h2>
        <slot name="subtitle" />
      </div>
      <button type="button" class="icon-button" aria-label="Fechar" :disabled="busy" @click="requestClose"><X class="size-4" aria-hidden="true" /></button>
    </header>
    <slot />
  </dialog>
</template>

<style scoped src="./stock-ui.css"></style>
