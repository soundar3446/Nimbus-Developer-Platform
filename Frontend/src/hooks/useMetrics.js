import { useQuery } from '@tanstack/react-query';
import { getDeploymentMetrics } from '../services/metricsService';

export const useDeploymentMetrics = (deploymentId, start, end) => {
  return useQuery({
    queryKey: ['metrics', 'deployment', deploymentId, { start, end }],
    queryFn: () => getDeploymentMetrics({ deploymentId, start, end }),
    enabled: !!deploymentId,
    // Typically metrics should refresh periodically
    refetchInterval: 15000, 
  });
};
