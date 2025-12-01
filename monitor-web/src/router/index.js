import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import SshConsole from '../views/SshConsole.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Dashboard // 首页直接指向我们写的 Dashboard
    },

    //添加 SshConsole 路由
    {
      path: '/ssh',
      name: 'ssh',
      component: SshConsole
    }
  ]
})

export default router