import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
  withCredentials: true,
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Handle global errors here
    return Promise.reject(error);
  }
);

export default api;
