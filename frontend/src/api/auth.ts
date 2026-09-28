import { apiClient } from './client';

export interface UserProfile {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  status: string;
  roles: string[];
  permissions: string[];
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: {
    code: string;
    message: string;
    details?: any;
  };
  timestamp: string;
  requestId: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export const authApi = {
  async login(data: LoginRequest): Promise<UserProfile> {
    const res = await apiClient.post<ApiResponse<UserProfile>>('/auth/login', data);
    return res.data.data;
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout');
  },

  async getCurrentUser(): Promise<UserProfile> {
    const res = await apiClient.get<ApiResponse<UserProfile>>('/auth/me');
    return res.data.data;
  },

  async refreshToken(): Promise<void> {
    await apiClient.post('/auth/refresh');
  },
};
