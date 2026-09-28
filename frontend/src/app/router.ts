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
    { path: '/module3', name: 'practice-review', component: () => import('../modules/practice-review/PracticeReviewPage.vue') },
    { path: '/module3/:projectId', name: 'practice-project', component: () => import('../modules/practice-review/PracticeReviewPage.vue') },
  ],
  scrollBehavior: () => ({ top: 0 }),
})
