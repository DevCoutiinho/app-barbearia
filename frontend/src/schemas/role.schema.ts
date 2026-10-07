import * as z from 'zod';

export const createRoleSchema = z.object({
        name: z
            .string({ required_error: 'Nome é obrigatório' })
            .min(3, 'Nome deve ter pelo menos 3 caracteres'),
        description: z
            .string()
            .optional()
            .nullable(),
        permissionIds: z
            .array(z.string())
            .min(1, 'Selecione pelo menos uma permissão')
    })
;

export const updateRoleSchema = z.object({
        description: z
            .string()
            .optional()
            .nullable(),
        permissionIds: z
            .array(z.string())
            .min(1, 'Selecione pelo menos uma permissão')
    })


export type CreateRoleData = z.infer<typeof createRoleSchema>;
export type UpdateRoleData = z.infer<typeof updateRoleSchema>;