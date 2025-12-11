import { createRouter, createWebHistory } from 'vue-router'

// 引入 Layout
import MainLayout from '@/layout/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 1. 登录页 (顶级路由，独占全屏),注册页
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' }
    },
      // 注册页
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/register/index.vue'),
      meta: {title:'注册账号'}
    },
    // 2. 业务页 (包裹在 Layout 中)
    {
      path: '/',
      component: MainLayout, // 根路径使用 Layout 组件
      redirect: '/dashboard',
      children: [
          // 仪表盘路由
        {
          path: 'dashboard', // 注意：子路由路径不需要加 /
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '监控大屏', requiresAuth: true }
        },
          // 监控路由
        {
          path: 'ssh',
          name: 'WebSsh',
          component: () => import('@/views/monitor/WebSsh.vue'),
          meta: { title: '远程终端', requiresAuth: true }
        },
          // 服务器管理路由
        {
          path: 'server',
          name: 'Server',
          component: () =>import('@/views/monitor/ServerManage.vue'),
          meta: { title: '服务器管理', requiresAuth: true }
        },
          // 菜单管理路由
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: { title: '个人中心', requiresAuth: true }
        },
         //用户管理路由
        {
          path: 'userManage',
          name: 'UserManage',
          component: () => import('@/views/system/UserManage.vue'),
          meta: { title: '用户管理', requiresAuth: true }
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