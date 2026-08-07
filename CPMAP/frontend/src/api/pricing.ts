import { apiClient } from './client'
import type { PricePolicyDto } from './types'

export async function approveRecommendation(id: number) {
  const { data } = await apiClient.patch(`/recommendations/${id}`, { status: 'APPROVED' })
  return data
}

export async function rejectRecommendation(id: number, reason?: string) {
  const { data } = await apiClient.patch(`/recommendations/${id}`, { status: 'REJECTED', reason })
  return data
}

export async function overrideRecommendation(id: number, price: number, reason: string, expiresAt: string) {
  const { data } = await apiClient.put(`/recommendations/${id}/override`, { price, reason, expiresAt })
  return data
}

export async function recalculateAll() {
  const { data } = await apiClient.post('/recommendations/recalculations')
  return data
}

export async function recalculateProduct(productId: number) {
  const { data } = await apiClient.post(`/products/${productId}/recommendations`)
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
  const { data } = await apiClient.post('/publish/website-runs', { recommendationIds })
  return data
}

export async function publishMerchant(recommendationIds?: number[]) {
  const { data } = await apiClient.post('/publish/merchant-runs', { recommendationIds })
  return data
}

export async function publishFullPipeline(recommendationIds?: number[]) {
  const { data } = await apiClient.post('/publish/pipeline-runs', { recommendationIds })
  return data
}
