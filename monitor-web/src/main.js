import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 👇 1. 引入 Element Plus 核心库
import ElementPlus from 'element-plus'
// 👇 2. 引入 Element Plus 的样式文件 (非常重要，不引只有功能没有样式)
import 'element-plus/dist/index.css'

// 👇 3. 引入图标库 (如果你以后要用图标的话)
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

app.use(router)

// 👇 4. 安装 Element Plus 插件
app.use(ElementPlus)

// 注册所有图标 (可选，建议加上)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.mount('#app')