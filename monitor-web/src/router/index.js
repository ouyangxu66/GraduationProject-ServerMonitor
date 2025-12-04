import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import SshConsole from '../views/SshConsole.vue'
import ServerManage from "@/views/ServerManage.vue";

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
  ]
})

export default router