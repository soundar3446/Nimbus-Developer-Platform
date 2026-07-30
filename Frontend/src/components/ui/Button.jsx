import { forwardRef } from 'react';

const Button = forwardRef(({ className, children, isLoading, variant = 'primary', ...props }, ref) => {
  const baseStyles = "relative w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl font-semibold transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed";
  
  const variants = {
    primary: "bg-primary hover:bg-primary-hover text-white shadow-lg shadow-primary/25",
    secondary: "bg-surface hover:bg-surface-hover text-text-main",
    outline: "bg-transparent border-2 border-surface-hover hover:border-primary text-text-main"
  };

  return (
    <button
      ref={ref}
      disabled={isLoading || props.disabled}
      className={`${baseStyles} ${variants[variant]} ${className || ''}`}
      {...props}
    >
      {isLoading ? (
        <span className="w-5 h-5 border-2 border-white/20 border-t-white rounded-full animate-spin absolute" />
      ) : null}
      <span className={isLoading ? 'opacity-0' : 'opacity-100'}>{children}</span>
    </button>
  );
});

Button.displayName = 'Button';
export default Button;
