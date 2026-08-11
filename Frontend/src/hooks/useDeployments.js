import { useMutation, useQueries, useQueryClient } from '@tanstack/react-query';
import {
  triggerDeployment,
  startDeployment,
  stopDeployment,
  restartDeployment,
  deleteDeployment,
  rollbackDeployment,
  getProjectDeployments,
} from '../services/deploymentService';

export const useAllProjectDeployments = (projects) => {
  const deploymentQueries = useQueries({
    queries: projects.map(project => ({
      queryKey: ['deployments', project.uuid],
      queryFn: () => getProjectDeployments(project.uuid),
      enabled: !!project.uuid,
    }))
  });

  const isLoading = deploymentQueries.some(q => q.isLoading);
  const data = deploymentQueries.map(q => q.data?.data || []).flat();

  return { data, isLoading };
};

export const useDeployments = () => {
  const queryClient = useQueryClient();

  const triggerMutation = useMutation({
    mutationFn: (projectUuid) => triggerDeployment(projectUuid),
    onSuccess: (_, projectUuid) => {
      queryClient.invalidateQueries({ queryKey: ['deployments', projectUuid] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });

  const startMutation = useMutation({
    mutationFn: (deploymentId) => startDeployment(deploymentId),
    onSuccess: () => {
      // Invalidate relevant queries (e.g. status)
      queryClient.invalidateQueries({ queryKey: ['deployments'] });
    },
  });

  const stopMutation = useMutation({
    mutationFn: (deploymentId) => stopDeployment(deploymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deployments'] });
    },
  });

  const restartMutation = useMutation({
    mutationFn: (deploymentId) => restartDeployment(deploymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deployments'] });
    },
  });

  const rollbackMutation = useMutation({
    mutationFn: (deploymentId) => rollbackDeployment(deploymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deployments'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ({ deploymentId, namespace }) => deleteDeployment(deploymentId, namespace),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deployments'] });
    },
  });

  return {
    triggerDeployment: triggerMutation.mutate,
    isTriggering: triggerMutation.isPending,
    
    startDeployment: startMutation.mutate,
    isStarting: startMutation.isPending,
    
    stopDeployment: stopMutation.mutate,
    isStopping: stopMutation.isPending,
    
    restartDeployment: restartMutation.mutate,
    isRestarting: restartMutation.isPending,
    
    rollbackDeployment: rollbackMutation.mutate,
    isRollingBack: rollbackMutation.isPending,

    deleteDeployment: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
  };
};
