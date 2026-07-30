import { Bell, Search, User } from "lucide-react";

export default function Topbar() {
  return (
    <header className="h-16 flex items-center justify-between px-6 border-b border-white/5 bg-transparent backdrop-blur-md z-20">
      <div className="flex-1 max-w-md relative group">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted group-focus-within:text-primary transition-colors duration-300" />
        <input 
          type="text" 
          placeholder="Search projects, deployments..." 
          className="w-full bg-surface/30 border border-white/5 focus:border-primary/50 focus:bg-surface/60 rounded-full py-2 pl-10 pr-4 text-sm text-text-main placeholder:text-text-muted outline-none transition-all duration-300 shadow-inner"
        />
      </div>
      
      <div className="flex items-center gap-4">
        <button className="relative p-2 text-text-muted hover:text-white transition-colors duration-200 rounded-full hover:bg-surface/50">
          <Bell className="w-5 h-5" />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-blue-500 rounded-full shadow-[0_0_8px_rgba(59,130,246,0.8)] animate-pulse" />
        </button>
        <div className="h-8 w-px bg-white/10 mx-1" />
        <button className="flex items-center gap-2 hover:opacity-80 transition-opacity">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center border border-white/10 shadow-lg">
            <User className="w-4 h-4 text-white" />
          </div>
        </button>
      </div>
    </header>
  );
}
