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

## 实现说明（com.example.gsb.invariant）

### 快速上手

```java
InvariantCheckerConfig<MyStructure> config = InvariantCheckerConfig.<MyStructure>builder()
        .enabled(debugMode)                       // false 时返回零开销空实现
        .sampleRate(0.1)                          // 只检查约 1/10 的调用
        .failurePolicy(FailurePolicy.LOG_AND_CONTINUE)
        .snapshotter(s -> "size=" + s.size())     // 违规时输出快照摘要
        .violationListener(ex -> log.warn("{}", ex.getMessage()))
        .build();

InvariantChecker<MyStructure> checker = InvariantCheckers.create(config);
checker.register("size-non-negative", s -> s.size() < 0 ? "size=" + s.size() : null);

// 方式一：自动在操作前后检查
checker.run("put", structure, () -> structure.put(k, v), k, v);
// 方式二：手动埋点
checker.checkBefore("remove", structure, k);
structure.remove(k);
checker.checkAfter("remove", structure, k);
```

### 设计要点

- **不变量注册**：`Invariant<T>` 返回 `null` 表示成立，否则返回违规描述；
  `InvariantChecker.execute/run` 自动在操作前后（`CheckPhase.BEFORE/AFTER`）执行全部已注册不变量。
- **检查上下文**：`CheckContext` 记录触发线程名与线程 id、操作类型、操作参数（不可变拷贝）、
  时间戳与本次检查耗时（纳秒）。
- **违规异常**：`InvariantViolationException` 携带完整 `CheckContext` 与 `Snapshotter`
  生成的对象快照摘要，异常 message 汇总全部字段，便于事后定位是哪次操作破坏了不变量。
- **生产零开销**：`enabled=false` 时 `InvariantCheckers.create()` 返回全局共享的
  `NoOpInvariantChecker` 单例——开关只在装配时判断一次，热路径上没有任何标志位读取、
  不分配上下文对象；空实现是 final 类，JIT 可将空调用内联消除，被守护操作以原生速度运行。
- **抽样限频**：`sampleRate ∈ (0,1]`，实现为确定性的周期抽样（每 `round(1/rate)` 次调用检查一次），
  基于全局 `AtomicLong` 计数器，多线程下比例精确且无随机数开销。
- **降级策略**：`FailurePolicy.ABORT` 立即抛出异常并阻止后续操作；
  `FailurePolicy.LOG_AND_CONTINUE` 把异常交给 `ViolationListener` 记录后继续执行。
- **统计**：`StatsSnapshot` 提供检查次数、失败次数、因抽样跳过次数、累计/平均检查耗时；
  计数器用 `LongAdder`，压测高并发下无 CAS 热点。

## 任务提示词

以下为本题完整的 User Prompt 原文，两次执行必须使用完全相同的文本。

我们的并发结构在压测时偶尔破坏内部不变量，但事后很难定位是哪次操作造成的。请从零实现一个并发不变量运行时断言框架。仓库目前只有一个空的 Maven 工程（pom.xml 只声明 JUnit 5 与 AssertJ）。要求：1) 支持注册不变量检查函数，可在关键操作前后自动执行；2) 支持检查上下文：记录触发检查的线程、操作类型、操作参数与检查耗时；3) 违反不变量时必须抛出带完整上下文的异常，并输出被检查对象的快照摘要；4) 支持只在调试模式启用，生产模式下零开销（需说明实现方式，例如开关判断或空实现）；5) 支持检查频率限制：高频操作下可按比例抽样检查，避免拖慢压测；6) 支持检查失败后的降级策略：记录并继续或立即中止，策略可配置；7) 提供统计：检查次数、失败次数、平均检查耗时与因抽样跳过的次数；8) 测试覆盖不变量通过、违反时报错与上下文完整、开关关闭零开销、抽样频率与统计；`mvn -q verify` 一条命令跑通。

## 提交要求

1. 在本仓库中完成提示词要求的全部内容。
2. `./mvnw -q verify` 必须通过。
3. 完成后在所属分支（A 或 B）上提交，产物快照的父提交必须是初始环境快照。
