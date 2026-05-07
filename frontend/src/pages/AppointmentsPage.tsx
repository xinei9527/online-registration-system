import { CalendarOutlined, CloseCircleOutlined, ReloadOutlined } from '@ant-design/icons';
import { Button, Card, Popconfirm, Space, Table, Tag, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Appointment, AppointmentStatus, TimeSlot } from '../types';

const timeSlotText: Record<TimeSlot, string> = {
  AM: '上午',
  PM: '下午',
};

const statusTag: Record<AppointmentStatus, { color: string; text: string }> = {
  PENDING: { color: 'processing', text: '待就诊' },
  CANCELLED: { color: 'default', text: '已取消' },
  COMPLETED: { color: 'success', text: '已完成' },
};

export default function AppointmentsPage() {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const load = async () => {
    setLoading(true);
    try {
      setAppointments(await api.appointments());
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const cancel = async (id: number) => {
    try {
      await api.cancelAppointment(id);
      message.success('预约已取消，号源已释放');
      load();
    } catch (error) {
      message.error((error as Error).message);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>我的预约</Typography.Title>
          <Typography.Paragraph>预约成功后 30 分钟内可取消，超过后不可取消。</Typography.Paragraph>
        </div>
        <Button icon={<ReloadOutlined />} onClick={load}>
          刷新
        </Button>
      </div>

      <Card
        className="compact-card"
        title={
          <Space>
            <CalendarOutlined />
            预约记录
          </Space>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          dataSource={appointments}
          scroll={{ x: 900 }}
          columns={[
            {
              title: '预约号',
              dataIndex: 'appointmentNo',
              render: (value: string, record: Appointment) => (
                <Button type="link" onClick={() => navigate(`/appointments/success/${record.id}`)}>
                  {value}
                </Button>
              ),
            },
            { title: '就诊人', dataIndex: 'patientName' },
            { title: '科室', dataIndex: 'departmentName' },
            {
              title: '医生',
              render: (_, record: Appointment) => `${record.doctorName} · ${record.doctorTitle}`,
            },
            {
              title: '就诊时间',
              render: (_, record: Appointment) => `${record.appointmentDate} ${timeSlotText[record.timeSlot]}`,
            },
            {
              title: '通知',
              dataIndex: 'noticeSent',
              render: (sent: boolean) => <Tag color={sent ? 'green' : 'orange'}>{sent ? '已发送' : '未发送'}</Tag>,
            },
            {
              title: '状态',
              dataIndex: 'status',
              render: (status: AppointmentStatus) => (
                <Tag color={statusTag[status].color}>{statusTag[status].text}</Tag>
              ),
            },
            {
              title: '操作',
              render: (_, record: Appointment) =>
                record.canCancel ? (
                  <Popconfirm title="确认取消该预约？" onConfirm={() => cancel(record.id)}>
                    <Button danger icon={<CloseCircleOutlined />}>
                      取消预约
                    </Button>
                  </Popconfirm>
                ) : (
                  <Typography.Text type="secondary">不可取消</Typography.Text>
                ),
            },
          ]}
        />
      </Card>
    </>
  );
}
