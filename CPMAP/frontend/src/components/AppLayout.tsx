import { useMemo, useState } from 'react'
import { Layout, Menu, Avatar, Dropdown, Typography, Space, FloatButton } from 'antd'
import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  ShoppingOutlined,
  TeamOutlined,
  ImportOutlined,
  ScheduleOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuthStore } from '../store/authStore'
import LanguageSwitcher from './LanguageSwitcher'

const { Header, Sider, Content } = Layout

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const { username, role, logout } = useAuthStore()
  const { t } = useTranslation()

  const menuItems: MenuProps['items'] = useMemo(
    () => [
      { key: '/', icon: <DashboardOutlined />, label: <Link to="/">{t('nav.dashboard')}</Link> },
      { key: '/products', icon: <ShoppingOutlined />, label: <Link to="/products">{t('nav.products')}</Link> },
      { key: '/competitors', icon: <TeamOutlined />, label: <Link to="/competitors">{t('nav.competitors')}</Link> },
      { key: '/imports', icon: <ImportOutlined />, label: <Link to="/imports">{t('nav.imports')}</Link> },
      { key: '/jobs', icon: <ScheduleOutlined />, label: <Link to="/jobs">{t('nav.jobs')}</Link> },
      { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">{t('nav.settings')}</Link> },
    ],
    [t],
  )

  const selectedKey = '/' + (location.pathname.split('/')[1] ?? '')

  const userMenu: MenuProps['items'] = [
    { key: 'logout', icon: <LogoutOutlined />, label: t('nav.logout') },
  ]

  const siderWidth = collapsed ? 80 : 200

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'fixed',
          insetInlineStart: 0,
          top: 0,
          bottom: 0,
        }}
      >
        <div style={{ color: '#fff', textAlign: 'center', padding: 16, fontWeight: 600 }}>
          {collapsed ? 'MC' : t('app.title')}
        </div>
        <Menu theme="dark" mode="inline" selectedKeys={[selectedKey === '/' ? '/' : selectedKey]} items={menuItems} />
      </Sider>
      <Layout style={{ marginInlineStart: siderWidth, transition: 'margin-inline-start 0.2s' }}>
        <Header
          style={{
            position: 'sticky',
            top: 0,
            zIndex: 10,
            width: '100%',
            background: '#fff',
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            paddingInline: 24,
          }}
        >
          <Space size="middle">
            <LanguageSwitcher />
            <Dropdown
              menu={{
                items: userMenu,
                onClick: (info) => {
                  if (info.key === 'logout') {
                    logout()
                    navigate('/login')
                  }
                },
              }}
            >
              <span style={{ cursor: 'pointer' }}>
                <Avatar icon={<UserOutlined />} style={{ marginRight: 8 }} />
                <Typography.Text>{username} ({role})</Typography.Text>
              </span>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 24 }}>
          <Outlet />
        </Content>
      </Layout>
      <FloatButton.BackTop />
    </Layout>
  )
}
