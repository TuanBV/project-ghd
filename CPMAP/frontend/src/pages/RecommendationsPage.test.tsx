import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../test/testUtils'
import RecommendationsPage from './RecommendationsPage'
import * as pricingApi from '../api/pricing'

describe('RecommendationsPage', () => {
  it('cho phep approve mot recommendation', async () => {
    vi.spyOn(pricingApi, 'listRecommendations').mockResolvedValue({
      content: [
        {
          id: 99,
          productId: 1,
          productTitle: 'Tivi Demo A',
          currentPrice: 5000000,
          finalSuggestedPrice: 5030000,
          status: 'REVIEW_REQUIRED',
          includedSourceCount: 2,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      last: true,
    })
    const approveSpy = vi.spyOn(pricingApi, 'approveRecommendation').mockResolvedValue({})

    renderWithProviders(<RecommendationsPage />)

    await waitFor(() => expect(screen.getByText('Tivi Demo A')).toBeInTheDocument())

    await userEvent.click(screen.getByRole('button', { name: 'Duyệt' }))

    await waitFor(() => expect(approveSpy.mock.calls[0]?.[0]).toBe(99))
  })
})
