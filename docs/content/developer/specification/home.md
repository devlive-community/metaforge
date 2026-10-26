---
title: 开发规范
---

本文档主要用来介绍 MetaForge 服务端和 UI 端的开发规范。

!!! danger

    请仔细阅读该开发规范，并且遵守该规范，否则可能导致服务端和 UI 端的代码提交无法审核通过。
!!!

## 代码提交规范

我们使用严格的提交信息格式：

```
<类型>(<范围>): <描述>

[可选 正文]

[可选 脚注]
```

类型必须是以下之一：

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 仅文档更改
- `style`: 不影响代码含义的更改（空格、格式化等）
- `refactor`: 既不修复错误也不添加功能的代码更改
- `perf`: 提高性能的代码更改
- `test`: 添加或修正测试
- `chore`: 构建过程或辅助工具的变动
- `revert`: 撤销之前的提交

范围应该是受影响的模块名称。

示例：
```
feat(auth): 实现基于 JWT 的身份验证

- 添加 JWT 中间件
- 实现令牌生成和验证
- 添加用户认证路由
- 集成 Redis 存储刷新令牌

# 如果涉及到关闭 GitHub 中的 issue 请添加以下信息，123 表示 issue 的编号
close (#123)
```

## 前端代码规范

!!! warning

    所有的 **组件** | **字段名** 必须都以驼峰命名法命名
!!!

``` vue title="正确示例"
export default defineComponent({
  name: 'RoleHome'
})

const showName = ref(false)
```

``` vue title="错误示例"
export default defineComponent({
  name: 'rolehome'
})

const showname = ref(false)
```

### 元素命名

!!! warning

    组件命名必须以 `Shadcn` 开头，如 `ShadcnButton`、`ShadcnInput`、`ShadcnSpace`
!!!

``` vue title="正确示例"
<ShadcnButton size="small" circle @click="handlerChangeInfo(true, row)">
    <ShadcnIcon icon="Pencil" size="15"/>
</ShadcnButton>
```

``` vue title="错误示例"
<shadcn-button size="small" circle @click="handlerChangeInfo(true, row)">
    <shadcn-icon icon="pencil" size="15"/>
</shadcn-button>
```

!!! warning

    属性命名首字母必须小写，如 `size`，多个单词必须已驼峰命名法命名，如 `showName`
!!!

``` vue title="正确示例"
const size = ref('small')

const showName = ref(false)
```

``` vue title="错误示例"
const Size = ref('small')

const showname = ref(false)
const show-name = ref(false)
```

!!! warning

    函数名必须符合以下规范
!!!

- 处理函数必须以 `handle` 开头，如 `handleChangeInfo`
- 回调函数必须以 `on` 开头，如 `onChange`
- 涉及到弹出层的函数必须以 `visible` 开头，如 `visibleInfo`

``` vue title="正确示例"
const handleChangeInfo = () => {}

const onChange = () => {}

const visibleInfo = () => {}
```

``` vue title="错误示例"
const changeinfo = () => {}

const change = () => {}

const aaaInfo = () => {}
```

### 组件换行

!!! note

    多个组件内必须以新行分割
!!!

``` vue title="正确示例"
<ShadcnSpace>
    <ShadcnTooltip :content="$t('common.editData')">
        <ShadcnButton size="small" circle @click="handlerChangeInfo(true, row)">
            <ShadcnIcon icon="Pencil" size="15"/>
        </ShadcnButton>
    </ShadcnTooltip>

    <ShadcnTooltip :content="$t('role.common.assignMenu').replace('$NAME', row?.name)">
        <ShadcnButton size="small" circle @click="handlerAssignMenu(true, row)">
            <ShadcnIcon icon="Menu" size="15"/>
        </ShadcnButton>
    </ShadcnTooltip>
</ShadcnSpace>
```

``` vue title="错误示例"
<ShadcnSpace>
    <ShadcnTooltip :content="$t('common.editData')">
        <ShadcnButton size="small" circle @click="handlerChangeInfo(true, row)">
            <ShadcnIcon icon="Pencil" size="15"/>
        </ShadcnButton>
    </ShadcnTooltip>
    <ShadcnTooltip :content="$t('role.common.assignMenu').replace('$NAME', row?.name)">
        <ShadcnButton size="small" circle @click="handlerAssignMenu(true, row)">
            <ShadcnIcon icon="Menu" size="15"/>
        </ShadcnButton>
    </ShadcnTooltip>
</ShadcnSpace>
```

### 属性排序

!!! warning

    请保证属性的顺序，以便于后续的维护
!!!

多属性的正常顺序是

- 系统属性 (必须都在第一位，如 `v-model`)
- 固定属性 (如 `show-total`)
- 动态变量 (如 `:page-size="pageSize"`)
- 动态事件 (如 `@on-change="onPageChange"`)

``` vue title="正确示例"
<ShadcnPagination v-model="pageIndex" show-total :page-size="pageSize" @on-change="onPageChange"/>
```

``` vue title="错误示例"
<ShadcnPagination v-model="pageIndex" @on-change="onPageChange" show-total :page-size="pageSize"/>
```

### 属性换行

!!! warning

    多个属性且超过 3 个必须以新行分割
!!!

``` vue title="正确示例"
<ShadcnPagination v-model="pageIndex" show-total @on-change="onPageChange"/>
                  
<ShadcnPagination v-model="pageIndex"
                  show-total
                  :page-size="pageSize"
                  @on-change="onPageChange"/>
```

``` vue title="错误示例"
<ShadcnPagination v-model="pageIndex"
                  show-total
                  @on-change="onPageChange"/>

<ShadcnPagination v-model="pageIndex" show-total :page-size="pageSize" @on-change="onPageChange"/>
```

## UI 组件规范

- 页面组件一律使用 `metaforge-ui` 组件库（`Mf` 前缀，如 `MfButton`、`MfSelect`），禁止引入其他 UI 框架。
- 样式必须使用语义 token 工具类（`bg-surface`、`text-content`、`border-line`、`bg-primary` 等），**不允许**写死调色板类（如 `bg-blue-500`）。
- 主题切换由 `--mf-*` CSS 变量驱动（light/dark），新组件样式必须保证两种主题下可读。
- 组件 props/事件命名遵循 antd 兼容映射，迁移或新写页面参考 `core/metaforge-web/docs/antd-to-mf.md`。
- 组件库开发与演示：`pnpm --filter metaforge-ui dev`（playground）；前端构建：`pnpm --filter metaforge-web build`。
