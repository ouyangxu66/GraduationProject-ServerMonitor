import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/monitor/Dashboard.vue'
import SshConsole from '../views/monitor/SshConsole.vue'
import ServerManage from "@/views/monitor/ServerManage.vue"
import Login from '../views/login/index.vue';
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
    },

     // 添加 ServerManage 路由
    {
      path: '/server',
      name: 'server',
      component: ServerManage
    },
    //添加 Login 路由
    {
      path: '/login',
      name: 'Login',
      component: Login
    }
  ]
})

export default router