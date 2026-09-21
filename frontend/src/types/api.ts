export interface ApiErrorDetail {
    field: string;
    message: string;
}

export interface ApiResponse<T = void> {
    status: number;
    message: string;
    data?: T;
    errors?: ApiErrorDetail[];
}