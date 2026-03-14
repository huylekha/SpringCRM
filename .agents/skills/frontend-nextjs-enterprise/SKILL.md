---
name: frontend-nextjs-enterprise
description: Implements scalable Next.js React architecture for enterprise and fintech platforms using feature modules, reusable UI systems, TanStack Query, Zustand, RHF, Zod, and performance-first patterns.
---

# Frontend Next.js Enterprise

## Apply When
- Building or refactoring frontend code in `frontend/*` or `src/*`.
- Designing feature modules, reusable UI patterns, or state/data flows.
- Implementing table/form/modal heavy enterprise screens.

## Core Rules
- Use feature-first architecture for business code.
- Keep global components UI-only and reusable.
- Keep component/file complexity under defined limits.
- Keep API in services and logic in hooks.
- Keep UI components free from direct fetch calls.

## Stack Enforcement
- Next.js for framework and routing.
- Zustand for global UI/app state.
- TanStack Query for server state and caching.
- React Hook Form + Zod for form state and validation.
- shadcn/ui + TailwindCSS for reusable UI system.
- TanStack Table for dashboard table patterns.
- Framer Motion for animation.
- Lucide for iconography.

## State and Data Rules
- Use TanStack Query for server state.
- Never use `useEffect` for API fetching.
- Use Zustand for global UI/app state.
- Never store server state in Zustand.
- Handle loading and error states for API-driven screens.

## Reusable Pattern Rules
- Table pattern: `components/table` + TanStack Table.
- Form pattern: `components/form` + React Hook Form + Zod.
- Modal pattern: `components/modal` with composable sections.

## Architecture Output Contract
- For architecture/design requests, always include:
  - architecture explanation
  - scalable folder structure
  - reusable patterns
  - concrete code examples

## Shared Hooks Baseline
- Prefer reusable hooks:
  - `useDebounce`
  - `usePagination`
  - `useModal`
  - `useLocalStorage`

## Access, Import, and Styling Rules
- Use permission guards (for example `RoleGuard`) for privileged UI actions.
- Use absolute imports with `@/...`.
- Use Tailwind design tokens and support dark mode across shared UI.

## Quality Rules
- If logic grows beyond 15 lines, extract a custom hook.
- If code repeats more than twice, extract reusable component/hook.
- Prefer small, readable components over large monoliths.
