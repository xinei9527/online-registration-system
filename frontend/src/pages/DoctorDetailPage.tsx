import { ArrowLeftOutlined, CalendarOutlined, CheckCircleOutlined, TeamOutlined } from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Modal,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../App';
import type { Doctor, Patient, Schedule, ScheduleStatus, TimeSlot } from '../types';

const timeSlotText: Record<TimeSlot, string> = {
  AM: '上午',
  PM: '下午',
};

const statusText: Record<ScheduleStatus, string> = {
  AVAILABLE: '可预约',
  FULL: '已约满',
  STOPPED: '停诊',
};

const statusClass: Record<ScheduleStatus, string> = {
  AVAILABLE: 'status-available',
  FULL: 'status-full',
  STOPPED: 'status-stopped',
};

export default function DoctorDetailPage() {
  const { doctorId } = useParams();
  const navigate = useNavigate();
  const { auth } = useAuth();
  const [doctor, setDoctor] = useState<Doctor | null>(null);
  const [schedules, setSchedules] = useState<Schedule[]>([]);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [selectedSchedule, setSelectedSchedule] = useState<Schedule | null>(null);
  const [selectedPatientId, setSelectedPatientId] = useState<number>();
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!doctorId) return;
    const load = async () => {
      setLoading(true);
      try {
        const [doctorData, scheduleData] = await Promise.all([
          api.doctor(Number(doctorId)),
          api.schedules(Number(doctorId)),
        ]);
        setDoctor(doctorData);
        setSchedules(scheduleData);
        if (auth) {
          setPatients(await api.patients());
        }
      } catch (error) {
        message.error((error as Error).message);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [auth, doctorId]);

  const patientOptions = useMemo(
    () =>
      patients.map((patient) => ({
        value: patient.id,
        label: `${patient.name} · ${patient.maskedIdCard}`,
      })),
    [patients],
  );

  const openBooking = (schedule: Schedule) => {
    if (!auth) {
      message.info('请先登录后再预约');
      navigate('/login');
      return;
    }
    if (!patients.length) {
      message.info('请先添加就诊人');
      navigate('/patients');
      return;
    }
    setSelectedSchedule(schedule);
    setSelectedPatientId(patients[0].id);
  };

  const submitBooking = async () => {
    if (!selectedSchedule || !selectedPatientId) return;
    setSubmitting(true);
    try {
      const appointment = await api.createAppointment({
        patientId: selectedPatientId,
        scheduleId: selectedSchedule.id,
        idempotencyKey:
          typeof crypto !== 'undefined' && 'randomUUID' in crypto
            ? crypto.randomUUID()
            : `${Date.now()}-${Math.random()}`,
      });
      message.success('预约成功');
      navigate(`/appointments/success/${appointment.id}`);
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Spin spinning={loading}>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>{doctor ? `${doctor.name} · ${doctor.title}` : '医生详情'}</Typography.Title>
          <Typography.Paragraph>{doctor?.departmentName} 在线排班</Typography.Paragraph>
        </div>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
          返回
        </Button>
      </div>

      {doctor && (
        <Card className="compact-card" style={{ marginBottom: 18 }}>
          <Descriptions column={{ xs: 1, sm: 2, md: 3 }}>
            <Descriptions.Item label="科室">{doctor.departmentName}</Descriptions.Item>
            <Descriptions.Item label="职称">{doctor.title}</Descriptions.Item>
            <Descriptions.Item label="专长">{doctor.specialty}</Descriptions.Item>
            <Descriptions.Item label="简介" span={3}>
              {doctor.bio}
            </Descriptions.Item>
          </Descriptions>
        </Card>
      )}

      <Card
        className="compact-card"
        title={
          <Space>
            <CalendarOutlined />
            未来 7 天号源
          </Space>
        }
      >
        {schedules.length ? (
          <Table
            rowKey="id"
            dataSource={schedules}
            pagination={false}
            columns={[
              { title: '日期', dataIndex: 'scheduleDate' },
              {
                title: '时段',
                dataIndex: 'timeSlot',
                render: (value: TimeSlot) => timeSlotText[value],
              },
              {
                title: '剩余号源',
                render: (_, record: Schedule) => `${record.remainCount}/${record.totalCount}`,
              },
              {
                title: '状态',
                dataIndex: 'status',
                render: (value: ScheduleStatus) => (
                  <span className={statusClass[value]}>{statusText[value]}</span>
                ),
              },
              {
                title: '操作',
                render: (_, record: Schedule) => (
                  <Button
                    type="primary"
                    icon={<CheckCircleOutlined />}
                    disabled={record.status !== 'AVAILABLE' || record.remainCount <= 0}
                    onClick={() => openBooking(record)}
                  >
                    预约
                  </Button>
                ),
              },
            ]}
          />
        ) : (
          <Empty description="暂无排班" />
        )}
      </Card>

      <Modal
        title="确认预约"
        open={Boolean(selectedSchedule)}
        confirmLoading={submitting}
        onCancel={() => setSelectedSchedule(null)}
        onOk={submitBooking}
        okText="提交预约"
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Alert
            type="info"
            showIcon
            message="同一就诊人同一科室同一天只能预约一次，提交后 30 分钟内可免费取消。"
          />
          <Descriptions column={1} size="small">
            <Descriptions.Item label="医生">{doctor?.name}</Descriptions.Item>
            <Descriptions.Item label="科室">{doctor?.departmentName}</Descriptions.Item>
            <Descriptions.Item label="时间">
              {selectedSchedule?.scheduleDate} {selectedSchedule ? timeSlotText[selectedSchedule.timeSlot] : ''}
            </Descriptions.Item>
          </Descriptions>
          <Select
            value={selectedPatientId}
            onChange={setSelectedPatientId}
            options={patientOptions}
            placeholder="选择就诊人"
            style={{ width: '100%' }}
            suffixIcon={<TeamOutlined />}
          />
        </Space>
      </Modal>
    </Spin>
  );
}
