import { apiClient } from './client'
import type { AliasDto, CompetitorListingDto, PageResponse, ProductDetail, ProductSummary } from './types'

export interface ProductSearchParams {
  keyword?: string
  category?: string
  availability?: string
  competitorId?: number
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

export interface ProductUpdatePayload {
  title: string | null
  brand: string | null
  googleCategory: string | null
  productType: string | null
  currentWebsitePrice: number | null
  active: boolean
  availability: string | null
  productUrl: string | null
}

export async function updateProduct(id: number, payload: ProductUpdatePayload): Promise<ProductDetail> {
  const { data } = await apiClient.put<ProductDetail>(`/products/${id}`, payload)
  return data
}

export async function createAlias(
  productId: number,
  payload: { aliasOriginal: string; aliasType: string; confirmed: boolean },
): Promise<AliasDto> {
  const { data } = await apiClient.post<AliasDto>(`/products/${productId}/aliases`, payload)
  return data
}

export async function confirmMatch(productId: number, matchId: number): Promise<CompetitorListingDto> {
  const { data } = await apiClient.patch<CompetitorListingDto>(`/products/${productId}/matches/${matchId}`, {
    status: 'MANUALLY_CONFIRMED',
  })
  return data
}

export async function rejectMatch(productId: number, matchId: number, reason?: string) {
  const { data } = await apiClient.patch(`/products/${productId}/matches/${matchId}`, { status: 'REJECTED', reason })
  return data
}

export async function refreshListingPrice(productId: number, matchId: number): Promise<CompetitorListingDto> {
  const { data } = await apiClient.post<CompetitorListingDto>(`/products/${productId}/matches/${matchId}/refresh-price`)
  return data
}

export async function submitManualPrice(
  productId: number,
  payload: { competitorListingId: number; price?: number; contactOnly?: boolean; note?: string },
) {
  const { data } = await apiClient.post(`/products/${productId}/manual-prices`, payload)
  return data
}

export async function getListingCandidates(productId: number, sku: string): Promise<CompetitorListingDto[]> {
  const { data } = await apiClient.get<CompetitorListingDto[]>(`/products/${productId}/listing-candidates`, {
    params: { sku },
  })
  return data
}

export async function claimListingCandidate(productId: number, listingId: number): Promise<CompetitorListingDto> {
  const { data } = await apiClient.put<CompetitorListingDto>(`/products/${productId}/listing-candidates/${listingId}`)
  return data
}
