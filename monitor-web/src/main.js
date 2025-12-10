import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入路由守卫
import './router/permission'

const app = createApp(App)

// 🟢 第一步：先创建并安装 Pinia
const pinia = createPinia()
app.use(pinia)

// 🟢 第二步：Pinia 安装完成后，才能初始化 ThemeStore
// 注意：这里需要把 import 放到顶部，但调用必须在 use(pinia) 之后
import { useThemeStore } from '@/stores/theme'
// 这一行代码会触发 getActivePinia()，所以必须在 app.use(pinia) 之后执行
const themeStore = useThemeStore()

// 注册图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(router)
app.use(ElementPlus)

app.mount('#app')