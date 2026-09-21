export interface HeroStat {
  value: string
  label: string
}

export interface AboutDiff {
  icon: 'check' | 'sparkles' | 'clock' | 'heart'
  label: string
}

export interface Barber {
  initials: string
  name: string
  specialty: string
}

export type ServiceIcon = 'cut' | 'beard' | 'combo' | 'brow' | 'platinum'

export interface Service {
  id: string
  icon: ServiceIcon
  title: string
  summary: string
  fullDescription: string
  includes: string[]
  duration: string
}

export interface Testimonial {
  initials: string
  name: string
  quote: string
  rating: number
}

export interface FaqItem {
  id: string
  question: string
  answer: string
}

export const navLinks = [
  { href: '#sobre', label: 'Sobre' },
  { href: '#servicos', label: 'Serviços e Barbeiros' },
  { href: '#faq', label: 'FAQ' },
  { href: '#depoimentos', label: 'Depoimentos' },
  { href: '#contato', label: 'Contato' },
] as const

export const heroStats: HeroStat[] = [
  { value: '312', label: 'clientes ativos' },
  { value: '4', label: 'barbeiros' },
  { value: '4,9', label: 'nota média' },
]

export const aboutDiffs: AboutDiff[] = [
  { icon: 'check', label: 'Ambiente climatizado e reservado' },
  { icon: 'sparkles', label: 'Profissionais especializados e atualizados' },
  { icon: 'clock', label: 'Atendimento individual, sem pressa' },
  { icon: 'heart', label: 'Produtos premium para pele e barba' },
]

export const barbers: Barber[] = [
  { initials: 'CS', name: 'Carlos Silva', specialty: 'Degradê e navalhado' },
  { initials: 'JS', name: 'João Santos', specialty: 'Barba e navalha quente' },
  { initials: 'RL', name: 'Rafael Lima', specialty: 'Coloração e platinado' },
  { initials: 'DR', name: 'Diego Rocha', specialty: 'Corte infantil e social' },
]

export const services: Service[] = [
  {
    id: 'corte',
    icon: 'cut',
    title: 'Corte Masculino',
    summary: 'Corte personalizado ao formato do rosto, finalizado com navalha.',
    fullDescription:
      'Corte sob medida de acordo com o formato do rosto, textura do cabelo e estilo do cliente, com lavagem e finalização caprichada na navalha.',
    includes: ['Lavagem e corte', 'Finalização com navalha', 'Hidratação leve'],
    duration: '40 min',
  },
  {
    id: 'barba',
    icon: 'beard',
    title: 'Barba',
    summary: 'Modelagem completa com toalha quente e acabamento na navalha.',
    fullDescription:
      'Modelagem completa da barba com toalha quente para abrir os poros, alinhamento preciso e acabamento na navalha para um resultado uniforme.',
    includes: ['Toalha quente', 'Modelagem e navalha', 'Hidratação pós-barba'],
    duration: '30 min',
  },
  {
    id: 'combo',
    icon: 'combo',
    title: 'Corte + Barba',
    summary: 'Corte e barba em um único atendimento, com acabamento completo.',
    fullDescription:
      'Combinação completa de corte e barba em um único atendimento, pensada para quem quer sair pronto sem precisar de duas visitas.',
    includes: ['Corte completo', 'Barba com toalha quente', 'Finalização e hidratação'],
    duration: '1h10',
  },
  {
    id: 'sobrancelha',
    icon: 'brow',
    title: 'Sobrancelha',
    summary: 'Design de sobrancelha masculina, alinhado ao formato do rosto.',
    fullDescription:
      'Design de sobrancelha masculina para um olhar mais expressivo, com alinhamento cuidadoso ao formato natural do rosto.',
    includes: ['Alinhamento e limpeza', 'Acabamento fino na navalha'],
    duration: '15 min',
  },
  {
    id: 'platinado',
    icon: 'platinum',
    title: 'Platinado',
    summary: 'Descoloração e tonalização para uma transformação marcante.',
    fullDescription:
      'Processo completo de descoloração e tonalização para quem busca uma transformação marcante, com cuidado extra na saúde do fio.',
    includes: ['Descoloração', 'Tonalização', 'Hidratação pós-química'],
    duration: '2h30',
  },
]

export const testimonials: Testimonial[] = [
  {
    initials: 'MV',
    name: 'Marcos Vieira',
    quote: 'Degradê impecável e não demorou. Já marquei o próximo.',
    rating: 5,
  },
  {
    initials: 'PN',
    name: 'Pedro Nunes',
    quote: 'Entendeu o que eu queria só pela foto. Acabamento na navalha é outro nível.',
    rating: 5,
  },
  {
    initials: 'LA',
    name: 'Lucas Andrade',
    quote: 'Corte muito bom, só atrasou uns dez minutos no sábado.',
    rating: 4,
  },
  {
    initials: 'RC',
    name: 'Rafael Costa',
    quote: 'Ambiente super agradável, atendimento atencioso do início ao fim.',
    rating: 5,
  },
  {
    initials: 'BA',
    name: 'Bruno Alves',
    quote: 'Melhor barba que já fiz na cidade, recomendo demais.',
    rating: 5,
  },
  {
    initials: 'TM',
    name: 'Thiago Moura',
    quote: 'Equipe caprichosa e ambiente muito limpo, virei cliente fiel.',
    rating: 5,
  },
]

export const faqItems: FaqItem[] = [
  {
    id: 'agendar',
    question: 'Preciso agendar horário para ser atendido?',
    answer:
      'Recomendamos entrar em contato antes pelo telefone ou WhatsApp para confirmar disponibilidade, principalmente aos sábados, quando o movimento é maior.',
  },
  {
    id: 'pagamento',
    question: 'Quais formas de pagamento vocês aceitam?',
    answer: 'Aceitamos dinheiro, cartão de débito, cartão de crédito e Pix diretamente na barbearia.',
  },
  {
    id: 'produtos',
    question: 'Preciso levar algum produto próprio?',
    answer:
      'Não é necessário. Trabalhamos com produtos profissionais próprios para pele, cabelo e barba, selecionados pela nossa equipe.',
  },
  {
    id: 'barbeiro',
    question: 'Como escolho o barbeiro?',
    answer:
      'Na seção Serviços e Barbeiros você conhece a especialidade de cada profissional e pode escolher pelo estilo que mais combina com você.',
  },
  {
    id: 'horario',
    question: 'Qual o horário de funcionamento?',
    answer: 'Funcionamos de segunda a sábado, das 08h às 19h. Aos domingos, ficamos fechados.',
  },
]

export const contact = {
  address: 'Rua da Aurora, 214 — Valença, BA',
  phoneDisplay: '(75) 3641-2200',
  phoneHref: 'tel:+557536412200',
  email: 'contato@barbeariaferro.com.br',
  hours: [
    'Segunda a sexta · 08:00 – 19:00',
    'Sábado · 08:00 – 19:00',
    'Domingo · Fechado',
  ],
}
