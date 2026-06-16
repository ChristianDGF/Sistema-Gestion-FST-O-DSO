import { test, expect } from '@playwright/test';

test.describe('Product CRUD Operations', () => {
  test.beforeEach(async ({ page }) => {

    await page.goto('/');
    
    await page.waitForURL(/.*realms.*/, { timeout: 5000 }).catch(() => {});
    
    if (page.url().includes('realms')) {
      await page.fill('#username', 'admin');
      await page.fill('#password', 'admin123');
      await page.click('#kc-login');
      
      await page.waitForURL('http://localhost:5173/');
    }
    
    await page.click('text=Products');
    await expect(page).toHaveURL(/.*products/);
  });

  test('debe permitir crear, editar y eliminar un producto', async ({ page }) => {
    const sku = `TEST-SKU-${Date.now()}`;
    const name = `Test Product ${Date.now()}`;

    await page.click('button:has-text("Add Product")');
    
    await page.fill('input[name="name"]', name);
    await page.fill('input[name="sku"]', sku);
    await page.fill('input[name="category"]', 'E2E Testing');
    await page.fill('textarea[name="description"]', 'Description from E2E test');
    await page.fill('input[name="price"]', '99.99');
    await page.fill('input[name="quantity"]', '10');
    await page.fill('input[name="minStock"]', '2');
    
    await page.click('button:has-text("Create Product")');

    const productRow = page.locator('tr', { hasText: sku });
    await expect(productRow).toBeVisible();

    await productRow.locator('button').first().click();
    
    await page.fill('input[name="price"]', '149.99');
    await page.click('button:has-text("Save Changes")');

    await expect(productRow.locator('text=$149.99')).toBeVisible();

    page.on('dialog', async dialog => {
      expect(dialog.message()).toContain('Are you sure you want to delete this product?');
      await dialog.accept();
    });

    await productRow.locator('button').nth(1).click();

    await expect(page.locator('tr', { hasText: sku })).not.toBeVisible();
  });
});
