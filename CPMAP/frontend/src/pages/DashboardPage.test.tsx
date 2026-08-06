import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../test/testUtils'
import DashboardPage from './DashboardPage'
import * as dashboardApi from '../api/dashboard'

vi.mock('echarts-for-react', () => ({
  default: () => <div data-testid="mock-echart" />,
}))

describe('DashboardPage', () => {
  it('hien thi cac KPI chinh sau khi tai du lieu', async () => {
    vi.spyOn(dashboardApi, 'getDashboardSummary').mockResolvedValue({
      totalMcProducts: 3265,
      matchedProducts: 825,
      unmatchedProducts: 1303,
      conflictProducts: 10,
      productsBySourceCount: { '0': 3200, '1': 50, '2': 10, '3+': 5 },
      recommendationsByStatus: { INSUFFICIENT_DATA: 3200, REVIEW_REQUIRED: 50, READY: 10, APPROVED: 3, PUBLISHED: 2, REJECTED: 0, FAILED: 0 },
      priceIncreasedCount: 5,
      priceDecreasedCount: 3,
      priceUnchangedCount: 1,
      totalPriceDifference: 1000000,
      averagePriceDifference: 200000,
      crawlSuccessRateByCompetitor: { 'sgt.com.vn': 0.9 },
      staleObservationCount: 2,
      lastJobRun: { jobKey: 'CompetitorCrawlJob', status: 'SUCCESS', finishedAt: '2026-08-03T02:00:00Z' },
      lastJobError: null,
      merchantSyncSuccessCount: 4,
      merchantSyncFailedCount: 1,
    })
    vi.spyOn(dashboardApi, 'getPriceTrends').mockResolvedValue({
      percentChangeDistribution: {},
      currentVsSuggestedByCategory: {},
      topIncreasing: [],
      topDecreasing: [],
      categoriesWithMostMissingData: {},
    })
    vi.spyOn(dashboardApi, 'getCompetitorHealth').mockResolvedValue({
      crawlSuccessRateByCompetitor: {},
      confirmedListingCountByCompetitor: {},
      dailyCrawlSuccessRate: {},
      lastErrorByCompetitor: {},
    })

    renderWithProviders(<DashboardPage />)

    await waitFor(() => expect(screen.getByText('3,265')).toBeInTheDocument())
    expect(screen.getByText('825')).toBeInTheDocument()
    expect(screen.getByText('1,303')).toBeInTheDocument()
  })
})
