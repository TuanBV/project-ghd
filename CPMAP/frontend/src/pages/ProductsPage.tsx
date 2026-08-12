import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Input, Select, Table, Tag, Typography, Space } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { searchProducts } from '../api/products'
import type { ProductSummary } from '../api/types'
import { statusLabel } from '../utils/statusLabel'

const AVAILABILITY_OPTIONS = ['IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN']

export default function ProductsPage() {
  const { t } = useTranslation()
  const [keyword, setKeyword] = useState('')
  const [availability, setAvailability] = useState<string | undefined>('IN_STOCK')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const competitorIdParam = searchParams.get('competitorId')
  const competitorId = competitorIdParam ? Number(competitorIdParam) : undefined
  const competitorName = searchParams.get('competitorName') ?? undefined

  const query = useQuery({
    queryKey: ['products', keyword, availability, competitorId, page, pageSize],
    queryFn: () => searchProducts({ keyword, availability, competitorId, page, size: pageSize }),
  })

  const columns = [
    { title: t('products.colSku'), dataIndex: 'skuOriginal', key: 'sku' },
    { title: t('products.colTitle'), dataIndex: 'title', key: 'title' },
    {
      title: t('products.colCurrentPrice'),
      dataIndex: 'currentWebsitePrice',
      key: 'currentWebsitePrice',
      render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
    },
    {
      title: t('products.colAvgCompetitorPrice'),
      dataIndex: 'averageCompetitorPrice',
      key: 'averageCompetitorPrice',
      render: (v: number | null) =>
        v == null ? (
          <Typography.Text type="secondary">-</Typography.Text>
        ) : (
          <Typography.Text strong style={{ color: '#389e0d' }}>
            {v.toLocaleString('vi-VN')}
          </Typography.Text>
        ),
    },
    {
      title: t('common.status'),
      dataIndex: 'recommendationStatus',
      key: 'recommendationStatus',
      render: (status: string | null, record: ProductSummary) => {
        const needsConfirmation = record.hasMatchConflict || status === 'INSUFFICIENT_DATA'
        if (needsConfirmation) {
          return <Tag color="orange">{t('products.matchConflict')}</Tag>
        }
        return status ? <Tag color={statusColor(status)}>{statusLabel(t, 'recommendation', status)}</Tag> : null
      },
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>{t('products.title')}</Typography.Title>
      {competitorId && (
        <Alert
          style={{ marginBottom: 16 }}
          type="info"
          showIcon
          message={t('products.filteredByCompetitor', {
            name: competitorName ?? competitorId,
            count: query.data?.totalElements ?? 0,
          })}
          action={
            <Button size="small" onClick={() => setSearchParams({})}>
              {t('products.clearCompetitorFilter')}
            </Button>
          }
        />
      )}
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder={t('products.searchPlaceholder')}
          allowClear
          style={{ width: 320 }}
          onSearch={(v) => {
            setKeyword(v)
            setPage(0)
          }}
        />
        <Select
          placeholder={t('products.availabilityPlaceholder')}
          allowClear
          value={availability}
          style={{ width: 180 }}
          options={AVAILABILITY_OPTIONS.map((a) => ({ label: statusLabel(t, 'availability', a), value: a }))}
          onChange={(v) => {
            setAvailability(v)
            setPage(0)
          }}
        />
      </Space>
      <Table
        rowKey="id"
        loading={query.isLoading}
        columns={columns}
        dataSource={query.data?.content ?? []}
        onRow={(record) => ({ onClick: () => navigate(`/products/${record.id}`) })}
        rowClassName={() => 'mc-clickable-row'}
        pagination={{
          current: page + 1,
          pageSize,
          total: query.data?.totalElements ?? 0,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          onChange: (p, size) => {
            // Doi so ban ghi/trang thi ve lai trang 1, tranh sai lech offset voi pageSize moi.
            setPage(size !== pageSize ? 0 : p - 1)
            setPageSize(size)
          },
        }}
      />
    </div>
  )
}

function statusColor(status: string): string {
  switch (status) {
    case 'READY':
      return 'blue'
    case 'APPROVED':
      return 'green'
    case 'PUBLISHED':
      return 'purple'
    case 'REVIEW_REQUIRED':
      return 'orange'
    case 'INSUFFICIENT_DATA':
      return 'default'
    case 'REJECTED':
    case 'FAILED':
      return 'red'
    default:
      return 'default'
  }
}
