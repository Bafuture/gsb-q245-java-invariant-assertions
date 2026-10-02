# 并发不变量运行时断言框架

Pair-wise GSB 标注任务仓库（第 16 批 / 245）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java, Maven, JUnit 5 |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

> 本仓库是**初始环境快照**：只有工程骨架，不含任何实现代码。
> 分支说明：`main` 为初始环境；`A`、`B` 为两次独立执行各自的工作分支，均从 `main` 的同一个提交拉出。

## 运行方式

```bash
./mvnw -q verify
```

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的并发结构在压测时偶尔破坏内部不变量，但事后很难定位是哪次操作造成的。请从零实现一个并发不变量运行时断言框架。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持注册不变量检查函数，可在关键操作前后自动执行；2) 支持检查上下文：记录触发检查的线程、操作类型、操作参数与检查耗时；3) 违反不变量时必须抛出带完整上下文的异常，并输出被检查对象的快照摘要；4) 支持只在调试模式启用，生产模式下零开销（需说明实现方式，例如开关判断或空实现）；5) 支持检查频率限制：高频操作下可按比例抽样检查，避免拖慢压测；6) 支持检查失败后的降级策略：记录并继续或立即中止，策略可配置；7) 提供统计：检查次数、失败次数、平均检查耗时与因抽样跳过的次数；8) 测试覆盖不变量通过、违反时报错与上下文完整、开关关闭零开销、抽样频率与统计；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。

---

## 并发不变量运行时断言框架（`com.example.gsb.invariant`）

### 用法

```java
// 1) 启动时配置（调试/压测环境；生产默认关闭）
Invariants.configure(new InvariantRuntimeBuilder()
        .samplingRate(0.1)                          // 按 10% 比例抽样
        .failurePolicy(FailurePolicy.LOG_AND_CONTINUE) // 或 ABORT
        .failureListener(FailureListener.stdErr()));

// 2) 注册不变量（按目标类型过滤；可在运行中安全注册）
Invariants.runtime().register(Account.class,
        Invariant.of("balance-nonnegative",
                (account, ctx) -> account.balance >= 0));

// 3) 在关键操作前后插桩
Invariants.runtime().runOperation("withdraw", account, () -> {
    account.balance -= amount;        // 被保护的操作
}, amount);
// 等价于 beforeOperation(...) / afterOperation(...)
```

也可通过系统属性开启：`-Dinvariant.assertions.enabled=true`
（可选 `invariant.sampling.rate=0.1`、`invariant.failure.policy=LOG_AND_CONTINUE`）。

### 需求与实现对照

1. **注册 / 前后自动检查**：`InvariantRuntime.register(type, invariant)`，
   `beforeOperation` / `afterOperation` / `runOperation`；`CopyOnWriteArrayList`
   保存检查器，按 `Class.isInstance` 过滤目标类型，注册与检查可并发进行。
2. **检查上下文**：`CheckContext` 记录触发线程（id 与名称）、操作类型、
   操作参数（防御性拷贝）、检查点（BEFORE/AFTER）、时间戳与检查耗时（ns）。
3. **违反异常 + 快照**：抛出 `InvariantViolationException`，消息含完整上下文；
   `Snapshot` 渲染被检对象摘要——有自定义 `toString` 的值对象直接用，
   否则输出 `类名@identityHash`；集合给出 size 且长度截断（512 字符），
   不依赖对象实现 `toString`，也避免海量输出。检查器自身抛异常会被捕获为 cause。
4. **零开销**：关闭时（默认）`Invariants.runtime()` 返回
   `NoopInvariantRuntime` 单例，所有钩子是空方法；热路径只有一次虚调用，
   无上下文/数组分配、无抽样判断、无计数写入，JIT 可完全内联消除。
   开启由启动期的一次性开关判断决定（Builder 或系统属性）。
5. **抽样限频**：`Sampler.rate(p)` 用 CAS 计数器做确定性的
   Bresenham 式比例选择——N 次调用恰好执行约 `pN` 次且均匀分布（无 PRNG
   抖动），被跳过的调用计入 `skippedBySampling`。
6. **降级策略**：`FailurePolicy.ABORT` 立即抛出异常中止；
   `LOG_AND_CONTINUE` 通过 `FailureListener` 记录并继续，最近违规保存在
   有界环形缓冲（`maxRecordedFailures`，默认 64），压测期间内存有界。
7. **统计**：`InvariantStats` 提供检查次数、失败次数、抽样跳过次数、
   总耗时与平均检查耗时（ns）；全部为 `AtomicLong`，并发下计数精确。
8. **测试**：6 个测试类、17 个用例，覆盖通过、违反异常与上下文完整性、
   关闭零开销、抽样比例、统计、降级、环形缓冲与 8 线程并发计数精确性。
