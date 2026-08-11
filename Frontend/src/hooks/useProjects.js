import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getProjects, createProject, getProjectById, updateProject, deleteProject, verifyCustomDomain } from '../services/projectService';

export const useProjects = () => {
  const queryClient = useQueryClient();

  const { data: response, isLoading, isError, error } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects
  });

  const createMutation = useMutation({
    mutationFn: createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    }
  });

  const updateMutation = useMutation({
    mutationFn: updateProject,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['projects', variables.uuid] });
    }
  });

  const deleteMutation = useMutation({
    mutationFn: deleteProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    }
  });

  const verifyDomainMutation = useMutation({
    mutationFn: verifyCustomDomain,
    onSuccess: (_, uuid) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      queryClient.invalidateQueries({ queryKey: ['projects', uuid] });
    }
  });

  return {
    projects: response?.data || [],
    isLoading,
    isError,
    error,
    
    createProject: createMutation.mutate,
    isCreating: createMutation.isPending,
    
    updateProject: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
    
    deleteProject: deleteMutation.mutate,
    isDeleting: deleteMutation.isPending,
    
    verifyCustomDomain: verifyDomainMutation.mutate,
    isVerifyingDomain: verifyDomainMutation.isPending,
  };
};

export const useProjectDetails = (uuid) => {
  return useQuery({
    queryKey: ['projects', uuid],
    queryFn: () => getProjectById(uuid),
    enabled: !!uuid,
  });
};
