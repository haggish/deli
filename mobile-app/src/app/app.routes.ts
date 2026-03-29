import { Routes } from '@angular/router';
import { authGuard } from './shared/guards/auth.guard';
import { courierGuard } from './shared/guards/role.guard';
import { customerGuard } from './shared/guards/role.guard';

export const APP_ROUTES: Routes = [
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/login/login.page').then((m) => m.LoginPage),
  },
  {
    // Courier shell — GPS streaming, route list, stop confirmation
    path: 'courier',
    canActivate: [authGuard, courierGuard],
    loadComponent: () =>
      import('./courier/courier-shell.component').then((m) => m.CourierShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./courier/dashboard/dashboard.page').then((m) => m.DashboardPage),
      },
      {
        path: 'route',
        loadComponent: () =>
          import('./courier/route/route.page').then((m) => m.RoutePage),
      },
      {
        path: 'stop/:stopId',
        loadComponent: () =>
          import('./courier/stop-detail/stop-detail.page').then((m) => m.StopDetailPage),
      },
      {
        path: 'stop/:stopId/confirm',
        loadComponent: () =>
          import('./courier/delivery-confirm/delivery-confirm.page').then(
            (m) => m.DeliveryConfirmPage,
          ),
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full',
      },
    ],
  },
  {
    // Customer shell — tracking screen, delivery history
    path: 'customer',
    canActivate: [authGuard, customerGuard],
    loadComponent: () =>
      import('./customer/customer-shell.component').then((m) => m.CustomerShellComponent),
    children: [
      {
        path: 'tracking',
        loadComponent: () =>
          import('./customer/tracking/tracking.page').then((m) => m.TrackingPage),
      },
      {
        path: 'history',
        loadComponent: () =>
          import('./customer/history/history.page').then((m) => m.HistoryPage),
      },
      {
        path: '',
        redirectTo: 'tracking',
        pathMatch: 'full',
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
