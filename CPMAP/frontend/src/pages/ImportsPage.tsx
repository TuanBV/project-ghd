import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Select, Space, Table, Tag, Typography, Upload, message } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { useTranslation } from 'react-i18next'
import { importMcFile, importComparisonFile, getImportIssues } from '../api/imports'
import { extractErrorMessage } from '../api/client'
import type { ImportRunDto } from '../api/types'

export default function ImportsPage() {
  const { t } = useTranslation()
  const [lastRun, setLastRun] = useState<ImportRunDto | null>(null)
  const [severityFilter, setSeverityFilter] = useState<string | undefined>()

  const mcMutation = useMutation({
    mutationFn: importMcFile,
    onSuccess: (data) => {
      message.success(t('imports.mcImportSuccess', { count: data.totalRows }))
      setLastRun(data)
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const comparisonMutation = useMutation({
    mutationFn: importComparisonFile,
    onSuccess: (data) => {
      message.success(t('imports.comparisonImportSuccess', { count: data.totalRows }))
      setLastRun(data)
    },
    onError: (e) => message.error(extractErrorMessage(e)),
  })

  const issuesQuery = useQuery({
    queryKey: ['import-issues', lastRun?.id],
    queryFn: () => getImportIssues(lastRun!.id, 0, 100),
    enabled: !!lastRun,
  })

  const mcUploadProps: UploadProps = {
    beforeUpload: (file) => {
      mcMutation.mutate(file)
      return false
    },
    showUploadList: false,
  }

  const comparisonUploadProps: UploadProps = {
    beforeUpload: (file) => {
      comparisonMutation.mutate(file)
      return false
    },
    showUploadList: false,
  }

  const filteredIssues = (issuesQuery.data?.content ?? []).filter(
    (issue) => !severityFilter || issue.severity === severityFilter,
  )

  return (
    <div>
      <Typography.Title level={3}>{t('imports.title')}</Typography.Title>
      <Space size="large" style={{ marginBottom: 16 }}>
        <Card title={t('imports.mcCardTitle')}>
          <Upload {...mcUploadProps}>
            <Button icon={<UploadOutlined />} loading={mcMutation.isPending}>
              {t('imports.chooseMcFile')}
            </Button>
          </Upload>
        </Card>
        <Card title={t('imports.comparisonCardTitle')}>
          <Upload {...comparisonUploadProps}>
            <Button icon={<UploadOutlined />} loading={comparisonMutation.isPending}>
              {t('imports.chooseComparisonFile')}
            </Button>
          </Upload>
        </Card>
      </Space>

      {lastRun && (
        <Card title={t('imports.resultTitle')} style={{ marginBottom: 16 }}>
          <Descriptions column={3} bordered size="small">
            <Descriptions.Item label={t('imports.colType')}>{lastRun.importType}</Descriptions.Item>
            <Descriptions.Item label={t('imports.colFile')}>{lastRun.fileName}</Descriptions.Item>
            <Descriptions.Item label={t('common.status')}><Tag>{lastRun.status}</Tag></Descriptions.Item>
            <Descriptions.Item label={t('imports.colTotalRows')}>{lastRun.totalRows}</Descriptions.Item>
            <Descriptions.Item label={t('imports.colSuccessRows')}>{lastRun.successRows}</Descriptions.Item>
            <Descriptions.Item label={t('imports.colIssueRows')}>{lastRun.issueRows}</Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      {lastRun && (
        <Card
          title={t('imports.issuesTitle')}
          extra={
            <Select
              allowClear
              placeholder={t('imports.filterSeverity')}
              style={{ width: 160 }}
              options={['INFO', 'WARNING', 'ERROR'].map((s) => ({ label: s, value: s }))}
              onChange={setSeverityFilter}
            />
          }
        >
          {issuesQuery.isError && <Alert type="error" message={extractErrorMessage(issuesQuery.error)} showIcon />}
          <Table
            rowKey="id"
            loading={issuesQuery.isLoading}
            dataSource={filteredIssues}
            columns={[
              { title: t('imports.colRow'), dataIndex: 'rowNumber' },
              { title: t('imports.colIssueType'), dataIndex: 'issueType' },
              {
                title: t('imports.colSeverity'),
                dataIndex: 'severity',
                render: (v: string) => <Tag color={v === 'ERROR' ? 'red' : v === 'WARNING' ? 'orange' : 'blue'}>{v}</Tag>,
              },
              { title: t('imports.colMessage'), dataIndex: 'message' },
            ]}
            pagination={{ pageSize: 20 }}
          />
        </Card>
      )}
    </div>
  )
}
