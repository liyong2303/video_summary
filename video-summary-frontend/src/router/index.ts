import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/SubmitView.vue'),
    },
    {
      path: '/history',
      name: 'history',
      component: () => import('../views/HistoryView.vue'),
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('../views/SettingsView.vue'),
    },
    {
      path: '/custom-prompts',
      name: 'custom-prompts',
      component: () => import('../views/CustomPromptView.vue'),
    },
    {
      path: '/task/:id',
      name: 'task',
      component: () => import('../views/TaskResultView.vue'),
    },
    { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue') },
    { path: '/register', name: 'Register', component: () => import('../views/RegisterView.vue') },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const publicPages = ['/login', '/register']
  if (!publicPages.includes(to.path) && !token) {
    return '/login'
  }
})

export default router
