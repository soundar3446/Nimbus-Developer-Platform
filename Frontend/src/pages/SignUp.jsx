import { Link } from 'react-router-dom';
import SignUpForm from '../features/auth/components/SignUpForm';

export default function SignUp() {
  return (
    <div className="min-h-screen flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background Effects */}
      <div className="absolute top-[-20%] right-[-10%] w-[50%] h-[50%] bg-primary/20 blur-[120px] rounded-full pointer-events-none" />
      <div className="absolute bottom-[-20%] left-[-10%] w-[50%] h-[50%] bg-indigo-600/20 blur-[120px] rounded-full pointer-events-none" />

      <div className="w-full max-w-md bg-surface/50 backdrop-blur-xl border border-white/10 p-8 sm:p-10 rounded-3xl shadow-2xl z-10">
        <div className="mb-10 text-center">
          <div className="w-12 h-12 bg-primary/20 text-primary flex items-center justify-center rounded-xl mx-auto mb-4 border border-primary/30 shadow-[0_0_15px_rgba(59,130,246,0.5)]">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z"/></svg>
          </div>
          <h1 className="text-3xl font-bold bg-gradient-to-br from-white to-white/60 bg-clip-text text-transparent mb-2">Join Nimbus</h1>
          <p className="text-text-muted">Start deploying seamlessly today</p>
        </div>

        <SignUpForm />

        <div className="mt-8 text-center text-sm text-text-muted">
          Already have an account?{' '}
          <Link to="/login" className="text-primary hover:text-primary-hover font-semibold transition-colors">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}
