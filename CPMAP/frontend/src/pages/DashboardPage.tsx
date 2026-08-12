import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic, Typography, Spin, Alert, Tag, Table } from 'antd'
import ReactECharts from 'echarts-for-react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { getDashboardSummary, getPriceTrends, getCompetitorHealth, type TopMover } from '../api/dashboard'
import { extractErrorMessage } from '../api/client'
import { statusLabel } from '../utils/statusLabel'
import { formatDateTime } from '../utils/formatDateTime'

function runStatusColor(status: string): string {
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

function percent(part: number, total: number): string {
  return total > 0 ? `${Math.round((part / total) * 100)}%` : '-'
}

export default function DashboardPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const summaryQuery = useQuery({ queryKey: ['dashboard', 'summary'], queryFn: getDashboardSummary })
  const trendsQuery = useQuery({ queryKey: ['dashboard', 'price-trends'], queryFn: getPriceTrends })
  const healthQuery = useQuery({ queryKey: ['dashboard', 'competitor-health'], queryFn: getCompetitorHealth })

  if (summaryQuery.isLoading) {
    return <Spin size="large" style={{ marginTop: 80, display: 'block', textAlign: 'center' }} />
  }
  if (summaryQuery.isError) {
    return <Alert type="error" message={extractErrorMessage(summaryQuery.error)} showIcon />
  }
  const summary = summaryQuery.data!

  const recommendationPie = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        data: Object.entries(summary.recommendationsByStatus).map(([name, value]) => ({
          name: statusLabel(t, 'recommendation', name),
          value,
        })),
      },
    ],
  }

  const sourceCoverageBar = {
    tooltip: {},
    xAxis: { type: 'category', data: Object.keys(summary.productsBySourceCount) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: Object.values(summary.productsBySourceCount) }],
  }

  const crawlSuccessBar = {
    tooltip: {},
    xAxis: { type: 'category', data: Object.keys(summary.crawlSuccessRateByCompetitor) },
    yAxis: { type: 'value', max: 1, axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%` } },
    series: [{ type: 'bar', data: Object.values(summary.crawlSuccessRateByCompetitor) }],
  }

  const percentChangeDistribution = trendsQuery.data
    ? {
        tooltip: {},
        xAxis: { type: 'category', data: Object.keys(trendsQuery.data.percentChangeDistribution) },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: Object.values(trendsQuery.data.percentChangeDistribution) }],
      }
    : null

  const dailyCrawlRate = healthQuery.data
    ? {
        tooltip: {},
        xAxis: { type: 'category', data: Object.keys(healthQuery.data.dailyCrawlSuccessRate) },
        yAxis: { type: 'value', max: 1, axisLabel: { formatter: (v: number) => `${Math.round(v * 100)}%` } },
        series: [{ type: 'line', data: Object.values(healthQuery.data.dailyCrawlSuccessRate) }],
      }
    : null

  const moverColumns = [
    {
      title: t('dashboard.colProduct'),
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: t('dashboard.colCurrentPrice'),
      dataIndex: 'currentPrice',
      render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
    },
    {
      title: t('dashboard.colSuggestedPrice'),
      dataIndex: 'suggestedPrice',
      render: (v: number | null) => (v == null ? '-' : v.toLocaleString('vi-VN')),
    },
    {
      title: t('dashboard.colChange'),
      dataIndex: 'percentChange',
      render: (v: number) => (
        <Typography.Text strong style={{ color: v > 0 ? '#cf1322' : v < 0 ? '#3f8600' : undefined }}>
          {v > 0 ? '+' : ''}
          {v.toFixed(1)}%
        </Typography.Text>
      ),
    },
  ]

  const moverRowProps = (record: TopMover) => ({ onClick: () => navigate(`/products/${record.productId}`) })

  return (
    <div>
      <Typography.Title level={3}>{t('dashboard.title')}</Typography.Title>
      <Typography.Paragraph type="secondary">{t('dashboard.pageHint')}</Typography.Paragraph>

      <Card title={t('dashboard.sectionProducts')} style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col flex="1">
            <Statistic title={t('dashboard.totalProducts')} value={summary.totalMcProducts} />
          </Col>
          <Col flex="1">
            <Statistic
              title={t('dashboard.matched')}
              value={summary.matchedProducts}
              valueStyle={{ color: '#3f8600' }}
              suffix={
                <Typography.Text type="secondary" style={{ fontSize: 14 }}>
                  ({percent(summary.matchedProducts, summary.totalMcProducts)})
                </Typography.Text>
              }
            />
          </Col>
          <Col flex="1">
            <Statistic
              title={t('dashboard.unmatched')}
              value={summary.unmatchedProducts}
              suffix={
                <Typography.Text type="secondary" style={{ fontSize: 14 }}>
                  ({percent(summary.unmatchedProducts, summary.totalMcProducts)})
                </Typography.Text>
              }
            />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.conflict')} value={summary.conflictProducts} valueStyle={{ color: '#cf1322' }} />
          </Col>
        </Row>
      </Card>

      <Card title={t('dashboard.sectionPricing')} style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col flex="1">
            <Statistic title={t('dashboard.priceIncreased')} value={summary.priceIncreasedCount} valueStyle={{ color: '#cf1322' }} />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.priceDecreased')} value={summary.priceDecreasedCount} valueStyle={{ color: '#3f8600' }} />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.unchanged')} value={summary.priceUnchangedCount} />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.totalDiff')} value={summary.totalPriceDifference} suffix="₫" />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.avgDiff')} value={summary.averagePriceDifference} suffix="₫" />
          </Col>
        </Row>
      </Card>

      <Card title={t('dashboard.sectionOperations')} style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col flex="1">
            <Typography.Text type="secondary">{t('dashboard.lastJob')}</Typography.Text>
            <div style={{ marginTop: 4 }}>
              {summary.lastJobRun ? (
                <>
                  <div>{t(`jobs.names.${summary.lastJobRun.jobKey}`, { defaultValue: summary.lastJobRun.jobKey })}</div>
                  <Tag color={runStatusColor(summary.lastJobRun.status)}>{statusLabel(t, 'run', summary.lastJobRun.status)}</Tag>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {formatDateTime(summary.lastJobRun.finishedAt)}
                  </Typography.Text>
                </>
              ) : (
                <Typography.Text type="secondary">{t('common.none')}</Typography.Text>
              )}
            </div>
          </Col>
          <Col flex="1">
            <Typography.Text type="secondary">{t('dashboard.lastError')}</Typography.Text>
            <div style={{ marginTop: 4 }}>
              {summary.lastJobError ? (
                <>
                  <div>{t(`jobs.names.${summary.lastJobError.jobKey}`, { defaultValue: summary.lastJobError.jobKey })}</div>
                  <Tag color="red">{statusLabel(t, 'run', summary.lastJobError.status)}</Tag>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    {formatDateTime(summary.lastJobError.finishedAt)}
                  </Typography.Text>
                </>
              ) : (
                <Tag color="green">{t('dashboard.noError')}</Tag>
              )}
            </div>
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.stale')} value={summary.staleObservationCount} />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.mcSyncSuccess')} value={summary.merchantSyncSuccessCount} valueStyle={{ color: '#3f8600' }} />
          </Col>
          <Col flex="1">
            <Statistic title={t('dashboard.mcSyncFailed')} value={summary.merchantSyncFailedCount} valueStyle={{ color: summary.merchantSyncFailedCount > 0 ? '#cf1322' : undefined }} />
          </Col>
        </Row>
      </Card>

      <Card title={t('dashboard.sectionTopMovers')} style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              {t('dashboard.topIncreasingTitle')}
            </Typography.Text>
            <Table
              rowKey="productId"
              size="small"
              pagination={false}
              loading={trendsQuery.isLoading}
              dataSource={trendsQuery.data?.topIncreasing ?? []}
              columns={moverColumns}
              locale={{ emptyText: t('dashboard.noMovers') }}
              onRow={moverRowProps}
              rowClassName={() => 'mc-clickable-row'}
            />
          </Col>
          <Col span={12}>
            <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
              {t('dashboard.topDecreasingTitle')}
            </Typography.Text>
            <Table
              rowKey="productId"
              size="small"
              pagination={false}
              loading={trendsQuery.isLoading}
              dataSource={trendsQuery.data?.topDecreasing ?? []}
              columns={moverColumns}
              locale={{ emptyText: t('dashboard.noMovers') }}
              onRow={moverRowProps}
              rowClassName={() => 'mc-clickable-row'}
            />
          </Col>
        </Row>
      </Card>

      <div className="mc-chart-grid">
        <Card title={t('dashboard.chartByStatus')}>
          <ReactECharts option={recommendationPie} style={{ height: 300 }} />
        </Card>
        <Card title={t('dashboard.chartBySource')}>
          <ReactECharts option={sourceCoverageBar} style={{ height: 300 }} />
        </Card>
        <Card title={t('dashboard.chartCrawlRate')}>
          <ReactECharts option={crawlSuccessBar} style={{ height: 300 }} />
        </Card>
        {percentChangeDistribution && (
          <Card title={t('dashboard.chartPercentChange')}>
            <ReactECharts option={percentChangeDistribution} style={{ height: 300 }} />
          </Card>
        )}
        {dailyCrawlRate && (
          <Card title={t('dashboard.chartDailyCrawlRate')}>
            <ReactECharts option={dailyCrawlRate} style={{ height: 300 }} />
          </Card>
        )}
      </div>
    </div>
  )
}
