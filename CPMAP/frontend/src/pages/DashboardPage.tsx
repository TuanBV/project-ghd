import { useQuery } from '@tanstack/react-query'
import { Card, Col, Row, Statistic, Typography, Spin, Alert } from 'antd'
import ReactECharts from 'echarts-for-react'
import { useTranslation } from 'react-i18next'
import { getDashboardSummary, getPriceTrends, getCompetitorHealth } from '../api/dashboard'
import { extractErrorMessage } from '../api/client'

export default function DashboardPage() {
  const { t } = useTranslation()
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
        data: Object.entries(summary.recommendationsByStatus).map(([name, value]) => ({ name, value })),
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

  return (
    <div>
      <Typography.Title level={3}>{t('dashboard.title')}</Typography.Title>

      <div className="mc-kpi-grid">
        <Card><Statistic title={t('dashboard.totalProducts')} value={summary.totalMcProducts} /></Card>
        <Card><Statistic title={t('dashboard.matched')} value={summary.matchedProducts} valueStyle={{ color: '#3f8600' }} /></Card>
        <Card><Statistic title={t('dashboard.unmatched')} value={summary.unmatchedProducts} /></Card>
        <Card><Statistic title={t('dashboard.conflict')} value={summary.conflictProducts} valueStyle={{ color: '#cf1322' }} /></Card>
        <Card><Statistic title={t('dashboard.priceIncreased')} value={summary.priceIncreasedCount} valueStyle={{ color: '#cf1322' }} /></Card>
        <Card><Statistic title={t('dashboard.priceDecreased')} value={summary.priceDecreasedCount} valueStyle={{ color: '#3f8600' }} /></Card>
        <Card><Statistic title={t('dashboard.unchanged')} value={summary.priceUnchangedCount} /></Card>
        <Card><Statistic title={t('dashboard.stale')} value={summary.staleObservationCount} /></Card>
        <Card><Statistic title={t('dashboard.mcSyncSuccess')} value={summary.merchantSyncSuccessCount} valueStyle={{ color: '#3f8600' }} /></Card>
        <Card><Statistic title={t('dashboard.mcSyncFailed')} value={summary.merchantSyncFailedCount} valueStyle={{ color: '#cf1322' }} /></Card>
        <Card>
          <Statistic
            title={t('dashboard.lastJob')}
            value={summary.lastJobRun ? `${summary.lastJobRun.jobKey}: ${summary.lastJobRun.status}` : t('common.none')}
          />
        </Card>
        <Card>
          <Statistic
            title={t('dashboard.lastError')}
            value={summary.lastJobError ? `${summary.lastJobError.jobKey}` : t('common.none')}
            valueStyle={{ color: summary.lastJobError ? '#cf1322' : undefined }}
          />
        </Card>
      </div>

      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={8}><Card><Statistic title={t('dashboard.totalDiff')} value={summary.totalPriceDifference} /></Card></Col>
        <Col span={8}><Card><Statistic title={t('dashboard.avgDiff')} value={summary.averagePriceDifference} /></Card></Col>
      </Row>

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
