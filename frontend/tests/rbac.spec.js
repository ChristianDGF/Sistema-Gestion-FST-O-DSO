import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

test.describe('Role-based UI visibility', () => {
  test('admin ve los 4 items de navegación y todas las acciones de gestión', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');

    await expect(page.locator('nav a', { hasText: 'Dashboard' })).toBeVisible();
    await expect(page.locator('nav a', { hasText: 'Products' })).toBeVisible();
    await expect(page.locator('nav a', { hasText: 'Stock Movements' })).toBeVisible();
    await expect(page.locator('nav a', { hasText: 'Auditoría' })).toBeVisible();

    await page.click('text=Products');
    await expect(page.locator('button:has-text("Add Product")')).toBeVisible();

    await page.click('text=Stock Movements');
    await expect(page.locator('button:has-text("Register Movement")')).toBeVisible();
  });

  test('employee solo ve Products y Stock Movements, sin botones de gestión', async ({ page }) => {
    await loginAs(page, 'employee', 'employee123');

    await expect(page.locator('nav a', { hasText: 'Dashboard' })).toHaveCount(0);
    await expect(page.locator('nav a', { hasText: 'Products' })).toBeVisible();
    await expect(page.locator('nav a', { hasText: 'Stock Movements' })).toBeVisible();
    await expect(page.locator('nav a', { hasText: 'Auditoría' })).toHaveCount(0);

    await page.click('text=Products');
    await expect(page.locator('button:has-text("Add Product")')).toHaveCount(0);

    await page.click('text=Stock Movements');
    await expect(page.locator('button:has-text("Register Movement")')).toHaveCount(0);
  });

  test('employee navegando directo a /audit ve Acceso denegado', async ({ page }) => {
    await loginAs(page, 'employee', 'employee123');

    await page.goto('/audit');
    await expect(page.locator('text=Acceso denegado')).toBeVisible();
  });

  test('employee navegando directo a / (Dashboard) ve Acceso denegado', async ({ page }) => {
    await loginAs(page, 'employee', 'employee123');

    await page.goto('/');
    await expect(page.locator('text=Acceso denegado')).toBeVisible();
  });
});
