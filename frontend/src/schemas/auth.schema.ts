import * as z from 'zod';

export const registerSchema = z.object({
        name: z
            .string({ required_error: 'Nome é obrigatório' })
            .min(3, 'Nome deve ter pelo menos 3 caracteres'),
        email: z
            .string({ required_error: 'E-mail é obrigatório' })
            .email('Informe um e-mail válido'),
        password: z
            .string({ required_error: 'Senha é obrigatória' })
            .min(6, 'Senha deve ter pelo menos 6 caracteres')
    })
;

export const loginSchema = z.object({
        email: z
            .string({ required_error: 'E-mail é obrigatório' })
            .email('Informe um e-mail válido'),
        password: z
            .string({ required_error: 'Senha é obrigatória' })
            .min(6, 'Senha deve ter pelo menos 6 caracteres')
    })


export type RegisterData = z.infer<typeof registerSchema>;
export type LoginData = z.infer<typeof loginSchema>;

export interface LoginDataGoogle {
    idToken: string;
}
