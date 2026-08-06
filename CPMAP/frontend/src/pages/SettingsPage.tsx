import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography, message } from 'antd'
import { useTranslation } from 'react-i18next'
import { getGlobalPolicy, updateGlobalPolicy } from '../api/pricing'
import { getJobSchedule, updateJobSchedule } from '../api/settings'
import { extractErrorMessage } from '../api/client'

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
    mutationFn: (values: { cron: string; timezone: string }) => updateJobSchedule(values.cron, values.timezone),
    onSuccess: () => {
      message.success(t('settings.cronSaved'))
      queryClient.invalidateQueries({ queryKey: ['settings', 'job-schedule'] })
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  return (
    <div>
      <Typography.Title level={3}>{t('settings.title')}</Typography.Title>

      <Card title={t('settings.pricePolicyTitle')} style={{ marginBottom: 16 }} loading={policyQuery.isLoading}>
        {policyQuery.data && (
          <Form layout="vertical" initialValues={policyQuery.data} onFinish={(values) => policyMutation.mutate(values)}>
            <Space size="large" wrap>
              <Form.Item name="minimumCompetitorCount" label={t('settings.minimumCompetitorCount')} rules={[{ required: true }]}>
                <InputNumber min={1} />
              </Form.Item>
              <Form.Item name="maxObservationAgeHours" label={t('settings.maxObservationAgeHours')} rules={[{ required: true }]}>
                <InputNumber min={1} />
              </Form.Item>
              <Form.Item name="roundingStep" label={t('settings.roundingStep')} rules={[{ required: true }]}>
                <InputNumber min={0} style={{ width: 160 }} />
              </Form.Item>
              <Form.Item name="maxIncreasePercent" label={t('settings.maxIncreasePercent')} rules={[{ required: true }]}>
                <InputNumber min={0} />
              </Form.Item>
              <Form.Item name="maxDecreasePercent" label={t('settings.maxDecreasePercent')} rules={[{ required: true }]}>
                <InputNumber min={0} />
              </Form.Item>
              <Form.Item name="outlierThresholdPercent" label={t('settings.outlierThresholdPercent')} rules={[{ required: true }]}>
                <InputNumber min={0} />
              </Form.Item>
              <Form.Item name="outlierStrategy" label={t('settings.outlierStrategy')} rules={[{ required: true }]}>
                <Select options={['FLAG_ONLY', 'EXCLUDE'].map((v) => ({ label: v, value: v }))} style={{ width: 160 }} />
              </Form.Item>
              <Form.Item name="requireManualApproval" label={t('settings.requireManualApproval')} valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="autoPublishEnabled" label={t('settings.autoPublishEnabled')} valuePropName="checked">
                <Switch />
              </Form.Item>
              <Form.Item name="minimumAllowedPrice" label={t('settings.minimumAllowedPrice')}>
                <InputNumber min={0} style={{ width: 160 }} />
              </Form.Item>
              <Form.Item name="maximumAllowedPrice" label={t('settings.maximumAllowedPrice')}>
                <InputNumber min={0} style={{ width: 160 }} />
              </Form.Item>
            </Space>
            <Button type="primary" htmlType="submit" loading={policyMutation.isPending}>
              {t('settings.savePricePolicy')}
            </Button>
          </Form>
        )}
      </Card>

      <Card title={t('settings.cronTitle')} loading={scheduleQuery.isLoading}>
        {scheduleQuery.data && (
          <Form layout="inline" initialValues={scheduleQuery.data} onFinish={(values) => scheduleMutation.mutate(values)}>
            <Form.Item name="cron" label={t('settings.cronExpressionLabel')} rules={[{ required: true }]}>
              <Input style={{ width: 220 }} placeholder="0 0 2 * * ?" />
            </Form.Item>
            <Form.Item name="timezone" label={t('settings.timezoneLabel')} rules={[{ required: true }]}>
              <Input style={{ width: 200 }} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={scheduleMutation.isPending}>
              {t('settings.saveCron')}
            </Button>
          </Form>
        )}
      </Card>
    </div>
  )
}
