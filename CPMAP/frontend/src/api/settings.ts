import { apiClient } from './client'

export interface CronDto {
  cron: string
  timezone: string
}

export async function getJobSchedule(): Promise<CronDto> {
  const { data } = await apiClient.get<CronDto>('/settings/job-schedule')
  return data
}

export async function updateJobSchedule(cron: string, timezone: string): Promise<CronDto> {
  const { data } = await apiClient.put<CronDto>('/settings/job-schedule', { cron, timezone })
  return data
}

export async function runCrawl(competitorIds?: number[]) {
  const { data } = await apiClient.post('/crawls', { competitorIds })
  return data
}
