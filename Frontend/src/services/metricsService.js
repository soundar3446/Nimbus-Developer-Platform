import api from '../api/axios';

export const getDeploymentMetrics = async ({ deploymentId, start, end }) => {
  const response = await api.get(`/v1/metrics/deployments/${deploymentId}`, {
    params: { start, end }
  });
  return response.data;
};
