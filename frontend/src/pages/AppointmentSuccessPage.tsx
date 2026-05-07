import { CheckCircleFilled, CalendarOutlined, FileTextOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Descriptions, Result, Space, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { Appointment, TimeSlot } from '../types';

const timeSlotText: Record<TimeSlot, string> = {
  AM: '上午',
  PM: '下午',
};

export default function AppointmentSuccessPage() {
  const { appointmentId } = useParams();
  const navigate = useNavigate();
  const [appointment, setAppointment] = useState<Appointment | null>(null);

  useEffect(() => {
    if (!appointmentId) return;
    api
      .appointment(Number(appointmentId))
      .then(setAppointment)
      .catch((error) => message.error((error as Error).message));
  }, [appointmentId]);

  return (
    <div className="success-panel">
      <Card className="compact-card">
        <Result
          icon={<CheckCircleFilled style={{ color: '#2f855a' }} />}
          status="success"
          title="预约成功"
          subTitle={appointment ? `预约号：${appointment.appointmentNo}` : '正在读取预约详情'}
          extra={[
            <Button type="primary" icon={<CalendarOutlined />} onClick={() => navigate('/appointments')} key="records">
              查看我的预约
            </Button>,
            <Button onClick={() => navigate('/')} key="home">
              继续挂号
            </Button>,
          ]}
        />
        {appointment && (
          <Descriptions bordered column={1} size="middle">
            <Descriptions.Item label="就诊人">{appointment.patientName}</Descriptions.Item>
            <Descriptions.Item label="科室">{appointment.departmentName}</Descriptions.Item>
            <Descriptions.Item label="医生">
              {appointment.doctorName} · {appointment.doctorTitle}
            </Descriptions.Item>
            <Descriptions.Item label="就诊时间">
              {appointment.appointmentDate} {timeSlotText[appointment.timeSlot]}
            </Descriptions.Item>
            <Descriptions.Item label="预约号">{appointment.appointmentNo}</Descriptions.Item>
            <Descriptions.Item label="通知状态">{appointment.noticeSent ? '已发送模拟短信/邮件' : '未发送'}</Descriptions.Item>
            <Descriptions.Item label="取消截止时间">{appointment.cancelDeadline}</Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Card
        className="compact-card"
        title={
          <Space>
            <FileTextOutlined />
            就诊注意事项
          </Space>
        }
      >
        <Alert type="info" showIcon message="请按预约时段提前 15 分钟到院取号或签到。" style={{ marginBottom: 16 }} />
        <ul className="notice-list">
          <li>携带本人有效身份证件，就诊人与证件信息需一致。</li>
          <li>如需取消，请在预约成功后 30 分钟内完成。</li>
          <li>医生停诊或排班变化时，以医院现场或通知为准。</li>
          <li>过号后需按医院现场规则重新排队或改约。</li>
        </ul>
        <Typography.Paragraph style={{ marginTop: 16 }}>
          本系统为模拟挂号系统，通知发送以预约记录中的“已发送通知”标记表示。
        </Typography.Paragraph>
      </Card>
    </div>
  );
}
