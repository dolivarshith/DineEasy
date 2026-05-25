import { Routes } from '@angular/router';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { Register } from './pages/register/register';
import { BookTable } from './pages/book-table/book-table';
import { MyBookings } from './pages/my-bookings/my-bookings';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'book', component: BookTable },
  { path: 'my-bookings', component: MyBookings },
  { path: 'admin', component: AdminDashboard },
  { path: '**', redirectTo: '' }
];
