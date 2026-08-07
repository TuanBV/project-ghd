import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Space, Table, Tag, Typography, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { listJobKeys, triggerJob, getJobHistory, retryJobRun } from '../api/jobs'
import { extractErrorMessage } from '../api/client'
import { statusLabel } from '../utils/statusLabel'
import { formatDateTime } from '../utils/formatDateTime'

function statusColor(status: string): string {
  switch (status) {
    case 'SUCCESS':
      return 'green'
    case 'PARTIAL_SUCCESS':
      return 'orange'
    case 'FAILED':
      return 'red'
    case 'RUNNING':
      return 'blue'
    default:
      return 'default'
  }
}

export default function JobsPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const jobsQuery = useQuery({ queryKey: ['jobs'], queryFn: listJobKeys })

  return (
    <div>
      <Typography.Title level={3}>{t('jobs.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('jobs.pageHint')}</Typography.Paragraph>
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
  const jobName = t(`jobs.names.${jobKey}`, { defaultValue: jobKey })
  const jobDescription = t(`jobs.descriptions.${jobKey}`, { defaultValue: '' })

  return (
    <Card
      title={
        <Space direction="vertical" size={0} style={{ padding: '4px 0' }}>
          <Typography.Text strong style={{ fontSize: 15 }}>{jobName}</Typography.Text>
          {jobDescription && (
            <Typography.Text type="secondary" style={{ fontWeight: 'normal', fontSize: 13 }}>
              {jobDescription}
            </Typography.Text>
          )}
        </Space>
      }
      extra={
        <Button type="primary" onClick={() => triggerMutation.mutate()} loading={triggerMutation.isPending}>
          {t('jobs.runNow')}
        </Button>
      }
    >
      {latest ? (
        <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }} size={6}>
          <Space size="middle" wrap>
            <Tag color={statusColor(latest.status)} style={{ fontSize: 13, padding: '2px 10px' }}>
              {statusLabel(t, 'run', latest.status)}
            </Tag>
            <Typography.Text type="secondary">
              {t('jobs.lastRun')}: {formatDateTime(latest.startedAt)} ({statusLabel(t, 'trigger', latest.triggerType)})
            </Typography.Text>
            <Typography.Text type="secondary">
              {t('jobs.successFailedInline', { success: latest.successItems, failed: latest.failedItems })}
            </Typography.Text>
          </Space>
          {latest.errorDetail && <Typography.Text type="danger">{latest.errorDetail}</Typography.Text>}
        </Space>
      ) : (
        <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 16 }}>
          {t('jobs.noRunsYet')}
        </Typography.Text>
      )}
      <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
        {t('jobs.historyTitle')}
      </Typography.Text>
      <Table
        rowKey="id"
        size="small"
        loading={historyQuery.isLoading}
        dataSource={historyQuery.data?.content ?? []}
        pagination={false}
        columns={[
          { title: t('jobs.colId'), dataIndex: 'id', width: 70 },
          {
            title: t('common.status'),
            dataIndex: 'status',
            render: (v: string) => <Tag color={statusColor(v)}>{statusLabel(t, 'run', v)}</Tag>,
          },
          { title: t('jobs.colStartedAt'), dataIndex: 'startedAt', render: (v: string) => formatDateTime(v) },
          { title: t('jobs.colFinishedAt'), dataIndex: 'finishedAt', render: (v: string) => formatDateTime(v) },
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
