import React from 'react';
import { PageContainer } from '@ant-design/pro-components';
import { Card, Spin, Alert, Empty, App } from 'antd';
import { useParams, history, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportEditUrl, getReport } from '@/services/requestService';

const isMockUrl = (url: string | null | undefined): boolean =>
  !!url && url.startsWith('https://example.invalid/');

const ReportEdit: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message: _message } = App.useApp();
  const intl = useIntl();

  const { data: reportData } = useRequest(() => getReport(params.id));
  const { data: editUrlData, loading } = useRequest(() => getReportEditUrl(params.id));

  const report = reportData?.data;
  const editUrl = editUrlData?.data;

  return (
    <PageContainer
      title={`${intl.formatMessage({ id: 'report.edit.title' })} ${report?.versionNumber || ''}`}
      onBack={() => history.push(`/report/${params.id}`)}
    >
      <Card>
        {loading ? (
          <Spin tip={intl.formatMessage({ id: 'common.search' })} />
        ) : !editUrl ? (
          <Empty
            description={intl.formatMessage({ id: 'report.edit.unavailable' })}
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : (
          <>
            {isMockUrl(editUrl) && (
              <Alert
                style={{ marginBottom: 12 }}
                type="info"
                showIcon
                message={intl.formatMessage({ id: 'report.edit.mockHint' })}
              />
            )}
            <iframe
              src={editUrl}
              style={{ width: '100%', height: 'calc(100vh - 240px)', border: 'none' }}
              title={intl.formatMessage({ id: 'report.edit.title' })}
            />
          </>
        )}
      </Card>
    </PageContainer>
  );
};

export default ReportEdit;