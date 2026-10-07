import * as z from 'zod';

export const createPermissionSchema = z.object({
        name: z
            .string({ required_error: 'Nome é obrigatório' })
            .min(3, 'Nome deve ter pelo menos 3 caracteres'),
        description: z
            .string()
            .optional()
            .nullable()
    })
;

export type CreatePermissionData = z.infer<typeof createPermissionSchema>;