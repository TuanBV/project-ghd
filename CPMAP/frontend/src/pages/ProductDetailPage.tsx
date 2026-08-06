import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useTranslation } from 'react-i18next'
import { getProductDetail, confirmMatch, rejectMatch, submitManualPrice } from '../api/products'
import { approveRecommendation, rejectRecommendation, overrideRecommendation, recalculateProduct } from '../api/pricing'
import { extractErrorMessage } from '../api/client'
import type { CompetitorListingDto } from '../api/types'

export default function ProductDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)
  const queryClient = useQueryClient()
  const [manualPriceListing, setManualPriceListing] = useState<CompetitorListingDto | null>(null)
  const [overrideOpen, setOverrideOpen] = useState(false)

  const query = useQuery({ queryKey: ['product', productId], queryFn: () => getProductDetail(productId) })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['product', productId] })

  const confirmMutation = useMutation({
    mutationFn: (matchId: number) => confirmMatch(productId, matchId),
    onSuccess: () => {
      message.success(t('productDetail.matchConfirmed'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const rejectMutation = useMutation({
    mutationFn: (matchId: number) => rejectMatch(productId, matchId, 'Rejected from UI'),
    onSuccess: () => {
      message.success(t('productDetail.matchRejected'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const recalcMutation = useMutation({
    mutationFn: () => recalculateProduct(productId),
    onSuccess: () => {
      message.success(t('productDetail.recalculated'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const approveMutation = useMutation({
    mutationFn: (recId: number) => approveRecommendation(recId),
    onSuccess: () => {
      message.success(t('common.approved'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const rejectRecMutation = useMutation({
    mutationFn: (recId: number) => rejectRecommendation(recId, 'Rejected from UI'),
    onSuccess: () => {
      message.success(t('common.rejected'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const manualPriceMutation = useMutation({
    mutationFn: (values: { price?: number; contactOnly?: boolean; note?: string }) =>
      submitManualPrice(productId, { competitorListingId: manualPriceListing!.id, ...values }),
    onSuccess: () => {
      message.success(t('productDetail.manualPriceSaved'))
      setManualPriceListing(null)
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const overrideMutation = useMutation({
    mutationFn: (values: { price: number; reason: string; expiresAt: string }) =>
      overrideRecommendation(query.data!.latestRecommendation!.id, values.price, values.reason, values.expiresAt),
    onSuccess: () => {
      message.success(t('productDetail.overrideSaved'))
      setOverrideOpen(false)
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  if (query.isLoading) {
    return <Spin size="large" style={{ marginTop: 80, display: 'block', textAlign: 'center' }} />
  }
  if (query.isError || !query.data) {
    return <Alert type="error" message={extractErrorMessage(query.error)} showIcon />
  }
  const product = query.data
  const rec = product.latestRecommendation

  const listingColumns = [
    { title: t('productDetail.colCompetitor'), dataIndex: 'competitorName', key: 'competitorName' },
    { title: 'URL', dataIndex: 'url', key: 'url', render: (v: string) => <a href={v} target="_blank" rel="noreferrer">{v}</a> },
    { title: t('productDetail.colMethod'), dataIndex: 'matchMethod', key: 'matchMethod' },
    { title: t('common.status'), dataIndex: 'matchStatus', key: 'matchStatus', render: (v: string) => <Tag>{v}</Tag> },
    {
      title: t('common.actions'),
      key: 'actions',
      render: (_: unknown, record: CompetitorListingDto) => (
        <Space>
          {record.matchStatus === 'REVIEW_REQUIRED' && (
            <>
              <Button size="small" onClick={() => confirmMutation.mutate(record.id)}>{t('common.confirm')}</Button>
              <Button size="small" danger onClick={() => rejectMutation.mutate(record.id)}>{t('common.reject')}</Button>
            </>
          )}
          <Button size="small" onClick={() => setManualPriceListing(record)}>{t('productDetail.manualPriceBtn')}</Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Typography.Title level={3}>{product.title}</Typography.Title>

      <Card title={t('productDetail.infoTitle')} style={{ marginBottom: 16 }}>
        <Descriptions column={2} bordered size="small">
          <Descriptions.Item label={t('productDetail.sku')}>{product.skuOriginal}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.mcOfferId')}>{product.mcOfferId}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.brand')}>{product.brand}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.category')}>{product.googleCategory}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.currentWebsitePrice')}>{product.currentWebsitePrice?.toLocaleString('vi-VN')}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.currentMcPrice')}>{product.currentMcPrice?.toLocaleString('vi-VN')}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.availability')}>{product.availability}</Descriptions.Item>
          <Descriptions.Item label={t('productDetail.url')}>
            {product.productUrl && <a href={product.productUrl} target="_blank" rel="noreferrer">{product.productUrl}</a>}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={t('productDetail.listingsTitle')} style={{ marginBottom: 16 }}>
        <Table rowKey="id" columns={listingColumns} dataSource={product.competitorListings} pagination={false} />
      </Card>

      <Card
        title={t('productDetail.recommendationTitle')}
        style={{ marginBottom: 16 }}
        extra={<Button onClick={() => recalcMutation.mutate()} loading={recalcMutation.isPending}>{t('productDetail.recalculate')}</Button>}
      >
        {!rec && <Typography.Text type="secondary">{t('productDetail.noRecommendation')}</Typography.Text>}
        {rec && (
          <>
            <Descriptions column={2} bordered size="small" style={{ marginBottom: 16 }}>
              <Descriptions.Item label={t('common.status')}><Tag>{rec.status}</Tag></Descriptions.Item>
              <Descriptions.Item label={t('productDetail.sourceCount')}>{rec.includedSourceCount}</Descriptions.Item>
              <Descriptions.Item label={t('productDetail.currentWebsitePrice')}>{rec.currentPrice?.toLocaleString('vi-VN')}</Descriptions.Item>
              <Descriptions.Item label={t('productDetail.rawAverage')}>{rec.rawAveragePrice?.toLocaleString('vi-VN')}</Descriptions.Item>
              <Descriptions.Item label={t('productDetail.rounded')}>{rec.roundedPrice?.toLocaleString('vi-VN')}</Descriptions.Item>
              <Descriptions.Item label={t('productDetail.finalPrice')}>{rec.finalSuggestedPrice?.toLocaleString('vi-VN')}</Descriptions.Item>
              {rec.overridePrice != null && (
                <Descriptions.Item label={t('productDetail.overridePriceLabel')} span={2}>
                  {rec.overridePrice.toLocaleString('vi-VN')} ({t('productDetail.overrideDetail', {
                    by: rec.overrideBy,
                    reason: rec.overrideReason,
                    expiresAt: rec.overrideExpiresAt,
                  })})
                </Descriptions.Item>
              )}
            </Descriptions>

            <Table
              rowKey="observationId"
              size="small"
              pagination={false}
              dataSource={rec.sources}
              columns={[
                { title: t('productDetail.colCompetitor'), dataIndex: 'competitorName' },
                { title: t('productDetail.colPrice'), dataIndex: 'price', render: (v: number | null) => v?.toLocaleString('vi-VN') ?? '-' },
                { title: t('productDetail.colCapturedAt'), dataIndex: 'capturedAt' },
                {
                  title: t('productDetail.colIncluded'),
                  dataIndex: 'included',
                  render: (v: boolean) => (v ? <Tag color="green">{t('common.yes')}</Tag> : <Tag>{t('common.no')}</Tag>),
                },
                { title: t('productDetail.colReason'), dataIndex: 'reason' },
              ]}
              style={{ marginBottom: 16 }}
            />

            <Space>
              {rec.status !== 'INSUFFICIENT_DATA' && rec.status !== 'APPROVED' && rec.status !== 'PUBLISHED' && (
                <Button type="primary" onClick={() => approveMutation.mutate(rec.id)} loading={approveMutation.isPending}>
                  {t('common.approve')}
                </Button>
              )}
              {rec.status !== 'INSUFFICIENT_DATA' && (
                <Button danger onClick={() => rejectRecMutation.mutate(rec.id)} loading={rejectRecMutation.isPending}>
                  {t('common.reject')}
                </Button>
              )}
              <Button onClick={() => setOverrideOpen(true)}>{t('productDetail.overrideBtn')}</Button>
            </Space>
          </>
        )}
      </Card>

      <Modal
        title={t('productDetail.manualPriceModalTitle', { competitor: manualPriceListing?.competitorName ?? '' })}
        open={!!manualPriceListing}
        onCancel={() => setManualPriceListing(null)}
        footer={null}
      >
        <Form layout="vertical" onFinish={(values) => manualPriceMutation.mutate(values)}>
          <Form.Item name="price" label={t('productDetail.priceLabel')}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="note" label={t('productDetail.noteLabel')}>
            <Input />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={manualPriceMutation.isPending}>
            {t('common.save')}
          </Button>
        </Form>
      </Modal>

      <Modal title={t('productDetail.overrideModalTitle')} open={overrideOpen} onCancel={() => setOverrideOpen(false)} footer={null}>
        <Form
          layout="vertical"
          onFinish={(values) =>
            overrideMutation.mutate({ price: values.price, reason: values.reason, expiresAt: values.expiresAt })
          }
        >
          <Form.Item name="price" label={t('productDetail.overridePriceInputLabel')} rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="reason" label={t('productDetail.reasonLabel')} rules={[{ required: true }]}>
            <Input.TextArea />
          </Form.Item>
          <Form.Item
            name="expiresAt"
            label={t('productDetail.expiresAtLabel')}
            rules={[{ required: true }]}
          >
            <Input placeholder="2026-12-31T00:00:00+07:00" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={overrideMutation.isPending}>
            {t('productDetail.saveOverride')}
          </Button>
        </Form>
      </Modal>
    </div>
  )
}
