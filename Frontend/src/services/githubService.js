import api from '../api/axios';

export const getGithubLoginUrl = async () => {
  const response = await api.get('/github/login');
  return response.data;
};

export const handleGithubCallback = async (code, state) => {
  const response = await api.get('/github/callback', {
    params: { code, state }
  });
  return response.data;
};

export const getGithubProfile = async () => {
  const response = await api.get('/github/profile');
  return response.data;
};

export const getGithubRepos = async () => {
  const response = await api.get('/github/repos');
  return response.data;
};

export const connectGithubRepo = async ({ projectId, repoUrl }) => {
  const response = await api.post(`/github/connect/${projectId}`, null, {
    params: { repoUrl }
  });
  return response.data;
};
