import { expect, test } from "@playwright/test";

test("renders owner workspace and security controls", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Обзор" })).toBeVisible();
  await expect(page.evaluate(() => typeof window.QRCode?.toCanvas)).resolves.toBe("function");
  await page.getByRole("button", { name: "Безопасность" }).click();
  await expect(page.getByRole("button", { name: "Создать 2FA QR" })).toBeVisible();
  await expect(page.getByRole("button", { name: "Отозвать все сессии" })).toBeVisible();
});

test("persists table filters locally", async ({ page }) => {
  await page.goto("/");
  await page.getByRole("button", { name: "Игры" }).click();
  await page.getByLabel("Поиск в таблице").first().fill("ключ");
  await page.reload();
  await page.getByRole("button", { name: "Игры" }).click();
  await expect(page.getByLabel("Поиск в таблице").first()).toHaveValue("ключ");
});
