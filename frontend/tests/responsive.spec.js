import { test, expect } from '@playwright/test';
import { loginAs } from './helpers/auth';

test.describe('Responsive layout', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, 'admin', 'admin123');
  });

  test('Dashboard no produce scroll horizontal a nivel de página en viewport móvil', async ({ page }) => {
    const hasHorizontalOverflow = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth
    );
    expect(hasHorizontalOverflow).toBe(false);
  });

  test('la tabla de Products se desplaza dentro de su contenedor, no rompe el layout de página', async ({ page }) => {
    await page.click('text=Products');
    await expect(page).toHaveURL(/.*products/);

    const table = page.locator('table');
    await expect(table).toBeVisible();

    const pageOverflows = await page.evaluate(
      () => document.documentElement.scrollWidth > document.documentElement.clientWidth
    );
    expect(pageOverflows).toBe(false);

    const tableWiderThanContainer = await table.evaluate((el) => el.scrollWidth > el.parentElement.clientWidth);
    // En viewport angosto la tabla real (muchas columnas) es más ancha que su contenedor;
    // el contenedor (`overflow-x-auto`) es el que debe absorber ese scroll, no la página.
    if (tableWiderThanContainer) {
      const containerOverflowX = await table.evaluate((el) => getComputedStyle(el.parentElement).overflowX);
      expect(containerOverflowX).toBe('auto');
    }
  });

  test('el modal de creación de producto es utilizable (botones visibles) en viewport móvil', async ({ page }) => {
    await page.click('text=Products');
    await page.click('button:has-text("Add Product")');

    await expect(page.locator('input[name="name"]')).toBeVisible();
    const submitButton = page.locator('button:has-text("Create Product")');
    // El modal tiene scroll propio (`overflow-y-auto`) y en móvil el formulario es más alto que
    // el viewport, así que el botón no está en pantalla sin scrollear — eso es esperado, no un bug.
    // Lo que importa es que sea alcanzable con scroll dentro del modal.
    await submitButton.scrollIntoViewIfNeeded();
    await expect(submitButton).toBeVisible();
  });

  test('Stock Movements es navegable y el formulario de registro es usable en viewport móvil', async ({ page }) => {
    await page.click('text=Stock Movements');
    await expect(page).toHaveURL(/.*stock-movements/);

    await page.locator('select').selectOption({ index: 1 });
    await page.click('button:has-text("Register Movement")');

    const submitButton = page.locator('button:has-text("Save Movement")');
    await submitButton.scrollIntoViewIfNeeded();
    await expect(submitButton).toBeVisible();
  });
});
