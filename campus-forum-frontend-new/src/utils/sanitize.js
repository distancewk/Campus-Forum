import DOMPurify from 'dompurify'

const ALLOWED_TAGS = [
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h2', 'h3',
  'ul', 'ol', 'li', 'blockquote', 'a', 'img'
]

const ALLOWED_ATTR = ['href', 'target', 'rel', 'src', 'alt', 'width', 'height']

const ALLOWED_URI_REGEXP = /^(?:(?:https?|mailto):|\/uploads\/)/i

/**
 * Sanitize untrusted HTML for rich post content with a single, shared
 * whitelist (mirrors the previous RichEditor.vue config) so all rendering
 * paths apply the same XSS protection.
 */
export function sanitizeHtml(html) {
  return DOMPurify.sanitize(html || '', {
    ALLOWED_TAGS,
    ALLOWED_ATTR,
    ALLOWED_URI_REGEXP
  })
}
