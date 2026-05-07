import {
  CalendarOutlined,
  DashboardOutlined,
  LoginOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Button, Layout, Menu, Space, Typography } from 'antd';
import { createContext, useContext, useMemo, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import type { AuthResponse } from './types';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AppointmentSuccessPage from './pages/AppointmentSuccessPage';
import AppointmentsPage from './pages/AppointmentsPage';
import DepartmentDoctorsPage from './pages/DepartmentDoctorsPage';
import DepartmentsPage from './pages/DepartmentsPage';
import DoctorDetailPage from './pages/DoctorDetailPage';
import LoginPage from './pages/LoginPage';
import PatientsPage from './pages/PatientsPage';

const { Header, Content } = Layout;

interface AuthContextValue {
  auth: AuthResponse | null;
  setAuth: (auth: AuthResponse | null) => void;
  logout: () => void;
}

const storedAuth = localStorage.getItem('auth');

export const AuthContext = createContext<AuthContextValue>({
  auth: storedAuth ? JSON.parse(storedAuth) : null,
  setAuth: () => undefined,
  logout: () => undefined,
});

export const useAuth = () => useContext(AuthContext);

function RequireAuth({ children, adminOnly = false }: { children: JSX.Element; adminOnly?: boolean }) {
  const { auth } = useAuth();
  if (!auth) {
    return <Navigate to="/login" replace />;
  }
  if (adminOnly && auth.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return children;
}

function AppShell() {
  const { auth, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const items = [
    { key: '/', icon: <MedicineBoxOutlined />, label: '科室挂号' },
    ...(auth
      ? [
          { key: '/patients', icon: <TeamOutlined />, label: '就诊人' },
          { key: '/appointments', icon: <CalendarOutlined />, label: '我的预约' },
        ]
      : []),
    ...(auth?.role === 'ADMIN'
      ? [{ key: '/admin', icon: <DashboardOutlined />, label: '管理面板' }]
      : []),
  ];

  const selectedKey = items.find((item) =>
    item.key === '/' ? location.pathname === '/' : location.pathname.startsWith(item.key),
  )?.key;

  return (
    <Layout className="app-root">
      <Header className="app-header">
        <div className="brand" onClick={() => navigate('/')}>
          <MedicineBoxOutlined />
          <span>医院在线挂号</span>
        </div>
        <Menu
          mode="horizontal"
          selectedKeys={selectedKey ? [selectedKey] : []}
          items={items}
          onClick={({ key }) => navigate(key)}
          className="top-menu"
        />
        <Space className="account-actions">
          {auth ? (
            <>
              <Typography.Text className="account-name">
                <UserOutlined /> {auth.phone || auth.email}
              </Typography.Text>
              <Button
                icon={<LogoutOutlined />}
                onClick={() => {
                  logout();
                  navigate('/');
                }}
              >
                退出
              </Button>
            </>
          ) : (
            <Button type="primary" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
              登录
            </Button>
          )}
        </Space>
      </Header>
      <Content className="app-content">
        <Routes>
          <Route path="/" element={<DepartmentsPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/departments/:departmentId" element={<DepartmentDoctorsPage />} />
          <Route path="/doctors/:doctorId" element={<DoctorDetailPage />} />
          <Route
            path="/patients"
            element={
              <RequireAuth>
                <PatientsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/appointments"
            element={
              <RequireAuth>
                <AppointmentsPage />
              </RequireAuth>
            }
          />
          <Route
            path="/appointments/success/:appointmentId"
            element={
              <RequireAuth>
                <AppointmentSuccessPage />
              </RequireAuth>
            }
          />
          <Route
            path="/admin"
            element={
              <RequireAuth adminOnly>
                <AdminDashboardPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Content>
    </Layout>
  );
}

export default function App() {
  const [authState, setAuthState] = useState<AuthResponse | null>(() => {
    const stored = localStorage.getItem('auth');
    return stored ? JSON.parse(stored) : null;
  });

  const value = useMemo<AuthContextValue>(
    () => ({
      auth: authState,
      setAuth: (nextAuth) => {
        setAuthState(nextAuth);
        if (nextAuth) {
          localStorage.setItem('auth', JSON.stringify(nextAuth));
          localStorage.setItem('token', nextAuth.token);
        } else {
          localStorage.removeItem('auth');
          localStorage.removeItem('token');
        }
      },
      logout: () => {
        setAuthState(null);
        localStorage.removeItem('auth');
        localStorage.removeItem('token');
      },
    }),
    [authState],
  );

  return (
    <AuthContext.Provider value={value}>
      <BrowserRouter>
        <AppShell />
      </BrowserRouter>
    </AuthContext.Provider>
  );
}
