import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { useAuthStore } from "../stores/auth";

const api = axios.create({
    baseURL: "http://localhost:8080",
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true,
});

let isRefreshing = false;
let failedQueue: { resolve: (token: string) => void, reject: (error: any) => void }[] = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token as string);
        }
    });
    failedQueue = [];
};

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
    _retry?: boolean;
}

export function setupAuthInterceptors(auth: Pick<ReturnType<typeof useAuthStore>, 'accessToken' | 'logout' | 'refresh'>): () => void {
    const requestInterceptor = api.interceptors.request.use(config => {
        if (auth.accessToken && !config.url?.startsWith('/auth/')) {
            config.headers.Authorization = `Bearer ${auth.accessToken}`;
        }
        return config;
    });

    const responseInterceptor = api.interceptors.response.use(response => response, async (error: AxiosError) => {
        const originalRequest = error.config as CustomAxiosRequestConfig;

        if (error.response?.status === 401 && !originalRequest._retry && auth.accessToken && !originalRequest.url?.startsWith('/auth/')) {
            if (isRefreshing) {
                return new Promise(function(resolve, reject) {
                    failedQueue.push({ resolve, reject });
                }).then(token => {
                    originalRequest.headers.Authorization = `Bearer ${token}`;
                    return api(originalRequest);
                }).catch(err => {
                    return Promise.reject(err);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                await auth.refresh();
                const newToken = auth.accessToken;
                processQueue(null, newToken!);
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest);
            } catch (err) {
                processQueue(err, null);
                return Promise.reject(err);
            } finally {
                isRefreshing = false;
            }
        }

        if (error.response?.status === 401 && auth.accessToken
            && error.config?.headers?.Authorization === `Bearer ${auth.accessToken}`) {
            auth.logout();
        }

        return Promise.reject(error);
    });

    return () => {
        api.interceptors.request.eject(requestInterceptor);
        api.interceptors.response.eject(responseInterceptor);
    };
}

export default api;
