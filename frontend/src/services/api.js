import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** 圖片上傳用（手機 HEIC 轉換 + Cloudflare 隧道較慢，需較長逾時） */
export const uploadApi = axios.create({
  baseURL: '/api',
  timeout: 120000,
  withCredentials: true,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const url = error.config?.url || ''
    if (status === 404 && url.includes('/current-round')) {
      return Promise.reject(error)
    }
    const message =
      (typeof error.response?.data === 'object' && error.response?.data?.message) ||
      (typeof error.response?.data === 'string' ? error.response.data : null) ||
      error.message ||
      '發生錯誤'
    const enriched = new Error(message)
    enriched.response = error.response
    enriched.cause = error
    console.error('API Error:', message)
    return Promise.reject(enriched)
  }
)

uploadApi.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      (typeof error.response?.data === 'object' && error.response?.data?.message) ||
      (typeof error.response?.data === 'string' ? error.response.data : null) ||
      error.message ||
      '發生錯誤'
    console.error('Upload API Error:', message)
    return Promise.reject(error)
  }
)

export default api
