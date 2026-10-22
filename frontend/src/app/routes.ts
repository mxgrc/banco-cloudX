import { Routes } from '@angular/router';
import { authGuard, roleGuard, activeGuard } from './core/guards';
import { Landing } from './pages/landing';
import { Shell } from './pages/shell';
export const routes:Routes=[
  {path:'',component:Landing,pathMatch:'full'},
  {path:'',component:Shell,canActivate:[authGuard],children:[
    {path:'estado',canActivate:[roleGuard],data:{role:'CLIENTE'},loadComponent:()=>import('./pages/status').then(m=>m.StatusPage)},
    {path:'beneficios',canActivate:[roleGuard,activeGuard],data:{role:'CLIENTE'},loadComponent:()=>import('./pages/benefits').then(m=>m.BenefitsPage)},
    {path:'inscripciones',canActivate:[roleGuard,activeGuard],data:{role:'CLIENTE'},loadComponent:()=>import('./pages/enrollments').then(m=>m.EnrollmentsPage)},
    {path:'admin/beneficios',canActivate:[roleGuard],data:{role:'ADMIN'},loadComponent:()=>import('./pages/admin-benefits').then(m=>m.AdminBenefits)},
    {path:'admin/clientes',canActivate:[roleGuard],data:{role:'ADMIN'},loadComponent:()=>import('./pages/admin-clients').then(m=>m.AdminClients)},
    {path:'admin/categorias',canActivate:[roleGuard],data:{role:'ADMIN'},loadComponent:()=>import('./pages/admin-categories').then(m=>m.AdminCategories)},
  ]},
  {path:'**',redirectTo:''}
];
