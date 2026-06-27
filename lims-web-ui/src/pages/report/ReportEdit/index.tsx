import { PageContainer } from '@ant-design/pro-components';
import { Card, Spin, App } from 'antd';
import { useParams, history, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportEditUrl, getReport } from '@/services/requestService';

const ReportEdit: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message } = App.useApp();
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
        ) : editUrl ? (
          <iframe
            src={editUrl}
            style={{ width: '100%', height: 'calc(100vh - 200px)', border: 'none' }}
            title={intl.formatMessage({ id: 'report.edit.title' })}
          />
        ) : (
          <div style={{ textAlign: 'center', padding: 48, color: '#999' }}>
            {intl.formatMessage({ id: 'report.edit.title' })} {intl.formatMessage({ id: 'common.fail' })}
          </div>
        )}
      </Card>
    </PageContainer>
  );
};

export default ReportEdit;