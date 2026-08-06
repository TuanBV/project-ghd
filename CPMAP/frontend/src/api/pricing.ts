import { apiClient } from './client'
import type { PageResponse, PricePolicyDto } from './types'

export interface RecommendationSummary {
  id: number
  productId: number
  productTitle: string
  currentPrice: number | null
  finalSuggestedPrice: number | null
  status: string
  includedSourceCount: number
}

export async function listRecommendations(status?: string, page = 0, size = 20): Promise<PageResponse<RecommendationSummary>> {
  const { data } = await apiClient.get<PageResponse<RecommendationSummary>>('/recommendations', {
    params: { status, page, size },
  })
  return data
}

export async function approveRecommendation(id: number) {
  const { data } = await apiClient.post(`/recommendations/${id}/approve`)
  return data
}

export async function rejectRecommendation(id: number, reason?: string) {
  const { data } = await apiClient.post(`/recommendations/${id}/reject`, { reason })
  return data
}

export async function overrideRecommendation(id: number, price: number, reason: string, expiresAt: string) {
  const { data } = await apiClient.post(`/recommendations/${id}/override`, { price, reason, expiresAt })
  return data
}

export async function recalculateAll() {
  const { data } = await apiClient.post('/pricing/recalculate')
  return data
}

export async function recalculateProduct(productId: number) {
  const { data } = await apiClient.post(`/pricing/products/${productId}/recalculate`)
  return data
}

export async function getGlobalPolicy(): Promise<PricePolicyDto> {
  const { data } = await apiClient.get<PricePolicyDto>('/settings/price-policy')
  return data
}

export async function updateGlobalPolicy(payload: Omit<PricePolicyDto, 'id' | 'scope' | 'category' | 'productId'>) {
  const { data } = await apiClient.put<PricePolicyDto>('/settings/price-policy', payload)
  return data
}

export async function publishWebsite(recommendationIds?: number[]) {
  const { data } = await apiClient.post('/publish/website', { recommendationIds })
  return data
}

export async function publishMerchant(recommendationIds?: number[]) {
  const { data } = await apiClient.post('/publish/merchant', { recommendationIds })
  return data
}

export async function publishFullPipeline(recommendationIds?: number[]) {
  const { data } = await apiClient.post('/publish/full-pipeline', { recommendationIds })
  return data
}
