import { apiClient } from './client'
import type { AliasDto, PageResponse, ProductDetail, ProductSummary } from './types'

export interface ProductSearchParams {
  keyword?: string
  category?: string
  availability?: string
  page?: number
  size?: number
}

export async function searchProducts(params: ProductSearchParams): Promise<PageResponse<ProductSummary>> {
  const { data } = await apiClient.get<PageResponse<ProductSummary>>('/products', { params })
  return data
}

export async function getProductDetail(id: number): Promise<ProductDetail> {
  const { data } = await apiClient.get<ProductDetail>(`/products/${id}`)
  return data
}

export async function createAlias(
  productId: number,
  payload: { aliasOriginal: string; aliasType: string; confirmed: boolean },
): Promise<AliasDto> {
  const { data } = await apiClient.post<AliasDto>(`/products/${productId}/aliases`, payload)
  return data
}

export async function confirmMatch(productId: number, matchId: number) {
  const { data } = await apiClient.post(`/products/${productId}/matches/${matchId}/confirm`)
  return data
}

export async function rejectMatch(productId: number, matchId: number, reason?: string) {
  const { data } = await apiClient.post(`/products/${productId}/matches/${matchId}/reject`, { reason })
  return data
}

export async function submitManualPrice(
  productId: number,
  payload: { competitorListingId: number; price?: number; contactOnly?: boolean; note?: string },
) {
  const { data } = await apiClient.post(`/products/${productId}/manual-price`, payload)
  return data
}
