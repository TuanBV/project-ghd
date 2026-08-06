import { Segmented } from 'antd'
import { useTranslation } from 'react-i18next'
import { SUPPORTED_LANGUAGES } from '../i18n'

/** Cong tac doi ngon ngu hien thi ro rang tren giao dien (khong an trong dropdown). */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation()
  const current = SUPPORTED_LANGUAGES.some((l) => l.code === i18n.language) ? i18n.language : 'vi'

  return (
    <Segmented
      value={current}
      onChange={(value) => i18n.changeLanguage(String(value))}
      options={SUPPORTED_LANGUAGES.map((lang) => ({
        value: lang.code,
        label: lang.code.toUpperCase(),
      }))}
    />
  )
}
