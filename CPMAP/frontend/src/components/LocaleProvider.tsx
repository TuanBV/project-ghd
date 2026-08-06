import { ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import enUS from 'antd/locale/en_US'
import { useTranslation } from 'react-i18next'
import type { ReactNode } from 'react'

const ANTD_LOCALES: Record<string, typeof viVN> = {
  vi: viVN,
  en: enUS,
}

/** Dong bo locale cua AntD (date picker, pagination...) voi ngon ngu dang chon trong i18next. */
export default function LocaleProvider({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation()
  const antdLocale = ANTD_LOCALES[i18n.language] ?? viVN

  return (
    <ConfigProvider locale={antdLocale} theme={{ token: { colorPrimary: '#1668dc' } }}>
      {children}
    </ConfigProvider>
  )
}
