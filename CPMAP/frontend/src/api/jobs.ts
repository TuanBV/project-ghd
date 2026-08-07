import { apiClient } from './client'
import type { JobRunDto, PageResponse } from './types'

export async function listJobKeys(): Promise<string[]> {
  const { data } = await apiClient.get<string[]>('/jobs')
  return data
}

export async function triggerJob(jobKey: string): Promise<JobRunDto> {
  const { data } = await apiClient.post<JobRunDto>(`/jobs/${jobKey}/runs`)
  return data
}

export async function getJobHistory(jobKey: string, page = 0, size = 10): Promise<PageResponse<JobRunDto>> {
  const { data } = await apiClient.get<PageResponse<JobRunDto>>(`/jobs/${jobKey}/runs`, { params: { page, size } })
  return data
}

export async function getJobRun(id: number): Promise<JobRunDto> {
  const { data } = await apiClient.get<JobRunDto>(`/job-runs/${id}`)
  return data
}

export async function retryJobRun(id: number): Promise<JobRunDto> {
  const { data } = await apiClient.post<JobRunDto>(`/job-runs/${id}/retries`)
  return data
}
