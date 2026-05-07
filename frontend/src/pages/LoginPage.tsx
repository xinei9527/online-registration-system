import { LockOutlined, MailOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Form, Input, Radio, Space, Typography, message } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../App';

type Mode = 'login' | 'register';

export default function LoginPage() {
  const [mode, setMode] = useState<Mode>('login');
  const [loading, setLoading] = useState(false);
  const { setAuth } = useAuth();
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const submit = async (values: Record<string, string>) => {
    setLoading(true);
    try {
      const auth =
        mode === 'login'
          ? await api.login({ account: values.account, password: values.password })
          : await api.register({
              phone: values.phone,
              email: values.email,
              password: values.password,
            });
      setAuth(auth);
      message.success(mode === 'login' ? '登录成功' : '注册成功');
      navigate('/');
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="success-panel">
      <Card className="compact-card">
        <div className="page-title">
          <div>
            <Typography.Title level={1}>{mode === 'login' ? '患者登录' : '患者注册'}</Typography.Title>
            <Typography.Paragraph>
              登录后可管理就诊人、提交预约、查看和取消预约记录。
            </Typography.Paragraph>
          </div>
        </div>
        <Radio.Group
          value={mode}
          onChange={(event) => {
            setMode(event.target.value);
            form.resetFields();
          }}
          optionType="button"
          buttonStyle="solid"
          style={{ marginBottom: 20 }}
          options={[
            { label: '登录', value: 'login' },
            { label: '注册', value: 'register' },
          ]}
        />
        <Form layout="vertical" form={form} onFinish={submit}>
          {mode === 'login' ? (
            <Form.Item name="account" label="手机号或邮箱" rules={[{ required: true, message: '请输入账号' }]}>
              <Input prefix={<UserOutlined />} placeholder="13900000000 或 patient@hospital.demo" />
            </Form.Item>
          ) : (
            <>
              <Form.Item name="phone" label="手机号">
                <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
              </Form.Item>
              <Form.Item name="email" label="邮箱">
                <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
              </Form.Item>
            </>
          )}
          <Form.Item name="password" label="密码" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="至少 6 位" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} block>
            {mode === 'login' ? '登录' : '注册并登录'}
          </Button>
        </Form>
      </Card>
      <Card className="compact-card">
        <Typography.Title level={3}>演示账号</Typography.Title>
        <Space direction="vertical" size="middle">
          <Alert
            type="info"
            showIcon
            message="患者账号"
            description="13900000000 / 123456，可直接体验预约流程。"
          />
          <Alert
            type="success"
            showIcon
            message="管理员账号"
            description="18800000000 / admin123，可查看统计并维护排班。"
          />
        </Space>
      </Card>
    </div>
  );
}
