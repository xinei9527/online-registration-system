# 在线挂号系统前端

## 技术栈

- React 18
- TypeScript
- Vite
- Ant Design
- Axios

## 启动步骤

```bash
npm install
npm run dev
```

默认访问地址：

```text
http://localhost:5173
```

如需修改后端地址，可添加 `.env`：

```text
VITE_API_BASE_URL=http://localhost:8080/api
```

## 页面说明

- 科室挂号：搜索科室或医生，查看科室下医生。
- 医生详情：查看医生资料和未来 7 天号源。
- 就诊人管理：新增、编辑、删除就诊人。
- 我的预约：查看预约记录，按规则取消预约。
- 预约成功页：展示预约号、预约详情和注意事项。
- 管理面板：管理员查看统计数据，维护医生排班。
