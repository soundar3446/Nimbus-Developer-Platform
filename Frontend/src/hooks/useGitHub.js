import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getGithubLoginUrl,
  handleGithubCallback,
  getGithubProfile,
  getGithubRepos,
  connectGithubRepo
} from '../services/githubService';

export const useGithubLoginUrl = () => {
  return useQuery({
    queryKey: ['github', 'loginUrl'],
    queryFn: getGithubLoginUrl,
  });
};

export const useGithubProfile = () => {
  return useQuery({
    queryKey: ['github', 'profile'],
    queryFn: getGithubProfile,
    retry: false, // Don't retry if not connected
  });
};

export const useGithubRepos = () => {
  return useQuery({
    queryKey: ['github', 'repos'],
    queryFn: getGithubRepos,
    retry: false,
  });
};

export const useGithubAuth = () => {
  const queryClient = useQueryClient();

  const callbackMutation = useMutation({
    mutationFn: ({ code, state }) => handleGithubCallback(code, state),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['github'] });
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    }
  });

  const connectMutation = useMutation({
    mutationFn: connectGithubRepo,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects', variables.projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    }
  });

  return {
    handleCallback: callbackMutation.mutate,
    isHandlingCallback: callbackMutation.isPending,
    
    connectRepo: connectMutation.mutate,
    isConnectingRepo: connectMutation.isPending,
  };
};
