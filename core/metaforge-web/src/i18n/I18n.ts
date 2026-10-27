import { createI18n } from 'vue-i18n'
import { inject, provide, ref } from 'vue'
import I18nService from '@/services/i18n'
import { createLocaleLoader, normalizeLocale } from './locale-loader'

// 默认语言配置
// Default locale
const DEFAULT_LOCALE = 'zh-cn'
const DEFAULT_MESSAGES = {
    'zh-cn': {
        // 基础的回退翻译
        // Basic fallback translation
        common: {
            loading: 'Loading...',
            networkError: 'Network error',
            loadingFailed: 'Loading failed'
        },
        setup: {
            title: '安装 MetaForge',
            subtitle: '首次启动引导，配置管理员账号后即可使用',
            installedTip: '该实例已完成安装，即将前往登录页',
            steps: {
                environment: '环境检查',
                database: '数据库',
                account: '管理员账号',
                finish: '完成安装'
            },
            database: {
                current: '使用当前数据库',
                currentTip: '应用已连接配置的数据库，直接在当前库中初始化',
                choose: '请选择用于安装的数据库',
                embedded: '使用内置存储（推荐）',
                embeddedTip: '无需外部数据库，数据保存在本地文件中，开箱即用，重启不丢失',
                mysql: '连接 MySQL',
                mysqlTip: '安装到外部 MySQL，请先创建空数据库；配置将写入 config/application.properties，安装后需重启服务',
                degradedTitle: '配置的数据库当前不可用',
                degradedTip: '服务已在内置降级模式下运行，原数据源：',
                host: '主机',
                port: '端口',
                name: '数据库名',
                username: '用户名',
                password: '密码',
                passwordPlaceholder: '请输入数据库密码',
                connectionSettings: 'MySQL 连接信息',
                connectionSettingsTip: '填写数据库凭据，测试连接通过后即可继续',
                connectionFailedHint: '请检查数据库地址、用户名和密码，确认数据库已启动并允许访问后重试。',
                errorDetails: '查看错误详情'
            },
            env: {
                database: '数据库',
                product: '数据库类型',
                version: '数据库版本',
                connected: '已连接',
                disconnected: '连接失败',
                test: '测试连接',
                testing: '测试中...',
                elapsed: '耗时',
                testFailed: '连接测试失败',
                embeddedMode: '内置模式',
                notConfigured: '尚未配置数据库',
                chooseDatabase: '请在下一步选择内置存储或配置 MySQL 数据库'
            },
            account: {
                username: '用户名',
                password: '密码',
                confirmPassword: '确认密码',
                name: '昵称（可选）',
                usernameTip: '请输入用户名',
                usernameSizeTip: '用户名长度须为 3-20 个字符',
                passwordTip: '请输入密码',
                passwordSizeTip: '密码长度须为 6-20 个字符',
                passwordNotMatchTip: '两次输入的密码不一致'
            },
            install: {
                running: '正在初始化数据库并创建管理员账号...',
                submit: '开始安装',
                success: '安装完成',
                successTip: '管理员账号已创建，请使用该账号登录系统',
                failed: '安装失败',
                goToSignin: '前往登录',
                restartTip: '数据库配置已写入下方文件，请重启服务后刷新页面完成安装',
                acknowledge: '我已重启，刷新页面',
                retry: '重试'
            },
            next: '下一步',
            back: '上一步'
        }
    },
    en: {
        common: {
            loading: 'Loading...',
            networkError: 'Network error',
            loadingFailed: 'Loading failed'
        },
        setup: {
            title: 'Install MetaForge',
            subtitle: 'First-boot wizard, set up the administrator to continue',
            installedTip: 'This instance is already installed, redirecting to sign in',
            steps: {
                environment: 'Environment',
                database: 'Database',
                account: 'Administrator',
                finish: 'Finish'
            },
            database: {
                current: 'Use current database',
                currentTip: 'Initialize the database already configured for this instance',
                choose: 'Choose the database to use for installation',
                embedded: 'Use embedded storage (recommended)',
                embeddedTip: 'No external database needed, data is kept in a local file and survives restarts',
                mysql: 'Connect MySQL',
                mysqlTip: 'Install into an external MySQL (create an empty database first); the datasource is persisted to config/application.properties and a restart is required',
                degradedTitle: 'The configured database is unavailable',
                degradedTip: 'The server is running on the embedded fallback store, original datasource:',
                host: 'Host',
                port: 'Port',
                name: 'Database',
                username: 'Username',
                password: 'Password',
                passwordPlaceholder: 'Enter the database password',
                connectionSettings: 'MySQL connection',
                connectionSettingsTip: 'Enter your database credentials and test the connection to continue',
                connectionFailedHint: 'Check the host, username and password, and make sure the database is running and allows connections.',
                errorDetails: 'View error details'
            },
            env: {
                database: 'Database',
                product: 'Database type',
                version: 'Database version',
                connected: 'Connected',
                disconnected: 'Connection failed',
                test: 'Test connection',
                testing: 'Testing...',
                elapsed: 'Elapsed',
                testFailed: 'Connection test failed',
                embeddedMode: 'Embedded mode',
                notConfigured: 'No database configured',
                chooseDatabase: 'Choose embedded storage or configure MySQL in the next step'
            },
            account: {
                username: 'Username',
                password: 'Password',
                confirmPassword: 'Confirm password',
                name: 'Nickname (optional)',
                usernameTip: 'Please input the username',
                usernameSizeTip: 'Username must be 3-20 characters',
                passwordTip: 'Please input the password',
                passwordSizeTip: 'Password must be 6-20 characters',
                passwordNotMatchTip: 'Passwords do not match'
            },
            install: {
                running: 'Initializing database and creating administrator...',
                submit: 'Install',
                success: 'Installation finished',
                successTip: 'The administrator account is ready, sign in with it',
                failed: 'Installation failed',
                goToSignin: 'Go to sign in',
                restartTip: 'The datasource was persisted to the file below, restart the server and refresh to finish',
                acknowledge: 'Restarted, refresh page',
                retry: 'Retry'
            },
            next: 'Next',
            back: 'Back'
        }
    }
}

const i18n = createI18n({
    fallbackLocale: DEFAULT_LOCALE,
    globalInjection: true,
    legacy: false,
    silentFallbackWarn: true,
    fallbackWarn: false,
    silentTranslationWarn: true,
    missingWarn: false,
    flatJson: true,
    locale: normalizeLocale(localStorage.getItem('locale') || navigator.language || DEFAULT_LOCALE),
    messages: DEFAULT_MESSAGES
})

const ensureLocaleLoaded = createLocaleLoader(
    locale => I18nService.getLocale(locale),
    (locale, messages) => i18n.global.mergeLocaleMessage(locale, messages),
)
let localeRequest = 0

export const I18N_KEY = Symbol('i18n-handler')

export const loadingState = ref(false)
export const loadingText = ref('')
export const errorState = ref(false)
export const errorMessage = ref('')

export function provideI18nHandler()
{
    const resetState = () => {
        loadingState.value = false
        loadingText.value = ''
        errorState.value = false
        errorMessage.value = ''
    }

    const loadLocale = async (locale: string) => {
        const request = ++localeRequest
        resetState()
        loadingState.value = true
        loadingText.value = i18n.global.t('common.loading')

        try {
            const normalized = await ensureLocaleLoaded(locale)
            if (request !== localeRequest) return false
            i18n.global.locale.value = normalized
            localStorage.setItem('locale', normalized)
            resetState()
            return true
        }
        catch (error) {
            if (request !== localeRequest) return false
            errorState.value = true
            errorMessage.value = error instanceof Error ? error.message : i18n.global.t('common.loadingFailed')

            console.error('Failed to load locale:', error)
            return false
        }
        finally {
            if (request === localeRequest) loadingState.value = false
        }
    }

    const i18nHandler = {
        loadLocale,
        loadingState,
        loadingText,
        errorState,
        errorMessage,
        resetState
    }

    provide(I18N_KEY, i18nHandler)

    return i18nHandler
}

// Hook
export function useI18nHandler()
{
    const i18nHandler = inject(I18N_KEY)
    if (!i18nHandler) {
        throw new Error('useI18nHandler must be used after provideI18nHandler')
    }
    return i18nHandler
}

export default i18n
