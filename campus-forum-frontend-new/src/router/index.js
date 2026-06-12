import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue')
      },
      {
        path: 'board/:id',
        name: 'Board',
        component: () => import('@/views/post/PostList.vue')
      },
      {
        path: 'post/:id',
        name: 'PostDetail',
        component: () => import('@/views/post/PostDetail.vue')
      },
      {
        path: 'post/create',
        name: 'PostCreate',
        component: () => import('@/views/post/PostCreate.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'post/:id/edit',
        name: 'PostEdit',
        component: () => import('@/views/post/PostCreate.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'profile/:id',
        name: 'UserProfile',
        component: () => import('@/views/profile/Profile.vue')
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/message/Message.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'ai',
        name: 'AiAsk',
        component: () => import('@/views/ai/AiAsk.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/SearchResult.vue')
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue')
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPassword.vue')
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/admin/Dashboard.vue')
      },
      {
        path: 'users',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'content',
        name: 'ContentAudit',
        component: () => import('@/views/admin/ContentAudit.vue')
      },
      {
        path: 'ai-knowledge',
        name: 'AiKnowledge',
        component: () => import('@/views/admin/AiKnowledge.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const isAuthenticated = to.meta.requiresAuth
    ? await userStore.ensureSession()
    : userStore.isLoggedIn

  if (to.meta.requiresAuth && !isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next({ name: 'Home' })
  } else {
    next()
  }
})

export default router
