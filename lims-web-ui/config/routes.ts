export default [
  {
    path: '/login',
    layout: false,
    component: './Login',
  },
  {
    path: '/',
    redirect: '/workspace',
  },
  {
    path: '/workspace',
    name: 'workspace',
    icon: 'DesktopOutlined',
    component: './Workspace',
  },
  {
    path: '/request',
    name: 'request',
    icon: 'FormOutlined',
    routes: [
      { path: '/request/list', name: 'list', component: './request/RequestList' },
      { path: '/request/create', name: 'create', component: './request/RequestCreate', hideInMenu: true },
      { path: '/request/:id', name: 'detail', component: './request/RequestDetail', hideInMenu: true },
      { path: '/request/kanban', name: 'kanban', component: './request/RequestKanban' },
    ],
  },
  {
    path: '/report',
    name: 'report',
    icon: 'FileTextOutlined',
    routes: [
      { path: '/report/list', name: 'list', component: './report/ReportList' },
      { path: '/report/:id', name: 'detail', component: './report/ReportDetail', hideInMenu: true },
      { path: '/report/:id/edit', name: 'edit', component: './report/ReportEdit', hideInMenu: true },
      { path: '/report/:id/revisions', name: 'revisions', component: './report/ReportRevisions', hideInMenu: true },
      { path: '/report/archive', name: 'archive', component: './report/ReportArchive', access: 'canManager' },
    ],
  },
  {
    path: '/equipment',
    name: 'equipment',
    icon: 'ToolOutlined',
    routes: [
      { path: '/equipment/list', name: 'list', component: './equipment/EquipmentList' },
      { path: '/equipment/status', name: 'status', component: './equipment/EquipmentStatus' },
      { path: '/equipment/repairs', name: 'repairs', component: './equipment/EquipmentRepairs' },
    ],
  },
  {
    path: '/dashboard',
    name: 'dashboard',
    icon: 'DashboardOutlined',
    access: 'canManager',
    routes: [
      { path: '/dashboard/request', name: 'requestStats', component: './dashboard/RequestDashboard' },
      { path: '/dashboard/cost', name: 'costStats', component: './dashboard/CostDashboard' },
    ],
  },
  {
    path: '/basic-data',
    name: 'basicData',
    icon: 'DatabaseOutlined',
    access: 'canAdmin',
    routes: [
      { path: '/basic-data/brands', name: 'brands', component: './basic-data/BrandList' },
      { path: '/basic-data/request-types', name: 'requestTypes', component: './basic-data/RequestTypeList' },
      { path: '/basic-data/holidays', name: 'holidays', component: './basic-data/HolidayList' },
      { path: '/basic-data/request-notes', name: 'requestNotes', component: './basic-data/RequestNoteList' },
      { path: '/basic-data/departments', name: 'departments', component: './basic-data/DepartmentList' },
    ],
  },
  {
    path: '/test-data',
    name: 'testData',
    icon: 'ExperimentOutlined',
    access: 'canAdmin',
    routes: [
      { path: '/test-data/groups', name: 'groups', component: './test-data/TestGroupList' },
      { path: '/test-data/sites', name: 'sites', component: './test-data/TestSiteList' },
      { path: '/test-data/analysis-types', name: 'analysisTypes', component: './test-data/AnalysisTypeList' },
      { path: '/test-data/analysis-items', name: 'analysisItems', component: './test-data/AnalysisItemList' },
      { path: '/test-data/specifications', name: 'specifications', component: './test-data/SpecificationList' },
    ],
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    icon: 'BookOutlined',
    component: './knowledge/KnowledgeList',
  },
  {
    path: '/admin',
    name: 'admin',
    icon: 'SettingOutlined',
    access: 'canAdmin',
    routes: [
      { path: '/admin/users', name: 'users', component: './admin/UserList' },
      { path: '/admin/logs', name: 'logs', component: './admin/LogList' },
      { path: '/admin/i18n', name: 'i18n', component: './admin/I18nList' },
    ],
  },
];
