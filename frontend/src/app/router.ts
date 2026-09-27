import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import AbilityGrowthPage from '../modules/ability-growth/AbilityGrowthPage.vue'
import DecisionSandboxPage from '../modules/decision-sandbox/DecisionSandboxPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomePage },
    { path: '/ability', name: 'ability-growth', component: AbilityGrowthPage },
    { path: '/decision', name: 'decision-sandbox', component: DecisionSandboxPage },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
