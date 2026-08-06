import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button, Select, Space, Table, Tag, Typography, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { listRecommendations, approveRecommendation, rejectRecommendation } from '../api/pricing'
import { extractErrorMessage } from '../api/client'

const STATUS_OPTIONS = ['REVIEW_REQUIRED', 'READY', 'APPROVED', 'INSUFFICIENT_DATA', 'PUBLISHED', 'REJECTED', 'FAILED']

export default function RecommendationsPage() {
  const { t } = useTranslation()
  const [status, setStatus] = useState<string>('REVIEW_REQUIRED')
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const query = useQuery({
    queryKey: ['recommendations', status, page],
    queryFn: () => listRecommendations(status, page, 20),
  })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['recommendations'] })

  const approveMutation = useMutation({
    mutationFn: approveRecommendation,
    onSuccess: () => {
      message.success(t('common.approved'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const rejectMutation = useMutation({
    mutationFn: (id: number) => rejectRecommendation(id, 'Rejected from recommendations list'),
    onSuccess: () => {
      message.success(t('common.rejected'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  return (
    <div>
      <Typography.Title level={3}>{t('recommendations.title')}</Typography.Title>
      <Select
        value={status}
        style={{ width: 220, marginBottom: 16 }}
        options={STATUS_OPTIONS.map((s) => ({ label: s, value: s }))}
        onChange={(v) => {
          setStatus(v)
          setPage(0)
        }}
      />
      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data?.content ?? []}
        pagination={{ current: page + 1, pageSize: 20, total: query.data?.totalElements ?? 0, onChange: (p) => setPage(p - 1) }}
        columns={[
          {
            title: t('recommendations.colProduct'),
            dataIndex: 'productTitle',
            render: (v: string, record) => <a onClick={() => navigate(`/products/${record.productId}`)}>{v}</a>,
          },
          { title: t('recommendations.colCurrentPrice'), dataIndex: 'currentPrice', render: (v: number | null) => v?.toLocaleString('vi-VN') ?? '-' },
          {
            title: t('recommendations.colSuggestedPrice'),
            dataIndex: 'finalSuggestedPrice',
            render: (v: number | null) => v?.toLocaleString('vi-VN') ?? '-',
          },
          { title: t('recommendations.colSourceCount'), dataIndex: 'includedSourceCount' },
          { title: t('common.status'), dataIndex: 'status', render: (v: string) => <Tag>{v}</Tag> },
          {
            title: t('common.actions'),
            key: 'actions',
            render: (_: unknown, record) => (
              <Space>
                <Button size="small" type="primary" onClick={() => approveMutation.mutate(record.id)}>
                  {t('common.approve')}
                </Button>
                <Button size="small" danger onClick={() => rejectMutation.mutate(record.id)}>
                  {t('common.reject')}
                </Button>
              </Space>
            ),
          },
        ]}
      />
    </div>
  )
}
