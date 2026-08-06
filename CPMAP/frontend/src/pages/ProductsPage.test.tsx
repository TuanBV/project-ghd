import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../test/testUtils'
import ProductsPage from './ProductsPage'
import * as productsApi from '../api/products'

describe('ProductsPage', () => {
  it('tim kiem theo tu khoa se goi lai API voi keyword moi', async () => {
    const searchSpy = vi.spyOn(productsApi, 'searchProducts').mockResolvedValue({
      content: [
        {
          id: 1,
          skuOriginal: 'AQT32K85FX',
          title: 'Google Tivi Aqua 32 inch',
          productUrl: null,
          brand: 'Aqua',
          googleCategory: 'TV',
          availability: 'IN_STOCK',
          currentWebsitePrice: 4000000,
          currentMcPrice: 4000000,
          currency: 'VND',
          validCompetitorSourceCount: 0,
          averageCompetitorPrice: null,
          suggestedPrice: null,
          recommendationStatus: 'INSUFFICIENT_DATA',
          hasMatchConflict: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      last: true,
    })

    renderWithProviders(<ProductsPage />)

    await waitFor(() => expect(screen.getByText('Google Tivi Aqua 32 inch')).toBeInTheDocument())

    const searchInput = screen.getByPlaceholderText('Tìm theo SKU, title, URL')
    await userEvent.type(searchInput, 'AQT32K85FX{enter}')

    await waitFor(() => {
      const lastCall = searchSpy.mock.calls.at(-1)?.[0]
      expect(lastCall?.keyword).toBe('AQT32K85FX')
    })
  })
})
