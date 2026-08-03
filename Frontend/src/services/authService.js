import api from '../api/axios';

export const login = async (credentials) => {
  const response = await api.post('/auth/login', credentials);
  const token = response.data.data.token;
  if (token) {
    localStorage.setItem('token', token);
  }
  return response.data;
};

export const signup = async (userData) => {
  const response = await api.post('/auth/register', userData);
  const token = response.data.data.token;
  if (token) {
    localStorage.setItem('token', token);
  }
  return response.data;
};

export const logout = async () => {
  await api.post('/auth/logout');
  localStorage.removeItem('token');
};

export const getMyProfile = async () => {
  const response = await api.get('/auth/me');
  return response.data;
};
