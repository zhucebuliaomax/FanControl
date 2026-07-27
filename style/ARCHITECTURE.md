# 模块化架构

## 模块图

```text
:app
 ├─ :feature:authorization ─┐
 ├─ :feature:fan ──────────┼─> :core:designsystem
 ├─ hardware / service     │
 ├─ overlay / tiles        │
 └─ data / host adapters   │
```

依赖只能朝右或朝下。`core` 不知道任何业务；`feature` 不知道独立应用、Root 实现、传感器实现或 Activity；`:app` 负责组合。

## 模块职责

### `:core:designsystem`

路径：`core/designsystem/`

提供原生设置页的最小视觉原语：

- `SettingsSectionTitle`
- `SettingsSegmentGroup`
- `SettingsPreferenceRow`
- `SettingsTokens`

这里不保存状态、不启动 Intent、不访问 SharedPreferences。

### `:feature:authorization`

路径：`feature/authorization/`

公开：

```kotlin
data class AuthorizationUiState(
    val rootGranted: Boolean,
    val notificationsEnabled: Boolean,
)

@Composable
fun AuthorizationManagementSection(
    state: AuthorizationUiState,
    onRefreshRoot: () -> Unit,
    onOpenKernelSu: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
)
```

模块拥有整张授权管理卡片的布局、文案和图标。宿主拥有 Root 检测和平台跳转，因此同一张卡片可以放在任意应用首页。

接入示例：

```kotlin
AuthorizationManagementSection(
    state = AuthorizationUiState(
        rootGranted = rootState,
        notificationsEnabled = notificationState,
    ),
    onRefreshRoot = rootManager::refresh,
    onOpenKernelSu = navigator::openKernelSu,
    onOpenAppInfo = navigator::openAppInfo,
    onOpenNotificationSettings = navigator::openNotificationSettings,
)
```

### `:feature:fan`

路径：`feature/fan/`

公开两块可以一起或分别嵌入的 UI：

- `FanProfilesSection`
- `FanTelemetrySection`

它们只接收展示数据，不依赖本项目的 sysfs、Root shell、服务或曲线仓库。

```kotlin
FanProfilesSection(
    state = FanProfileSectionState(
        enabled = activeCurve != null,
        activeCurveName = activeCurve?.name.orEmpty(),
        controlPointCount = activeCurve?.points?.size ?: 0,
    ),
    onSelectCurve = { /* 打开宿主选择器 */ },
    onEditCurve = { /* 打开宿主编辑器 */ },
)
```

```kotlin
FanTelemetrySection(
    state = FanTelemetrySectionState(
        overlayEnabled = overlayEnabled,
        overlayPermissionGranted = canDrawOverlays,
        cpu = TemperatureTileUiState(cpuAverage, cpuHottest),
        gpu = TemperatureTileUiState(gpuAverage, gpuHottest),
        memoryTemperature = memoryTemperature,
        batteryTemperature = batteryTemperature,
    ),
    onOverlayClick = { /* 切换或申请权限 */ },
    onOverlayEnabledChange = { enabled -> /* 保存 */ },
    onCpuClick = { /* 详情 */ },
    onGpuClick = { /* 详情 */ },
)
```

### `:app`

路径：`app/`

独立应用壳层负责：

- `DashboardScreen` 组合三个可复用区块。
- `DashboardViewModel` 将 SharedPreferences、Telemetry 和 feature UI contract 连接起来。
- `SystemControlService` 拥有唯一的硬件写入循环、通知和浮窗生命周期。
- `RootAccessManager`、Intent 跳转、Quick Settings tile 和首启授权属于宿主能力。
- 曲线选择器、曲线编辑器和温度详情是当前宿主的交互实现。

这样拆分后，将卡片合并到其他应用时不需要携带独立应用的 Activity、Service 或 Manifest 入口。

## 曲线配置模型

曲线不再由固定枚举决定。每条曲线包含：

```text
id             稳定、不可见、不可随重命名改变
builtIn        可选；用于本地化内置曲线名称
customName     可选；用户重命名后覆盖内置名称
points         当前生效控制点
defaultPoints  该曲线自己的“重置”基线
```

持久化键为 `fan_curve_catalog_v2`，当前选择仍写入兼容键 `fan_mode`，其值为曲线 ID 或 `OFF`。

旧版迁移规则：

- `QUIET` → `quiet`
- `NORMAL` → `normal`
- `PERFORMANCE` / `SPORT` → `performance`
- 只有检测到旧自定义数据或旧选择为 `CUSTOM` 时，才创建 `legacy-custom`
- 原曲线点保留，默认点使用原厂曲线

关键不变量：

- 一条曲线至少有两个不同温度的控制点。
- 温度范围 `20..100℃`，风扇范围 `0..100%`。
- 删除当前曲线后选择目录中第一条；目录为空则切换为 `OFF`。
- “设置默认”同时保存当前点与该曲线的 `defaultPoints`。
- “重置”只读取当前曲线自己的 `defaultPoints`。

## 状态流

```text
SharedPreferences
   │
   ├─> DashboardViewModel ─> feature UI state
   │
   └─> SystemControlService
          ├─> FanController (唯一硬件写入)
          ├─> Notification
          └─> TelemetryRepository
                    ├─> Dashboard
                    └─> TelemetryOverlay
```

曲线在浮窗中被 `− / +` 修改后：

1. `FanCurvePreferences` 更新当前曲线。
2. Service 的偏好监听器重新装载曲线目录。
3. 下一轮控制循环更新硬件与 `TelemetryRepository`。
4. 浮窗小图表从 Repository 收到新控制点并重绘。

## 新增功能模块的模板

未来合并其他项目时使用同样结构：

```text
feature/<name>/
 ├─ build.gradle.kts
 ├─ src/main/AndroidManifest.xml
 ├─ src/main/java/.../<Name>Section.kt
 └─ src/main/res/values[-locale]/strings.xml
```

要求：

1. 对外只有不可变 `UiState` 和事件回调。
2. 不依赖 `:app`。
3. 不直接持有 Activity、Service 或全局单例。
4. 共用设置行必须来自 `:core:designsystem`。
5. 宿主专属行为由 adapter 或 navigator 注入。
