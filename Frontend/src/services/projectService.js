import api from '../api/axios';

export const getProjects = async () => {
  const response = await api.get('/projects');
  return response.data;
};

export const createProject = async (projectData) => {
  const response = await api.post('/projects', projectData);
  return response.data;
};

export const getProjectById = async (uuid) => {
  const response = await api.get(`/projects/${uuid}`);
  return response.data;
};

export const updateProject = async ({ uuid, projectData }) => {
  const response = await api.put(`/projects/${uuid}`, projectData);
  return response.data;
};

export const deleteProject = async (uuid) => {
  const response = await api.delete(`/projects/${uuid}`);
  return response.data;
};

export const verifyCustomDomain = async (uuid) => {
  const response = await api.post(`/projects/${uuid}/verify-domain`);
  return response.data;
};
