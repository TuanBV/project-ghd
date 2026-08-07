import { useEffect, useState } from 'react'
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
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from 'antd'
import { useTranslation } from 'react-i18next'
import {
  getProductDetail,
  confirmMatch,
  rejectMatch,
  submitManualPrice,
  getListingCandidates,
  claimListingCandidate,
  updateProduct,
} from '../api/products'
import { approveRecommendation, rejectRecommendation, overrideRecommendation, recalculateProduct } from '../api/pricing'
import { extractErrorMessage } from '../api/client'
import type { CompetitorListingDto } from '../api/types'
import { statusLabel } from '../utils/statusLabel'

const AVAILABILITY_OPTIONS = ['IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN']

export default function ProductDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)
  const queryClient = useQueryClient()
  const [manualPriceListing, setManualPriceListing] = useState<CompetitorListingDto | null>(null)
  const [overrideOpen, setOverrideOpen] = useState(false)
  const [candidateSku, setCandidateSku] = useState('')
  const [debouncedSku, setDebouncedSku] = useState('')

  const query = useQuery({ queryKey: ['product', productId], queryFn: () => getProductDetail(productId) })

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['product', productId] })

  // Tim tu dong khi go, khong can bam nut rieng — debounce nhe de tranh goi API moi ky tu.
  useEffect(() => {
    const handle = setTimeout(() => setDebouncedSku(candidateSku.trim()), 400)
    return () => clearTimeout(handle)
  }, [candidateSku])

  const candidatesQuery = useQuery({
    queryKey: ['listing-candidates', productId, debouncedSku],
    queryFn: () => getListingCandidates(productId, debouncedSku),
    enabled: debouncedSku.length >= 2,
  })

  const confirmMutation = useMutation({
    mutationFn: (matchId: number) => confirmMatch(productId, matchId),
    onSuccess: (result) => {
      if (result.lastPrice != null) {
        message.success(t('productDetail.matchConfirmedWithPrice', { price: result.lastPrice.toLocaleString('vi-VN') }))
      } else {
        message.warning(t('productDetail.matchConfirmedNoPrice'))
      }
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const rejectMutation = useMutation({
    mutationFn: (matchId: number) => rejectMatch(productId, matchId, 'Deleted from UI'),
    onSuccess: () => {
      message.success(t('productDetail.matchDeleted'))
      queryClient.invalidateQueries({ queryKey: ['listing-candidates', productId] })
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const claimMutation = useMutation({
    mutationFn: (listingId: number) => claimListingCandidate(productId, listingId),
    onSuccess: (result) => {
      message.success(
        result.lastPrice != null
          ? t('productDetail.claimSuccessWithPrice', { price: result.lastPrice.toLocaleString('vi-VN') })
          : t('productDetail.claimSuccessNoPrice'),
      )
      queryClient.invalidateQueries({ queryKey: ['listing-candidates', productId] })
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const handleClaim = (candidate: CompetitorListingDto) => {
    // Neu URL nay von da thuoc CHINH san pham dang xem (vua bi "Xoa" — tuc REJECTED — truoc do),
    // day chi la them lai, khong phai chuyen tu san pham khac sang nen khong can hoi xac nhan.
    if (candidate.productId === productId) {
      claimMutation.mutate(candidate.id)
      return
    }
    Modal.confirm({
      title: t('productDetail.claimConfirmTitle'),
      content: t('productDetail.claimConfirmContent', {
        product: `${candidate.productTitle ?? ''} (${candidate.productSku ?? ''})`,
      }),
      okText: t('common.confirm'),
      cancelText: t('common.cancel'),
      onOk: () => claimMutation.mutate(candidate.id),
    })
  }

  const updateAvailabilityMutation = useMutation({
    mutationFn: (availability: string) =>
      // Gui du toan bo field hien tai (khong chi availability) vi PUT /products/{id} thay the toan bo
      // ban ghi — thieu field nao se bi Jackson gan mac dinh (vd active: boolean -> false) hoac bi ghi de thanh null.
      updateProduct(productId, {
        title: product.title,
        brand: product.brand,
        googleCategory: product.googleCategory,
        productType: product.productType,
        currentWebsitePrice: product.currentWebsitePrice,
        active: product.active,
        availability,
      }),
    onSuccess: () => {
      message.success(t('productDetail.availabilityUpdated'))
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
    {
      title: t('productDetail.colMethod'),
      dataIndex: 'matchMethod',
      key: 'matchMethod',
      render: (v: string) => statusLabel(t, 'matchMethod', v),
    },
    { title: t('common.status'), dataIndex: 'matchStatus', key: 'matchStatus', render: (v: string) => <Tag>{statusLabel(t, 'match', v)}</Tag> },
    {
      title: t('productDetail.colCompetitorPrice'),
      dataIndex: 'lastPrice',
      key: 'lastPrice',
      render: (v: number | null) =>
        v == null ? (
          <Typography.Text type="secondary">{t('productDetail.noPriceYet')}</Typography.Text>
        ) : (
          <Typography.Text strong style={{ color: '#389e0d' }}>
            {v.toLocaleString('vi-VN')}
          </Typography.Text>
        ),
    },
    {
      title: t('common.actions'),
      key: 'actions',
      render: (_: unknown, record: CompetitorListingDto) => (
        <Space>
          {record.matchStatus === 'REVIEW_REQUIRED' && (
            <Button size="small" onClick={() => confirmMutation.mutate(record.id)}>{t('common.confirm')}</Button>
          )}
          <Button
            size="small"
            danger
            loading={rejectMutation.isPending && rejectMutation.variables === record.id}
            onClick={() => rejectMutation.mutate(record.id)}
          >
            {t('common.delete')}
          </Button>
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
          <Descriptions.Item label={t('productDetail.availability')}>
            <Select
              size="small"
              style={{ minWidth: 140 }}
              value={product.availability}
              loading={updateAvailabilityMutation.isPending}
              onChange={(value) => updateAvailabilityMutation.mutate(value)}
              options={AVAILABILITY_OPTIONS.map((a) => ({ label: statusLabel(t, 'availability', a), value: a }))}
            />
          </Descriptions.Item>
          <Descriptions.Item label={t('productDetail.url')}>
            {product.productUrl && <a href={product.productUrl} target="_blank" rel="noreferrer">{product.productUrl}</a>}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title={t('productDetail.listingsTitle')} style={{ marginBottom: 16 }}>
        <Input
          placeholder={t('productDetail.candidateSearchPlaceholder')}
          value={candidateSku}
          onChange={(e) => setCandidateSku(e.target.value)}
          allowClear
          style={{ marginBottom: 12, maxWidth: 320 }}
        />
        {debouncedSku.length >= 2 && (
          <Table
            rowKey="id"
            size="small"
            style={{ marginBottom: 16 }}
            loading={candidatesQuery.isLoading}
            dataSource={candidatesQuery.data ?? []}
            locale={{ emptyText: t('productDetail.candidateEmpty') }}
            pagination={false}
            columns={[
              {
                title: 'URL',
                dataIndex: 'url',
                ellipsis: true,
                render: (url: string) => (
                  <a href={url} target="_blank" rel="noreferrer">
                    {url}
                  </a>
                ),
              },
              { title: t('productDetail.colCompetitor'), dataIndex: 'competitorName' },
              {
                title: t('productDetail.candidateColCurrentProduct'),
                key: 'currentProduct',
                render: (_: unknown, record: CompetitorListingDto) =>
                  record.productId === productId ? (
                    <Typography.Text type="secondary" italic>
                      {t('productDetail.candidatePreviouslyDeleted')}
                    </Typography.Text>
                  ) : (
                    <Typography.Text type="secondary">
                      {record.productTitle} ({record.productSku})
                    </Typography.Text>
                  ),
              },
              {
                title: t('common.actions'),
                key: 'actions',
                render: (_: unknown, record: CompetitorListingDto) => (
                  <Button
                    size="small"
                    type="primary"
                    loading={claimMutation.isPending && claimMutation.variables === record.id}
                    onClick={() => handleClaim(record)}
                  >
                    {t('productDetail.candidateAddButton')}
                  </Button>
                ),
              },
            ]}
          />
        )}
        <Table
          rowKey="id"
          columns={listingColumns}
          dataSource={product.competitorListings.filter((l) => l.matchStatus !== 'REJECTED')}
          pagination={false}
        />
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
              <Descriptions.Item label={t('common.status')}><Tag>{statusLabel(t, 'recommendation', rec.status)}</Tag></Descriptions.Item>
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
