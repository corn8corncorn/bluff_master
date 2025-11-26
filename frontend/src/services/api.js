import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true, // 確保發送 cookie（session）
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  response => response,
  error => {
    const message = error.response?.data?.message || error.message || '發生錯誤'
    console.error('API Error:', message)
    return Promise.reject(error)
  }
)

export default api

