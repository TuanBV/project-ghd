import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Input, Select, Table, Tag, Typography, Space } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { searchProducts } from '../api/products'
import type { ProductSummary } from '../api/types'
import { statusLabel } from '../utils/statusLabel'

const AVAILABILITY_OPTIONS = ['IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN']

export default function ProductsPage() {
  const { t } = useTranslation()
  const [keyword, setKeyword] = useState('')
  const [availability, setAvailability] = useState<string | undefined>()
  const [page, setPage] = useState(0)
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const competitorIdParam = searchParams.get('competitorId')
  const competitorId = competitorIdParam ? Number(competitorIdParam) : undefined
  const competitorName = searchParams.get('competitorName') ?? undefined

  const query = useQuery({
    queryKey: ['products', keyword, availability, competitorId, page],
    queryFn: () => searchProducts({ keyword, availability, competitorId, page, size: 20 }),
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
      title: t('products.colMatchStatus'),
      key: 'matchStatus',
      render: (_: unknown, record: ProductSummary) =>
        record.validCompetitorSourceCount > 0 ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            {t('products.matchedTag', { count: record.validCompetitorSourceCount })}
          </Tag>
        ) : (
          <Tag>{t('products.notMatchedTag')}</Tag>
        ),
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
      title: t('products.colSuggestedPrice'),
      dataIndex: 'suggestedPrice',
      key: 'suggestedPrice',
      render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
    },
    { title: t('products.colSourceCount'), dataIndex: 'validCompetitorSourceCount', key: 'validCompetitorSourceCount' },
    {
      title: t('common.status'),
      dataIndex: 'recommendationStatus',
      key: 'recommendationStatus',
      render: (status: string | null, record: ProductSummary) => (
        <Space>
          {status && <Tag color={statusColor(status)}>{statusLabel(t, 'recommendation', status)}</Tag>}
          {record.hasMatchConflict && <Tag color="red">{t('products.matchConflict')}</Tag>}
        </Space>
      ),
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
          pageSize: 20,
          total: query.data?.totalElements ?? 0,
          onChange: (p) => setPage(p - 1),
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
