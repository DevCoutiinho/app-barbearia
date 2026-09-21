import { AxiosError } from 'axios';
import { toast } from 'vue-sonner';
import type { ApiResponse } from '../types/api';

/**
 * Trata erros de requisição da API de forma padronizada.
 * 
 * - Mapeia erros específicos de campos para o vee-validate (se setFieldError for fornecido)
 * - Dispara toast de erro com a mensagem mais relevante
 * - Trata falhas de conexão de rede
 */
export function handleApiError(
    error: unknown,
    setFieldError?: (field: any, message: string) => void,
    defaultMessage = 'Erro inesperado ao processar solicitação.'
) {
    if (error instanceof AxiosError) {
        const apiError = error.response?.data as ApiResponse | undefined;

        // Se o backend retornou erros específicos de validação de campos
        if (apiError?.errors && apiError.errors.length > 0) {
            if (setFieldError) {
                apiError.errors.forEach((err) => {
                    if (err.field && err.message) {
                        setFieldError(err.field, err.message);
                    }
                });
            }

            toast.error(apiError.errors[0].message);
            return;
        }

        // Se o backend retornou uma mensagem geral no corpo da resposta
        if (apiError?.message) {
            toast.error(apiError.message);
            return;
        }

        // Se não houve resposta (servidor fora do ar ou sem internet)
        if (!error.response) {
            toast.error('Não foi possível conectar ao servidor. Verifique sua conexão.');
            return;
        }

        toast.error(defaultMessage);
        return;
    }

    toast.error(defaultMessage);
}
