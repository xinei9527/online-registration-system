# 后端测试说明

核心预约业务测试位于：

```text
src/test/java/com/hospital/registration/service/AppointmentServiceTest.java
```

该测试类覆盖：

- 同一就诊人同一科室同一天重复预约拦截
- 提交预约后号源扣减，最后一个号源约满后状态变为 `FULL`
- 取消预约后释放号源，并恢复为可预约状态
- 超过 30 分钟取消窗口后不可取消
- 相同 `idempotencyKey` 重复提交时返回已有预约结果

运行命令：

```bash
mvn test
```
