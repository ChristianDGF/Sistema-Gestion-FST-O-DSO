import { test, expect } from '@playwright/test';

test.describe('Login Flow', () => {
  test('debe redirigir a Keycloak e iniciar sesión', async ({ page }) => {

    await page.goto('/');

    await expect(page).toHaveURL(/.*realms.*/);
    
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    
    await page.click('#kc-login');

    await expect(page).toHaveURL('/');

    await expect(page.locator('text=Dashboard').first()).toBeVisible({ timeout: 10000 });
  });
});
