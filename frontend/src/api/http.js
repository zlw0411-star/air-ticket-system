import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const TOKEN_KEY = 'air_ticket_token'
const USER_KEY = 'air_ticket_user'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  import('../stores/auth').then(({ useAuthStore }) => {
    try {
      useAuthStore().logout()
    } catch (e) {
      // store not active yet; localStorage already cleared
    }
  })
}

function redirectToLogin() {
  if (router.currentRoute.value.path !== '/login') router.push('/login')
}

function handleAuthFailure(status, message) {
  clearSession()
  redirectToLogin()
  const fallback = status === 403 ? '没有权限' : '未登录'
  ElMessage.error(message || fallback)
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data
    if (body && (body.code === 401 || body.code === 403)) {
      handleAuthFailure(body.code, body.message)
      return Promise.reject(body)
    }
    if (body && body.code !== 0 && body.code !== undefined) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(body)
    }
    return body?.data !== undefined ? body.data : body
  },
  (err) => {
    const status = err.response?.status
    const backendMessage = err.response?.data?.message
    if (status === 401 || status === 403) {
      handleAuthFailure(status, backendMessage)
      return Promise.reject(err)
    }
    ElMessage.error(backendMessage || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
