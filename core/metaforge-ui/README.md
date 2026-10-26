# metaforge-ui

MetaForge 的组件库：**纯 Tailwind CSS + 设计 token**，Vue 3 组件统一以 `Mf` 为前缀（`MfButton`、`MfInput`…）。

## 结构

```
core/metaforge-ui
├── src
│   ├── components        # Mf* 组件（仅使用语义化 token 工具类）
│   ├── styles
│   │   ├── tokens.css        # --mf-* 语义 token（light / dark）
│   │   └── metaforge-ui.css  # Tailwind 入口 + @theme inline 映射
│   ├── theme             # 运行时主题 API（preset / applyMfTheme）
│   └── index.ts          # 导出 + Vue 插件
└── playground            # pnpm dev 演示页（组件总览 + 主题切换）
```

与 `core/metaforge-web`（应用层）通过 pnpm workspace 关联；web 已全量使用本组件库（ant-design-vue 已移除），新页面参考 `metaforge-web/docs/antd-to-mf.md` 映射指南。

## 组件清单

基础：MfButton（variant/size/loading/shape）、MfInput（密码切换/前后缀/清除）、MfInputNumber、MfTextarea、MfSelect（单选/多选/分组/搜索/标签创建）、MfCheckbox、MfCheckboxGroup、MfRadio(Group)（含按钮变体）、MfSwitch（checkedValue 映射）

布局：MfCard、MfSpace、MfDivider、MfTabs/MfTabPane（自定义标签/禁用）、MfBreadcrumb(Item)

反馈：MfAlert、MfMessage（命令式 toast）、MfModal、MfDrawer、MfTooltip、MfPopover、MfSpinner、MfSkeleton、MfTag（语义色/自定义色）、MfBadge、MfAvatar、MfResult

数据：MfForm/MfFormItem（rules 校验：required/min/max/pattern/validator/type）、MfTable（bodyCell/列插槽）、MfPagination、MfTree（异步加载）、MfDropdown、MfMenu/MfMenuItem/MfSubMenu、MfUpload

主题：MfConfigProvider + `applyMfTheme` / `setMfThemeMode` / `createMfTheme`（light/dark preset）

## Token 体系

组件不写死任何颜色/圆角/阴影，全部走语义 token：

| Token | 用途 |
| --- | --- |
| `--mf-color-primary(-hover/-active)`、`--mf-color-on-primary` | 品牌色 |
| `--mf-color-success/warning/danger/info(-subtle)` | 语义反馈 |
| `--mf-color-page/surface(-hover/-active)` | 表面 |
| `--mf-color-content(-secondary/-tertiary/-disabled/-inverse)` | 文本 |
| `--mf-color-line(-strong)`、`--mf-color-overlay` | 边框 / 遮罩 |
| `--mf-radius-*`、`--mf-shadow-*`、`--mf-duration-*`、`--mf-easing`、`--mf-font-*` | 其余维度 |

`metaforge-ui.css` 通过 Tailwind v4 的 `@theme inline` 把 token 映射为工具类（`bg-primary`、`text-content`、`border-line`…），
因此**运行时换主题只是替换 CSS 变量**，无需重新生成样式。

## 主题

```ts
import { applyMfTheme, createMfTheme, mfLightPreset, setMfThemeMode } from 'metaforge-ui'

setMfThemeMode('dark')                                  // light / dark
applyMfTheme(createMfTheme({ 'color-primary': '#7c3aed' })) // 用户自定义主题
```

组件级作用域使用 `MfConfigProvider`（`mode` / `tokens` props，默认作用于子树，`global` 作用于根节点）。

## 在应用中接入

```ts
// 应用自身使用 Tailwind v4 时，css 入口引入：
@import 'metaforge-ui/styles/metaforge-ui.css';
// 应用不用 Tailwind 时，仅引 token 变量：
@import 'metaforge-ui/styles/tokens.css';
```

```ts
import MetaforgeUI from 'metaforge-ui'

app.use(MetaforgeUI) // 注册全部 Mf* 组件
```

## 开发

```bash
pnpm --filter metaforge-ui dev    # playground（组件总览 / 主题切换）
pnpm --filter metaforge-ui build  # vue-tsc 类型检查
```
