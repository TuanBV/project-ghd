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
  refreshListingPrice,
  submitManualPrice,
  getListingCandidates,
  claimListingCandidate,
  updateProduct,
} from '../api/products'
import { extractErrorMessage } from '../api/client'
import type { CompetitorListingDto } from '../api/types'
import { statusLabel } from '../utils/statusLabel'

const AVAILABILITY_OPTIONS = ['IN_STOCK', 'OUT_OF_STOCK', 'PREORDER', 'UNKNOWN']
const CONFIRMED_MATCH_STATUSES = ['AUTO_CONFIRMED', 'MANUALLY_CONFIRMED']
const AVG_PRICE_WARNING_THRESHOLD_PERCENT = 10

export default function ProductDetailPage() {
  const { t } = useTranslation()
  const { id } = useParams<{ id: string }>()
  const productId = Number(id)
  const queryClient = useQueryClient()
  const [manualPriceListing, setManualPriceListing] = useState<CompetitorListingDto | null>(null)
  const [candidateSku, setCandidateSku] = useState('')
  const [debouncedSku, setDebouncedSku] = useState('')
  const [infoForm] = Form.useForm()
  const [infoDirty, setInfoDirty] = useState(false)

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
        message.info(t('productDetail.matchConfirmedContactOnly'))
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

  const refreshPriceMutation = useMutation({
    mutationFn: (matchId: number) => refreshListingPrice(productId, matchId),
    onSuccess: (result) => {
      if (result.lastPrice != null) {
        message.success(t('productDetail.refreshPriceSuccess', { price: result.lastPrice.toLocaleString('vi-VN') }))
      } else {
        message.info(t('productDetail.refreshPriceContactOnly'))
      }
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const claimMutation = useMutation({
    mutationFn: (listingId: number) => claimListingCandidate(productId, listingId),
    onSuccess: (result) => {
      if (result.lastPrice != null) {
        message.success(t('productDetail.claimSuccessWithPrice', { price: result.lastPrice.toLocaleString('vi-VN') }))
      } else {
        message.info(t('productDetail.claimSuccessContactOnly'))
      }
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

  const updateInfoMutation = useMutation({
    mutationFn: (values: { currentWebsitePrice?: number; availability?: string; productUrl?: string }) =>
      // Gui du toan bo field hien tai (khong chi cac field vua sua) vi PUT /products/{id} thay the toan bo
      // ban ghi — thieu field nao se bi Jackson gan mac dinh (vd active: boolean -> false) hoac bi ghi de thanh null.
      // brand/googleCategory khong con sua duoc tren UI nen luon gui lai gia tri hien co, tranh bi xoa mat.
      // Neu productUrl thay doi, backend se tu dong crawl URL moi de lay gia mac dinh.
      updateProduct(productId, {
        title: product.title,
        brand: product.brand,
        googleCategory: product.googleCategory,
        productType: product.productType,
        currentWebsitePrice: values.currentWebsitePrice ?? null,
        active: product.active,
        availability: values.availability ?? product.availability,
        productUrl: values.productUrl?.trim() || null,
      }),
    onSuccess: () => {
      message.success(t('productDetail.infoSaved'))
      setInfoDirty(false)
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

  const applyAveragePriceMutation = useMutation({
    mutationFn: (averagePrice: number) =>
      updateProduct(productId, {
        title: product.title,
        brand: product.brand,
        googleCategory: product.googleCategory,
        productType: product.productType,
        currentWebsitePrice: averagePrice,
        active: product.active,
        availability: product.availability,
        productUrl: product.productUrl,
      }),
    onSuccess: (_data, averagePriceApplied) => {
      message.success(t('productDetail.avgPriceApplied'))
      // Form khong tu dong dong bo lai initialValues khi query refetch — can gan tay de o
      // "Gia hien tai" cap nhat ngay, khong doi nguoi dung phai tai lai trang moi thay.
      infoForm.setFieldValue('currentWebsitePrice', averagePriceApplied)
      setInfoDirty(false)
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const handleApplyAveragePrice = (averagePrice: number, currentPrice: number | null) => {
    if (currentPrice != null && currentPrice !== averagePrice) {
      Modal.confirm({
        title: t('productDetail.applyAvgPriceConfirmTitle'),
        content: t('productDetail.applyAvgPriceConfirmContent', {
          current: currentPrice.toLocaleString('vi-VN'),
          average: averagePrice.toLocaleString('vi-VN'),
        }),
        okText: t('common.confirm'),
        cancelText: t('common.cancel'),
        onOk: () => applyAveragePriceMutation.mutate(averagePrice),
      })
      return
    }
    applyAveragePriceMutation.mutate(averagePrice)
  }

  if (query.isLoading) {
    return <Spin size="large" style={{ marginTop: 80, display: 'block', textAlign: 'center' }} />
  }
  if (query.isError || !query.data) {
    return <Alert type="error" message={extractErrorMessage(query.error)} showIcon />
  }
  const product = query.data

  const confirmedListingsWithPrice = product.competitorListings.filter(
    (l) => CONFIRMED_MATCH_STATUSES.includes(l.matchStatus) && l.lastPrice != null,
  )
  const averagePrice = confirmedListingsWithPrice.length > 0
    ? Math.round(confirmedListingsWithPrice.reduce((sum, l) => sum + l.lastPrice!, 0) / confirmedListingsWithPrice.length / 10_000) * 10_000
    : null
  const avgPercentDiff = averagePrice != null && product.currentWebsitePrice && product.currentWebsitePrice > 0
    ? ((averagePrice - product.currentWebsitePrice) / product.currentWebsitePrice) * 100
    : null
  const avgExceedsThreshold = avgPercentDiff != null && avgPercentDiff > AVG_PRICE_WARNING_THRESHOLD_PERCENT

  const listingColumns = [
    { title: t('productDetail.colCompetitor'), dataIndex: 'competitorName', key: 'competitorName' },
    { title: 'URL', dataIndex: 'url', key: 'url', render: (v: string) => <a href={v} target="_blank" rel="noreferrer">{v}</a> },
    { title: t('common.status'), dataIndex: 'matchStatus', key: 'matchStatus', render: (v: string) => <Tag>{statusLabel(t, 'match', v)}</Tag> },
    {
      title: t('productDetail.colCompetitorPrice'),
      dataIndex: 'lastPrice',
      key: 'lastPrice',
      render: (v: number | null, record: CompetitorListingDto) =>
        v != null ? (
          <Typography.Text strong style={{ color: '#389e0d' }}>
            {v.toLocaleString('vi-VN')}
          </Typography.Text>
        ) : record.lastPriceStatus != null ? (
          // Da tung thu lay gia (co it nhat 1 lan crawl) nhung khong ra so — coi nhu doi thu de
          // gia "Lien he", khong phan biet ly do ky thuat (CONTACT_ONLY/NO_PRICE/loi parse...).
          <Tag color="blue">{t('productDetail.contactOnly')}</Tag>
        ) : (
          <Typography.Text type="secondary">{t('productDetail.noPriceYet')}</Typography.Text>
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
          {CONFIRMED_MATCH_STATUSES.includes(record.matchStatus) && (
            <Button
              size="small"
              loading={refreshPriceMutation.isPending && refreshPriceMutation.variables === record.id}
              onClick={() => refreshPriceMutation.mutate(record.id)}
            >
              {t('productDetail.refreshPriceBtn')}
            </Button>
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
        <Form
          form={infoForm}
          initialValues={{
            currentWebsitePrice: product.currentWebsitePrice ?? undefined,
            availability: product.availability,
            productUrl: product.productUrl ?? undefined,
          }}
          onValuesChange={() => setInfoDirty(true)}
          onFinish={(values) => updateInfoMutation.mutate(values)}
        >
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label={t('productDetail.sku')}>{product.skuOriginal}</Descriptions.Item>
            <Descriptions.Item label={t('productDetail.mcOfferId')}>{product.mcOfferId}</Descriptions.Item>
            <Descriptions.Item label={t('productDetail.currentWebsitePrice')}>
              <Form.Item name="currentWebsitePrice" noStyle>
                <InputNumber size="small" style={{ width: '100%' }} min={0} />
              </Form.Item>
            </Descriptions.Item>
            <Descriptions.Item label={t('productDetail.currentMcPrice')}>{product.currentMcPrice?.toLocaleString('vi-VN')}</Descriptions.Item>
            <Descriptions.Item label={t('productDetail.availability')}>
              <Form.Item name="availability" noStyle>
                <Select
                  size="small"
                  style={{ minWidth: 140 }}
                  options={AVAILABILITY_OPTIONS.map((a) => ({ label: statusLabel(t, 'availability', a), value: a }))}
                />
              </Form.Item>
            </Descriptions.Item>
            <Descriptions.Item label={t('productDetail.url')} span={2}>
              <Space.Compact style={{ width: '100%' }}>
                <Form.Item name="productUrl" noStyle>
                  <Input size="small" placeholder={t('productDetail.urlPlaceholder')} />
                </Form.Item>
                {product.productUrl && (
                  <Button size="small" href={product.productUrl} target="_blank" rel="noreferrer">
                    {t('productDetail.openUrl')}
                  </Button>
                )}
              </Space.Compact>
              <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                {t('productDetail.urlHint')}
              </Typography.Text>
            </Descriptions.Item>
          </Descriptions>
          {infoDirty && (
            <div style={{ marginTop: 12, textAlign: 'right' }}>
              <Button type="primary" htmlType="submit" loading={updateInfoMutation.isPending}>
                {t('common.save')}
              </Button>
            </div>
          )}
        </Form>
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

      <Card title={t('productDetail.avgPriceTitle')} style={{ marginBottom: 16 }}>
        {confirmedListingsWithPrice.length === 0 ? (
          <Typography.Text type="secondary">{t('productDetail.avgPriceNoConfirmed')}</Typography.Text>
        ) : (
          <>
            <Typography.Paragraph style={{ marginBottom: 8 }}>
              <Typography.Text type="secondary">{t('productDetail.avgPriceSourceCount')}: </Typography.Text>
              <Typography.Text strong>{confirmedListingsWithPrice.length}</Typography.Text>
            </Typography.Paragraph>
            <Typography.Title level={4} style={{ marginTop: 0, marginBottom: 12 }}>
              {averagePrice!.toLocaleString('vi-VN')} ₫
            </Typography.Title>
            {avgExceedsThreshold && (
              <Typography.Text type="danger" style={{ display: 'block', marginBottom: 12 }}>
                {t('productDetail.avgPriceWarning', { percent: avgPercentDiff!.toFixed(1) })}
              </Typography.Text>
            )}
            <Button
              type="primary"
              onClick={() => handleApplyAveragePrice(averagePrice!, product.currentWebsitePrice)}
              loading={applyAveragePriceMutation.isPending}
            >
              {t('productDetail.applyAvgPrice')}
            </Button>
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
    </div>
  )
}
