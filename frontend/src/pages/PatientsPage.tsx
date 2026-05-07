import { DeleteOutlined, EditOutlined, PlusOutlined, TeamOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, Modal, Popconfirm, Space, Table, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Patient } from '../types';

export default function PatientsPage() {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState<Patient | null>(null);
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      setPatients(await api.patients());
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openForm = (patient?: Patient) => {
    setEditing(patient || null);
    setOpen(true);
    form.setFieldsValue(patient || { name: '', idCard: '', phone: '' });
  };

  const submit = async (values: { name: string; idCard: string; phone: string }) => {
    try {
      if (editing) {
        await api.updatePatient(editing.id, values);
        message.success('就诊人已更新');
      } else {
        await api.createPatient(values);
        message.success('就诊人已添加');
      }
      setOpen(false);
      load();
    } catch (error) {
      message.error((error as Error).message);
    }
  };

  const remove = async (id: number) => {
    try {
      await api.deletePatient(id);
      message.success('就诊人已删除');
      load();
    } catch (error) {
      message.error((error as Error).message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>就诊人管理</Typography.Title>
          <Typography.Paragraph>一个账号可维护多个就诊人，预约时从列表中选择。</Typography.Paragraph>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openForm()}>
          添加就诊人
        </Button>
      </div>

      <Card className="compact-card">
        <Table
          rowKey="id"
          loading={loading}
          dataSource={patients}
          columns={[
            { title: '姓名', dataIndex: 'name' },
            { title: '身份证号', dataIndex: 'maskedIdCard' },
            { title: '手机号', dataIndex: 'phone' },
            {
              title: '操作',
              render: (_, record: Patient) => (
                <Space>
                  <Button icon={<EditOutlined />} onClick={() => openForm(record)}>
                    编辑
                  </Button>
                  <Popconfirm title="确认删除该就诊人？" onConfirm={() => remove(record.id)}>
                    <Button danger icon={<DeleteOutlined />}>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />
      </Card>

      <Modal
        title={
          <Space>
            <TeamOutlined />
            {editing ? '编辑就诊人' : '添加就诊人'}
          </Space>
        }
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => form.submit()}
        okText="保存"
      >
        <Form layout="vertical" form={form} onFinish={submit}>
          <Form.Item name="name" label="姓名" rules={[{ required: true, message: '请输入姓名' }]}>
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item name="idCard" label="身份证号" rules={[{ required: true, message: '请输入身份证号' }]}>
            <Input placeholder="请输入身份证号" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="手机号"
            rules={[
              { required: true, message: '请输入手机号' },
              { pattern: /^1\d{10}$/, message: '手机号格式不正确' },
            ]}
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}
