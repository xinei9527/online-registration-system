import axios from 'axios';
import type {
  Appointment,
  AuthResponse,
  Department,
  Doctor,
  Patient,
  Schedule,
  ScheduleStatus,
  StatsResponse,
  TimeSlot,
} from '../types';

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>;
    if (typeof envelope?.code === 'number' && envelope.code !== 0) {
      return Promise.reject(new Error(envelope.message || '请求失败'));
    }
    return envelope?.data ?? response.data;
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络异常';
    return Promise.reject(new Error(message));
  },
);

export const api = {
  login: (payload: { account: string; password: string }) =>
    apiClient.post<unknown, AuthResponse>('/auth/login', payload),

  register: (payload: { phone?: string; email?: string; password: string }) =>
    apiClient.post<unknown, AuthResponse>('/auth/register', payload),

  departments: (keyword?: string) =>
    apiClient.get<unknown, Department[]>('/departments', { params: { keyword } }),

  doctorsByDepartment: (departmentId: number) =>
    apiClient.get<unknown, Doctor[]>(`/departments/${departmentId}/doctors`),

  searchDoctors: (keyword: string) =>
    apiClient.get<unknown, Doctor[]>('/doctors/search', { params: { keyword } }),

  doctor: (doctorId: number) => apiClient.get<unknown, Doctor>(`/doctors/${doctorId}`),

  schedules: (doctorId: number) =>
    apiClient.get<unknown, Schedule[]>(`/doctors/${doctorId}/schedules`),

  patients: () => apiClient.get<unknown, Patient[]>('/patients'),

  createPatient: (payload: { name: string; idCard: string; phone: string }) =>
    apiClient.post<unknown, Patient>('/patients', payload),

  updatePatient: (id: number, payload: { name: string; idCard: string; phone: string }) =>
    apiClient.put<unknown, Patient>(`/patients/${id}`, payload),

  deletePatient: (id: number) => apiClient.delete<unknown, void>(`/patients/${id}`),

  createAppointment: (payload: { patientId: number; scheduleId: number; idempotencyKey: string }) =>
    apiClient.post<unknown, Appointment>('/appointments', payload),

  appointments: () => apiClient.get<unknown, Appointment[]>('/appointments/my'),

  appointment: (id: number) => apiClient.get<unknown, Appointment>(`/appointments/${id}`),

  cancelAppointment: (id: number) =>
    apiClient.post<unknown, Appointment>(`/appointments/${id}/cancel`),

  statistics: () => apiClient.get<unknown, StatsResponse>('/admin/statistics'),

  upsertSchedule: (payload: {
    doctorId: number;
    scheduleDate: string;
    timeSlot: TimeSlot;
    totalCount: number;
    remainCount: number;
    status: ScheduleStatus;
  }) => apiClient.post<unknown, Schedule>('/admin/schedules', payload),
};
