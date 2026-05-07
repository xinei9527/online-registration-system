import { ArrowLeftOutlined, CalendarOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { Doctor } from '../types';

export default function DepartmentDoctorsPage() {
  const { departmentId } = useParams();
  const navigate = useNavigate();
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!departmentId) return;
    api
      .doctorsByDepartment(Number(departmentId))
      .then(setDoctors)
      .catch((error) => message.error((error as Error).message))
      .finally(() => setLoading(false));
  }, [departmentId]);

  const departmentName = doctors[0]?.departmentName || '科室医生';

  return (
    <>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>{departmentName}</Typography.Title>
          <Typography.Paragraph>医生信息包含职称、专长和简介，进入详情后可查看号源。</Typography.Paragraph>
        </div>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/')}>
          返回科室
        </Button>
      </div>
      <Spin spinning={loading}>
        {doctors.length ? (
          <div className="doctor-grid">
            {doctors.map((doctor) => (
              <Card
                key={doctor.id}
                className="compact-card"
                title={`${doctor.name} · ${doctor.title}`}
                actions={[
                  <Button type="link" icon={<CalendarOutlined />} onClick={() => navigate(`/doctors/${doctor.id}`)}>
                    查看排班
                  </Button>,
                ]}
              >
                <Space direction="vertical" size="small">
                  <Tag color="geekblue">{doctor.departmentName}</Tag>
                  <Typography.Text strong>专长：{doctor.specialty}</Typography.Text>
                  <Typography.Paragraph className="doctor-meta">{doctor.bio}</Typography.Paragraph>
                </Space>
              </Card>
            ))}
          </div>
        ) : (
          <Empty description="该科室暂未配置医生" />
        )}
      </Spin>
    </>
  );
}
