import { useState } from 'react'
import { Button, Card, Form, Input, Typography, Alert } from 'antd'
import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { login } from '../api/auth'
import { useAuthStore } from '../store/authStore'
import { extractErrorMessage } from '../api/client'
import LanguageSwitcher from '../components/LanguageSwitcher'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.login)
  const [error, setError] = useState<string | null>(null)
  const { t } = useTranslation()

  const mutation = useMutation({
    mutationFn: (values: { username: string; password: string }) => login(values.username, values.password),
    onSuccess: (data) => {
      setAuth(data.accessToken, data.refreshToken, data.username, data.role)
      navigate('/')
    },
    onError: (err) => setError(extractErrorMessage(err)),
  })

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <div style={{ position: 'absolute', top: 16, right: 24 }}>
        <LanguageSwitcher />
      </div>
      <Card style={{ width: 380 }}>
        <Typography.Title level={3} style={{ textAlign: 'center' }}>
          {t('app.title')}
        </Typography.Title>
        <Typography.Paragraph type="secondary" style={{ textAlign: 'center' }}>
          {t('app.subtitle')}
        </Typography.Paragraph>
        {error && <Alert type="error" message={error} style={{ marginBottom: 16 }} showIcon />}
        <Form layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item name="username" label={t('login.usernameLabel')} rules={[{ required: true, message: t('login.usernameRequired') }]}>
            <Input autoFocus />
          </Form.Item>
          <Form.Item name="password" label={t('login.passwordLabel')} rules={[{ required: true, message: t('login.passwordRequired') }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={mutation.isPending}>
            {t('login.submit')}
          </Button>
        </Form>
      </Card>
    </div>
  )
}
