import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import AbilityGrowthPage from '../modules/ability-growth/AbilityGrowthPage.vue'
import DecisionPlaceholder from '../pages/DecisionPlaceholder.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/ability', name: 'ability-growth', component: AbilityGrowthPage },
    { path: '/decision', name: 'decision-sandbox', component: DecisionPlaceholder },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
