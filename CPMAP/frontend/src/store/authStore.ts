import { create } from 'zustand'

export interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  username: string | null
  role: string | null
  login: (accessToken: string, refreshToken: string, username: string, role: string) => void
  logout: () => void
}

const STORAGE_KEY = 'mcprice.auth'

function loadInitial(): Pick<AuthState, 'accessToken' | 'refreshToken' | 'username' | 'role'> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return { accessToken: null, refreshToken: null, username: null, role: null }
    }
    return JSON.parse(raw)
  } catch {
    return { accessToken: null, refreshToken: null, username: null, role: null }
  }
}

export const useAuthStore = create<AuthState>((set) => ({
  ...loadInitial(),
  login: (accessToken, refreshToken, username, role) => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ accessToken, refreshToken, username, role }))
    set({ accessToken, refreshToken, username, role })
  },
  logout: () => {
    localStorage.removeItem(STORAGE_KEY)
    set({ accessToken: null, refreshToken: null, username: null, role: null })
  },
}))
