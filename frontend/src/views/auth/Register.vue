<script lang="ts" setup>
import { toTypedSchema } from '@vee-validate/zod';
import { useForm, useField } from 'vee-validate';
import { registerSchema } from '../../schemas/auth.schema';
import { useRouter } from 'vue-router';
import { toast } from 'vue-sonner';
import { ref } from 'vue';
import { User, Mail, Lock, Scissors, ArrowRight, Loader2 } from '@lucide/vue';
import authService from '../../services/authService';
import heroImg from '../../assets/barbershop-hero.jpg';
import { handleApiError } from '../../utils/errorHandler';

const router = useRouter();
const isLoading = ref(false);
const showPassword = ref(false);

const validationSchema = toTypedSchema(registerSchema);

const { handleSubmit, errors, setFieldError } = useForm({
    validationSchema
});

const { value: name } = useField<string>('name');
const { value: email } = useField<string>('email');
const { value: password } = useField<string>('password');

const onSubmit = handleSubmit(async (values) => {
    try {
        isLoading.value = true;
        await authService.register(values);
        toast.success('Conta criada com sucesso!');
        router.push({ path: '/login', replace: true });
    } catch (error) {
        handleApiError(error, setFieldError, 'Erro ao registrar usuário.');
    } finally {
        isLoading.value = false;
    }
});
</script>

<template>
    <div class="flex min-h-screen font-sans bg-surface">
        <!-- Painel Esquerdo - Hero (Visível a partir de telas lg) -->
        <div class="relative hidden flex-1 overflow-hidden bg-dark lg:flex">
            <img :src="heroImg" alt="BarberShop" class="absolute inset-0 h-full w-full object-cover opacity-50" />
            <div class="absolute inset-0 bg-gradient-to-b from-dark/40 via-dark/70 to-dark/95"></div>

            <div class="relative z-10 flex h-full w-full flex-col justify-between p-12">
                <!-- Logo Topo -->
                <div class="flex items-center gap-3">
                    <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-gold to-gold-light text-dark shadow-md">
                        <Scissors :size="24" />
                    </div>
                    <div>
                        <h2 class="text-xl font-bold leading-tight tracking-tight text-white">BarberShop</h2>
                        <p class="text-xs tracking-wide text-white/50">Sistema de gestão</p>
                    </div>
                </div>

                <!-- Chamada Principal -->
                <div class="max-w-md">
                    <h1 class="mb-4 text-4xl font-extrabold leading-tight tracking-tight text-white xl:text-5xl">
                        Sua barbearia no
                        <span class="block bg-gradient-to-r from-gold to-gold-light bg-clip-text text-transparent">
                            próximo nível.
                        </span>
                    </h1>
                    <p class="text-base leading-relaxed text-white/60">
                        Agendamento, equipe, catálogo e estoque em um só lugar. Seus barbeiros olham para o cliente, não para planilhas.
                    </p>
                </div>

                <!-- Estatísticas / Indicadores -->
                <div class="flex gap-10 border-t border-white/10 pt-8">
                    <div class="flex flex-col gap-1">
                        <span class="text-3xl font-extrabold tracking-tight text-white">312</span>
                        <span class="text-xs text-white/45">clientes ativos</span>
                    </div>
                    <div class="flex flex-col gap-1">
                        <span class="text-3xl font-extrabold tracking-tight text-white">4</span>
                        <span class="text-xs text-white/45">barbeiros</span>
                    </div>
                    <div class="flex flex-col gap-1">
                        <span class="text-3xl font-extrabold tracking-tight text-white">47</span>
                        <span class="text-xs text-white/45">cortes no sábado</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Painel Direito - Formulário de Cadastro -->
        <div class="flex min-h-screen flex-1 items-center justify-center p-6 sm:p-10 lg:min-h-0 lg:max-w-[560px] lg:p-12">
            <div class="w-full max-w-md">
                <!-- Logo no Mobile -->
                <div class="mb-8 flex items-center gap-3 lg:hidden">
                    <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-gold to-gold-light text-dark shadow-sm">
                        <Scissors :size="20" />
                    </div>
                    <span class="text-lg font-bold tracking-tight text-dark">BarberShop</span>
                </div>

                <!-- Cabeçalho -->
                <div class="mb-8">
                    <h1 class="mb-2 text-3xl font-extrabold tracking-tight text-dark">Criar sua conta</h1>
                    <p class="text-sm leading-relaxed text-gray-500">
                        Preencha os dados abaixo para começar a usar o BarberShop.
                    </p>
                </div>

                <form @submit.prevent="onSubmit" class="flex flex-col gap-5" novalidate>
                    <!-- Campo Nome Completo -->
                    <div class="flex flex-col gap-1.5">
                        <label for="register-name" class="text-xs font-bold uppercase tracking-wider text-gray-700">
                            Nome completo
                        </label>
                        <div class="group relative flex items-center">
                            <User :size="18" class="pointer-events-none absolute left-3.5 text-gray-400 transition-colors group-focus-within:text-gold" />
                            <input
                                id="register-name"
                                type="text"
                                v-model="name"
                                placeholder="Seu nome completo"
                                autocomplete="name"
                                class="w-full rounded-xl border bg-white py-3.5 pr-4 pl-11 text-sm text-dark placeholder:text-gray-300 outline-none transition-all duration-200 focus:ring-2"
                                :class="errors?.name
                                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15'
                                    : 'border-gray-200 focus:border-gold focus:ring-gold/15'"
                            />
                        </div>
                        <span v-if="errors?.name" class="pl-1 text-xs font-medium text-red-500 transition-all">
                            {{ errors.name }}
                        </span>
                    </div>

                    <!-- Campo E-mail -->
                    <div class="flex flex-col gap-1.5">
                        <label for="register-email" class="text-xs font-bold uppercase tracking-wider text-gray-700">
                            E-mail
                        </label>
                        <div class="group relative flex items-center">
                            <Mail :size="18" class="pointer-events-none absolute left-3.5 text-gray-400 transition-colors group-focus-within:text-gold" />
                            <input
                                id="register-email"
                                type="email"
                                v-model="email"
                                placeholder="voce@email.com"
                                autocomplete="email"
                                class="w-full rounded-xl border bg-white py-3.5 pr-4 pl-11 text-sm text-dark placeholder:text-gray-300 outline-none transition-all duration-200 focus:ring-2"
                                :class="errors?.email
                                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15'
                                    : 'border-gray-200 focus:border-gold focus:ring-gold/15'"
                            />
                        </div>
                        <span v-if="errors?.email" class="pl-1 text-xs font-medium text-red-500 transition-all">
                            {{ errors.email }}
                        </span>
                    </div>

                    <!-- Campo Senha -->
                    <div class="flex flex-col gap-1.5">
                        <label for="register-password" class="text-xs font-bold uppercase tracking-wider text-gray-700">
                            Senha
                        </label>
                        <div class="group relative flex items-center">
                            <Lock :size="18" class="pointer-events-none absolute left-3.5 text-gray-400 transition-colors group-focus-within:text-gold" />
                            <input
                                id="register-password"
                                :type="showPassword ? 'text' : 'password'"
                                v-model="password"
                                placeholder="Mínimo 6 caracteres"
                                autocomplete="new-password"
                                class="w-full rounded-xl border bg-white py-3.5 pr-20 pl-11 text-sm text-dark placeholder:text-gray-300 outline-none transition-all duration-200 focus:ring-2"
                                :class="errors?.password
                                    ? 'border-red-500 focus:border-red-500 focus:ring-red-500/15'
                                    : 'border-gray-200 focus:border-gold focus:ring-gold/15'"
                            />
                            <button
                                type="button"
                                @click="showPassword = !showPassword"
                                class="absolute right-3 cursor-pointer rounded-md px-2 py-1 text-xs font-semibold text-gray-400 transition-colors hover:bg-gold/10 hover:text-gold"
                            >
                                {{ showPassword ? 'Ocultar' : 'Mostrar' }}
                            </button>
                        </div>
                        <span v-if="errors?.password" class="pl-1 text-xs font-medium text-red-500 transition-all">
                            {{ errors.password }}
                        </span>
                    </div>

                    <!-- Botão de Submit -->
                    <button
                        type="submit"
                        :disabled="isLoading"
                        class="mt-2 flex w-full cursor-pointer items-center justify-center gap-2 rounded-xl bg-dark py-3.5 text-sm font-bold text-white shadow-md transition-all duration-200 hover:bg-dark-soft hover:shadow-lg active:scale-[0.99] disabled:cursor-not-allowed disabled:opacity-70"
                    >
                        <template v-if="!isLoading">
                            <span>Criar Conta</span>
                            <ArrowRight :size="18" />
                        </template>
                        <template v-else>
                            <Loader2 :size="18" class="animate-spin" />
                            <span>Criando conta...</span>
                        </template>
                    </button>
                </form>

                <!-- Link para Login -->
                <p class="mt-8 text-center text-sm text-gray-500">
                    Já tem uma conta?
                    <router-link
                        to="/login"
                        class="ml-1 font-bold text-dark no-underline transition-colors hover:text-gold"
                    >
                        Entrar
                    </router-link>
                </p>
            </div>
        </div>
    </div>
</template>