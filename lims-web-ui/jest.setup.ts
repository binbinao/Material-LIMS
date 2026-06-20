// jest.setup.ts — runs after the test framework is installed, before
// any test. Registers @testing-library/jest-dom's custom matchers
// (toBeInTheDocument, toHaveTextContent, etc.) globally, and mocks
// the Umi runtime so structural page tests don't pull in the real
// @umijs/max build (which depends on esbuild and breaks under jsdom).
//
// Issue #10: this file is referenced from jest.config.js so a single
// import here covers every *.test.ts(x) in the project.
// Issue #69 (audit): mock @umijs/max hooks + Umi history/request so
// page-level structural tests can render without booting Umi.

import '@testing-library/jest-dom';

// Polyfill window.matchMedia — Ant Design ProTable / Grid / Responsive
// components call it on mount; jsdom doesn't implement it. Without this
// the components throw "window.matchMedia is not a function" on render.
if (typeof window !== 'undefined' && typeof window.matchMedia !== 'function') {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }),
  });
}

// NOTE on App.useApp(): 16 pages call `const { message } = App.useApp();`
// at module top-level (e.g. src/pages/request/RequestList/index.tsx:8).
// That is a React anti-pattern — hooks must be called inside a render —
// and under jsdom it throws "Cannot read properties of null
// (reading 'useContext')" because there is no <App> provider. We do NOT
// mock antd here because @ant-design/pro-components depends on the real
// antd exports (Button.Group, Space, etc.) and breaks the moment antd is
// stubbed. The right fix is in the page files: move the
// `App.useApp()` call inside the component function.

// Mock @umijs/max — the Umi framework runtime. The real package depends
// on esbuild + a CLI runtime that crashes under jsdom. We replace its
// exports with lightweight stubs that satisfy the structural tests.
// Hooks are `jest.fn()` (not plain functions) so tests can do
// `(useParams as jest.Mock).mockReturnValue({ id: 'req-001' })`.
jest.mock('@umijs/max', () => {
  const noop = jest.fn();
  const useParams = jest.fn(() => ({}));
  const useModel = jest.fn(() => ({ initialState: { currentUser: { id: 'user-001' } } }));
  const useRequest = jest.fn((fn: () => unknown) => ({
    data: undefined, loading: false, run: fn, refresh: noop,
  }));
  const useIntl = jest.fn(() => ({
    formatMessage: ({ id, defaultMessage }: { id?: string; defaultMessage?: string }) =>
      defaultMessage ?? id ?? '',
    locale: 'en-US',
  }));
  const useAccess = jest.fn(() => ({
    canAdmin: true,
    canManager: true,
    canEngineer: true,
    canTechnician: true,
    canRequester: true,
  }));
  return {
    history: {
      push: jest.fn(),
      replace: jest.fn(),
      goBack: jest.fn(),
      go: jest.fn(),
      listen: jest.fn(),
      location: { pathname: '/', search: '', hash: '' },
    },
    // Umi re-exports the request helper as a named export — tests
    // import it directly as `import { request } from '@umijs/max'`.
    request: jest.fn(() =>
      Promise.resolve({ code: 200, data: { records: [], total: 0 }, message: 'success' }),
    ),
    useAccess,
    useIntl,
    useModel,
    useLocation: jest.fn(() => ({ pathname: '/', search: '', hash: '' })),
    useNavigate: jest.fn(),
    useParams,
    useRouteProps: jest.fn(() => ({})),
    useRequest,
    __esModule: true,
  };
});

// Mock umi-request (used by requestService.ts and directly by tests
// that cast `request` as jest.Mock). The default export + named `request`
// are both jest.fn() so tests can override per-case.
jest.mock('umi-request', () => ({
  __esModule: true,
  default: jest.fn(() => Promise.resolve({ code: 200, data: {}, message: 'success' })),
  request: jest.fn(() => Promise.resolve({ code: 200, data: {}, message: 'success' })),
  extend: jest.fn(),
  RequestError: class RequestError extends Error {},
}));

// Auto-mock every exported function from @/services/requestService so
// tests can do `(getBrands as jest.Mock).mockResolvedValue(...)`.
// Default envelope matches the backend R<T> shape used by pages — paginated
// list endpoints expect `data: { records, total }` so ProTable renders an
// empty state instead of crashing on undefined.total.
jest.mock('@/services/requestService', () => {
  const emptyPage = () =>
    jest.fn(() =>
      Promise.resolve({
        code: 200,
        data: { records: [], total: 0, current: 1, size: 20 },
        message: 'success',
      }),
    );
  const single = () =>
    jest.fn(() => Promise.resolve({ code: 200, data: {}, message: 'success' }));
  return {
    __esModule: true,
    // paginated list endpoints (data.records + data.total expected)
    getRequests: emptyPage(),
    getAdminLogs: emptyPage(),
    getAdminUsers: emptyPage(),
    getRequestTypes: emptyPage(),
    getDepartments: emptyPage(),
    getAnalysisItems: emptyPage(),
    getAnalysisItemGroups: emptyPage(),
    getAnalysisItemCascade: emptyPage(),
    getEquipments: emptyPage(),
    getEquipmentRepairs: emptyPage(),
    getEquipmentStats: single(),
    getKnowledgeDocs: emptyPage(),
    getHolidays: emptyPage(),
    getI18nMessages: jest.fn(() =>
      Promise.resolve({
        code: 200,
        data: { 'common.create': 'Create', 'common.cancel': 'Cancel' },
        message: 'success',
      }),
    ),
    getReports: emptyPage(),
    getMyPendingTasks: emptyPage(),
    // single-object endpoints (data is the entity directly)
    getRequest: single(),
    createRequest: single(),
    submitRequest: single(),
    assignRequest: single(),
    rejectRequest: single(),
    receiveSample: single(),
    startReporting: single(),
    completeRequest: single(),
    updateAnalysisTask: single(),
    getRequestTasks: emptyPage(),
    getRequestWorkflow: single(),
    getMyDashboard: single(),
    getAuthUrl: single(),
    getCostStats: single(),
    getBrands: emptyPage(),
    getBrand: single(),
    createBrand: single(),
    updateBrand: single(),
    deleteBrand: single(),
    getReport: single(),
    getReportRevisions: emptyPage(),
    submitReport: single(),
    approveReport: single(),
    rejectReport: single(),
    reviseReport: single(),
    getReportEditUrl: single(),
    syncReportFromSharePoint: single(),
    createReport: single(),
  };
});

