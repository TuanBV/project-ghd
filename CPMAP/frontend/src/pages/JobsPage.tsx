import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Descriptions, Space, Table, Tag, Typography, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { listJobKeys, triggerJob, getJobHistory, retryJobRun } from '../api/jobs'
import { extractErrorMessage } from '../api/client'

export default function JobsPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const jobsQuery = useQuery({ queryKey: ['jobs'], queryFn: listJobKeys })

  return (
    <div>
      <Typography.Title level={3}>{t('jobs.title')}</Typography.Title>
      <Space direction="vertical" style={{ width: '100%' }} size="large">
        {(jobsQuery.data ?? []).map((jobKey) => (
          <JobCard key={jobKey} jobKey={jobKey} onChanged={() => queryClient.invalidateQueries({ queryKey: ['job-history', jobKey] })} />
        ))}
      </Space>
    </div>
  )
}

function JobCard({ jobKey, onChanged }: { jobKey: string; onChanged: () => void }) {
  const { t } = useTranslation()
  const historyQuery = useQuery({ queryKey: ['job-history', jobKey], queryFn: () => getJobHistory(jobKey, 0, 5) })

  const triggerMutation = useMutation({
    mutationFn: () => triggerJob(jobKey),
    onSuccess: (data) => {
      message.success(t('jobs.triggered', { id: data.id }))
      onChanged()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const retryMutation = useMutation({
    mutationFn: retryJobRun,
    onSuccess: () => {
      message.success(t('jobs.retried'))
      onChanged()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const latest = historyQuery.data?.content?.[0]

  return (
    <Card
      title={jobKey}
      extra={
        <Button type="primary" onClick={() => triggerMutation.mutate()} loading={triggerMutation.isPending}>
          {t('jobs.runNow')}
        </Button>
      }
    >
      {latest && (
        <Descriptions column={4} size="small" bordered style={{ marginBottom: 12 }}>
          <Descriptions.Item label={t('common.status')}><Tag>{latest.status}</Tag></Descriptions.Item>
          <Descriptions.Item label={t('jobs.progress')}>{latest.progressPercent}%</Descriptions.Item>
          <Descriptions.Item label={t('jobs.successFailed')}>{latest.successItems}/{latest.failedItems}</Descriptions.Item>
          <Descriptions.Item label={t('jobs.trigger')}>{latest.triggerType}</Descriptions.Item>
          {latest.errorDetail && (
            <Descriptions.Item label={t('jobs.error')} span={4}>
              {latest.errorDetail}
            </Descriptions.Item>
          )}
        </Descriptions>
      )}
      <Table
        rowKey="id"
        size="small"
        loading={historyQuery.isLoading}
        dataSource={historyQuery.data?.content ?? []}
        pagination={false}
        columns={[
          { title: t('jobs.colId'), dataIndex: 'id' },
          { title: t('common.status'), dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> },
          { title: t('jobs.colStartedAt'), dataIndex: 'startedAt' },
          { title: t('jobs.colFinishedAt'), dataIndex: 'finishedAt' },
          {
            title: t('common.actions'),
            key: 'actions',
            render: (_: unknown, record) =>
              record.status === 'FAILED' || record.status === 'PARTIAL_SUCCESS' ? (
                <Button size="small" onClick={() => retryMutation.mutate(record.id)}>
                  {t('common.retry')}
                </Button>
              ) : null,
          },
        ]}
      />
    </Card>
  )
}
