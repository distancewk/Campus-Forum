# Campus Forum UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved clean campus community UI redesign to the Vue frontend.

**Architecture:** Keep the existing Vue 3, Vue Router, Pinia, Element Plus, and SCSS structure. Centralize the new visual language in `tokens.scss` and `global.scss`, then update focused layout and page components.

**Tech Stack:** Vue 3, Vite, Element Plus, SCSS, Pinia.

---

### Task 1: Design Tokens And Global UI Polish

**Files:**
- Modify: `campus-forum-frontend-new/src/assets/styles/tokens.scss`
- Modify: `campus-forum-frontend-new/src/assets/styles/global.scss`

- [ ] Replace the old Element Plus blue-only palette with cool gray, navy, blue-green accent, semantic surfaces, radii, shadows, spacing, and motion tokens.
- [ ] Add global body, focus, Element Plus input/button/card/menu overrides that match the new system.
- [ ] Run `npm run build` after component work is complete.

### Task 2: App Shell

**Files:**
- Modify: `campus-forum-frontend-new/src/components/layout/MainLayout.vue`
- Modify: `campus-forum-frontend-new/src/components/layout/Navbar.vue`
- Modify: `campus-forum-frontend-new/src/components/layout/Sidebar.vue`

- [ ] Redesign the fixed header into a cleaner app bar.
- [ ] Redesign the sidebar into a board rail with selected-state emphasis and no default menu look.
- [ ] Update desktop/mobile layout spacing and content width.

### Task 3: Feed Pages And Cards

**Files:**
- Modify: `campus-forum-frontend-new/src/components/PostCard.vue`
- Modify: `campus-forum-frontend-new/src/views/home/Home.vue`
- Modify: `campus-forum-frontend-new/src/views/post/PostList.vue`

- [ ] Improve post card hierarchy, metadata, summary, board tags, hover, and stat treatment.
- [ ] Add a right insight rail to the home feed.
- [ ] Align board listing page header and empty/loading states.

### Task 4: Auth And Writing Surfaces

**Files:**
- Modify: `campus-forum-frontend-new/src/views/auth/Login.vue`
- Modify: `campus-forum-frontend-new/src/views/post/PostCreate.vue`
- Modify: `campus-forum-frontend-new/src/components/RichEditor.vue`
- Modify: `campus-forum-frontend-new/src/views/post/PostDetail.vue`

- [ ] Restyle auth into a product-consistent panel instead of the old purple gradient.
- [ ] Restyle editor and post detail surfaces using the new tokens.
- [ ] Keep all submit, validation, and interaction logic unchanged.

### Task 5: Verification

**Files:**
- No source edits unless verification reveals defects.

- [ ] Run `npm run build` in `campus-forum-frontend-new`.
- [ ] Use the in-app browser at `http://127.0.0.1:5173/`.
- [ ] Check desktop and mobile viewport rendering.
- [ ] Confirm `/api/boards` still loads through the running backend.
