import axios from 'axios'
import { useAuthStore } from '../store/authStore'

export const apiClient = axios.create({
  baseURL: '/api',
})

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

interface LoginResponseBody {
  accessToken: string
  refreshToken: string
  username: string
  role: string
}

// Nhieu request co the cung nhan 401 (token het han) cung luc; chi goi refresh mot lan
// va cho cac request khac dung ket qua chung, tranh spam /api/auth/refresh.
let refreshPromise: Promise<string> | null = null

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    const { refreshToken, username, role } = useAuthStore.getState()
    refreshPromise = axios
      .post<LoginResponseBody>('/api/auth/refresh', { refreshToken })
      .then(({ data }) => {
        useAuthStore.getState().login(data.accessToken, data.refreshToken, data.username ?? username, data.role ?? role)
        return data.accessToken
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const status = error.response?.status
    const isAuthEndpoint = originalRequest?.url?.includes('/auth/')

    if (status === 401 && !isAuthEndpoint && !originalRequest._retry) {
      const { refreshToken } = useAuthStore.getState()
      if (!refreshToken) {
        useAuthStore.getState().logout()
        return Promise.reject(error)
      }
      originalRequest._retry = true
      try {
        const newAccessToken = await refreshAccessToken()
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return apiClient(originalRequest)
      } catch {
        useAuthStore.getState().logout()
        return Promise.reject(error)
      }
    }

    return Promise.reject(error)
  },
)

export interface ApiErrorBody {
  message?: string
  fieldErrors?: { field: string; message: string }[]
}

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined
    return body?.message ?? error.message
  }
  return String(error)
}
