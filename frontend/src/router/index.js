import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  { path: '/register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: () => import('../layouts/PassengerLayout.vue'),
    children: [
      { path: '', component: () => import('../views/passenger/Home.vue') },
      { path: 'flights', component: () => import('../views/passenger/FlightList.vue') },
      { path: 'flights/:id', component: () => import('../views/passenger/FlightDetail.vue') },
      { path: 'book/:id', component: () => import('../views/passenger/Book.vue'), meta: { auth: true } },
      { path: 'orders', component: () => import('../views/passenger/Orders.vue'), meta: { auth: true } },
      { path: 'orders/:id', component: () => import('../views/passenger/OrderDetail.vue'), meta: { auth: true } },
      { path: 'profile', component: () => import('../views/passenger/Profile.vue'), meta: { auth: true } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'users', component: () => import('../views/admin/Users.vue') },
      { path: 'flights', component: () => import('../views/admin/Flights.vue') },
      { path: 'orders', component: () => import('../views/admin/Orders.vue') }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLogin) return '/login'
  if (to.meta.admin && !auth.user?.role) return '/'
  if (to.meta.admin && !auth.isAdmin) return '/'
})

export default router
