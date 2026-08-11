import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Progress,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  message,
} from 'antd'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import {
  listCompetitors,
  createCompetitor,
  updateCompetitor,
  deleteCompetitor,
  triggerDiscovery,
  triggerPriceSync,
  getCompetitorListings,
  type CompetitorUpsertPayload,
} from '../api/competitors'
import { getJobRun } from '../api/jobs'
import type { CompetitorDto } from '../api/types'
import { extractErrorMessage } from '../api/client'
import { statusLabel } from '../utils/statusLabel'
import { formatDateTime } from '../utils/formatDateTime'

const CRAWL_MODES = ['STATIC_HTML', 'BROWSER', 'MANUAL_ONLY']
const TERMINAL_STATUSES = ['SUCCESS', 'PARTIAL_SUCCESS', 'FAILED', 'CANCELLED']

function DiscoveryProgress({ competitor }: { competitor: CompetitorDto }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  // Chi bao toast khi CHINH component nay chung kien tien trinh chuyen tu dang chay sang xong —
  // khong bao lai cho job da xong tu truoc (vd nguoi dung vao lai trang sau khi da quet xong).
  const previousStatusRef = useRef<string | null>(null)
  const jobRunId = competitor.lastDiscoveryJobRunId

  const runQuery = useQuery({
    queryKey: ['competitor-discovery-run', jobRunId],
    queryFn: () => getJobRun(jobRunId as number),
    enabled: jobRunId != null,
    initialData: jobRunId != null
      ? {
          id: jobRunId,
          jobKey: '',
          triggerType: 'MANUAL',
          status: competitor.lastDiscoveryStatus ?? 'QUEUED',
          totalItems: 0,
          successItems: 0,
          failedItems: 0,
          progressPercent: competitor.lastDiscoveryProgressPercent ?? 0,
          startedAt: null,
          finishedAt: null,
          errorDetail: null,
          correlationId: null,
        }
      : undefined,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status && TERMINAL_STATUSES.includes(status) ? false : 2000
    },
  })

  const run = runQuery.data
  useEffect(() => {
    if (!run) {
      return
    }
    const previousStatus = previousStatusRef.current
    previousStatusRef.current = run.status
    const wasRunning = previousStatus != null && !TERMINAL_STATUSES.includes(previousStatus)
    if (!wasRunning || !TERMINAL_STATUSES.includes(run.status)) {
      return
    }
    if (run.status === 'FAILED') {
      message.error(t('competitors.discoveryToastFailed', { name: competitor.name }))
    } else {
      message.success(t('competitors.discoveryToastSuccess', { name: competitor.name }))
    }
    queryClient.invalidateQueries({ queryKey: ['competitors'] })
  }, [run, competitor.name, t, queryClient])

  if (!jobRunId || !run) {
    return <Typography.Text type="secondary">{t('competitors.discoveryNone')}</Typography.Text>
  }

  const statusLabelKey: Record<string, string> = {
    QUEUED: 'competitors.discoveryQueued',
    RUNNING: 'competitors.discoveryRunning',
    SUCCESS: 'competitors.discoverySuccess',
    PARTIAL_SUCCESS: 'competitors.discoveryPartial',
    FAILED: 'competitors.discoveryFailed',
  }
  const progressStatus = run.status === 'FAILED' ? 'exception' : run.status === 'RUNNING' || run.status === 'QUEUED' ? 'active' : 'success'

  return (
    <Space direction="vertical" size={0} style={{ minWidth: 140 }}>
      <Progress percent={run.progressPercent} size="small" status={progressStatus} />
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        {t(statusLabelKey[run.status] ?? run.status)}
      </Typography.Text>
    </Space>
  )
}

function CrawlProgress({ competitor }: { competitor: CompetitorDto }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  // Cung logic voi DiscoveryProgress: chi bao toast khi CHINH component nay chung kien tien
  // trinh chuyen tu dang chay sang xong, khong bao lai cho job da xong tu truoc.
  const previousStatusRef = useRef<string | null>(null)
  const jobRunId = competitor.lastCrawlJobRunId

  const runQuery = useQuery({
    queryKey: ['competitor-crawl-run', jobRunId],
    queryFn: () => getJobRun(jobRunId as number),
    enabled: jobRunId != null,
    initialData: jobRunId != null
      ? {
          id: jobRunId,
          jobKey: '',
          triggerType: 'MANUAL',
          status: competitor.lastCrawlStatus ?? 'QUEUED',
          totalItems: 0,
          successItems: 0,
          failedItems: 0,
          progressPercent: competitor.lastCrawlProgressPercent ?? 0,
          startedAt: null,
          finishedAt: null,
          errorDetail: null,
          correlationId: null,
        }
      : undefined,
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status && TERMINAL_STATUSES.includes(status) ? false : 2000
    },
  })

  const run = runQuery.data
  useEffect(() => {
    if (!run) {
      return
    }
    const previousStatus = previousStatusRef.current
    previousStatusRef.current = run.status
    const wasRunning = previousStatus != null && !TERMINAL_STATUSES.includes(previousStatus)
    if (!wasRunning || !TERMINAL_STATUSES.includes(run.status)) {
      return
    }
    if (run.status === 'FAILED') {
      message.error(t('competitors.crawlToastFailed', { name: competitor.name }))
    } else {
      message.success(t('competitors.crawlToastSuccess', { name: competitor.name }))
    }
    queryClient.invalidateQueries({ queryKey: ['competitors'] })
  }, [run, competitor.name, t, queryClient])

  if (!jobRunId || !run) {
    return <Typography.Text type="secondary">{t('competitors.crawlNone')}</Typography.Text>
  }

  const statusLabelKey: Record<string, string> = {
    QUEUED: 'competitors.discoveryQueued',
    RUNNING: 'competitors.discoveryRunning',
    SUCCESS: 'competitors.discoverySuccess',
    PARTIAL_SUCCESS: 'competitors.discoveryPartial',
    FAILED: 'competitors.discoveryFailed',
  }
  const progressStatus = run.status === 'FAILED' ? 'exception' : run.status === 'RUNNING' || run.status === 'QUEUED' ? 'active' : 'success'

  return (
    <Space direction="vertical" size={0} style={{ minWidth: 140 }}>
      <Progress percent={run.progressPercent} size="small" status={progressStatus} />
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        {t(statusLabelKey[run.status] ?? run.status)}
      </Typography.Text>
    </Space>
  )
}

export default function CompetitorsPage() {
  const { t } = useTranslation()
  const [editing, setEditing] = useState<CompetitorDto | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [listingsFor, setListingsFor] = useState<CompetitorDto | null>(null)
  const [listingsPage, setListingsPage] = useState(0)
  const [listingsPageSize, setListingsPageSize] = useState(20)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const query = useQuery({ queryKey: ['competitors'], queryFn: listCompetitors, refetchInterval: 4000 })
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['competitors'] })

  const listingsQuery = useQuery({
    queryKey: ['competitor-listings', listingsFor?.id, listingsPage, listingsPageSize],
    queryFn: () => getCompetitorListings(listingsFor!.id, listingsPage, listingsPageSize),
    enabled: !!listingsFor,
  })

  const saveMutation = useMutation({
    mutationFn: (payload: CompetitorUpsertPayload) =>
      editing ? updateCompetitor(editing.id, payload) : createCompetitor(payload),
    onSuccess: () => {
      message.success(t('competitors.saved'))
      setModalOpen(false)
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const deleteMutation = useMutation({
    mutationFn: deleteCompetitor,
    onSuccess: () => {
      message.success(t('common.deleted'))
      invalidate()
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const toggleEnabledMutation = useMutation({
    mutationFn: (competitor: CompetitorDto) =>
      updateCompetitor(competitor.id, {
        name: competitor.name,
        baseUrl: competitor.baseUrl,
        enabled: !competitor.enabled,
        crawlMode: competitor.crawlMode,
        requestsPerMinute: competitor.requestsPerMinute,
        timeoutSeconds: competitor.timeoutSeconds,
        extractorConfig: competitor.extractorConfig,
      }),
    onSuccess: () => invalidate(),
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const discoverMutation = useMutation({
    mutationFn: (id: number) => triggerDiscovery(id),
    onSuccess: () => invalidate(),
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const priceSyncMutation = useMutation({
    mutationFn: (id: number) => triggerPriceSync(id),
    onSuccess: () => invalidate(),
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  return (
    <div>
      <div className="mc-page-header">
        <Typography.Title level={3}>{t('competitors.title')}</Typography.Title>
        <Button
          type="primary"
          onClick={() => {
            setEditing(null)
            setModalOpen(true)
          }}
        >
          {t('competitors.add')}
        </Button>
      </div>

      <Table
        rowKey="id"
        loading={query.isLoading}
        dataSource={query.data ?? []}
        columns={[
          { title: t('competitors.colName'), dataIndex: 'name' },
          { title: t('competitors.colBaseUrl'), dataIndex: 'baseUrl' },
          { title: t('competitors.colCrawlMode'), dataIndex: 'crawlMode', render: (v: string) => <Tag>{statusLabel(t, 'crawlMode', v)}</Tag> },
          {
            title: t('competitors.colEnabled'),
            key: 'enabled',
            render: (_: unknown, record: CompetitorDto) => (
              <Switch
                checked={record.enabled}
                loading={toggleEnabledMutation.isPending && toggleEnabledMutation.variables?.id === record.id}
                onChange={() => toggleEnabledMutation.mutate(record)}
              />
            ),
          },
          {
            title: t('competitors.colUrlCount'),
            key: 'urlCount',
            render: (_: unknown, record: CompetitorDto) => (
              <Space direction="vertical" size={0}>
                <Typography.Text strong>{record.discoveredUrlCount.toLocaleString()}</Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  {t('competitors.urlCountMatchedHint', { count: record.matchedProductCount })}
                </Typography.Text>
              </Space>
            ),
          },
          {
            title: t('competitors.colDiscovery'),
            key: 'discovery',
            render: (_: unknown, record: CompetitorDto) => <DiscoveryProgress competitor={record} />,
          },
          {
            title: t('competitors.colCrawl'),
            key: 'crawl',
            render: (_: unknown, record: CompetitorDto) => <CrawlProgress competitor={record} />,
          },
          {
            title: t('competitors.colLastSuccess'),
            dataIndex: 'lastSuccessAt',
            render: (v: string | null) => formatDateTime(v),
          },
          {
            title: t('common.actions'),
            key: 'actions',
            render: (_: unknown, record: CompetitorDto) => (
              <Space wrap>
                <Button
                  size="small"
                  onClick={() => {
                    setEditing(record)
                    setModalOpen(true)
                  }}
                >
                  {t('common.edit')}
                </Button>
                <Button
                  size="small"
                  loading={discoverMutation.isPending && discoverMutation.variables === record.id}
                  onClick={() => discoverMutation.mutate(record.id)}
                >
                  {t('competitors.rediscover')}
                </Button>
                <Button
                  size="small"
                  type="primary"
                  ghost
                  loading={priceSyncMutation.isPending && priceSyncMutation.variables === record.id}
                  onClick={() => priceSyncMutation.mutate(record.id)}
                >
                  {t('competitors.priceSync')}
                </Button>
                <Button
                  size="small"
                  onClick={() => {
                    setListingsPage(0)
                    setListingsFor(record)
                  }}
                >
                  {t('competitors.viewUrls')}
                </Button>
                <Button
                  size="small"
                  onClick={() =>
                    navigate(`/products?competitorId=${record.id}&competitorName=${encodeURIComponent(record.name)}`)
                  }
                >
                  {t('competitors.viewMatchedProducts')}
                </Button>
                <Button size="small" danger onClick={() => deleteMutation.mutate(record.id)}>{t('common.delete')}</Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal
        title={editing ? t('competitors.edit') : t('competitors.add')}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form
          layout="vertical"
          initialValues={editing ?? { enabled: true, crawlMode: 'STATIC_HTML', requestsPerMinute: 10, timeoutSeconds: 10 }}
          onFinish={(values) => saveMutation.mutate(values)}
        >
          <Form.Item name="name" label={t('competitors.nameLabel')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="baseUrl" label={t('competitors.baseUrlLabel')} rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="crawlMode" label={t('competitors.crawlModeLabel')} rules={[{ required: true }]}>
            <Select options={CRAWL_MODES.map((m) => ({ label: statusLabel(t, 'crawlMode', m), value: m }))} />
          </Form.Item>
          <Form.Item name="requestsPerMinute" label={t('competitors.requestsPerMinuteLabel')} rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="timeoutSeconds" label={t('competitors.timeoutLabel')} rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          {/* enabled duoc bat/tat qua Switch rieng ngoai bang, khong hien thi o day — nhung van
              phai giu field nay trong Form de tranh Jackson tu dong doi ve false khi thieu key. */}
          <Form.Item name="enabled" hidden>
            <Input type="hidden" />
          </Form.Item>
          <Typography.Paragraph type="secondary" style={{ marginTop: -8 }}>
            {t('competitors.autoDiscoveryHint')}
          </Typography.Paragraph>
          <Button type="primary" htmlType="submit" loading={saveMutation.isPending}>
            {t('common.save')}
          </Button>
        </Form>
      </Modal>

      <Modal
        title={t('competitors.viewUrlsModalTitle', {
          name: listingsFor?.name ?? '',
          count: listingsQuery.data?.totalElements ?? 0,
        })}
        open={!!listingsFor}
        onCancel={() => setListingsFor(null)}
        footer={null}
        width={900}
      >
        <Typography.Paragraph type="secondary">
          {t('competitors.viewUrlsHint', {
            matched: listingsFor?.matchedProductCount ?? 0,
            total: listingsFor?.discoveredUrlCount ?? 0,
          })}
        </Typography.Paragraph>
        <Table
          rowKey="id"
          size="small"
          loading={listingsQuery.isLoading}
          dataSource={listingsQuery.data?.content ?? []}
          locale={{ emptyText: t('competitors.listingEmpty') }}
          pagination={{
            current: listingsPage + 1,
            pageSize: listingsPageSize,
            total: listingsQuery.data?.totalElements ?? 0,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50', '100'],
            onChange: (page, size) => {
              setListingsPage(size !== listingsPageSize ? 0 : page - 1)
              setListingsPageSize(size)
            },
          }}
          columns={[
            { title: t('competitors.listingColSku'), dataIndex: 'productSku' },
            {
              title: t('competitors.listingColUrl'),
              dataIndex: 'url',
              ellipsis: true,
              render: (url: string) => (
                <a href={url} target="_blank" rel="noreferrer">
                  {url}
                </a>
              ),
            },
            {
              title: t('competitors.listingColStatus'),
              dataIndex: 'matchStatus',
              render: (status: string) => <Tag>{statusLabel(t, 'match', status)}</Tag>,
            },
            {
              title: t('competitors.listingColPrice'),
              dataIndex: 'lastPrice',
              render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
            },
          ]}
        />
      </Modal>
    </div>
  )
}
