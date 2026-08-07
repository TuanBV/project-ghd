import dayjs from 'dayjs'

/** Rut gon datetime day du (co mili-giay + timezone) ve dang "yyyy-MM-dd HH:mm:ss" de de doc tren bang. */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '-'
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : value
}
