export type AppointmentStatus = 'Confirmado' | 'Concluído' | 'Pendente' | 'Cancelado'

export interface Appointment {
  id: number
  date: string
  time: string
  client: string
  service: string
  barber: string
  price: number
  status: AppointmentStatus
}

// Demonstration data only; no appointments are sent to the API.
export const dashboardDate = '2026-09-23'
export const appointments: Appointment[] = [
  { id: 1, date: dashboardDate, time: '09:00', client: 'Marcos Vieira', service: 'Corte Masculino', barber: 'Carlos Silva', price: 40, status: 'Concluído' },
  { id: 2, date: dashboardDate, time: '09:30', client: 'Pedro Nunes', service: 'Corte + Barba', barber: 'João Santos', price: 70, status: 'Concluído' },
  { id: 3, date: dashboardDate, time: '10:30', client: 'Lucas Andrade', service: 'Platinado', barber: 'Rafael Lima', price: 150, status: 'Concluído' },
  { id: 4, date: dashboardDate, time: '11:00', client: 'Rafael Costa', service: 'Corte Masculino', barber: 'Diego Rocha', price: 40, status: 'Concluído' },
  { id: 5, date: dashboardDate, time: '13:00', client: 'Bruno Alves', service: 'Barba', barber: 'João Santos', price: 35, status: 'Concluído' },
  { id: 6, date: dashboardDate, time: '14:00', client: 'Thiago Moura', service: 'Corte + Barba', barber: 'Carlos Silva', price: 70, status: 'Confirmado' },
  { id: 7, date: dashboardDate, time: '14:30', client: 'Felipe Oliveira', service: 'Corte Masculino', barber: 'Diego Rocha', price: 40, status: 'Confirmado' },
  { id: 8, date: dashboardDate, time: '15:30', client: 'Gabriel Souza', service: 'Sobrancelha', barber: 'Rafael Lima', price: 20, status: 'Pendente' },
  { id: 9, date: dashboardDate, time: '16:00', client: 'André Martins', service: 'Corte + Barba', barber: 'João Santos', price: 70, status: 'Confirmado' },
  { id: 10, date: dashboardDate, time: '17:00', client: 'Matheus Lima', service: 'Corte Masculino', barber: 'Carlos Silva', price: 40, status: 'Cancelado' },
]

export const weeklyRevenue = [
  { label: 'Seg', date: '21/09', value: 420 },
  { label: 'Ter', date: '22/09', value: 560 },
  { label: 'Qua', date: '23/09', value: appointments.filter(item => item.status === 'Concluído').reduce((sum, item) => sum + item.price, 0) },
  { label: 'Qui', date: '24/09', value: 0 },
  { label: 'Sex', date: '25/09', value: 0 },
  { label: 'Sáb', date: '26/09', value: 0 },
]
