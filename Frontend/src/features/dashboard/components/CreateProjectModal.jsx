import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import Modal from '../../../components/ui/Modal';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { useProjects } from '../../../hooks/useProjects';

const createProjectSchema = z.object({
  name: z.string().min(1, "Project name is required").max(100),
  subdomain: z.string().min(3).max(63).regex(/^[a-z0-9-]+$/, "Only lowercase letters, numbers, and hyphens"),
  githubRepo: z.string().min(1, "GitHub Repo URL is required"),
  defaultBranch: z.string().default("main"),
  dockerfilePath: z.string().default("Dockerfile"),
  contextPath: z.string().default("."),
  imageName: z.string().min(1, "Image name is required"),
});

export default function CreateProjectModal({ isOpen, onClose }) {
  const { createProject, isCreating } = useProjects();
  
  const { register, handleSubmit, formState: { errors }, reset } = useForm({
    resolver: zodResolver(createProjectSchema),
    defaultValues: {
      defaultBranch: "main",
      dockerfilePath: "Dockerfile",
      contextPath: ".",
    }
  });

  const onSubmit = (data) => {
    createProject(data, {
      onSuccess: () => {
        reset();
        onClose();
      }
    });
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New Project">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
        <Input 
          label="Project Name" 
          placeholder="My Awesome App" 
          error={errors.name?.message}
          {...register('name')}
        />
        
        <Input 
          label="Subdomain (nimbus.app)" 
          placeholder="my-awesome-app" 
          error={errors.subdomain?.message}
          {...register('subdomain')}
        />
        
        <Input 
          label="GitHub Repository URL" 
          placeholder="https://github.com/user/repo" 
          error={errors.githubRepo?.message}
          {...register('githubRepo')}
        />

        <Input 
          label="Image Name" 
          placeholder="my-app-image" 
          error={errors.imageName?.message}
          {...register('imageName')}
        />

        <div className="grid grid-cols-2 gap-4">
          <Input 
            label="Dockerfile Path" 
            placeholder="Dockerfile" 
            error={errors.dockerfilePath?.message}
            {...register('dockerfilePath')}
          />
          <Input 
            label="Context Path" 
            placeholder="." 
            error={errors.contextPath?.message}
            {...register('contextPath')}
          />
        </div>

        <div className="pt-4 flex justify-end gap-3 border-t border-white/5">
          <Button type="button" variant="secondary" onClick={onClose} className="bg-transparent border border-white/10 hover:bg-white/5">
            Cancel
          </Button>
          <Button type="submit" isLoading={isCreating}>
            Create Project
          </Button>
        </div>
      </form>
    </Modal>
  );
}
