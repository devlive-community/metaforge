// Must stay the first import: turns old /#/ addresses into paths before the router is created.
import './legacy-hash-entry'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './styles/theme.css'
import './style.css'
// metaforge-ui（Tailwind v4 + token）入口：加载在本地样式之后，
// 其生成的工具类与 --mf-* token 对全应用生效
import 'metaforge-ui/styles/metaforge-ui.css'
import App from './App.vue'
import router from '@/router'
import i18n from '@/i18n/I18n'
import { createIcons } from '@/fontawesome'
import MetaforgeUI from 'metaforge-ui'
import { sharedReportPath, sharedScreenPath } from '@/router/shared'
import PublicReportApp from '@/views/public/PublicReportApp.vue'
import PublicScreenApp from '@/views/public/PublicScreenApp.vue'

// Shared report addresses open a standalone public page without the app router and sign-in.
const sharedScreen = sharedScreenPath(window.location.pathname)
const sharedReport = sharedScreen ? null : sharedReportPath(window.location.pathname)
const shared = sharedScreen || sharedReport
const app = sharedScreen ? createApp(PublicScreenApp, sharedScreen) : sharedReport ? createApp(PublicReportApp, sharedReport) : createApp(App)
createIcons(app)
// Disable warnings
app.config.warnHandler = () => null
if (!shared) app.use(router)
app.use(i18n)
app.use(createPinia())
app.use(MetaforgeUI)
app.provide('$t', i18n.global.t).mount('#app')
