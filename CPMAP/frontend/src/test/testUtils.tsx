import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { I18nextProvider, initReactI18next } from 'react-i18next'
import i18next from 'i18next'
import vi from '../i18n/locales/vi.json'
import en from '../i18n/locales/en.json'

// i18n instance rieng cho test, co dinh ngon ngu 'vi' de assertion khong phu thuoc vao
// navigator.language cua moi trung jsdom (khac voi instance chinh trong src/i18n co auto-detect).
const testI18n = i18next.createInstance()
testI18n.use(initReactI18next).init({
  resources: { vi: { translation: vi }, en: { translation: en } },
  lng: 'vi',
  fallbackLng: 'vi',
  interpolation: { escapeValue: false },
})

export function renderWithProviders(ui: ReactElement, initialEntries: string[] = ['/']) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <I18nextProvider i18n={testI18n}>
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
      </QueryClientProvider>
    </I18nextProvider>,
  )
}
