import axios from "axios";
import type { useAuthStore } from "../stores/auth";

const api = axios.create({
    baseURL: "http://localhost:8080",
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true,
});

export function setupAuthInterceptors(auth: Pick<ReturnType<typeof useAuthStore>, 'accessToken' | 'logout'>): () => void {
    const requestInterceptor = api.interceptors.request.use(config => {
        if (auth.accessToken && !config.url?.startsWith('/auth/')) {
            config.headers.Authorization = `Bearer ${auth.accessToken}`;
        }
        return config;
    });

    const responseInterceptor = api.interceptors.response.use(response => response, error => {
        if (error.response?.status === 401 && auth.accessToken
            && error.config?.headers?.Authorization === `Bearer ${auth.accessToken}`) auth.logout();
        return Promise.reject(error);
    });

    return () => {
        api.interceptors.request.eject(requestInterceptor);
        api.interceptors.response.eject(responseInterceptor);
    };
}

export default api;
