import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import type { ReactElement } from 'react'

export default function RequireAuth({ children }: { children: ReactElement }) {
  const accessToken = useAuthStore((s) => s.accessToken)
  const location = useLocation()

  if (!accessToken) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return children
}
