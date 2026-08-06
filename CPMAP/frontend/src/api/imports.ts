import { apiClient } from './client'
import type { ImportIssueDto, ImportRunDto, PageResponse } from './types'

export async function importMcFile(file: File): Promise<ImportRunDto> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await apiClient.post<ImportRunDto>('/imports/mc', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function importComparisonFile(file: File): Promise<ImportRunDto> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await apiClient.post<ImportRunDto>('/imports/comparison', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function getImportRun(id: number): Promise<ImportRunDto> {
  const { data } = await apiClient.get<ImportRunDto>(`/imports/${id}`)
  return data
}

export async function getImportIssues(id: number, page = 0, size = 50): Promise<PageResponse<ImportIssueDto>> {
  const { data } = await apiClient.get<PageResponse<ImportIssueDto>>(`/imports/${id}/issues`, {
    params: { page, size },
  })
  return data
}
