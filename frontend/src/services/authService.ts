import api from "./api";
import type { RegisterData, LoginData, LoginDataGoogle } from "../schemas/auth.schema";
import type { ApiResponse } from "../types/api";

export interface LoginResponse {
    accessToken: string;
}

const authService = {

    async register(data: RegisterData): Promise<ApiResponse<void>> {
        const response = await api.post('/auth/register', data);
        return response.data;
    },
    async login(data: LoginData): Promise<ApiResponse<LoginResponse[]>> {
        const response = await api.post('/auth/login', data);
        return response.data;
    },
    async loginGoogle(data: LoginDataGoogle): Promise<ApiResponse<LoginResponse[]>> {
        const response = await api.post('/auth/login/google', data);
        return response.data;
    },
    async logout(): Promise<ApiResponse<void>> {
        const response = await api.post('/auth/logout');
        return response.data;
    },
    async refreshToken(): Promise<ApiResponse<LoginResponse[]>> {
        const response = await api.post('/auth/refresh');
        return response.data;
    }
}

export default authService;
