# 原生样式与交互规范

## 视觉基准

设置页以 `system.png` 为准，不以单张卡片自由发挥：

- 页面背景：`MaterialTheme.colorScheme.surfaceContainer`
- 分组行背景：`surfaceContainerHighest`
- 卡片圆角：首尾行使用 Material 3 Expressive segmented shapes
- 组内间距：`ListItemDefaults.SegmentedGap`
- 页面左右留白：`16dp`
- 分组标题左缩进：`16dp`
- 标题与卡片间距：`8dp`
- 分组之间：`22dp`
- 页面底部：`40dp`

所有数值集中在 `SettingsTokens`。其他项目需要变化时覆盖宿主外围间距，不复制并修改 feature 内部实现。

## 字体

使用系统默认字体和 `MaterialTheme.typography`：

| 内容 | 样式 | 字重 |
|---|---|---|
| 页面标题 | `LargeTopAppBar` 默认 | Bold |
| 分组标题 | `titleSmall` | SemiBold |
| 设置行标题 | Material 3 默认 | Medium |
| 设置行摘要 | Material 3 supporting content | Normal |
| 弹窗标题 | `titleLarge` / `headlineSmall` | SemiBold |

不写死中文字体，不单独缩放日文或英文。

## 颜色

设置页禁止写死 RGB：

- 主要文字：`onSurface`
- 次要文字与普通尾部图标：`onSurfaceVariant`
- 强调：`primary`
- 危险操作：`error`
- 选中项：`primaryContainer`

浮窗是跨应用高对比场景，可以使用其专属半透明深色容器与橙色强调；这些颜色不得反向带入设置页。

## 授权管理卡片

顺序和尾部图标固定：

| 行 | 标题 | Google Fonts / Material 图标 |
|---|---|---|
| 1 | Root 权限 | 未授权 `refresh`；已授权 `check` |
| 2 | KernelSU | `open_in_new` |
| 3 | 应用信息 | `open_in_new` |
| 4 | 通知设置 | `open_in_new` |

图标位于行末，标准视觉尺寸 `24dp`。图标来自 Google Material icon set；不要混用 Emoji、字符图标或第三方图标包。

## 曲线选择弹窗

参照 `list.png`：

1. 标题为“选择风扇曲线”。
2. “关闭”和每条曲线都是整行可点的单选项。
3. 曲线来自动态目录，不允许在 UI 写死列表。
4. 列表尾部是 `add` 图标与“添加曲线”。
5. 不再为新安装默认创建“自定义”项。
6. 添加后立即选中新曲线并进入编辑器。

## 曲线编辑弹窗

标题行：

- 只显示曲线名，不添加“编辑”或“曲线”前后缀。
- 名称后是 Google Fonts Material Symbol `edit_square`。
- 最右是 `delete_forever`，使用 `error` 语义色。
- 删除必须再弹出“取消 / 删除”确认。

工具栏顺序：

```text
设置默认  →  重置  →  导入  →  导出
```

- “设置默认”把当前图上的点保存为当前曲线自己的 reset baseline。
- “重置”读取当前曲线的 baseline。
- 窄屏工具栏允许水平滚动，不能压缩文字或使按钮越界。

## 通知

单行内容固定为：

```text
曲线名 · 风扇百分比 · CPU xx℃ · GPU xx℃
```

示例：

```text
标准 · 35% · CPU 52.4℃ · GPU 49.8℃
```

无数据时温度位置显示本地化的“无数据 / N/A”，字段顺序不改变。

## 温度浮窗

数据区可点击，并按注册顺序循环切换布局。当前注册两种布局，但 UI 不显示“精简/详细”等固定名称：

1. `data_only`：标题、CPU、GPU、DDR、电池。
2. `data_fan_curve`：数据 + `− / +` 调节 + 当前曲线小图表。

扩展规则：

- 新布局作为 `OverlayDisplayMode` 新条目加入。
- `next()` 自动参与循环，持久化使用稳定 `storageValue`。
- 关闭按钮不得触发布局切换。
- 拖动仍作用于整个浮窗。
- 小图表只做折线示意：当前点、折线和当前温度标记；不复制完整编辑器坐标轴。
- 调节曲线后必须由共享状态源驱动重绘，不在图表内部伪造局部点。

## 可访问性

- 所有可操作图标使用至少 `48dp` 的 IconButton 点击区；紧凑浮窗按钮保持现有 `42dp`，因为窗口宽度受限。
- 纯装饰的单选圆点可以没有 content description；独立操作图标必须提供本地化描述。
- 文本允许系统字体缩放；窄屏优先滚动、换行或省略，不裁切功能按钮。
