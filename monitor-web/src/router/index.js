import { createRouter, createWebHistory } from 'vue-router'

// 引入 Layout
import MainLayout from '@/layout/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 1. 登录页 (顶级路由，独占全屏)
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
    // 2. 业务页 (包裹在 Layout 中)
    {
      path: '/',
      component: MainLayout, // 根路径使用 Layout 组件
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard', // 注意：子路由路径不需要加 /
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '监控大屏', requiresAuth: true }
        },
        {
          path: 'ssh',
          name: 'WebSsh',
          component: () => import('@/views/monitor/WebSsh.vue'),
          meta: { title: '远程终端', requiresAuth: true }
        },
        {
          path: 'server',
          name: 'Server',
          component: () =>import('@/views/monitor/ServerManage.vue')
        }
      ]
    },
    // 404
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard'
    }
  ]
})

export default router