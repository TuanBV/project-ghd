import { apiClient } from './client'
import type { CompetitorDto, CompetitorListingDto, PageResponse } from './types'

export interface CompetitorUpsertPayload {
  name: string
  baseUrl: string
  enabled: boolean
  crawlMode: string
  requestsPerMinute: number
  timeoutSeconds: number
  extractorConfig?: Record<string, unknown>
}

export async function listCompetitors(): Promise<CompetitorDto[]> {
  const { data } = await apiClient.get<CompetitorDto[]>('/competitors')
  return data
}

export async function createCompetitor(payload: CompetitorUpsertPayload): Promise<CompetitorDto> {
  const { data } = await apiClient.post<CompetitorDto>('/competitors', payload)
  return data
}

export async function updateCompetitor(id: number, payload: CompetitorUpsertPayload): Promise<CompetitorDto> {
  const { data } = await apiClient.put<CompetitorDto>(`/competitors/${id}`, payload)
  return data
}

export async function deleteCompetitor(id: number): Promise<void> {
  await apiClient.delete(`/competitors/${id}`)
}

export async function triggerDiscovery(id: number): Promise<{ jobRunId: number }> {
  const { data } = await apiClient.post<{ jobRunId: number }>(`/competitors/${id}/discovery-jobs`)
  return data
}

export async function triggerPriceSync(id: number): Promise<{ jobRunId: number }> {
  const { data } = await apiClient.post<{ jobRunId: number }>(`/competitors/${id}/price-sync-jobs`)
  return data
}

export async function getCompetitorListings(
  id: number,
  page = 0,
  size = 20,
  sku?: string,
): Promise<PageResponse<CompetitorListingDto>> {
  const { data } = await apiClient.get<PageResponse<CompetitorListingDto>>(`/competitors/${id}/listings`, {
    params: { page, size, sku: sku || undefined },
  })
  return data
}

export async function addCompetitorListing(
  competitorId: number,
  payload: { productId: number; url: string },
): Promise<CompetitorListingDto> {
  const { data } = await apiClient.post<CompetitorListingDto>(`/competitors/${competitorId}/listings`, payload)
  return data
}
