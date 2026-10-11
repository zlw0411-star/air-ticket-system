import { defineStore } from 'pinia'
import http from '../api/http'

const TOKEN_KEY = 'air_ticket_token'
const USER_KEY = 'air_ticket_user'

function migrateLegacyKeys() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

function readUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (e) {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    return null
  }
}

migrateLegacyKeys()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser()
  }),
  getters: {
    isLogin: (s) => !!s.token,
    isAdmin: (s) => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(payload) {
      const data = await http.post('/auth/login', payload)
      this.setSession(data)
    },
    async register(payload) {
      const data = await http.post('/auth/register', payload)
      this.setSession(data)
    },
    setSession(data) {
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
