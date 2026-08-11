import api from '../api/axios';

export const getProjectDeployments = async (projectUuid) => {
  const response = await api.get(`/deployments/project/${projectUuid}`);
  return response.data;
};

export const triggerDeployment = async (projectUuid) => {
  const response = await api.post(`/deployments/${projectUuid}/clone`);
  return response.data;
};

export const startDeployment = async (deploymentId) => {
  const response = await api.post(`/deployments/${deploymentId}/start`);
  return response.data;
};

export const stopDeployment = async (deploymentId) => {
  const response = await api.post(`/deployments/${deploymentId}/stop`);
  return response.data;
};

export const restartDeployment = async (deploymentId) => {
  const response = await api.post(`/deployments/${deploymentId}/restart`);
  return response.data;
};

export const getDeploymentLogs = async (deploymentId) => {
  const response = await api.get(`/deployments/${deploymentId}/logs`);
  return response.data;
};

export const getDeploymentStatus = async (deploymentId) => {
  const response = await api.get(`/deployments/${deploymentId}/status`);
  return response.data;
};

export const rollbackDeployment = async (deploymentId) => {
  const response = await api.post(`/deployments/${deploymentId}/rollback`);
  return response.data;
};

export const deleteDeployment = async (deploymentId, namespace = 'default') => {
  const response = await api.delete(`/deployments/${deploymentId}`, {
    params: { namespace }
  });
  return response.data;
};
