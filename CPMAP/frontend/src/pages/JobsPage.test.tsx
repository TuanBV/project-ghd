import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '../test/testUtils'
import JobsPage from './JobsPage'
import * as jobsApi from '../api/jobs'

describe('JobsPage', () => {
  it('hien thi tien do job gan nhat', async () => {
    vi.spyOn(jobsApi, 'listJobKeys').mockResolvedValue(['CompetitorCrawlJob'])
    vi.spyOn(jobsApi, 'getJobHistory').mockResolvedValue({
      content: [
        {
          id: 1,
          jobKey: 'CompetitorCrawlJob',
          triggerType: 'MANUAL',
          status: 'PARTIAL_SUCCESS',
          totalItems: 10,
          successItems: 7,
          failedItems: 3,
          progressPercent: 100,
          startedAt: '2026-08-03T02:00:00Z',
          finishedAt: '2026-08-03T02:05:00Z',
          errorDetail: null,
          correlationId: 'abc',
        },
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
      last: true,
    })

    renderWithProviders(<JobsPage />)

    expect(await screen.findByText('CompetitorCrawlJob')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByText('100%')).toBeInTheDocument())
    expect(screen.getAllByText('PARTIAL_SUCCESS').length).toBeGreaterThan(0)
  })
})
