import { BrowserRouter, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import RequireAuth from './components/RequireAuth'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ProductsPage from './pages/ProductsPage'
import ProductDetailPage from './pages/ProductDetailPage'
import RecommendationsPage from './pages/RecommendationsPage'
import CompetitorsPage from './pages/CompetitorsPage'
import ImportsPage from './pages/ImportsPage'
import JobsPage from './pages/JobsPage'
import SettingsPage from './pages/SettingsPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="products" element={<ProductsPage />} />
          <Route path="products/:id" element={<ProductDetailPage />} />
          <Route path="recommendations" element={<RecommendationsPage />} />
          <Route path="competitors" element={<CompetitorsPage />} />
          <Route path="imports" element={<ImportsPage />} />
          <Route path="jobs" element={<JobsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
