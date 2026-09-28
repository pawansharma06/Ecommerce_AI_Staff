import axios from 'axios';
import { useQuery } from '@tanstack/react-query';

export interface HealthResponse {
  status: string;
  application: string;
  version: string;
  database: string;
  redis: string;
}

export const fetchHealth = async (): Promise<HealthResponse> => {
  const response = await axios.get<HealthResponse>('/api/v1/health');
  return response.data;
};

export const useHealth = () => {
  return useQuery<HealthResponse, Error>({
    queryKey: ['health'],
    queryFn: fetchHealth,
    refetchInterval: 10000,
  });
};
