import { request } from '@umijs/max';

const API_PREFIX = '/api/v1';

/** Brand API */
export async function getBrands(params?: { page?: number; size?: number }) {
  return request(`${API_PREFIX}/brands`, { params });
}

export async function createBrand(data: API.Brand) {
  return request(`${API_PREFIX}/brands`, { method: 'POST', data });
}

export async function updateBrand(id: string, data: API.Brand) {
  return request(`${API_PREFIX}/brands/${id}`, { method: 'PUT', data });
}

export async function deleteBrand(id: string) {
  return request(`${API_PREFIX}/brands/${id}`, { method: 'DELETE' });
}

/** Request API */
export async function getRequests(params?: Record<string, any>) {
  return request(`${API_PREFIX}/requests`, { params });
}

export async function getRequest(id: string) {
  return request(`${API_PREFIX}/requests/${id}`);
}

export async function createRequest(data: API.RequestCreateDTO) {
  return request(`${API_PREFIX}/requests`, { method: 'POST', data });
}

export async function submitRequest(id: string) {
  return request(`${API_PREFIX}/requests/${id}/submit`, { method: 'POST' });
}

export async function assignRequest(
  id: string,
  assignments: { taskId: string; engineerId: string }[],
  priority?: string,
) {
  return request(`${API_PREFIX}/requests/${id}/assign`, {
    method: 'POST',
    data: assignments,
    params: priority ? { priority } : {},
  });
}

export async function rejectRequest(id: string, data: { reason: string }) {
  return request(`${API_PREFIX}/requests/${id}/reject`, { method: 'POST', data });
}

export async function receiveSample(id: string, data: { deliveryNote: string }) {
  return request(`${API_PREFIX}/requests/${id}/receive-sample`, { method: 'POST', data });
}

export async function startReporting(id: string) {
  return request(`${API_PREFIX}/requests/${id}/start-reporting`, { method: 'POST' });
}

export async function completeRequest(id: string) {
  return request(`${API_PREFIX}/requests/${id}/complete`, { method: 'POST' });
}

export async function getRequestTasks(id: string) {
  return request(`${API_PREFIX}/requests/${id}/tasks`);
}

export async function updateAnalysisTask(taskId: string, data: { status: string; delayReason?: string }) {
  return request(`${API_PREFIX}/requests/tasks/${taskId}`, { method: 'PUT', data });
}

export async function getRequestWorkflow(id: string) {
  return request(`${API_PREFIX}/requests/${id}/workflow`);
}

export async function getMyPendingTasks() {
  return request(`${API_PREFIX}/requests/my-tasks`);
}

/** External API */
export async function searchParts(keyword: string) {
  return request(`${API_PREFIX}/external/parts`, { params: { keyword } });
}

export async function searchSuppliers(keyword: string) {
  return request(`${API_PREFIX}/external/suppliers`, { params: { keyword } });
}

/** Analysis Items */
export async function getAnalysisItemCascade() {
  return request(`${API_PREFIX}/analysis-items/cascade`);
}

export async function getAnalysisItems(params?: Record<string, any>) {
  return request(`${API_PREFIX}/analysis-items`, { params });
}

export async function createAnalysisItem(data: any) {
  return request(`${API_PREFIX}/analysis-items`, { method: 'POST', data });
}

export async function updateAnalysisItem(id: string, data: any) {
  return request(`${API_PREFIX}/analysis-items/${id}`, { method: 'PUT', data });
}

export async function deleteAnalysisItem(id: string) {
  return request(`${API_PREFIX}/analysis-items/${id}`, { method: 'DELETE' });
}

/** Request Type API */
export async function getRequestTypes(params?: Record<string, any>) {
  return request(`${API_PREFIX}/request-types`, { params });
}

export async function createRequestType(data: any) {
  return request(`${API_PREFIX}/request-types`, { method: 'POST', data });
}

export async function updateRequestType(id: string, data: any) {
  return request(`${API_PREFIX}/request-types/${id}`, { method: 'PUT', data });
}

export async function deleteRequestType(id: string) {
  return request(`${API_PREFIX}/request-types/${id}`, { method: 'DELETE' });
}

/** Department API */
export async function getDepartments() {
  return request(`${API_PREFIX}/departments`);
}

export async function createDepartment(data: any) {
  return request(`${API_PREFIX}/departments`, { method: 'POST', data });
}

export async function updateDepartment(id: string, data: any) {
  return request(`${API_PREFIX}/departments/${id}`, { method: 'PUT', data });
}

export async function deleteDepartment(id: string) {
  return request(`${API_PREFIX}/departments/${id}`, { method: 'DELETE' });
}

/** Holiday API */
export async function getHolidays(params?: Record<string, any>) {
  return request(`${API_PREFIX}/holidays`, { params });
}

export async function createHoliday(data: any) {
  return request(`${API_PREFIX}/holidays`, { method: 'POST', data });
}

export async function batchImportHolidays(data: any[]) {
  return request(`${API_PREFIX}/holidays/batch`, { method: 'POST', data });
}

export async function deleteHoliday(id: string) {
  return request(`${API_PREFIX}/holidays/${id}`, { method: 'DELETE' });
}

/** Equipment API */
export async function getEquipments(params?: Record<string, any>) {
  return request(`${API_PREFIX}/equipments`, { params });
}

export async function getEquipment(id: string) {
  return request(`${API_PREFIX}/equipments/${id}`);
}

export async function createEquipment(data: any) {
  return request(`${API_PREFIX}/equipments`, { method: 'POST', data });
}

export async function updateEquipment(id: string, data: any) {
  return request(`${API_PREFIX}/equipments/${id}`, { method: 'PUT', data });
}

export async function deleteEquipment(id: string) {
  return request(`${API_PREFIX}/equipments/${id}`, { method: 'DELETE' });
}

export async function getEquipmentStats() {
  return request(`${API_PREFIX}/dashboard/equipment-stats`);
}

/** Report API */
export async function getReports(params?: Record<string, any>) {
  return request(`${API_PREFIX}/reports`, { params });
}

export async function getReport(id: string) {
  return request(`${API_PREFIX}/reports/${id}`);
}

export async function createReport(requestId: string) {
  return request(`${API_PREFIX}/reports/requests/${requestId}/reports`, { method: 'POST' });
}

export async function submitReport(id: string) {
  return request(`${API_PREFIX}/reports/${id}/submit`, { method: 'POST' });
}

export async function approveReport(id: string) {
  return request(`${API_PREFIX}/reports/${id}/approve`, { method: 'POST' });
}

export async function rejectReport(id: string) {
  return request(`${API_PREFIX}/reports/${id}/reject`, { method: 'POST' });
}

export async function reviseReport(id: string, data: { revisionNote: string }) {
  return request(`${API_PREFIX}/reports/${id}/revise`, { method: 'POST', data });
}

export async function getReportRevisions(id: string) {
  return request(`${API_PREFIX}/reports/${id}/revisions`);
}

export async function getReportEditUrl(id: string) {
  return request(`${API_PREFIX}/reports/${id}/edit-url`);
}

export async function syncReportFromSharePoint(id: string) {
  return request(`${API_PREFIX}/reports/${id}/sync`, { method: 'POST' });
}

/** Dashboard API */
export async function getMyDashboard(userId: string) {
  return request(`${API_PREFIX}/dashboard/my-tasks`, { params: { userId } });
}

export async function getManagerOverview() {
  return request(`${API_PREFIX}/dashboard/manager-overview`);
}

export async function getRequestStats(params?: Record<string, any>) {
  return request(`${API_PREFIX}/dashboard/request-stats`, { params });
}

export async function getCostStats(params?: Record<string, any>) {
  return request(`${API_PREFIX}/dashboard/cost-stats`, { params });
}

/** Auth API */
export async function getAuthUrl() {
  return request(`${API_PREFIX}/auth/azure-ad/url`);
}

export async function handleAuthCallback(code: string) {
  return request(`${API_PREFIX}/auth/azure-ad/callback`, { method: 'POST', data: { code } });
}

/** Admin API */
export async function getAdminUsers(params?: Record<string, any>) {
  return request(`${API_PREFIX}/admin/users`, { params });
}

export async function updateUserRoles(id: string, data: { roles: string }) {
  return request(`${API_PREFIX}/admin/users/${id}/roles`, { method: 'PUT', data });
}

export async function toggleUserActive(id: string) {
  return request(`${API_PREFIX}/admin/users/${id}/toggle-active`, { method: 'PUT' });
}

export async function getAdminLogs(params?: Record<string, any>) {
  return request(`${API_PREFIX}/admin/logs`, { params });
}

export async function getAdminLog(id: string) {
  return request(`${API_PREFIX}/admin/logs/${id}`);
}

/** Equipment Repair API */
export async function getEquipmentRepairs(params?: Record<string, any>) {
  return request(`${API_PREFIX}/equipment-repairs`, { params });
}

export async function getEquipmentRepair(id: string) {
  return request(`${API_PREFIX}/equipment-repairs/${id}`);
}

export async function createEquipmentRepair(data: any) {
  return request(`${API_PREFIX}/equipment-repairs`, { method: 'POST', data });
}

export async function updateEquipmentRepair(id: string, data: any) {
  return request(`${API_PREFIX}/equipment-repairs/${id}`, { method: 'PUT', data });
}

export async function completeEquipmentRepair(id: string, data: any) {
  return request(`${API_PREFIX}/equipment-repairs/${id}/complete`, { method: 'POST', data });
}

export async function deleteEquipmentRepair(id: string) {
  return request(`${API_PREFIX}/equipment-repairs/${id}`, { method: 'DELETE' });
}

/** i18n API */
export async function getI18nMessages(locale: string) {
  return request(`${API_PREFIX}/i18n/messages`, { params: { locale } });
}

export async function upsertI18nMessage(data: { messageKey: string; locale: string; messageValue: string }) {
  return request(`${API_PREFIX}/i18n/messages`, { method: 'POST', data });
}

export async function deleteI18nMessage(params: { messageKey: string; locale: string }) {
  return request(`${API_PREFIX}/i18n/messages`, { method: 'DELETE', params });
}

/** Knowledge Hub API */
export async function getKnowledgeDocs(params?: Record<string, any>) {
  return request(`${API_PREFIX}/knowledge-docs`, { params });
}

export async function uploadKnowledgeDoc(formData: FormData) {
  // multipart upload: don't set requestType so axios derives the boundary automatically.
  return request(`${API_PREFIX}/knowledge-docs`, {
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export async function deleteKnowledgeDoc(id: string) {
  return request(`${API_PREFIX}/knowledge-docs/${id}`, { method: 'DELETE' });
}

/** Holiday business-day calc */
export async function calcDueDate(params: { startDate: string; days: number }) {
  return request(`${API_PREFIX}/holidays/calculate-due-date`, { params });
}
