import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { User, Mail, Lock } from 'lucide-react';
import Input from '../../../components/ui/Input';
import Button from '../../../components/ui/Button';
import { useAuth } from '../../../hooks/useAuth';

const signupSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
  email: z.string().min(1, "Email is required").email("Invalid email address"),
  password: z.string().min(6, "Password must be at least 6 characters"),
});

export default function SignUpForm() {
  const { signup, isSigningUp, signupError } = useAuth();
  
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(signupSchema)
  });

  const onSubmit = (data) => {
    signup(data);
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-5 w-full">
      <div className="relative">
        <Input 
          type="text" 
          placeholder="Jane Doe" 
          label="Full Name"
          error={errors.name?.message}
          {...register('name')}
          className="pl-11"
        />
        <User className="absolute left-4 top-[34px] w-5 h-5 text-text-muted" />
      </div>

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

      <Button type="submit" isLoading={isSigningUp} className="mt-2">
        Create Account
      </Button>

      {signupError && (
        <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-500 text-sm text-center">
          Failed to create account. Please try again.
        </div>
      )}
    </form>
  );
}
