import { ref } from 'vue'
import { defineStore } from 'pinia'
import schedulingService from '../services/schedulingService'
import type {
  AvailableSlot,
  BarberSummary,
  BookingSelection,
  SchedulingResponse,
  ServiceCatalogItem,
} from '../types/scheduling'

export const useSchedulingStore = defineStore('scheduling', () => {
  // Estado do wizard
  const selection = ref<BookingSelection>({
    service: null,
    barber: null,
    date: null,
    slot: null,
  })

  // Dados carregados da API
  const catalog = ref<ServiceCatalogItem[]>([])
  const barbers = ref<BarberSummary[]>([])
  const slots = ref<AvailableSlot[]>([])
  const mySchedulings = ref<SchedulingResponse[]>([])

  // Estados de carregamento
  const loadingCatalog = ref(false)
  const loadingBarbers = ref(false)
  const loadingSlots = ref(false)
  const loadingSchedulings = ref(false)
  const submitting = ref(false)

  // Erros
  const catalogError = ref<string | null>(null)
  const barbersError = ref<string | null>(null)
  const slotsError = ref<string | null>(null)
  const schedulingsError = ref<string | null>(null)

  // Controle de requisição em andamento para evitar race condition nos slots
  let slotsRequestId = 0

  function selectService(service: ServiceCatalogItem) {
    selection.value.service = service
    // Invalida seleções dependentes
    selection.value.barber = null
    selection.value.date = null
    selection.value.slot = null
    slots.value = []
  }

  function selectBarber(barber: BarberSummary) {
    selection.value.barber = barber
    // Invalida slot ao trocar barbeiro
    selection.value.slot = null
    slots.value = []
  }

  function selectDate(date: string) {
    selection.value.date = date
    // Invalida slot ao trocar data
    selection.value.slot = null
    slots.value = []
  }

  function selectSlot(slot: AvailableSlot) {
    selection.value.slot = slot
  }

  function resetSelection() {
    selection.value = { service: null, barber: null, date: null, slot: null }
    slots.value = []
  }

  async function fetchCatalog() {
    loadingCatalog.value = true
    catalogError.value = null
    try {
      const response = await schedulingService.getCatalog()
      catalog.value = response.data ?? []
    } catch (err: any) {
      catalogError.value = err?.response?.data?.message ?? 'Erro ao carregar serviços.'
    } finally {
      loadingCatalog.value = false
    }
  }

  async function fetchBarbers() {
    loadingBarbers.value = true
    barbersError.value = null
    try {
      const response = await schedulingService.getBarbers()
      barbers.value = response.data ?? []
    } catch (err: any) {
      barbersError.value = err?.response?.data?.message ?? 'Erro ao carregar barbeiros.'
    } finally {
      loadingBarbers.value = false
    }
  }

  async function fetchSlots() {
    const { service, barber, date } = selection.value
    if (!service || !barber || !date) return

    const requestId = ++slotsRequestId
    loadingSlots.value = true
    slotsError.value = null
    slots.value = []

    try {
      const response = await schedulingService.getAvailableSlots(barber.id, service.id, date)
      // Descarta resposta se uma requisição mais recente foi iniciada
      if (requestId !== slotsRequestId) return
      slots.value = response.data ?? []
    } catch (err: any) {
      if (requestId !== slotsRequestId) return
      slotsError.value = err?.response?.data?.message ?? 'Erro ao consultar horários.'
    } finally {
      if (requestId === slotsRequestId) loadingSlots.value = false
    }
  }

  async function fetchMySchedulings() {
    loadingSchedulings.value = true
    schedulingsError.value = null
    try {
      const response = await schedulingService.getMySchedulings()
      mySchedulings.value = response.data ?? []
    } catch (err: any) {
      schedulingsError.value = err?.response?.data?.message ?? 'Erro ao carregar agendamentos.'
    } finally {
      loadingSchedulings.value = false
    }
  }

  async function confirmBooking(): Promise<{ success: boolean; conflict: boolean; message: string }> {
    const { service, barber, date, slot } = selection.value
    if (!service || !barber || !date || !slot) {
      return { success: false, conflict: false, message: 'Seleção incompleta.' }
    }

    submitting.value = true
    try {
      // Converte data + horário para Instant ISO-8601 no fuso de Brasília
      const localDateTimeStr = `${date}T${slot.startTime}`
      // O backend usa America/Sao_Paulo; enviamos o Instant correspondente
      const instant = localDateTimeToInstant(localDateTimeStr)

      await schedulingService.create({
        barberId: barber.id,
        serviceId: service.id,
        scheduledAt: instant,
      })

      await fetchMySchedulings()
      resetSelection()
      return { success: true, conflict: false, message: 'Agendamento criado com sucesso!' }
    } catch (err: any) {
      const status: number = err?.response?.status ?? 0
      const message: string =
        err?.response?.data?.message ?? 'Erro ao confirmar agendamento.'

      // Conflito de horário: HTTP 409 com ResourceConflictException
      if (status === 409) {
        // Recarrega slots para refletir o horário já reservado
        await fetchSlots()
        selection.value.slot = null
        return { success: false, conflict: true, message }
      }

      return { success: false, conflict: false, message }
    } finally {
      submitting.value = false
    }
  }

  return {
    selection,
    catalog,
    barbers,
    slots,
    mySchedulings,
    loadingCatalog,
    loadingBarbers,
    loadingSlots,
    loadingSchedulings,
    submitting,
    catalogError,
    barbersError,
    slotsError,
    schedulingsError,
    selectService,
    selectBarber,
    selectDate,
    selectSlot,
    resetSelection,
    fetchCatalog,
    fetchBarbers,
    fetchSlots,
    fetchMySchedulings,
    confirmBooking,
  }
})

/**
 * Converte "YYYY-MM-DDTHH:mm:ss" interpretado como America/Sao_Paulo para Instant ISO-8601.
 * O backend espera um Instant e usa ZoneId.of("America/Sao_Paulo") internamente.
 */
function localDateTimeToInstant(localDateTimeStr: string): string {
  // Obtém o offset de Brasília no momento da data informada
  const date = new Date(localDateTimeStr + 'Z') // parse como UTC temporário
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    timeZoneName: 'shortOffset',
  })
  const parts = formatter.formatToParts(date)
  const offsetPart = parts.find(p => p.type === 'timeZoneName')?.value ?? '-03:00'
  // Extrai o offset numérico (ex: "GMT-3" → "-03:00")
  const match = offsetPart.match(/GMT([+-]\d+)(?::(\d+))?/)
  const hours = match ? parseInt(match[1], 10) : -3
  const minutes = match?.[2] ? parseInt(match[2], 10) : 0
  const offsetMs = -(hours * 60 + (hours < 0 ? -minutes : minutes)) * 60 * 1000

  const localMs = new Date(localDateTimeStr).getTime()
  return new Date(localMs + offsetMs).toISOString()
}
