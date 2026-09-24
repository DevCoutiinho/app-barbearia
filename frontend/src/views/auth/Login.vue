<script lang="ts" setup>
import { loginSchema } from '../../schemas/auth.schema';
import { toTypedSchema } from '@vee-validate/zod';
import { useForm, useField } from 'vee-validate';
import { useRouter } from 'vue-router';
import { toast } from 'vue-sonner';
import { ref } from 'vue';
import { Mail, Lock, Scissors, ArrowRight, Loader2 } from '@lucide/vue';
import { useAuthStore } from '../../stores/auth';
import heroImg from '../../assets/barbershop-hero.jpg';
import { GoogleSignInButton } from "vue3-google-signin";
import { handleApiError } from '../../utils/errorHandler';

const router = useRouter();
const auth = useAuthStore();
const isLoading = ref(false);
const showPassword = ref(false);

const validationSchema = toTypedSchema(loginSchema);

const { handleSubmit, errors, setFieldError } = useForm({
    validationSchema
});

const { value: email } = useField<string>('email');
const { value: password } = useField<string>('password');

const onSubmit = handleSubmit(async (values) => {
    try {
        isLoading.value = true;
        await auth.login(values);
        toast.success('Login realizado com sucesso!');
        await router.replace({ name: 'home' });
    } catch (error) {
        handleApiError(error, setFieldError, 'Erro ao fazer login. Verifique suas credenciais.');
    } finally {
        isLoading.value = false;
    }
});

const handleGoogleSuccess = async (response: any) => {
    try {
        isLoading.value = true;
        await auth.loginGoogle({ idToken: response.credential });
        toast.success('Login com Google realizado com sucesso!');
        await router.replace({ name: 'home' });
    } catch (error) {
        handleApiError(error, undefined, 'Erro ao realizar login com o Google.');
    } finally {
        isLoading.value = false;
    }
};

const handleGoogleError = () => {
    toast.error('Não foi possível autenticar com o Google.');
};
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
                        Gerencie com maestria,
                        <span class="block bg-gradient-to-r from-gold to-gold-light bg-clip-text text-transparent">
                            atenda com excelência.
                        </span>
                    </h1>
                    <p class="text-base leading-relaxed text-white/60">
                        O painel de controle definitivo para acompanhar seus agendamentos, clientes e equipe em tempo real.
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
                        <span class="text-3xl font-extrabold tracking-tight text-white">99.8%</span>
                        <span class="text-xs text-white/45">disponibilidade</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- Painel Direito - Formulário de Login -->
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
                    <h1 class="mb-2 text-3xl font-extrabold tracking-tight text-dark">Acesse sua conta</h1>
                    <p class="text-sm leading-relaxed text-gray-500">
                        Bem-vindo de volta! Insira suas credenciais para acessar o painel.
                    </p>
                </div>

                <form @submit.prevent="onSubmit" class="flex flex-col gap-5" novalidate>
                    <!-- Campo E-mail -->
                    <div class="flex flex-col gap-1.5">
                        <label for="login-email" class="text-xs font-bold uppercase tracking-wider text-gray-700">
                            E-mail
                        </label>
                        <div class="group relative flex items-center">
                            <Mail :size="18" class="pointer-events-none absolute left-3.5 text-gray-400 transition-colors group-focus-within:text-gold" />
                            <input
                                id="login-email"
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
                        <div class="flex items-center justify-between">
                            <label for="login-password" class="text-xs font-bold uppercase tracking-wider text-gray-700">
                                Senha
                            </label>
                        </div>
                        <div class="group relative flex items-center">
                            <Lock :size="18" class="pointer-events-none absolute left-3.5 text-gray-400 transition-colors group-focus-within:text-gold" />
                            <input
                                id="login-password"
                                :type="showPassword ? 'text' : 'password'"
                                v-model="password"
                                placeholder="Sua senha de acesso"
                                autocomplete="current-password"
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
                            <span>Entrar</span>
                            <ArrowRight :size="18" />
                        </template>
                        <template v-else>
                            <Loader2 :size="18" class="animate-spin" />
                            <span>Entrando...</span>
                        </template>
                    </button>
                </form>

                <!-- Divisor -->
                <div class="relative my-6 flex items-center justify-center">
                    <div class="w-full border-t border-gray-200"></div>
                    <span class="absolute bg-surface px-3 text-xs font-semibold uppercase tracking-wider text-gray-400">
                        ou
                    </span>
                </div>

                <!-- Botão Google Sign-In Customizado -->
                <div class="relative w-full">
                    <!-- Botão Visual Personalizado BarberShop -->
                    <div
                        class="flex w-full items-center justify-center gap-3 rounded-xl border border-gray-200 bg-white py-3.5 px-4 text-sm font-semibold text-gray-700 shadow-sm transition-all duration-200 hover:border-gold hover:bg-gold/5 hover:text-dark hover:shadow-md cursor-pointer"
                    >
                        <svg class="h-5 w-5 shrink-0" viewBox="0 0 24 24">
                            <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17Z"/>
                            <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.36 24 12 24Z"/>
                            <path fill="#FBBC05" d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.16 0 9.98 0 12s.45 3.84 1.25 5.42l4.03-3.15Z"/>
                            <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.36 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98Z"/>
                        </svg>
                        <span>Continue com o Google</span>
                    </div>

                    <!-- Botão Oficial Invisível Sobreposto para Captura do Clique -->
                    <div class="absolute inset-0 z-10 flex items-center justify-center opacity-0 overflow-hidden cursor-pointer">
                        <GoogleSignInButton
                            @success="handleGoogleSuccess"
                            @error="handleGoogleError"
                            width="400"
                            size="large"
                            class="scale-150 cursor-pointer"
                        />
                    </div>
                </div>

                <!-- Link para Registro -->
                <p class="mt-8 text-center text-sm text-gray-500">
                    Não tem uma conta?
                    <router-link
                        to="/register"
                        class="ml-1 font-bold text-dark no-underline transition-colors hover:text-gold"
                    >
                        Cadastre-se
                    </router-link>
                </p>
            </div>
        </div>
    </div>
</template>
