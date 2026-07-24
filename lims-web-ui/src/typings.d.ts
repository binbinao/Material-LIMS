declare namespace API {
  interface CurrentUser {
    id: string;
    email: string;
    displayName: string;
    roles: string;
    deptId?: string;
  }

  interface Brand {
    id?: string;
    name: string;
    description?: string;
    sortOrder?: number;
  }

  interface RequestCreateDTO {
    brandId: string;
    deptId?: string;
    typeId: string;
    partNumber?: string;
    partName?: string;
    eco?: string;
    supplierCode?: string;
    supplierName?: string;
    requestReason: string;
    priority?: string;
    proxyRequest?: boolean;
    realRequesterName?: string;
    analysisItemIds: string[];
  }

  interface Request {
    id: string;
    requestNo: string;
    brandId: string;
    deptId?: string;
    typeId: string;
    requesterId: string;
    proxyRequesterId?: string;
    realRequesterName?: string;
    partNumber?: string;
    partName?: string;
    eco?: string;
    supplierCode?: string;
    supplierName?: string;
    requestReason?: string;
    priority: string;
    status: string;
    dueDate?: string;
    sampleDeliveryNote?: string;
    totalCost?: number;
    processInstanceId?: string;
    submittedAt?: string;
    assignedAt?: string;
    createdAt: string;
    updatedAt: string;
  }

  interface AnalysisTask {
    id: string;
    requestId: string;
    itemId: string;
    assigneeId?: string;
    status: string;
    delayReason?: string;
    startedAt?: string;
    completedAt?: string;
    sortOrder?: number;
  }

  interface Report {
    id: string;
    requestId: string;
    taskId?: string;
    authorId: string;
    versionNumber: string;
    revisionNote?: string;
    status: string;
    fileUrl?: string;
    pdfUrl?: string;
    sharepointFileId?: string;
    sharepointEditUrl?: string;
    approvedBy?: string;
    approvedAt?: string;
    submittedAt?: string;
    createdAt: string;
    updatedAt: string;
  }

  interface RequestType {
    id?: string;
    name: string;
    code: string;
    taskDurationDays?: number;
    description?: string;
    active?: boolean;
  }

  interface Department {
    id?: string;
    name: string;
    code: string;
    parentId?: string;
    sortOrder?: number;
  }

  interface Holiday {
    id?: string;
    date: string;
    name: string;
    type: string;
  }

  interface Equipment {
    id?: string;
    name: string;
    model?: string;
    serialNumber?: string;
    status: string;
    location?: string;
    purchaseDate?: string;
    warrantyExpiry?: string;
    description?: string;
  }

  interface AnalysisItem {
    id?: string;
    name: string;
    code: string;
    groupId?: string;
    testSite?: string;
    analysisType?: string;
    cost?: number;
    durationDays?: number;
    specificationId?: string;
    active?: boolean;
  }

  interface ISysUser {
    id: string;
    email: string;
    displayName: string;
    loginId?: string;
    deptId?: string;
    roles: string;
    externalId?: string;
    isActive?: boolean;
    lastLoginAt?: string;
  }

  interface IPageResult<T> {
    records: T[];
    total: number;
    size: number;
    current: number;
  }

  /** Issue #87: Unified API response wrapper */
  interface IApiResponse<T = any> {
    code: number;
    message?: string;
    data: T;
  }

  /** Issue #87: Equipment repair entity */
  interface EquipmentRepair {
    id?: string;
    equipmentId: string;
    issue: string;
    status: string;
    repairDate?: string;
    completedAt?: string;
    cost?: number;
    remark?: string;
  }

  /** Issue #87: Knowledge document entity */
  interface KnowledgeDoc {
    id?: string;
    title: string;
    category: string;
    description?: string;
    fileUrl?: string;
    fileSize?: number;
    uploadedBy?: string;
  }

  /** Issue #87: Sample entity (Issue #80) */
  interface Sample {
    id?: string;
    requestId: string;
    sampleName?: string;
    barcode?: string;
    batchNo?: string;
    container?: string;
    quantity?: number;
    quantityUnit?: string;
    storageLocation?: string;
    custodianId?: string;
    receivedCondition?: string;
    sampleStatus?: string;
    receivedAt?: string;
    disposedAt?: string;
    disposalMethod?: string;
    parentSampleId?: string;
  }

  /** Issue #87: Test result entity (Issue #79) */
  interface TestResult {
    id?: string;
    analysisTaskId: string;
    requestId: string;
    itemId?: string;
    testMethod?: string;
    equipmentId?: string;
    rawValue?: string;
    enteredValue?: number;
    unit?: string;
    specLower?: number;
    specUpper?: number;
    judgment?: string;
    uncertainty?: number;
    repeatCount?: number;
    remark?: string;
    enteredBy?: string;
    enteredAt?: string;
    reviewedBy?: string;
    reviewedAt?: string;
    status?: string;
  }

  /** Issue #87: Equipment calibration entity (Issue #81) */
  interface EquipmentCalibration {
    id?: string;
    equipmentId: string;
    calibrationType: string;
    calibratedAt: string;
    nextCalibrationDate?: string;
    certificateNo?: string;
    certificateUrl?: string;
    calibratedBy?: string;
    result: string;
    rangeMin?: number;
    rangeMax?: number;
    accuracy?: string;
    remark?: string;
  }
}
