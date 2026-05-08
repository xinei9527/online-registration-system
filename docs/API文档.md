# API 文档

## 通用说明

接口基础地址：

```text
http://localhost:8080/api
```

除登录、注册、科室医生查询外，其余接口需要请求头：

```text
Authorization: Bearer <token>
```

统一响应格式：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 状态码说明

| code | 说明 |
| --- | --- |
| 0 | 成功 |
| 400 | 业务规则校验失败 |
| 401 | 未登录或登录失效 |
| 403 | 无权限 |
| 422 | 请求参数校验失败 |
| 500 | 系统异常 |

## 失败响应示例

### 未登录

访问需要登录的接口但没有携带 `Authorization` 请求头：

```json
{
  "code": 401,
  "message": "请先登录",
  "data": null
}
```

### 无权限

患者账号访问管理员接口：

```json
{
  "code": 403,
  "message": "仅管理员可访问",
  "data": null
}
```

### 号源已满或停诊

提交预约时，排班状态为 `FULL`、`STOPPED` 或剩余号源为 0：

```json
{
  "code": 400,
  "message": "当前号源不可预约",
  "data": null
}
```

### 重复预约

同一就诊人同一科室同一天已有有效预约：

```json
{
  "code": 400,
  "message": "同一就诊人同一科室同一天只能预约一次",
  "data": null
}
```

### 取消超时

预约创建超过 30 分钟后取消：

```json
{
  "code": 400,
  "message": "预约超过30分钟，已不可取消",
  "data": null
}
```

## 1. 注册

```http
POST /api/auth/register
```

请求：

```json
{
  "phone": "13900000002",
  "email": "demo@example.com",
  "password": "123456"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 3,
    "phone": "13900000002",
    "email": "demo@example.com",
    "role": "PATIENT",
    "token": "eyJhbGciOi..."
  }
}
```

## 2. 登录

```http
POST /api/auth/login
```

请求：

```json
{
  "account": "13900000000",
  "password": "123456"
}
```

响应同注册。

## 3. 科室列表

```http
GET /api/departments?keyword=内科
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "内科",
      "intro": "常见慢性病、呼吸、消化、心血管等内科疾病诊疗。"
    }
  ]
}
```

## 4. 科室下医生列表

```http
GET /api/departments/1/doctors
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "departmentId": 1,
      "departmentName": "内科",
      "name": "王明",
      "title": "主任医师",
      "specialty": "高血压、冠心病、糖尿病综合管理",
      "bio": "从事内科临床工作二十余年...",
      "avatarUrl": null
    }
  ]
}
```

## 5. 医生排班查询

```http
GET /api/doctors/1/schedules
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "doctorId": 1,
      "scheduleDate": "2026-05-07",
      "timeSlot": "AM",
      "totalCount": 12,
      "remainCount": 12,
      "status": "AVAILABLE"
    }
  ]
}
```

号源状态：

| 状态 | 说明 |
| --- | --- |
| AVAILABLE | 可预约 |
| FULL | 已约满 |
| STOPPED | 停诊 |

## 6. 就诊人管理

### 查询就诊人

```http
GET /api/patients
```

### 新增就诊人

```http
POST /api/patients
```

请求：

```json
{
  "name": "张三",
  "idCard": "110101199001011234",
  "phone": "13900000000"
}
```

## 7. 提交预约

```http
POST /api/appointments
```

请求：

```json
{
  "patientId": 1,
  "scheduleId": 1,
  "idempotencyKey": "6ad7b5f6-2d4a-4a40-8a38-c0f089557111"
}
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "appointmentNo": "GH202605071030001234",
    "patientName": "张三",
    "departmentName": "内科",
    "doctorName": "王明",
    "appointmentDate": "2026-05-07",
    "timeSlot": "AM",
    "status": "PENDING",
    "noticeSent": true,
    "canCancel": true
  }
}
```

主要业务错误：

| message | 场景 |
| --- | --- |
| 当前号源不可预约 | 已约满、停诊或剩余号源为 0 |
| 同一就诊人同一科室同一天只能预约一次 | 重复预约 |
| 只能预约未来7天内号源 | 号源日期不符合规则 |

## 8. 我的预约记录

```http
GET /api/appointments/my
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "appointmentNo": "GH202605071030001234",
      "patientName": "张三",
      "departmentName": "内科",
      "doctorName": "王明",
      "appointmentDate": "2026-05-07",
      "timeSlot": "AM",
      "status": "PENDING",
      "canCancel": true
    }
  ]
}
```

## 9. 取消预约

```http
POST /api/appointments/1/cancel
```

成功后预约状态变为 `CANCELLED`，对应号源释放 1 个。

失败示例：

```json
{
  "code": 400,
  "message": "预约超过30分钟，已不可取消",
  "data": null
}
```

## 10. 管理员统计

```http
GET /api/admin/statistics
```

响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "todayAppointments": 8,
    "pendingAppointments": 8,
    "hotDepartments": [
      {
        "departmentId": 1,
        "departmentName": "内科",
        "appointmentCount": 4
      }
    ]
  }
}
```

## 11. 管理员维护排班

```http
POST /api/admin/schedules
```

请求：

```json
{
  "doctorId": 1,
  "scheduleDate": "2026-05-08",
  "timeSlot": "AM",
  "totalCount": 12,
  "remainCount": 12,
  "status": "AVAILABLE"
}
```
