import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Divider, Form, InputNumber, Select, Space, Switch, TimePicker, Typography, message } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import { useTranslation } from 'react-i18next'
import { getGlobalPolicy, updateGlobalPolicy } from '../api/pricing'
import { getJobSchedule, updateJobSchedule } from '../api/settings'
import { extractErrorMessage } from '../api/client'

const DAILY_RUN_TIMEZONE = 'Asia/Ho_Chi_Minh'

/** Cron Quartz dang "0 <phut> <gio> * * ?" — chi can doc phut/gio de hien thi len TimePicker. */
function parseCronToTime(cron?: string | null): Dayjs | null {
  if (!cron) {
    return null
  }
  const parts = cron.trim().split(/\s+/)
  if (parts.length < 3) {
    return null
  }
  const minute = Number(parts[1])
  const hour = Number(parts[2])
  if (Number.isNaN(minute) || Number.isNaN(hour)) {
    return null
  }
  return dayjs().hour(hour).minute(minute).second(0)
}

function buildCronFromTime(time: Dayjs): string {
  return `0 ${time.minute()} ${time.hour()} * * ?`
}

export default function SettingsPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const policyQuery = useQuery({ queryKey: ['settings', 'price-policy'], queryFn: getGlobalPolicy })
  const scheduleQuery = useQuery({ queryKey: ['settings', 'job-schedule'], queryFn: getJobSchedule })

  const policyMutation = useMutation({
    mutationFn: updateGlobalPolicy,
    onSuccess: () => {
      message.success(t('settings.pricePolicySaved'))
      queryClient.invalidateQueries({ queryKey: ['settings', 'price-policy'] })
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const scheduleMutation = useMutation({
    mutationFn: (time: Dayjs) => updateJobSchedule(buildCronFromTime(time), DAILY_RUN_TIMEZONE),
    onSuccess: () => {
      message.success(t('settings.cronSaved'))
      queryClient.invalidateQueries({ queryKey: ['settings', 'job-schedule'] })
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const outlierStrategyOptions = [
    { label: t('settings.outlierStrategyOptions.FLAG_ONLY'), value: 'FLAG_ONLY' },
    { label: t('settings.outlierStrategyOptions.EXCLUDE'), value: 'EXCLUDE' },
  ]

  return (
    <div>
      <Typography.Title level={3}>{t('settings.title')}</Typography.Title>

      <Card title={t('settings.cronTitle')} style={{ marginBottom: 16 }} loading={scheduleQuery.isLoading}>
        {scheduleQuery.data && (
          <>
            <Typography.Paragraph type="secondary">{t('settings.dailyRunTimeHint')}</Typography.Paragraph>
            <Form
              layout="inline"
              initialValues={{ time: parseCronToTime(scheduleQuery.data.cron) }}
              onFinish={(values) => scheduleMutation.mutate(values.time)}
            >
              <Form.Item name="time" label={t('settings.dailyRunTimeLabel')} rules={[{ required: true, message: t('settings.dailyRunTimeRequired') }]}>
                <TimePicker format="HH:mm" minuteStep={5} style={{ width: 140 }} />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={scheduleMutation.isPending}>
                {t('settings.saveCron')}
              </Button>
            </Form>
          </>
        )}
      </Card>

      <Card title={t('settings.pricePolicyTitle')} loading={policyQuery.isLoading}>
        {policyQuery.data && (
          <Form layout="vertical" initialValues={policyQuery.data} onFinish={(values) => policyMutation.mutate(values)}>
            <Typography.Text strong>{t('settings.groupSourceThresholds')}</Typography.Text>
            <div style={{ marginTop: 12 }}>
              <Space size="large" wrap>
                <Form.Item
                  name="minimumCompetitorCount"
                  label={t('settings.minimumCompetitorCount')}
                  tooltip={t('settings.minimumCompetitorCountHint')}
                  rules={[{ required: true }]}
                >
                  <InputNumber min={1} style={{ width: 160 }} />
                </Form.Item>
                <Form.Item
                  name="maxObservationAgeHours"
                  label={t('settings.maxObservationAgeHours')}
                  tooltip={t('settings.maxObservationAgeHoursHint')}
                  rules={[{ required: true }]}
                >
                  <InputNumber min={1} style={{ width: 160 }} />
                </Form.Item>
              </Space>
            </div>

            <Divider />
            <Typography.Text strong>{t('settings.groupPriceLimits')}</Typography.Text>
            <div style={{ marginTop: 12 }}>
              <Space size="large" wrap>
                <Form.Item name="roundingStep" label={t('settings.roundingStep')} rules={[{ required: true }]}>
                  <InputNumber min={0} style={{ width: 160 }} />
                </Form.Item>
                <Form.Item name="maxIncreasePercent" label={t('settings.maxIncreasePercent')} rules={[{ required: true }]}>
                  <InputNumber min={0} style={{ width: 160 }} />
                </Form.Item>
                <Form.Item name="maxDecreasePercent" label={t('settings.maxDecreasePercent')} rules={[{ required: true }]}>
                  <InputNumber min={0} style={{ width: 160 }} />
                </Form.Item>
              </Space>
            </div>

            <Divider />
            <Typography.Text strong>{t('settings.groupOutlier')}</Typography.Text>
            <div style={{ marginTop: 12 }}>
              <Space size="large" wrap>
                <Form.Item
                  name="outlierThresholdPercent"
                  label={t('settings.outlierThresholdPercent')}
                  tooltip={t('settings.outlierThresholdPercentHint')}
                  rules={[{ required: true }]}
                >
                  <InputNumber min={0} style={{ width: 160 }} />
                </Form.Item>
                <Form.Item name="outlierStrategy" label={t('settings.outlierStrategy')} rules={[{ required: true }]}>
                  <Select options={outlierStrategyOptions} style={{ width: 280 }} />
                </Form.Item>
              </Space>
            </div>

            <Divider />
            <Typography.Text strong>{t('settings.groupApprovalPublish')}</Typography.Text>
            <div style={{ marginTop: 12 }}>
              <Space size="large" wrap>
                <Form.Item name="requireManualApproval" label={t('settings.requireManualApproval')} valuePropName="checked">
                  <Switch />
                </Form.Item>
                <Form.Item name="autoPublishEnabled" label={t('settings.autoPublishEnabled')} valuePropName="checked">
                  <Switch />
                </Form.Item>
              </Space>
            </div>

            <Divider />
            <Typography.Text strong>{t('settings.groupAbsoluteLimits')}</Typography.Text>
            <div style={{ marginTop: 12, marginBottom: 20 }}>
              <Space size="large" wrap>
                <Form.Item name="minimumAllowedPrice" label={t('settings.minimumAllowedPrice')}>
                  <InputNumber min={0} style={{ width: 200 }} />
                </Form.Item>
                <Form.Item name="maximumAllowedPrice" label={t('settings.maximumAllowedPrice')}>
                  <InputNumber min={0} style={{ width: 200 }} />
                </Form.Item>
              </Space>
            </div>

            <Button type="primary" htmlType="submit" loading={policyMutation.isPending}>
              {t('settings.savePricePolicy')}
            </Button>
          </Form>
        )}
      </Card>
    </div>
  )
}
