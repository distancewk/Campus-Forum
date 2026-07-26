import { describe, it, expect } from 'vitest'
import { formatTime } from '@/utils/format'

const secondsAgo = (s) => new Date(Date.now() - s * 1000)

describe('formatTime', () => {
  it('returns 刚刚 for less than a minute', () => {
    expect(formatTime(secondsAgo(5))).toBe('刚刚')
    expect(formatTime(new Date())).toBe('刚刚')
  })

  it('returns X 分钟前 for minutes', () => {
    expect(formatTime(secondsAgo(5 * 60))).toBe('5 分钟前')
    expect(formatTime(secondsAgo(59 * 60))).toBe('59 分钟前')
  })

  it('returns X 小时前 for hours', () => {
    expect(formatTime(secondsAgo(3 * 3600))).toBe('3 小时前')
    expect(formatTime(secondsAgo(23 * 3600))).toBe('23 小时前')
  })

  it('returns X 天前 for days (< 30 days)', () => {
    expect(formatTime(secondsAgo(2 * 86400))).toBe('2 天前')
  })

  it('returns absolute YYYY-MM-DD for old dates', () => {
    expect(formatTime(new Date('2020-01-15T10:00:00'))).toBe('2020-01-15')
  })

  it('handles invalid / empty input', () => {
    expect(formatTime('')).toBe('')
    expect(formatTime(null)).toBe('')
    expect(formatTime(undefined)).toBe('')
    expect(formatTime('not-a-date')).toBe('')
    expect(formatTime('2020-13-45')).toBe('')
  })

  it('accepts ISO string input', () => {
    expect(formatTime(new Date('2020-01-15T10:00:00').toISOString())).toBe('2020-01-15')
  })
})
