export type UserRole = 'PATIENT' | 'ADMIN';
export type TimeSlot = 'AM' | 'PM';
export type ScheduleStatus = 'AVAILABLE' | 'FULL' | 'STOPPED';
export type AppointmentStatus = 'PENDING' | 'CANCELLED' | 'COMPLETED';

export interface AuthResponse {
  userId: number;
  phone?: string;
  email?: string;
  role: UserRole;
  token: string;
}

export interface Department {
  id: number;
  name: string;
  intro: string;
}

export interface Doctor {
  id: number;
  departmentId: number;
  departmentName: string;
  name: string;
  title: string;
  specialty: string;
  bio: string;
  avatarUrl?: string;
}

export interface Schedule {
  id: number;
  doctorId: number;
  scheduleDate: string;
  timeSlot: TimeSlot;
  totalCount: number;
  remainCount: number;
  status: ScheduleStatus;
}

export interface Patient {
  id: number;
  name: string;
  idCard: string;
  maskedIdCard: string;
  phone: string;
}

export interface Appointment {
  id: number;
  appointmentNo: string;
  patientId: number;
  patientName: string;
  patientPhone: string;
  departmentId: number;
  departmentName: string;
  doctorId: number;
  doctorName: string;
  doctorTitle: string;
  appointmentDate: string;
  timeSlot: TimeSlot;
  status: AppointmentStatus;
  noticeSent: boolean;
  cancelDeadline: string;
  canCancel: boolean;
  createdAt: string;
}

export interface StatsResponse {
  todayAppointments: number;
  pendingAppointments: number;
  hotDepartments: Array<{
    departmentId: number;
    departmentName: string;
    appointmentCount: number;
  }>;
}
