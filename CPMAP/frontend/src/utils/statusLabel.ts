import type { TFunction } from 'i18next'

/** Dich 1 gia tri enum (vd "REVIEW_REQUIRED") sang text hien thi qua i18n key `status.<group>.<value>`.
 * Neu chua co ban dich (key la), giu nguyen gia tri goc thay vi de trong. */
export function statusLabel(t: TFunction, group: string, value?: string | null): string {
  if (!value) {
    return '-'
  }
  return t(`status.${group}.${value}`, { defaultValue: value })
}
