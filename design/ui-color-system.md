## MetaForge UI Color System

### 1. 品牌主色

| Token         | 色值        | 用途                       |
| ------------- | --------- | ------------------------ |
| `primary-50`  | `#F2FBF8` | 页面浅色区域、Hover 背景          |
| `primary-100` | `#DDF5EC` | 选中菜单背景、浅色 Tag            |
| `primary-200` | `#B9EAD9` | 辅助装饰、浅边框                 |
| `primary-300` | `#87D9BE` | 插画、渐变辅助                  |
| `primary-400` | `#49C399` | 次级强调                     |
| `primary-500` | `#18A779` | 主品牌绿色                    |
| `primary-600` | `#0A8F67` | **主按钮 / Active / 核心品牌色** |
| `primary-700` | `#087A59` | Hover                    |
| `primary-800` | `#086247` | Pressed                  |
| `primary-900` | `#064D39` | 深色强调                     |

我建议真正作为 MetaForge 的核心品牌色固定为：

```css
--primary: #0A8F67;
```

---

## 2. 页面背景

你刚才原型里整个 Body 背景不要使用纯白。

| Token   | 色值                      | 用途           |
| ------- | ----------------------- | ------------ |
| 页面背景    | `#F6FAF9`               | Body 主背景     |
| 内容背景    | `#FFFFFF`               | Card / Panel |
| 浅绿色背景   | `#F3FBF8`               | 品牌区域         |
| 更浅绿色    | `#F7FCFA`               | 大面积装饰区域      |
| Sidebar | `#FBFDFC`               | 左侧菜单         |
| Header  | `rgba(255,255,255,.92)` | 顶栏           |

推荐：

```css
body {
  background: #F6FAF9;
}
```

如果想保留现在原型那种很轻的绿色环境光，可以：

```css
body {
  background:
    radial-gradient(
      circle at 80% 0%,
      rgba(10, 143, 103, 0.06),
      transparent 32%
    ),
    #F6FAF9;
}
```

这样比直接铺一张大背景图更适合后台系统。

---

## 3. 文字颜色

不要全部使用纯黑 `#000`。

| Token                | 色值        | 用途          |
| -------------------- | --------- | ----------- |
| `text-primary`       | `#14201C` | 标题、主要文字     |
| `text-secondary`     | `#4F625B` | 普通说明        |
| `text-tertiary`      | `#7D8D87` | 辅助文字        |
| `text-placeholder`   | `#A8B3AF` | Placeholder |
| `text-disabled`      | `#BDC6C2` | Disabled    |
| `text-inverse`       | `#FFFFFF` | 深色按钮文字      |
| `text-primary-brand` | `#0A8F67` | 品牌强调文字      |

页面标题：

```css
color: #14201C;
```

说明文本：

```css
color: #7D8D87;
```

---

## 4. Border / Divider

| 类型             | 色值        |
| -------------- | --------- |
| 默认 Border      | `#E3EAE7` |
| 浅 Border       | `#EDF2F0` |
| Hover Border   | `#C9D8D2` |
| Primary Border | `#87D9BE` |
| Focus Border   | `#0A8F67` |
| Divider        | `#EDF1EF` |

比如 Input 默认：

```css
border: 1px solid #E3EAE7;
```

Focus：

```css
border-color: #0A8F67;
box-shadow: 0 0 0 3px rgba(10, 143, 103, 0.10);
```

---

## 5. Card

我建议 Card 不要太厚重。

```css
background: #FFFFFF;
border: 1px solid #E9EFEC;
border-radius: 12px;
box-shadow:
  0 1px 2px rgba(15, 23, 42, 0.02),
  0 4px 16px rgba(15, 23, 42, 0.035);
```

Hover Card：

```css
box-shadow:
  0 4px 10px rgba(15, 23, 42, 0.03),
  0 12px 30px rgba(15, 23, 42, 0.05);
```

重点是：

**白色 Card + 极浅灰绿背景 + 很轻的阴影。**

---

# 6. Button

### Primary Button

正常：

```css
background: #0A8F67;
color: #FFFFFF;
```

Hover：

```css
background: #087A59;
```

Pressed：

```css
background: #086247;
```

Disabled：

```css
background: #B9EAD9;
color: rgba(255,255,255,.8);
```

---

### Secondary Button

```css
background: #FFFFFF;
border: 1px solid #DDE6E2;
color: #34443E;
```

Hover：

```css
background: #F6FAF9;
border-color: #B9CAC3;
```

---

### Ghost Button

```css
background: transparent;
color: #52635D;
```

Hover：

```css
background: #F0F7F4;
```

---

## 7. Sidebar

当前设计最适合这种：

默认菜单：

```css
color: #40534C;
background: transparent;
```

Hover：

```css
background: #F2F8F5;
```

Active：

```css
background: #DDF5EC;
color: #087A59;
```

Active 左侧条：

```css
background: #0A8F67;
```

也可以使用：

```css
border-left: 3px solid #0A8F67;
```

---

# 8. 状态颜色

不要只使用绿色。

| 状态      | 主色        | 浅背景       | 边框        |
| ------- | --------- | --------- | --------- |
| Success | `#16A66A` | `#EAF8F1` | `#BDEAD2` |
| Info    | `#3B82F6` | `#EFF6FF` | `#BFDBFE` |
| Warning | `#E5A11A` | `#FFF8E6` | `#F7DA94` |
| Error   | `#DC5656` | `#FFF1F1` | `#F4C1C1` |

例如：

账户正常：

```css
background: #EAF8F1;
color: #128052;
```

管理员：

```css
background: #EAF4FF;
color: #3478D4;
```

这样「管理员」和「正常」不要全部做成绿色。

---

# 9. Input

默认：

```css
background: #FFFFFF;
border: 1px solid #E0E7E4;
color: #24332E;
```

Hover：

```css
border-color: #B8C8C1;
```

Focus：

```css
border-color: #0A8F67;
box-shadow: 0 0 0 3px rgba(10,143,103,.10);
```

Disabled：

```css
background: #F5F7F6;
color: #9BA8A3;
```

---

# 10. Avatar

默认头像背景可以固定成：

```css
background:
  linear-gradient(
    145deg,
    #70D1AE 0%,
    #0A8F67 100%
  );
```

头像外围：

```css
border: 4px solid #FFFFFF;
box-shadow: 0 6px 18px rgba(10,143,103,.12);
```

---

# 11. Hero / Banner

你刚才喜欢的绿色效果建议固定：

```css
background:
  linear-gradient(
    120deg,
    #F4FCF9 0%,
    #E8F9F2 46%,
    #DDF5EC 100%
  );
```

上面叠一些非常淡的：

* Wave
* 数据流线
* 几何 Cube
* 圆形光斑

但透明度控制在：

```text
4% ～ 15%
```

不要抢内容。

---

# 12. 整个 Body 的视觉基调

最终我建议 MetaForge 页面保持：

```css
background:
  radial-gradient(
    circle at 90% 5%,
    rgba(87, 211, 167, 0.08),
    transparent 25%
  ),
  radial-gradient(
    circle at 0% 90%,
    rgba(10, 143, 103, 0.04),
    transparent 24%
  ),
  #F6FAF9;
```

这样实际上**不需要依赖整张 Body 图片**，适配不同分辨率会更稳定。

---

# 13. Dark Text hierarchy

以后所有页面统一：

```text
Page Title
#14201C

Card Title
#182A24

Body
#465A52

Description
#7D8D87

Placeholder
#A8B3AF
```

---

# 14. MetaForge 最终核心色板

这是我建议你真正保存到项目里的核心 12 色：

| 名称             | 色值        |
| -------------- | --------- |
| Brand          | `#0A8F67` |
| Brand Hover    | `#087A59` |
| Brand Light    | `#DDF5EC` |
| Brand Surface  | `#F3FBF8` |
| Body           | `#F6FAF9` |
| Surface        | `#FFFFFF` |
| Border         | `#E3EAE7` |
| Title          | `#14201C` |
| Text           | `#465A52` |
| Secondary Text | `#7D8D87` |
| Success        | `#16A66A` |
| Danger         | `#DC5656` |

---

## CSS Variables 可以直接这样定义

```css
:root {
  /* Brand */
  --dc-primary: #0A8F67;
  --dc-primary-hover: #087A59;
  --dc-primary-active: #086247;
  --dc-primary-light: #DDF5EC;
  --dc-primary-surface: #F3FBF8;

  /* Background */
  --dc-bg: #F6FAF9;
  --dc-surface: #FFFFFF;
  --dc-sidebar: #FBFDFC;

  /* Text */
  --dc-text-primary: #14201C;
  --dc-text-secondary: #465A52;
  --dc-text-tertiary: #7D8D87;
  --dc-text-placeholder: #A8B3AF;
  --dc-text-disabled: #BDC6C2;

  /* Border */
  --dc-border: #E3EAE7;
  --dc-border-light: #EDF2F0;
  --dc-border-hover: #C9D8D2;

  /* Status */
  --dc-success: #16A66A;
  --dc-info: #3B82F6;
  --dc-warning: #E5A11A;
  --dc-danger: #DC5656;

  /* Radius */
  --dc-radius-sm: 6px;
  --dc-radius-md: 10px;
  --dc-radius-lg: 12px;
  --dc-radius-xl: 16px;

  /* Shadow */
  --dc-shadow-card:
    0 1px 2px rgba(15, 23, 42, 0.02),
    0 4px 16px rgba(15, 23, 42, 0.035);

  --dc-shadow-focus:
    0 0 0 3px rgba(10, 143, 103, 0.10);
}

