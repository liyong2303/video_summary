import axios from 'axios'

// 请求拦截：自动携带 token
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['token'] = token
  }
  return config
})

// 响应拦截：401 跳转登录
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export interface UserInfo {
  userId: number
  username: string
  role: string
  todayUsed: number
  dailyLimit: number   // -1 = unlimited
}

export async function register(username: string, password: string): Promise<string> {
  const res = await axios.post('/api/auth/register', { username, password })
  return res.data.data.token
}

export async function login(username: string, password: string): Promise<string> {
  const res = await axios.post('/api/auth/login', { username, password })
  return res.data.data.token
}

export async function getMe(): Promise<UserInfo> {
  const res = await axios.get('/api/auth/me')
  return res.data.data
}

export function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  window.location.href = '/login'
}
