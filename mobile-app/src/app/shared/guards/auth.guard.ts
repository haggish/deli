import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

export const courierGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isCourier()) return true;
  // Customer trying to access courier routes → redirect to their home
  if (authService.isCustomer()) return router.createUrlTree(['/customer/tracking']);
  return router.createUrlTree(['/login']);
};

export const customerGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isCustomer()) return true;
  if (authService.isCourier()) return router.createUrlTree(['/courier/dashboard']);
  return router.createUrlTree(['/login']);
};
