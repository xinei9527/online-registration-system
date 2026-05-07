import { CalendarOutlined, LineChartOutlined, SaveOutlined } from '@ant-design/icons';
import {
  Button,
  Card,
  DatePicker,
  Form,
  InputNumber,
  Select,
  Space,
  Statistic,
  Table,
  Typography,
  message,
} from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useEffect, useState } from 'react';
import { api } from '../api/client';
import type { Department, Doctor, ScheduleStatus, StatsResponse, TimeSlot } from '../types';

interface ScheduleFormValues {
  departmentId: number;
  doctorId: number;
  scheduleDate: Dayjs;
  timeSlot: TimeSlot;
  totalCount: number;
  remainCount: number;
  status: ScheduleStatus;
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm<ScheduleFormValues>();

  const loadStats = async () => {
    try {
      setStats(await api.statistics());
    } catch (error) {
      message.error((error as Error).message);
    }
  };

  const loadDepartments = async () => {
    const data = await api.departments();
    setDepartments(data);
    if (data[0]) {
      form.setFieldValue('departmentId', data[0].id);
      loadDoctors(data[0].id);
    }
  };

  const loadDoctors = async (departmentId: number) => {
    const data = await api.doctorsByDepartment(departmentId);
    setDoctors(data);
    form.setFieldValue('doctorId', data[0]?.id);
  };

  useEffect(() => {
    loadStats();
    loadDepartments().catch((error) => message.error((error as Error).message));
  }, []);

  const submit = async (values: ScheduleFormValues) => {
    setLoading(true);
    try {
      await api.upsertSchedule({
        doctorId: values.doctorId,
        scheduleDate: values.scheduleDate.format('YYYY-MM-DD'),
        timeSlot: values.timeSlot,
        totalCount: values.totalCount,
        remainCount: values.remainCount,
        status: values.status,
      });
      message.success('排班已保存');
      loadStats();
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>管理面板</Typography.Title>
          <Typography.Paragraph>查看预约统计，并维护医生未来日期的号源数量。</Typography.Paragraph>
        </div>
      </div>

      <div className="stat-band">
        <Card className="compact-card">
          <Statistic title="今日预约量" value={stats?.todayAppointments || 0} prefix={<CalendarOutlined />} />
        </Card>
        <Card className="compact-card">
          <Statistic title="待就诊记录" value={stats?.pendingAppointments || 0} prefix={<LineChartOutlined />} />
        </Card>
        <Card className="compact-card">
          <Statistic title="热门科室数" value={stats?.hotDepartments.length || 0} />
        </Card>
      </div>

      <div className="success-panel">
        <Card
          className="compact-card"
          title={
            <Space>
              <SaveOutlined />
              排班维护
            </Space>
          }
        >
          <Form
            layout="vertical"
            form={form}
            onFinish={submit}
            initialValues={{
              scheduleDate: dayjs(),
              timeSlot: 'AM',
              totalCount: 12,
              remainCount: 12,
              status: 'AVAILABLE',
            }}
          >
            <Form.Item name="departmentId" label="科室" rules={[{ required: true, message: '请选择科室' }]}>
              <Select
                options={departments.map((department) => ({ label: department.name, value: department.id }))}
                onChange={loadDoctors}
              />
            </Form.Item>
            <Form.Item name="doctorId" label="医生" rules={[{ required: true, message: '请选择医生' }]}>
              <Select options={doctors.map((doctor) => ({ label: `${doctor.name} · ${doctor.title}`, value: doctor.id }))} />
            </Form.Item>
            <Form.Item name="scheduleDate" label="日期" rules={[{ required: true, message: '请选择日期' }]}>
              <DatePicker style={{ width: '100%' }} disabledDate={(date) => date.isBefore(dayjs().startOf('day'))} />
            </Form.Item>
            <Form.Item name="timeSlot" label="时段" rules={[{ required: true, message: '请选择时段' }]}>
              <Select
                options={[
                  { label: '上午', value: 'AM' },
                  { label: '下午', value: 'PM' },
                ]}
              />
            </Form.Item>
            <Space.Compact block>
              <Form.Item name="totalCount" label="总号源" rules={[{ required: true, message: '请输入总号源' }]} style={{ width: '50%' }}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item name="remainCount" label="剩余号源" rules={[{ required: true, message: '请输入剩余号源' }]} style={{ width: '50%' }}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Space.Compact>
            <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
              <Select
                options={[
                  { label: '可预约', value: 'AVAILABLE' },
                  { label: '已约满', value: 'FULL' },
                  { label: '停诊', value: 'STOPPED' },
                ]}
              />
            </Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={loading} block>
              保存排班
            </Button>
          </Form>
        </Card>

        <Card
          className="compact-card"
          title={
            <Space>
              <LineChartOutlined />
              热门科室
            </Space>
          }
        >
          <Table
            rowKey="departmentId"
            pagination={false}
            dataSource={stats?.hotDepartments || []}
            columns={[
              { title: '科室', dataIndex: 'departmentName' },
              { title: '预约量', dataIndex: 'appointmentCount' },
            ]}
          />
        </Card>
      </div>
    </>
  );
}
