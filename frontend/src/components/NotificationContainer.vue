<template>
  <div class="fixed top-2 right-2 sm:top-4 sm:right-4 z-50 space-y-2 max-w-[calc(100vw-1rem)] sm:max-w-md">
    <TransitionGroup name="notification" tag="div">
      <div
        v-for="notification in notifications"
        :key="notification.id"
        :class="[
          'px-3 sm:px-4 py-2 sm:py-3 rounded-lg shadow-lg w-full max-w-full flex items-center justify-between',
          getNotificationClass(notification.type)
        ]"
      >
        <div class="flex items-center gap-2">
          <component
            :is="getIcon(notification.type)"
            class="w-5 h-5"
          />
          <span class="text-xs sm:text-sm font-medium break-words">{{ notification.message }}</span>
        </div>
        <button
          @click="remove(notification.id)"
          class="ml-4 text-gray-500 hover:text-gray-700"
        >
          <XMarkIcon class="w-4 h-4" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useNotification } from '../composables/useNotification'
import { XMarkIcon, CheckCircleIcon, ExclamationCircleIcon, InformationCircleIcon, ExclamationTriangleIcon } from '@heroicons/vue/24/outline'

const { notifications, remove } = useNotification()

const getNotificationClass = (type) => {
  const classes = {
    success: 'bg-green-50 border border-green-200 text-green-800',
    error: 'bg-red-50 border border-red-200 text-red-800',
    warning: 'bg-yellow-50 border border-yellow-200 text-yellow-800',
    info: 'bg-blue-50 border border-blue-200 text-blue-800'
  }
  return classes[type] || classes.info
}

const getIcon = (type) => {
  const icons = {
    success: CheckCircleIcon,
    error: ExclamationCircleIcon,
    warning: ExclamationTriangleIcon,
    info: InformationCircleIcon
  }
  return icons[type] || InformationCircleIcon
}
</script>

<style scoped>
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.notification-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>

