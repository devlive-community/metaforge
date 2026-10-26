# ant-design-vue → metaforge-ui 迁移映射

目的：将 `a-*` 组件替换为 metaforge-ui 的 `Mf*` 组件，**保持功能与业务逻辑完全不变**。所有 Mf 组件已全局注册，模板中直接使用，无需 import；`import { message } from 'ant-design-vue'` 必须替换。

## 组件映射

| antd | metaforge-ui | 说明 |
|---|---|---|
| `a-button type="primary"` | `MfButton variant="primary"` | variant: primary/secondary(默认)/outline/ghost/danger/link |
| `a-button type="text"` | `MfButton variant="ghost"` | |
| `a-button html-type="submit"` | `MfButton type="submit"` | |
| `a-button danger` | `MfButton variant="danger"` | |
| `a-button :loading :disabled block size` | 同名 props | size: sm/md/lg |
| `a-input v-model:value="x"` | `MfInput v-model="x"` | **v-model:value → v-model** |
| `a-input-password` | `MfInput type="password" show-password` | |
| `a-input #prefix/#suffix` | `MfInput` 同名 slot | |
| `a-textarea` | `MfTextarea v-model="x"` | |
| `a-input-number v-model:value` | `MfInputNumber v-model` | min/max/step/precision/disabled |
| `a-select v-model:value` + `a-select-option` 子组件 | `MfSelect v-model :options="[{label, value, disabled?, group?}]"` | 选项改为数组；`a-select-opt-group` → option 带 `group: '分组名'`；`mode="multiple"` → `multiple`；`allowClear` → `clearable` |
| `a-checkbox v-model:checked` | `MfCheckbox v-model` | 数组用法（value + :model-value）同 antd |
| `a-checkbox-group :options` | `MfCheckboxGroup :options v-model` | |
| `a-radio-group v-model:value` + `a-radio :value` | `MfRadioGroup v-model` + `MfRadio :value label` | |
| `a-radio-group` + `a-radio-button` | `MfRadioGroup v-model variant="button"` + `MfRadio :value label` | |
| `a-switch v-model:checked` | `MfSwitch v-model` | size: sm/md |
| `a-form :model layout @finish @finishFailed` | `MfForm :model layout @finish @finish-failed` | |
| `a-form-item name label :rules` | `MfFormItem name label :rules` | rules 支持 required/min/max/pattern/message/`validator(value, model) => true \| 错误消息`（antd 的 `(_rule, value) => Promise` 改写成此签名） |
| `a-card title #extra` | `MfCard title #extra`（标题 slot 名为 `#header`） | |
| `a-tag color="xxx"` | `MfTag variant="default/primary/success/warning/danger/info"` | closable 保留 |
| `a-alert type message description show-icon closable banner` | `MfAlert variant title(内容放默认 slot)` | banner 视觉自行加 `rounded-none!` |
| `a-spin size="large"` | `MfSpinner size="lg"` | |
| `a-avatar :size="34" src` | `MfAvatar :size="34" src`（数字或 xs/sm/md/lg） | |
| `a-badge :count` | `MfBadge :count` | dot/max 保留 |
| `a-tooltip :title` | `MfTooltip :content` | |
| `a-popover #content trigger placement` | `MfPopover` 同名 | |
| `a-modal v-model:open title width #footer` | `MfModal` 同名 | |
| `a-drawer v-model:open title width placement` | `MfDrawer` 同名 | |
| `a-dropdown #overlay placement trigger="click"` | `MfDropdown` 同名 | |
| `a-menu :selectedKeys @click` + `a-menu-item :key` + `a-sub-menu` | `MfMenu :selected-keys @click` + `MfMenuItem :key_` + `MfSubMenu :key_ #title` | **`:key` → `:key_`**（Vue 保留字）；click 参数 `{key, keyPath}` 不变 |
| `a-menu-divider` | `<li class="my-1 border-t border-line" role="separator" />` | |
| `a-tabs v-model:activeKey @change` + `a-tab-pane key tab` | `MfTabs v-model:active-key @change` + `MfTabPane :item-key tab` | tab-position 保留 |
| `a-table :columns :data-source row-key` | `MfTable :columns :data-source row-key` | 自定义单元格：antd `#bodyCell="{ column, record }"` → `#cell-{columnKey}="{ record, text, index }"`；列头 `#headerCell` → `#header-{key}` |
| `a-tree :tree-data v-model:selectedKeys v-model:expandedKeys :load-data` | `MfTree :data v-model:selected-keys v-model:expanded-keys :load-data` | 节点 {key, title, children, isLeaf, disabled}；`#title="{ node }"` slot |
| `a-pagination v-model:current :total :page-size @change` | `MfPagination v-model:current :total :page-size @change` | |
| `a-skeleton :loading` | `MfSkeleton :loading` | |
| `a-result status title sub-title #extra` | `MfResult` 同名 | |
| `a-breadcrumb` + `a-breadcrumb-item` | `MfBreadcrumb` + `MfBreadcrumbItem` | |
| `a-upload action multiple` | `MfUpload action multiple v-model:file-list @change` | |
| `a-divider` | `MfDivider`（label/dashed 保留） | |
| `a-space direction="vertical" :size` | `MfSpace direction="vertical" :size` | size: xs/sm/md/lg；占满宽度加 `fill` |
| `a-layout / a-layout-sider / a-layout-content / a-layout-header` | 原生 div + Tailwind | `flex min-h-screen`、侧栏 `w-60 shrink-0` 等按原布局意图还原 |
| `a-row :gutter` + `a-col :span` | Tailwind grid | `:gutter="16"` → `gap-4`；span 24 栅格 → `grid-cols-24` 或换算成 `grid-cols-N`（span 12→2 列布局用 `sm:grid-cols-2`）；响应式 xs/sm/md 属性 → `sm:` `md:` 前缀 |
| `a-empty` | `<div class="py-10 text-center text-sm text-content-tertiary">{{ $t('common.noData') }}</div>` | |
| `a-progress` / `a-steps` 等 | 少见；遇到时用 Tailwind 还原相同交互并保留数据绑定 | |

## 命令式 API

- `import { message } from 'ant-design-vue'` → `import { MfMessage } from 'metaforge-ui'`，`message.success/error/info/warning(x)` → `MfMessage.success/...`（完全同名兼容）
- `Modal.confirm` 本仓库未使用

## 硬性规则

1. **不改业务逻辑**：script 中除以下情况外一行不动：
   - `v-model:value` → `v-model`（`v-model:checked`/`v-model:open`/`v-model:activeKey` → Mf 对应绑定，`v-model:open`/`v-model:activeKey` 名称不变）
   - antd `message` 导入与调用替换
   - antd 专属类型/导入（`import { theme } from 'ant-design-vue'` 等）删除
   - validator 函数签名改写
2. **保留全部 i18n**（`$t(...)` 原样）、图标（`@ant-design/icons-vue` 继续使用，不改名）、router/service 调用。
3. **样式**：antd 类（如 `text-gray-400`）可用语义 token 类替换（`text-content-tertiary` 等），但不确定时保留原类不删；scoped style 里 `.ant-*` 深度选择器整块删除或改写为普通类。
4. **props 命名**：模板内 kebab-case（`:selected-keys`、`@finish-failed`）。
5. 遇到映射表未覆盖的 antd 组件：优先用已注册 Mf 组件组合 + Tailwind 还原同样交互，禁止保留任何 `a-*`。
6. `:style` 绑定、`class` 原样保留。

## 常用语义类（来自 --mf-* token）

`bg-page bg-surface bg-surface-hover bg-surface-active text-content text-content-secondary text-content-tertiary border-line border-line-strong rounded-mf-md rounded-mf-lg shadow-mf-sm text-primary bg-primary text-on-primary text-danger text-success text-warning text-info bg-danger-subtle`
