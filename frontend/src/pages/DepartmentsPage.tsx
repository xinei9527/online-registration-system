import { SearchOutlined } from '@ant-design/icons';
import { Button, Card, Empty, Input, Space, Spin, Tag, Typography, message } from 'antd';
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import type { Department, Doctor } from '../types';

export default function DepartmentsPage() {
  const [keyword, setKeyword] = useState('');
  const [departments, setDepartments] = useState<Department[]>([]);
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const load = async (nextKeyword = keyword) => {
    setLoading(true);
    try {
      const [departmentData, doctorData] = await Promise.all([
        api.departments(nextKeyword || undefined),
        nextKeyword ? api.searchDoctors(nextKeyword) : Promise.resolve([]),
      ]);
      setDepartments(departmentData);
      setDoctors(doctorData);
    } catch (error) {
      message.error((error as Error).message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load('');
  }, []);

  return (
    <>
      <div className="page-title">
        <div>
          <Typography.Title level={1}>科室与医生</Typography.Title>
          <Typography.Paragraph>选择科室查看医生，再选择未来 7 天内的可预约号源。</Typography.Paragraph>
        </div>
      </div>

      <div className="toolbar">
        <Input.Search
          allowClear
          prefix={<SearchOutlined />}
          placeholder="搜索科室名称或医生姓名"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={load}
          style={{ maxWidth: 420 }}
        />
      </div>

      <Spin spinning={loading}>
        <Typography.Title level={3}>科室列表</Typography.Title>
        {departments.length ? (
          <div className="department-grid">
            {departments.map((department) => (
              <Card
                key={department.id}
                className="compact-card"
                title={department.name}
                actions={[
                  <Button type="link" onClick={() => navigate(`/departments/${department.id}`)}>
                    查看医生
                  </Button>,
                ]}
              >
                <Typography.Paragraph>{department.intro}</Typography.Paragraph>
                <Tag color="cyan">可在线预约</Tag>
              </Card>
            ))}
          </div>
        ) : (
          <Empty description="没有匹配的科室" />
        )}

        {keyword && (
          <>
            <Typography.Title level={3} style={{ marginTop: 28 }}>
              医生搜索结果
            </Typography.Title>
            {doctors.length ? (
              <div className="doctor-grid">
                {doctors.map((doctor) => (
                  <Card
                    key={doctor.id}
                    className="compact-card"
                    title={`${doctor.name} · ${doctor.title}`}
                    actions={[
                      <Button type="link" onClick={() => navigate(`/doctors/${doctor.id}`)}>
                        查看排班
                      </Button>,
                    ]}
                  >
                    <Space direction="vertical">
                      <Tag color="blue">{doctor.departmentName}</Tag>
                      <Typography.Text>{doctor.specialty}</Typography.Text>
                    </Space>
                  </Card>
                ))}
              </div>
            ) : (
              <Empty description="没有匹配的医生" />
            )}
          </>
        )}
      </Spin>
    </>
  );
}
