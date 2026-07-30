import { forwardRef } from 'react';

const Input = forwardRef(({ className, label, error, ...props }, ref) => {
  return (
    <div className="w-full flex flex-col gap-1.5">
      {label && <label className="text-sm font-medium text-text-muted">{label}</label>}
      <input
        ref={ref}
        className={`w-full px-4 py-2.5 bg-surface border border-surface-hover rounded-xl text-text-main focus:outline-none focus:ring-2 focus:ring-primary/50 focus:border-primary transition-all placeholder:text-text-muted/50 ${className || ''}`}
        {...props}
      />
      {error && <span className="text-xs text-red-500 font-medium">{error}</span>}
    </div>
  );
});

Input.displayName = 'Input';
export default Input;
