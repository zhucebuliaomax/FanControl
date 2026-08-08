# Button Layout 模块实现说明

## 目标与边界

Button Layout 模块负责 RP6 内置控制器的四项设置：AB/XY 布局、M1 映射、M2 映射和 Trigger mode。布局支持 `Xbox` 与 `Nintendo`；背键支持 `None`、`Home`、`Select`、`Start`、`Back`、`A/B/X/Y`、`L1/L2/L3`、`R1/R2/R3` 和方向键；Trigger mode 支持 `Both`、`Analog` 与 `Digital`。

RetroControl 不修改 Retroid 系统设置应用的 SharedPreferences。它只在有效配置明确引用某个 Button Layout profile 时写入硬件；profile 中的 `buttonLayoutId == null` 表示“不管理”，此时保留设备当前值。旧 catalog 或导入文件没有 Trigger mode 字段时统一使用 `Both`。

## 设备依据

实现依据 RP6 实机读取结果与 LineageOS/Ayn 开源代码，而不是根据系统设置截图模拟点击：

- 系统设置项定义位于 AynParts 的 `res/xml/ayn_panel.xml`。系统界面的 M1 对应驱动节点 `m0_function`，M2 对应 `m1_function`。
- Moorechip 驱动暴露以下 sysfs 节点：

| 功能 | sysfs 路径 | 合法值 |
| --- | --- | --- |
| AB/XY 布局 | `/sys/class/moorechip-joystick/joystick/layout` | `xbox`, `nintendo` |
| M1 | `/sys/class/moorechip-joystick/joystick/m0_function` | 下述标准按键值 |
| M2 | `/sys/class/moorechip-joystick/joystick/m1_function` | 下述标准按键值 |
| Trigger mode | `/sys/class/moorechip-joystick/joystick/triggers` | `both`, `analog`, `digital` |

背键节点的合法值为 `none/home/select/start/back/a/b/x/y/l1/l2/l3/r1/r2/r3/down/up/left/right`。M1 与 M2 可以映射同一个值。`analog` 根据按压深度提供线性输出，`digital` 只提供开关状态并在触发时输出 100%，`both` 同时启用两者。节点权限为 `0660 system:system`，普通第三方应用无法写入，因此由 libsu 的持久 root shell 操作。

参考仓库：

- [RP6 device tree](https://github.com/LineageOS/android_device_retroidpocket_RP6)
- [Ayn hardware repository](https://github.com/LineageOS/android_hardware_ayn)
- [Ayn common kernel modules](https://github.com/LineageOS/android_kernel_ayn_common-modules)

## 数据模型与持久化

核心模型位于 `data/ButtonLayoutProfileData.kt`：

- `FaceButtonLayout` 是布局枚举，其 `sysfsValue` 与驱动 ABI 一一对应。
- `GamepadButtonMapping` 是可映射按键枚举，不接受任意字符串。
- `ButtonLayoutProfile` 将布局、M1、M2 和 Trigger mode 组成可命名配置。
- `ButtonLayoutProfileCatalog` 提供稳定 ID 的 Nintendo/Xbox 初始配置以及增删改查，Nintendo 是默认配置。两个内置配置的名称和 Layout 固定：Nintendo 始终使用 `nintendo`，Xbox 始终使用 `xbox`；它们不能重命名或删除。编辑内置配置时仍显示已选中的 Layout，但该行置灰，只允许修改 M1、M2 和 Trigger mode。用户创建的配置默认使用 Nintendo，可以重命名、删除，并可在 Nintendo/Xbox Layout 之间选择。
- `ButtonLayoutProfilePreferences` 使用 `button_layout_profile_catalog_v1` 保存配置库。

所有 profile 在进入 catalog 前都会归一化名称。M1 与 M2 是互相独立的映射，允许选择相同按键。Xbox/Nintendo 工厂 profile、旧 catalog 和缺失 Trigger mode 的导入数据均使用 `Both`。

## 配置解析优先级

Button Layout 与风扇、摇杆灯和性能配置使用相同的 profile/app 模型。运行时解析顺序为：

1. Quick Settings Tile 明确选择的 Button Layout profile；
2. 当前前台应用的 `AppControlProfile.buttonLayoutId`；
3. 当前应用选中的 profile，或对应“游戏/非游戏”默认 profile 的 `ControlPreset.buttonLayoutId`；
4. 若最终为 `null`，则不管理硬件；已保存的非空失效引用会在加载时回退到 Nintendo。

应用覆盖只覆盖 Button Layout 自身，不会复制或改变其他控制项。删除一个用户 Button Layout profile 后，预设和应用中原本明确引用它的 Button Layout 选项会在同一操作内回退到内置 Nintendo；原本为 `null` 的“不管理/跟随 profile”值保持不变。

## 硬件写入流程

`hardware/GamepadController.kt` 是唯一直接访问这三个节点的类。一次应用按以下流程执行：

1. 通过 root shell 读取 Layout、M1、M2 和 Trigger mode；
2. 解析并验证驱动返回值，未知值作为错误上报，不猜测默认值；
3. 比较目标值，只为发生变化的字段生成命令；
4. 依次写 M1、M2、Trigger mode，最后写 Layout；
5. 重新读取四个节点，必须与目标完全一致才报告成功。

驱动处理 Layout 或 Trigger mode 写入时都会注销并重新注册 Android input device，因此必须避免重复写，并把背键映射放在它们之前。M1/M2 的驱动映射会考虑当前 Layout，应用层不自行交换 A/B 或 X/Y。

所有 apply 操作由协程 `Mutex` 串行化，避免前台应用快速切换或配置编辑造成交错写入。写入值只能来自枚举常量，因此不会把配置名称或导入 JSON 直接拼入 shell。

## 服务生命周期

`SystemControlService` 在以下时机重新解析有效 Button Layout：

- 服务创建或收到 `ACTION_UPDATE`；
- 前台应用变化；
- preset、默认 profile 或应用覆盖变化；
- Button Layout 配置库变化。

服务缓存上一次成功应用的完整 profile 内容。即使 ID 不变，只要 Layout/M1/M2/Trigger mode 被编辑，仍会重新应用；失败时不更新缓存，以便下一次事件重试。`null` 目标只切换到“不管理”状态，不会把设备恢复为某个假定默认值。

RetroControl 的开机行为仍由原有自动启动设置决定。它不会改写 AynParts 的偏好，因此系统 AynParts 在 locked boot 恢复自身设置后，RetroControl 只有在服务实际启动且有效 profile 非空时才会接管。

## UI 与导入导出

Controls 中的 Button Layout 页面是配置库入口，支持创建、重命名、编辑、滑动删除、导入和导出。Preset 编辑器和应用设置页都使用同一配置库；`Unmanaged`/`Follow profile` 项分别对应 preset 的 `null` 和应用覆盖的 `null`。

Button Layout Quick Settings Tile 使用 `icon/sports_esports_24dp_E3E3E3_FILL1_wght400_GRAD0_opsz24.svg` 转换后的 Android Vector 图标。Tile 始终为 Active（配置库为空时为 Unavailable），短按按 catalog 顺序循环 Nintendo、Xbox 和全部用户 profile，末尾回到第一项；长按复用 Tile 选择弹窗，只显示当前 catalog 的 profile，不提供关闭项。Tile 选择会作为最高优先级覆盖应用/preset 自动化。

`ControlItemJson` 继续使用交换格式版本 1，并增加：

- 独立类型 `button-layout-profile`；
- preset 数据中的可选内嵌对象 `buttonLayout`。

导出 preset 时会嵌入其引用的 Button Layout profile。导入后先创建新的本地 profile ID，再把 preset 引用指向新 ID，因此不会依赖来源设备的 catalog。

## 测试与扩展

本模块的纯逻辑测试覆盖默认 catalog 与 `Both` fallback、相同背键映射、应用覆盖优先级、Tile 循环、驱动值解析以及 M1/M2/Trigger/Layout 写入顺序。硬件节点访问本身需要 RP6 与 root 权限，不能由 JVM 单元测试替代。

如果后续设备使用不同节点或合法值，不应在 UI 或 ViewModel 中添加设备分支。应先确认其驱动 ABI，再抽象硬件后端；数据模型仍应保持枚举验证、差异写入和回读校验三个约束。
