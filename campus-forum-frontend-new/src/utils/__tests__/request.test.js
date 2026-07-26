import { describe, it, expect, vi, beforeEach } from 'vitest'

// vi.hoisted ensures handlers/instance are initialized before the hoisted vi.mock factory.
const { handlers, instance } = vi.hoisted(() => {
  const handlers = { responseRejected: null }
  const instance = vi.fn()
  instance.interceptors = {
    request: { use: vi.fn() },
    response: {
      use: vi.fn((_onFulfilled, onRejected) => {
        handlers.responseRejected = onRejected
      })
    }
  }
  return { handlers, instance }
})

vi.mock('axios', () => {
  const mockAxios = { create: vi.fn(() => instance) }
  mockAxios.__handlers = handlers
  mockAxios.__instance = instance
  return { default: mockAxios }
})

const mockStore = {
  accessToken: 'old-token',
  refreshAccessToken: vi.fn().mockImplementation(async () => {
    mockStore.accessToken = 'new-token'
  }),
  clearSession: vi.fn()
}
vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => mockStore)
}))
vi.mock('@/router', () => ({
  default: { push: vi.fn() }
}))
vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() }
}))

import axios from 'axios'
import request from '@/utils/request'

beforeEach(() => {
  mockStore.accessToken = 'old-token'
  mockStore.refreshAccessToken.mockClear()
  instance.mockReset()
  instance.mockResolvedValue({ data: { code: 0 } })
})

describe('request interceptor 401 handling', () => {
  it('triggers token refresh and retries the original request with the new token', async () => {
    const error = { response: { status: 401 }, config: { headers: {} } }
    await handlers.responseRejected(error)

    expect(mockStore.refreshAccessToken).toHaveBeenCalledTimes(1)

    const retryCall = instance.mock.calls.find(
      (c) => c[0]?.headers?.Authorization === 'Bearer new-token'
    )
    expect(retryCall).toBeTruthy()
  })

  it('queues concurrent 401s and reuses a single refresh', async () => {
    const makeError = () => ({ response: { status: 401 }, config: { headers: {} } })

    const p1 = handlers.responseRejected(makeError())
    const p2 = handlers.responseRejected(makeError())
    const p3 = handlers.responseRejected(makeError())
    await Promise.all([p1, p2, p3])

    expect(mockStore.refreshAccessToken).toHaveBeenCalledTimes(1)
    // One retry per request (the original + two queued)
    expect(instance).toHaveBeenCalledTimes(3)
  })
})
