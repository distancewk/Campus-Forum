import { test, expect } from '@playwright/test'
import { getCredentials, login } from './helpers/auth.js'

const creds = getCredentials()

// Skip the whole suite when no backend credentials are provided.
test.skip(!creds, 'E2E_USER / E2E_PASS env vars are required to run these specs')

test.describe('core forum flows', () => {
  test('login redirects to the home page', async ({ page }) => {
    await login(page)
    await expect(page).toHaveURL((url) => !url.pathname.includes('/login'))
    await expect(page.getByRole('link', { name: /首页|论坛/ })).toBeVisible()
  })

  test('create a post and land on its detail page', async ({ page }) => {
    await login(page)

    await page.goto('/post/create')
    await page.getByPlaceholder(/标题/).fill('E2E 自动化测试帖')
    // Pick a board from a select, if present.
    const boardSelect = page.getByRole('combobox').first()
    if (await boardSelect.isVisible()) {
      await boardSelect.selectOption({ index: 1 })
    }
    // Rich text editor is a contenteditable region.
    const editor = page.locator('.editor-content')
    await editor.click()
    await editor.fill('这是一条由 Playwright E2E 自动发布的帖子内容。')
    await page.getByRole('button', { name: /发布|发表/ }).click()

    await page.waitForURL(/\/post\/\d+/, { timeout: 10000 })
    await expect(page.getByRole('heading', { level: 1 })).toContainText('E2E 自动化测试帖')
  })

  test('comment on a post and see it in the list', async ({ page }) => {
    await login(page)

    await page.goto('/post/1')
    const commentBox = page.getByPlaceholder(/说点什么|评论|写下你的评论/)
    await commentBox.fill('E2E 自动评论')
    await page.getByRole('button', { name: /发表评论|发送/ }).click()

    await expect(page.getByText('E2E 自动评论').first()).toBeVisible()
  })

  test('send a private message in a conversation', async ({ page }) => {
    await login(page)

    await page.goto('/message')
    await page.getByRole('listitem').first().click()
    const msgBox = page.getByPlaceholder(/输入消息|发送消息/)
    await msgBox.fill('E2E 私信测试')
    await page.getByRole('button', { name: /发送/ }).click()

    await expect(page.getByText('E2E 私信测试').first()).toBeVisible()
  })

  test('search navigates to results containing the keyword', async ({ page }) => {
    await login(page)

    const searchBox = page.getByPlaceholder(/搜索|Search/i).first()
    await searchBox.fill('E2E')
    await searchBox.press('Enter')

    await expect(page).toHaveURL(/[?&]q=E2E|search/)
    await expect(page.getByText(/结果|E2E/).first()).toBeVisible()
  })
})
