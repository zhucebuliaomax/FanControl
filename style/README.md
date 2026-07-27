# Fan Control 可复用架构与样式入口

本目录是其他 Android 项目复用本项目模块和视觉语言时的唯一入口。目标是：

1. 独立应用继续由 `:app` 正常运行。
2. 其他应用可以只引入授权管理，或只引入风扇方案与实时温度。
3. 所有设置卡片保持与原生 System Settings 一致的 Material 3 Expressive 分组样式。
4. 宿主项目拥有权限跳转、数据读取和持久化；功能模块只接收状态与回调。

## 从哪里开始

- [ARCHITECTURE.md](ARCHITECTURE.md)：模块边界、依赖方向、状态流和接入示例。
- [VISUAL_SPEC.md](VISUAL_SPEC.md)：原生设置样式、尺寸、语义色、图标和弹窗规则。
- [reuse-manifest.json](reuse-manifest.json)：供脚本、代码生成器或 AI 读取的机器可读规范。

视觉参照：

- `../system.png`：系统设置分组卡片基准。
- `../fancontrol.png`：改造前本应用对照图。
- `../list.png`：单选列表与“+ 添加”入口基准。

## 最小复用路径

在目标工程的 `settings.gradle.kts` 中包含所需模块，然后按需依赖：

```kotlin
dependencies {
    implementation(project(":feature:authorization"))
    // 或
    implementation(project(":feature:fan"))
}
```

两个 feature 模块都会通过 Gradle 自动带入 `:core:designsystem`。不要让 feature 反向依赖宿主 `:app`。

## 设计原则

- State down, events up：状态由宿主传入，点击事件通过回调返回。
- 语义色优先：只使用 `MaterialTheme.colorScheme`，支持动态色、深色和浅色。
- 文案可替换：模块内有英/中/日默认资源，业务名称由状态模型传入。
- 平台行为留在宿主：Root、KernelSU、应用信息、通知设置、传感器和风扇写入不隐藏在 UI 组件内。
- 稳定 ID：曲线以 ID 关联，名称允许修改，绝不以显示名称作为持久化主键。
