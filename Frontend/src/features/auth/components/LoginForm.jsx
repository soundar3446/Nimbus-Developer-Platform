import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Mail, Lock } from 'lucide-react';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { useAuth } from '../../../hooks/useAuth';

const loginSchema = z.object({
  email: z.string().min(1, "Email is required").email("Invalid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

export default function LoginForm() {
  const { login, isLoggingIn, loginError } = useAuth();
  
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(loginSchema)
  });

  const onSubmit = (data) => {
    login(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5 w-full">
      <div className="relative">
        <Input 
          type="email" 
          placeholder="name@company.com" 
          label="Email Address"
          error={errors.email?.message}
          {...register('email')}
          className="pl-11"
        />
        <Mail className="absolute left-4 top-[34px] w-5 h-5 text-text-muted" />
      </div>
      
      <div className="relative">
        <Input 
          type="password" 
          placeholder="••••••••" 
          label="Password"
          error={errors.password?.message}
          {...register('password')}
          className="pl-11"
        />
        <Lock className="absolute left-4 top-[34px] w-5 h-5 text-text-muted" />
      </div>

      <div className="flex items-center justify-between text-sm">
        <label className="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" className="rounded border-surface-hover bg-surface text-primary focus:ring-primary/50" />
          <span className="text-text-muted">Remember me</span>
        </label>
        <a href="#" className="text-primary hover:text-primary-hover font-medium transition-colors">Forgot password?</a>
      </div>

      <Button type="submit" isLoading={isLoggingIn} className="mt-2">
        Sign In
      </Button>

      {loginError && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-500 text-sm text-center">
          Authentication failed. Please check your credentials.
        </div>
      )}
    </form>
  );
}
