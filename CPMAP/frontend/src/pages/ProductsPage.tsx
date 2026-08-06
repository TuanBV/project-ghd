import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Input, Select, Table, Tag, Typography, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { searchProducts } from '../api/products'
import type { ProductSummary } from '../api/types'

const AVAILABILITY_OPTIONS = ['IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN']

export default function ProductsPage() {
  const { t } = useTranslation()
  const [keyword, setKeyword] = useState('')
  const [availability, setAvailability] = useState<string | undefined>()
  const [page, setPage] = useState(0)
  const navigate = useNavigate()

  const query = useQuery({
    queryKey: ['products', keyword, availability, page],
    queryFn: () => searchProducts({ keyword, availability, page, size: 20 }),
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
      render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
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
          {status && <Tag color={statusColor(status)}>{status}</Tag>}
          {record.hasMatchConflict && <Tag color="red">CONFLICT</Tag>}
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>{t('products.title')}</Typography.Title>
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
          options={AVAILABILITY_OPTIONS.map((a) => ({ label: a, value: a }))}
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
