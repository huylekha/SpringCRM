# Frontend Folder Structure (Next.js App Router)

## 1. Frontend Workspace Layout

```text
frontend/
  package.json
  next.config.ts
  tsconfig.json
  tailwind.config.ts
  postcss.config.mjs
  src/
    app/
      (auth)/
        login/
          page.tsx
      (dashboard)/
        layout.tsx
        customers/
          page.tsx
        leads/
          page.tsx
        opportunities/
          page.tsx
      api/
      layout.tsx
      page.tsx
    components/
      ui/
      common/
      layout/
    features/
      auth/
        components/
        hooks/
        services/
        schemas/
      customer/
      lead/
      opportunity/
      activity/
      task/
      note/
    services/
      api/
        client.ts
        interceptors.ts
      sentry/
      query/
    hooks/
      useAuth.ts
      usePermission.ts
    store/
      auth.store.ts
      ui.store.ts
    lib/
      constants/
      utils/
      types/
```

## 2. Architectural Principles

- App Router controls route segments and page composition.
- `features/*` owns business behavior, hooks, schemas, and API calls per domain.
- `components/ui` contains reusable shadcn/ui wrappers and design primitives.
- Global services (`axios`, query client, sentry init) live under `services/`.
- Local UI state uses Zustand; server state uses TanStack Query.

## 3. Layering by Responsibility

- `app/`
  - Route-level composition, layouts, metadata, and page shell concerns.
- `features/`
  - Business domain logic, forms, data orchestration, and guards.
- `components/`
  - Reusable presentational blocks shared across features.
- `services/`
  - API client, interceptors, base query functions, and observability setup.
- `store/`
  - Global client-side state (tokens, session flags, ui preferences).
- `hooks/`
  - Cross-feature reusable hooks.

## 4. Auth and Access Control Placement

- Login route under `app/(auth)/login`.
- Session/token state in `store/auth.store.ts`.
- Axios interceptors in `services/api/interceptors.ts`:
  - Attach access token.
  - Handle refresh flow and request replay.
- Route/feature guards:
  - `hooks/usePermission.ts`
  - Optional guard component in `features/auth/components/RoleGuard.tsx`.

## 5. Form, Validation, and Table Standards

- Form management: React Hook Form.
- Validation: Zod schema colocated inside each feature (`features/<module>/schemas`).
- Table rendering: TanStack Table with server-driven pagination/sorting.
- Charting: Recharts components inside feature analytics widgets.

## 6. Naming Conventions

- Components: `PascalCase.tsx`.
- Hooks: `useXxx.ts`.
- Stores: `<domain>.store.ts`.
- API functions: `verbNoun` pattern (`getCustomers`, `createLead`).
- Zod schemas: `<domain>.schema.ts`.

## 7. Testing and Quality Placement

- Unit/component tests under feature-local `__tests__` folders.
- E2E scenarios under `frontend/e2e` (if Playwright is enabled).
- Query key definitions centralized under `services/query` to avoid drift.

## 8. Example Feature Internal Structure

```text
features/customer/
  components/
    CustomerTable.tsx
    CustomerFormDialog.tsx
  hooks/
    useCustomerList.ts
    useCreateCustomer.ts
  services/
    customer.api.ts
  schemas/
    customer.schema.ts
  types/
    customer.types.ts
```
