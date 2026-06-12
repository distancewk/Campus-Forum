# Campus Forum UI Redesign Design

## Goal

Redesign the new Vue frontend into a clean, modern campus community workspace while keeping all existing routes, API calls, and Element Plus usage intact.

## Visual Direction

- Product type: campus community forum, content-first application interface.
- Style: clean white content surfaces, cool gray page background, deep navy text, blue-green accent, subtle borders and shadows.
- Density: comfortable but not oversized; posts remain quick to scan.
- Icon treatment: use the existing Element Plus icon set; avoid emoji for structural navigation.
- Motion: subtle hover/press transitions only, respecting reduced motion.

## Layout

- Fixed top navigation with brand, search, and session actions.
- Left board navigation as a calm rail on desktop, hidden on mobile.
- Main content uses a centered two-column workspace on desktop: feed/content plus a lightweight right insight rail where useful.
- Mobile collapses to a single column with compact header spacing.

## Components

- `Navbar`: clearer brand mark, larger accessible search, consistent icon buttons and primary post action.
- `Sidebar`: section label, stronger selected state, board rows with consistent icon container and count badge.
- `PostCard`: improved hierarchy for title, metadata, summary, board tag, and stats.
- `Home` and `PostList`: page header with compact intro and segmented sorting where applicable.
- `Login`: replace purple gradient with the same product design language and a split auth panel.
- `PostCreate`, `RichEditor`, `PostDetail`: align surface, spacing, borders, typography, and actions with the new system.

## Constraints

- Do not introduce a new UI framework.
- Do not change backend contracts.
- Do not remove existing user workflows.
- Keep code scoped to `campus-forum-frontend-new` unless verification exposes a shared issue.

## Verification

- Run `npm run build` in `campus-forum-frontend-new`.
- Verify desktop page load through the in-app browser.
- Verify mobile viewport has no horizontal overflow and key text remains readable.
