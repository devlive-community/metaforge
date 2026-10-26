import { RouteLocationNormalizedLoaded } from 'vue-router'

/**
 * Vue Router parameter utility class
 * Vue Router 参数工具类
 */
export class RouterUtils
{
    /**
     * Get route parameter by name
     * 通过参数名获取路由参数
     *
     * @param route - Current route object from useRoute() / 来自 useRoute() 的当前路由对象
     * @param paramName - Parameter name to extract / 要提取的参数名称
     * @returns Parameter value or empty string / 参数值或空字符串
     */
    static getParam(route: RouteLocationNormalizedLoaded, paramName: string): string
    {
        return (route.params[paramName] as string) || ''
    }

    /**
     * Get route parameter by path index (useful for getting last segment)
     * 通过路径索引获取路由参数（适用于获取最后一段）
     *
     * @param route - Current route object from useRoute() / 来自 useRoute() 的当前路由对象
     * @param index - Index from end of path (default: -1 for last segment) / 从路径末尾开始的索引（默认：-1表示最后一段）
     * @returns Path segment or empty string / 路径段或空字符串
     */
    static getPathSegment(route: RouteLocationNormalizedLoaded, index: number = -1): string
    {
        const segments = route.path.split('/').filter(Boolean)
        const actualIndex = index < 0 ? segments.length + index : index
        return segments[actualIndex] || ''
    }

    /**
     * Get query parameter by name
     * 通过参数名获取查询参数
     *
     * @param route - Current route object from useRoute() / 来自 useRoute() 的当前路由对象
     * @param queryName - Query parameter name / 查询参数名称
     * @returns Query value or empty string / 查询参数值或空字符串
     */
    static getQuery(route: RouteLocationNormalizedLoaded, queryName: string): string
    {
        return (route.query[queryName] as string) || ''
    }
}
