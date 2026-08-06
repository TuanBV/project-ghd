import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '../test/testUtils'
import ImportsPage from './ImportsPage'
import * as importsApi from '../api/imports'

describe('ImportsPage', () => {
  it('hien thi bang issue sau khi import xong', async () => {
    vi.spyOn(importsApi, 'importMcFile').mockResolvedValue({
      id: 42,
      importType: 'MC',
      fileName: 'MC.xlsx',
      status: 'PARTIAL_SUCCESS',
      totalRows: 3265,
      successRows: 3255,
      issueRows: 10,
      startedAt: '2026-08-03T00:00:00Z',
      finishedAt: '2026-08-03T00:01:00Z',
    })
    vi.spyOn(importsApi, 'getImportIssues').mockResolvedValue({
      content: [
        { id: 1, importRowId: 5, rowNumber: 5, issueType: 'MISSING_ITEM_GROUP_ID', severity: 'WARNING', message: 'Thieu item_group_id', resolved: false },
      ],
      page: 0,
      size: 100,
      totalElements: 1,
      totalPages: 1,
      last: true,
    })

    renderWithProviders(<ImportsPage />)

    const inputs = document.querySelectorAll('input[type="file"]')
    const mcInput = inputs[0] as HTMLInputElement
    const file = new File(['dummy'], 'MC.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    await userEvent.upload(mcInput, file)

    await waitFor(() => expect(screen.getByText('MISSING_ITEM_GROUP_ID')).toBeInTheDocument(), { timeout: 3000 })
  })
})
