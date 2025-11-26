import { ref } from 'vue'

// 全局通知狀態
const notifications = ref([])

export function useNotification() {
  const show = (message, type = 'error') => {
    const id = Date.now() + Math.random()
    const notification = {
      id,
      message,
      type // 'success', 'error', 'warning', 'info'
    }
    
    notifications.value.push(notification)
    
    // 自動移除通知
    setTimeout(() => {
      remove(id)
    }, 3000)
    
    return id
  }
  
  const remove = (id) => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      notifications.value.splice(index, 1)
    }
  }
  
  const success = (message) => show(message, 'success')
  const error = (message) => show(message, 'error')
  const warning = (message) => show(message, 'warning')
  const info = (message) => show(message, 'info')
  
  return {
    notifications,
    show,
    remove,
    success,
    error,
    warning,
    info
  }
}

