import { apiClient } from './client'
import type { DashboardSummary } from './types'

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await apiClient.get<DashboardSummary>('/dashboard/summary')
  return data
}

export interface CategoryPrice {
  avgCurrentPrice: number
  avgSuggestedPrice: number
  productCount: number
}

export interface TopMover {
  productId: number
  title: string
  currentPrice: number
  suggestedPrice: number
  percentChange: number
}

export interface PriceTrends {
  percentChangeDistribution: Record<string, number>
  currentVsSuggestedByCategory: Record<string, CategoryPrice>
  topIncreasing: TopMover[]
  topDecreasing: TopMover[]
  categoriesWithMostMissingData: Record<string, number>
}

export async function getPriceTrends(): Promise<PriceTrends> {
  const { data } = await apiClient.get<PriceTrends>('/dashboard/price-trends')
  return data
}

export interface CompetitorHealth {
  crawlSuccessRateByCompetitor: Record<string, number>
  confirmedListingCountByCompetitor: Record<string, number>
  dailyCrawlSuccessRate: Record<string, number>
  lastErrorByCompetitor: Record<string, string>
}

export async function getCompetitorHealth(): Promise<CompetitorHealth> {
  const { data } = await apiClient.get<CompetitorHealth>('/dashboard/competitor-health')
  return data
}
