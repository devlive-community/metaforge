import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AuthResponse } from '@/model/user/response/auth'
import { TokenUtils } from '@/utils/token'
import Common from '@/utils/common'
import UserService from '@/services/user'
import { normalizeNavigation } from '@/router/navigation'

export const useUserStore = defineStore('user', () => {
    // 初始化用户数据
    const userInfo = ref<any>(TokenUtils.getAuthUser() || {} as any)
    const menu = ref(normalizeNavigation(TokenUtils.getUserMenu(), TokenUtils.getAuthUser()))

    const isLoggedIn = computed(() => {
        return !!userInfo.value && Object.keys(userInfo.value).length > 0
    })

    // 获取最新用户信息
    const fetchUserInfo = async () => {
        try {
            const response = await UserService.getInfo()
            userInfo.value = response.data
            return true
        }
        catch (error) {
            console.error('Failed to fetch user info:', error)
            return false
        }
    }

    // 更新菜单
    const updateMenu = (newMenu: any) => {
        menu.value = normalizeNavigation(newMenu, TokenUtils.getAuthUser())
        localStorage.setItem(Common.menu, JSON.stringify(menu.value))
    }

    const fetchMenus = async () => {
        try {
            const response = await UserService.getMenus()
            if (response.status && Array.isArray(response.data)) updateMenu(response.data)
        }
        catch (error) { console.error('Failed to refresh navigation:', error) }
    }

    // 登出
    const logout = () => {
        userInfo.value = {} as AuthResponse
        menu.value = []
        localStorage.removeItem(Common.token)
        localStorage.removeItem(Common.menu)
        localStorage.removeItem(Common.userEditorConfigure)
    }

    return {
        userInfo,
        menu,
        isLoggedIn,
        fetchUserInfo,
        updateMenu,
        fetchMenus,
        logout
    }
})
