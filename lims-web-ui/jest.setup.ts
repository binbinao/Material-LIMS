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

// Mock @umijs/max — the Umi framework runtime. The real package depends
// on esbuild + a CLI runtime that crashes under jsdom. We replace its
// exports with lightweight stubs that satisfy the structural tests
// (which only call hooks like useParams, useIntl, useAccess, useModel
// for code paths, not for actual rendering).
jest.mock('@umijs/max', () => {
  const noop = () => undefined;
  return {
    history: {
      push: noop,
      replace: noop,
      goBack: noop,
      go: noop,
      listen: noop,
      location: { pathname: '/', search: '', hash: '' },
    },
    useAccess: () => ({
      canAdmin: true,
      canManager: true,
      canEngineer: true,
      canTechnician: true,
      canRequester: true,
    }),
    useIntl: () => ({
      formatMessage: ({ defaultMessage }: { defaultMessage?: string }) =>
        defaultMessage ?? '',
      locale: 'en-US',
    }),
    useModel: () => ({ initialState: { currentUser: { id: 'dev-user-0001' } } }),
    useLocation: () => ({ pathname: '/', search: '', hash: '' }),
    useNavigate: () => noop,
    useParams: () => ({}),
    useRouteProps: () => ({}),
    useRequest: (fn: () => unknown) => ({ data: undefined, loading: false, run: fn, refresh: noop }),
    __esModule: true,
  };
});

// Mock umi-request (used by requestService.ts). The real package POSTs
// fetch() calls — we replace it with a no-op that resolves to a
// harmless R<{}> envelope so structural tests don't make network calls.
jest.mock('umi-request', () => ({
  __esModule: true,
  default: () => Promise.resolve({ code: 200, data: {}, message: 'success' }),
  request: () => Promise.resolve({ code: 200, data: {}, message: 'success' }),
  extend: () => undefined,
  RequestError: class RequestError extends Error {},
}));

// Auto-mock every exported function from @/services/requestService so
// tests can do `(getBrands as jest.Mock).mockResolvedValue(...)`.
// Each function returns a benign R<{}> envelope by default; tests
// override per-case.
jest.mock('@/services/requestService', () => {
  const make = () => jest.fn(() => Promise.resolve({ code: 200, data: {}, message: 'success' }));
  return {
    __esModule: true,
    getRequests: make(),
    getRequest: make(),
    createRequest: make(),
    submitRequest: make(),
    assignRequest: make(),
    rejectRequest: make(),
    receiveSample: make(),
    startReporting: make(),
    completeRequest: make(),
    updateAnalysisTask: make(),
    getRequestTasks: make(),
    getRequestWorkflow: make(),
    getAdminUsers: make(),
    getAdminLogs: make(),
    getMyDashboard: make(),
    getMyPendingTasks: make(),
    getAuthUrl: make(),
    getBrands: make(),
    getBrand: make(),
    createBrand: make(),
    updateBrand: make(),
    deleteBrand: make(),
    getRequestTypes: make(),
    getDepartments: make(),
    getAnalysisItems: make(),
    getAnalysisItemGroups: make(),
    getEquipments: make(),
    getEquipmentRepairs: make(),
    getKnowledgeDocs: make(),
    getHolidays: make(),
    getReport: make(),
    getReports: make(),
    getReportRevisions: make(),
    submitReport: make(),
    approveReport: make(),
    rejectReport: make(),
    reviseReport: make(),
    getReportEditUrl: make(),
    syncReportFromSharePoint: make(),
    createReport: make(),
  };
});

