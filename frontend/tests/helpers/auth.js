export async function loginAs(page, username, password) {
  await page.goto('/');

  await page.waitForURL(/.*realms.*/, { timeout: 5000 }).catch(() => {});

  if (page.url().includes('realms')) {
    await page.fill('#username', username);
    await page.fill('#password', password);
    await page.click('#kc-login');

    await page.waitForURL('http://localhost:5173/');
  }
}
