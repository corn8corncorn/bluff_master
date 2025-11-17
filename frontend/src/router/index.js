import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import CreateRoom from '../views/CreateRoom.vue'
import JoinRoom from '../views/JoinRoom.vue'
import Room from '../views/Room.vue'
import Game from '../views/Game.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/create',
    name: 'CreateRoom',
    component: CreateRoom
  },
  {
    path: '/join',
    name: 'JoinRoom',
    component: JoinRoom
  },
  {
    path: '/room/:roomId',
    name: 'Room',
    component: Room
  },
  {
    path: '/game/:roomId',
    name: 'Game',
    component: Game
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router

