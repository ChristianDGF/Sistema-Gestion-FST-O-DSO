import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

// Scoped to structural, data-independent views on purpose: pages that render live seed data
// (Products list, Audit history, Dashboard counters) would make these snapshots flaky for
// reasons unrelated to a real UI regression every time the dataset changes.
test.describe('Visual regression', () => {
  test('pantalla de login de Keycloak', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL(/.*realms.*/, { timeout: 5000 });

    await expect(page).toHaveScreenshot('login.png');
  });

  test('Sidebar de navegación (rol admin)', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');

    const sidebar = page.locator('nav').first().locator('xpath=ancestor::div[contains(@class,"w-64")]');
    await expect(sidebar).toHaveScreenshot('sidebar-admin.png');
  });

  test('modal de creación de producto (estado vacío)', async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
    await page.click('text=Products');
    await page.click('button:has-text("Add Product")');

    await expect(page.locator('input[name="name"]')).toBeVisible();
    const modal = page
      .getByRole('heading', { name: 'Add New Product' })
      .locator('xpath=ancestor::div[contains(@class,"rounded-2xl")]');
    await expect(modal).toHaveScreenshot('product-modal-empty.png');
  });
});
