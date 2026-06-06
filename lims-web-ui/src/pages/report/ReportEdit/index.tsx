import { PageContainer } from '@ant-design/pro-components';
import { Card, Spin, App } from 'antd';
import { useParams, history } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportEditUrl, getReport } from '@/services/requestService';

const ReportEdit: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message } = App.useApp();

  const { data: reportData } = useRequest(() => getReport(params.id));
  const { data: editUrlData, loading } = useRequest(() => getReportEditUrl(params.id));

  const report = reportData?.data;
  const editUrl = editUrlData?.data;

  return (
    <PageContainer
      title={`Edit Report ${report?.versionNumber || ''}`}
      onBack={() => history.push(`/report/${params.id}`)}
    >
      <Card>
        {loading ? (
          <Spin tip="Loading editor..." />
        ) : editUrl ? (
          <iframe
            src={editUrl}
            style={{ width: '100%', height: 'calc(100vh - 200px)', border: 'none' }}
            title="M365 Online Editor"
          />
        ) : (
          <div style={{ textAlign: 'center', padding: 48, color: '#999' }}>
            Microsoft 365 online editing is not available. Please configure SharePoint integration.
          </div>
        )}
      </Card>
    </PageContainer>
  );
};

export default ReportEdit;
