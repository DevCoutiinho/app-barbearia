<script setup lang="ts">
import { computed } from 'vue'
import { DAY_LABELS, type ScheduleBlock, type WeeklySchedule } from '../../types/agenda'
import {
  blockSegmentOn, dayOfWeekOf, fromMinutes, isFullDayBlock, isoTimePart, pausesBetween,
  toIsoDate, toMinutes, visibleHourRange, weekDates,
} from '../../utils/agenda'

const props = defineProps<{
  weekStart: Date
  schedule: WeeklySchedule | null
  blocks: ScheduleBlock[]
  today: string
}>()

const ROW_HEIGHT = 52

const range = computed(() => visibleHourRange(props.schedule))
const hours = computed(() => Array.from({ length: range.value.end - range.value.start }, (_, index) => range.value.start + index))
const columnHeight = computed(() => hours.value.length * ROW_HEIGHT)

type Segment = { key: string; top: number; height: number; label: string; detail?: string; title?: string }

/** Converte minutos do dia em posição na coluna, recortando o que fica fora da faixa visível. */
function place(start: number, end: number) {
  const visibleStart = Math.max(start, range.value.start * 60)
  const visibleEnd = Math.min(end, range.value.end * 60)
  if (visibleEnd <= visibleStart) return null
  return {
    top: (visibleStart - range.value.start * 60) / 60 * ROW_HEIGHT,
    height: (visibleEnd - visibleStart) / 60 * ROW_HEIGHT,
  }
}

const columns = computed(() => weekDates(props.weekStart).map(date => {
  const isoDate = toIsoDate(date)
  const dayOfWeek = dayOfWeekOf(date)
  const intervals = props.schedule?.days.find(day => day.dayOfWeek === dayOfWeek)?.intervals ?? []
  const dayOff = intervals.length === 0

  const unavailable: Segment[] = []
  if (!dayOff) {
    const first = toMinutes(intervals[0].start)
    const last = toMinutes(intervals[intervals.length - 1].end)
    const gaps = [
      { start: 0, end: first, label: '' },
      ...pausesBetween(intervals).map(pause => ({ start: toMinutes(pause.start), end: toMinutes(pause.end), label: 'Intervalo' })),
      { start: last, end: 24 * 60, label: '' },
    ]
    gaps.forEach((gap, index) => {
      const position = place(gap.start, gap.end)
      if (position) unavailable.push({ key: `gap-${index}`, ...position, label: gap.label })
    })
  }

  const blocks: Segment[] = props.blocks.flatMap(block => {
    const segment = blockSegmentOn(block, isoDate)
    const position = segment && place(segment.start, segment.end)
    if (!segment || !position) return []
    const detail = isFullDayBlock(block) ? 'Dia inteiro' : `${fromMinutes(segment.start)}–${segment.end === 1440 ? '24:00' : fromMinutes(segment.end)}`
    return [{ key: block.id, ...position, label: block.reasonLabel, detail, title: block.description ?? undefined }]
  })

  return {
    isoDate,
    dayOfWeek,
    label: DAY_LABELS[dayOfWeek].short,
    dayNumber: String(date.getDate()).padStart(2, '0'),
    isToday: isoDate === props.today,
    isPast: isoDate < props.today,
    dayOff,
    workLabel: intervals.map(range => `${range.start}–${range.end}`).join(', '),
    unavailable,
    blocks,
  }
}))

const blockTimeLabel = (block: ScheduleBlock) => isFullDayBlock(block) ? 'dia inteiro' : `${isoTimePart(block.startsAt)} às ${isoTimePart(block.endsAt)}`
const summary = computed(() => columns.value.map(column => {
  const work = column.dayOff ? 'folga' : `expediente ${column.workLabel}`
  const dayBlocks = props.blocks.filter(block => blockSegmentOn(block, column.isoDate))
  const blocks = dayBlocks.length ? `; bloqueios: ${dayBlocks.map(block => `${block.reasonLabel} ${blockTimeLabel(block)}`).join(', ')}` : ''
  return `${DAY_LABELS[column.dayOfWeek].long} ${column.dayNumber}: ${work}${blocks}`
}).join('. '))
</script>

<template>
  <div class="overflow-x-auto">
    <div class="min-w-[760px] px-4 pb-2 sm:px-5" role="img" :aria-label="summary">
      <div class="grid grid-cols-[56px_repeat(7,minmax(0,1fr))] border-b border-line">
        <span aria-hidden="true"></span>
        <div v-for="column in columns" :key="column.isoDate" class="border-l border-line py-3 text-center" :class="{ 'opacity-60': column.isPast }">
          <p class="text-[11px] font-medium" :class="column.isToday ? 'text-[#a67039]' : 'text-[#74747f]'">{{ column.label }}</p>
          <p class="mt-0.5 text-[15px] font-bold" :class="column.isToday ? 'text-[#a67039]' : 'text-ink'">{{ column.dayNumber }}</p>
        </div>
      </div>

      <div class="grid grid-cols-[56px_repeat(7,minmax(0,1fr))]" aria-hidden="true">
        <div class="relative" :style="{ height: `${columnHeight}px` }">
          <span
            v-for="(hour, index) in hours"
            :key="hour"
            class="absolute right-2 text-[11px] text-[#74747f]"
            :style="{ top: `${index * ROW_HEIGHT + 6}px` }"
          >{{ fromMinutes(hour * 60) }}</span>
        </div>

        <div
          v-for="column in columns"
          :key="column.isoDate"
          class="hour-lines relative border-l border-line"
          :style="{ height: `${columnHeight}px`, '--row': `${ROW_HEIGHT}px` }"
        >
          <div v-if="column.dayOff" class="absolute inset-0 flex items-center justify-center bg-paper-deep/70 text-[11px] text-[#74747f]">Folga</div>
          <template v-else>
            <div
              v-for="segment in column.unavailable"
              :key="segment.key"
              class="absolute inset-x-0 flex items-center justify-center bg-paper text-[11px] text-[#74747f]"
              :style="{ top: `${segment.top}px`, height: `${segment.height}px` }"
            >{{ segment.height >= 18 ? segment.label : '' }}</div>
          </template>

          <div
            v-for="segment in column.blocks"
            :key="segment.key"
            class="absolute inset-x-1 overflow-hidden rounded-lg border-l-[3px] border-red-700 bg-red-50 px-2 py-1"
            :style="{ top: `${segment.top + 2}px`, height: `${Math.max(segment.height - 4, 18)}px` }"
            :title="segment.title"
          >
            <p class="truncate text-[12px] leading-4 font-semibold text-ink">{{ segment.label }}</p>
            <p v-if="segment.height >= 36" class="truncate text-[11px] leading-4 text-red-800">{{ segment.detail }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hour-lines {
  background-image: repeating-linear-gradient(to bottom, transparent 0, transparent calc(var(--row) - 1px), var(--color-line) calc(var(--row) - 1px), var(--color-line) var(--row));
}
</style>
