import '@testing-library/jest-dom';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

class ResizeObserverMock {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserverMock as unknown as typeof ResizeObserver;

const mockHistory = {
  push: jest.fn(),
  back: jest.fn(),
  replace: jest.fn(),
};

jest.mock('@umijs/max', () => {
  const React = require('react');
  return {
    history: mockHistory,
    useIntl: () => ({
      formatMessage: ({ id }: { id: string }) => id,
    }),
    useParams: jest.fn(() => ({ id: 'req-001' })),
    useModel: jest.fn(() => ({
      initialState: {
        currentUser: { id: 'dev-user-0001', displayName: 'Dev User' },
      },
    })),
    request: jest.fn(),
    Link: ({ children, to }: { children: unknown; to?: string }) =>
      React.createElement('a', { href: to }, children),
  };
});

jest.mock('@ant-design/icons', () => {
  const React = require('react');
  const iconCache: Record<string, React.FC> = {};
  const getIcon = (name: string) => {
    if (!iconCache[name]) {
      const Icon: React.FC = () => React.createElement('span', { 'data-testid': `icon-${name}` });
      Icon.displayName = name;
      iconCache[name] = Icon;
    }
    return iconCache[name];
  };
  return new Proxy(
    { __esModule: true },
    {
      get: (_target, prop) => {
        if (prop === '__esModule') return true;
        if (typeof prop !== 'string') return undefined;
        return getIcon(prop);
      },
    },
  );
});

jest.mock('echarts-for-react', () => ({
  __esModule: true,
  default: () => null,
}));

jest.mock('@/services/requestService', () => ({
  getAuthUrl: jest.fn(),
  getBrands: jest.fn(),
  getRequestTypes: jest.fn(),
  createRequest: jest.fn(),
  getRequests: jest.fn(),
  getRequest: jest.fn(),
  getRequestTasks: jest.fn(),
  getRequestWorkflow: jest.fn(),
  submitRequest: jest.fn(),
  rejectRequest: jest.fn(),
  receiveSample: jest.fn(),
  startReporting: jest.fn(),
  completeRequest: jest.fn(),
  updateAnalysisTask: jest.fn(),
  getMyDashboard: jest.fn(),
  getMyPendingTasks: jest.fn(),
  getAnalysisItemCascade: jest.fn(),
  searchParts: jest.fn(),
  searchSuppliers: jest.fn(),
  createBrand: jest.fn(),
  updateBrand: jest.fn(),
  deleteBrand: jest.fn(),
  getDepartments: jest.fn(),
  createDepartment: jest.fn(),
  updateDepartment: jest.fn(),
  deleteDepartment: jest.fn(),
  getHolidays: jest.fn(),
  createHoliday: jest.fn(),
  deleteHoliday: jest.fn(),
  createRequestType: jest.fn(),
  updateRequestType: jest.fn(),
  deleteRequestType: jest.fn(),
  getAnalysisItems: jest.fn(),
  createAnalysisItem: jest.fn(),
  updateAnalysisItem: jest.fn(),
  deleteAnalysisItem: jest.fn(),
  getEquipments: jest.fn(),
  createEquipment: jest.fn(),
  updateEquipment: jest.fn(),
  updateEquipmentStatus: jest.fn(),
  getEquipmentStats: jest.fn(),
  getEquipmentRepairs: jest.fn(),
  createEquipmentRepair: jest.fn(),
  completeEquipmentRepair: jest.fn(),
  deleteEquipmentRepair: jest.fn(),
  getReports: jest.fn(),
  getReport: jest.fn(),
  submitReport: jest.fn(),
  approveReport: jest.fn(),
  rejectReport: jest.fn(),
  reviseReport: jest.fn(),
  getReportEditUrl: jest.fn(),
  syncReportFromSharePoint: jest.fn(),
  getReportRevisions: jest.fn(),
  getCostStats: jest.fn(),
  getAdminUsers: jest.fn(),
  updateUserRoles: jest.fn(),
  toggleUserActive: jest.fn(),
  getAdminLogs: jest.fn(),
  getI18nMessages: jest.fn(),
  upsertI18nMessage: jest.fn(),
  deleteI18nMessage: jest.fn(),
  getKnowledgeDocs: jest.fn(),
  uploadKnowledgeDoc: jest.fn(),
  deleteKnowledgeDoc: jest.fn(),
}));

export { mockHistory };
