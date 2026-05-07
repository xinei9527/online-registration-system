CREATE DATABASE IF NOT EXISTS hospital_registration
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE hospital_registration;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS idempotency_records;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS doctor_schedules;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS departments;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NULL,
  email VARCHAR(128) NULL,
  password_hash VARCHAR(128) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'PATIENT',
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_users_phone (phone),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE patients (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  id_card VARCHAR(32) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  KEY idx_patients_user (user_id),
  UNIQUE KEY uk_patients_user_id_card_active (user_id, id_card, deleted),
  CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE departments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  intro VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  UNIQUE KEY uk_departments_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department_id BIGINT NOT NULL,
  name VARCHAR(50) NOT NULL,
  title VARCHAR(50) NOT NULL,
  specialty VARCHAR(255) NOT NULL,
  bio VARCHAR(1000) NOT NULL,
  avatar_url VARCHAR(255) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  KEY idx_doctors_department (department_id),
  KEY idx_doctors_name (name),
  CONSTRAINT fk_doctors_department FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE doctor_schedules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  doctor_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  schedule_date DATE NOT NULL,
  time_slot VARCHAR(10) NOT NULL,
  total_count INT NOT NULL,
  remain_count INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  version INT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_schedule_doctor_date_slot (doctor_id, schedule_date, time_slot),
  KEY idx_schedule_doctor_date (doctor_id, schedule_date),
  KEY idx_schedule_department_date (department_id, schedule_date),
  CONSTRAINT fk_schedules_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_schedules_department FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT ck_schedule_count CHECK (total_count >= 0 AND remain_count >= 0 AND remain_count <= total_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE appointments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  appointment_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  appointment_date DATE NOT NULL,
  time_slot VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  notice_sent TINYINT(1) NOT NULL DEFAULT 0,
  cancel_deadline DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  cancelled_at DATETIME NULL,
  active_flag TINYINT AS (CASE WHEN status IN ('PENDING', 'COMPLETED') THEN 1 ELSE NULL END) STORED,
  UNIQUE KEY uk_appointments_no (appointment_no),
  UNIQUE KEY uk_patient_dept_day_active (patient_id, department_id, appointment_date, active_flag),
  KEY idx_appointments_user_created (user_id, created_at),
  KEY idx_appointments_schedule (schedule_id),
  KEY idx_appointments_doctor_date (doctor_id, appointment_date),
  CONSTRAINT fk_appointments_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
  CONSTRAINT fk_appointments_department FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
  CONSTRAINT fk_appointments_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedules(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idempotency_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL,
  appointment_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL,
  UNIQUE KEY uk_idempotency_user_key (user_id, idempotency_key),
  CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_idempotency_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO users (id, phone, email, password_hash, role, created_at) VALUES
(1, '18800000000', 'admin@hospital.demo', '{noop}admin123', 'ADMIN', NOW()),
(2, '13900000000', 'patient@hospital.demo', '{noop}123456', 'PATIENT', NOW());

INSERT INTO patients (user_id, name, id_card, phone, deleted, created_at) VALUES
(2, '张三', '110101199001011234', '13900000000', 0, NOW()),
(2, '李四', '110101199202022345', '13900000001', 0, NOW());

INSERT INTO departments (id, name, intro, sort_order, enabled) VALUES
(1, '内科', '常见慢性病、呼吸、消化、心血管等内科疾病诊疗。', 1, 1),
(2, '外科', '普外、创伤、术后复诊及常见外科疾病咨询。', 2, 1),
(3, '儿科', '儿童发热、咳嗽、消化不良、生长发育咨询。', 3, 1),
(4, '妇产科', '妇科常见病、孕期咨询、产后复查。', 4, 1),
(5, '皮肤科', '湿疹、皮炎、痤疮、过敏及皮肤感染诊疗。', 5, 1);

INSERT INTO doctors (department_id, name, title, specialty, bio, avatar_url, enabled) VALUES
(1, '王明', '主任医师', '高血压、冠心病、糖尿病综合管理', '从事内科临床工作二十余年，擅长慢病长期管理和多病共治方案制定。', NULL, 1),
(1, '陈静', '副主任医师', '呼吸道感染、哮喘、慢阻肺', '关注呼吸系统常见病和慢性呼吸疾病随访，诊疗风格细致。', NULL, 1),
(2, '赵强', '主任医师', '甲状腺、胆囊、疝气等普外疾病', '长期从事普外科临床和围手术期管理，重视术后康复指导。', NULL, 1),
(2, '刘洋', '主治医师', '创伤处理、外科换药、术后复查', '熟悉门诊外科常见问题处理，沟通耐心，重视风险评估。', NULL, 1),
(3, '孙悦', '副主任医师', '儿童呼吸、发热、过敏性疾病', '从事儿科临床十五年，擅长儿童常见病规范诊治和家庭护理指导。', NULL, 1),
(3, '周宁', '主治医师', '儿童消化、营养、生长发育', '关注儿童营养和成长评估，擅长消化系统常见问题处理。', NULL, 1),
(4, '吴芳', '主任医师', '妇科炎症、月经异常、孕期咨询', '具有丰富妇产科门诊经验，擅长个体化诊疗和孕期健康管理。', NULL, 1),
(4, '郑敏', '副主任医师', '产后康复、妇科内分泌', '长期从事妇科内分泌和产后恢复指导，重视连续随访。', NULL, 1),
(5, '何睿', '副主任医师', '痤疮、湿疹、银屑病', '擅长皮肤慢性病规范治疗和用药指导。', NULL, 1),
(5, '马琳', '主治医师', '过敏、皮炎、皮肤感染', '熟悉常见皮肤病诊疗，注重诱因分析和生活方式建议。', NULL, 1);

INSERT INTO doctor_schedules (
  doctor_id, department_id, schedule_date, time_slot,
  total_count, remain_count, status, version, created_at, updated_at
)
SELECT
  d.id,
  d.department_id,
  DATE_ADD(CURDATE(), INTERVAL days.n DAY),
  slots.time_slot,
  CASE WHEN d.id % 2 = 0 THEN 15 ELSE 12 END AS total_count,
  CASE WHEN d.id % 2 = 0 THEN 15 ELSE 12 END AS remain_count,
  'AVAILABLE',
  0,
  NOW(),
  NOW()
FROM doctors d
JOIN (
  SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
  UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) days
JOIN (
  SELECT 'AM' AS time_slot UNION ALL SELECT 'PM'
) slots;

UPDATE doctor_schedules
SET status = 'STOPPED', remain_count = 0
WHERE doctor_id = 1 AND schedule_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY) AND time_slot = 'PM';

UPDATE doctor_schedules
SET status = 'FULL', remain_count = 0
WHERE doctor_id = 2 AND schedule_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY) AND time_slot = 'AM';
