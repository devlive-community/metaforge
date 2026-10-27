import * as LucideIcons from '@lucide/vue'
import {
    CircleCheck,
    CircleHelp,
    CircleX,
    Clock,
    Columns3,
    Eraser,
    Info,
    LayoutDashboard,
    LayoutGrid,
    Layers,
    MoveVertical,
    Network,
    PanelLeftClose,
    PanelLeftOpen,
    Play,
    RefreshCw,
    RotateCw,
    Server,
    Settings,
    SquareFunction,
    SquareX,
    Boxes,
    FileCode2,
    ArrowUpNarrowWide,
    Rows3,
    Type,
    TriangleAlert,
    Zap,
    ArrowUpToLine
} from '@lucide/vue'

/**
 * 历史兼容：lucide 迁移前，存量菜单 icon 字段保存的是 antd 图标名（去掉 Outlined 后缀）。
 * 这里把这些旧名称映射到最接近的 lucide 图标，保证升级部署的存量菜单仍可渲染；
 * 与 lucide 导出同名的旧名称（Table、History、User、Delete、Edit 等）无需映射，
 * 已由下方全量注册覆盖。
 */
const LEGACY_ANTD_ALIASES: Record<string, any> = {
    Appstore: LayoutGrid,
    CaretRight: Play,
    CheckCircle: CircleCheck,
    Clear: Eraser,
    ClockCircle: Clock,
    CloseCircle: CircleX,
    CloseSquare: SquareX,
    CloudServer: Server,
    Cluster: Network,
    ColumnHeight: MoveVertical,
    Dashboard: LayoutDashboard,
    DeploymentUnit: Boxes,
    FontSize: Type,
    Function: SquareFunction,
    InfoCircle: Info,
    InsertRowRight: Columns3,
    MenuFold: PanelLeftClose,
    MenuUnfold: PanelLeftOpen,
    Partition: Layers,
    QuestionCircle: CircleHelp,
    Reload: RotateCw,
    Setting: Settings,
    Snippets: FileCode2,
    SortAscending: ArrowUpNarrowWide,
    SplitCells: Rows3,
    Sync: RefreshCw,
    Thunderbolt: Zap,
    VerticalAlignTop: ArrowUpToLine,
    Warning: TriangleAlert
}

/**
 * 菜单图标注册表：key 为存储在菜单 icon 字段的图标名，value 为渲染组件。
 * 图标名统一使用 lucide 命名（种子数据见 metaforge-server/schema/setup-data.sql），
 * 全量收录 @lucide/vue 导出的图标组件，供菜单图标选择器搜索与渲染。
 */

// @lucide/vue 的辅助导出（非图标组件），不参与注册。
const NON_ICON_EXPORTS = new Set(['icons', 'createLucideIcon'])

export const MENU_ICONS: Record<string, any> = {}

for (const [name, component] of Object.entries(LEGACY_ANTD_ALIASES)) {
    MENU_ICONS[name] = component
}

for (const [name, component] of Object.entries(LucideIcons)) {
    if (NON_ICON_EXPORTS.has(name)) {
        continue
    }
    if (!MENU_ICONS[name]) {
        MENU_ICONS[name] = component
    }
}
