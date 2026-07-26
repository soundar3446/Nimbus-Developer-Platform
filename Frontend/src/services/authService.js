import api from '../api/axios';

export const login = async (credentials) => {
  // Mock API call for demo since backend isn't ready
  return new Promise((resolve) => setTimeout(() => resolve({ data: { user: { name: 'Demo User' }, token: 'mock_token' } }), 1000));
  // const response = await api.post('/auth/login', credentials);
  // return response.data;
};

export const signup = async (userData) => {
  // Mock API call for demo
  return new Promise((resolve) => setTimeout(() => resolve({ data: { user: { name: userData.name }, token: 'mock_token' } }), 1000));
  // const response = await api.post('/auth/signup', userData);
  // return response.data;
};
