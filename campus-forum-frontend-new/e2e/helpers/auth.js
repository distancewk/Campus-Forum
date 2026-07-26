/**
 * Auth helpers for E2E specs.
 *
 * These specs are env-guarded: set E2E_USER and E2E_PASS (and optionally
 * E2E_BASE_URL) to run them against a real backend + frontend. When those
 * env vars are absent the specs skip gracefully (no backend required to
 * keep the repo lint/typecheck/build clean).
 */

export function getCredentials() {
  const user = process.env.E2E_USER
  const pass = process.env.E2E_PASS
  return user && pass ? { user, pass } : null
}

/**
 * Log in through the UI. Assumes a login page at /login with a username
 * (student id / email) field, a password field, and a 登录 submit button.
 */
export async function login(page) {
  const creds = getCredentials()
  if (!creds) {
    throw new Error('E2E_USER / E2E_PASS not set')
  }

  await page.goto('/login')
  await page.getByPlaceholder(/学号|邮箱|用户名|账号/i).fill(creds.user)
  await page.getByPlaceholder(/密码/i).fill(creds.pass)
  await page.getByRole('button', { name: /登录/ }).click()
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 })
}
