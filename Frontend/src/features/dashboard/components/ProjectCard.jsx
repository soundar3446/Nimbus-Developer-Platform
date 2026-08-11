import { GitBranch, Globe, MoreVertical, Activity, Rocket } from "lucide-react";
import { useState, useRef, useEffect } from "react";
import { useDeployments } from "../../../hooks/useDeployments";

export default function ProjectCard({ project }) {
  const { triggerDeployment, isTriggering } = useDeployments();
  const [showMenu, setShowMenu] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setShowMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleTrigger = (e) => {
    e.stopPropagation();
    triggerDeployment(project.uuid);
    setShowMenu(false);
  };

  const statusColors = {
    CONNECTED: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
    CREATED: "bg-blue-500/20 text-blue-400 border-blue-500/30",
    BUILDING: "bg-amber-500/20 text-amber-400 border-amber-500/30 animate-pulse",
    FAILED: "bg-rose-500/20 text-rose-400 border-rose-500/30",
    ACTIVE: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
  };

  const formatDate = (dateString) => {
    if (!dateString) return "Unknown";
    return new Date(dateString).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="bg-surface/30 backdrop-blur-md border border-white/5 rounded-2xl p-5 hover:bg-surface/50 hover:border-white/10 transition-all duration-300 group cursor-pointer relative overflow-hidden flex flex-col justify-between min-h-[160px]">
      <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full blur-[50px] group-hover:bg-primary/10 transition-all duration-500 pointer-events-none" />
      
      <div>
        <div className="flex justify-between items-start mb-4 relative z-10">
          <div>
            <h3 className="text-lg font-bold text-white group-hover:text-primary transition-colors truncate max-w-[200px]">{project.name}</h3>
            <p className="text-xs text-text-muted mt-1 max-w-[200px] truncate">{project.subdomain}.nimbus.app</p>
          </div>
          <div className="relative" ref={menuRef}>
            <button 
              onClick={(e) => { e.stopPropagation(); setShowMenu(!showMenu); }} 
              className="text-text-muted hover:text-white p-1 rounded-md hover:bg-white/5 transition-colors"
            >
              <MoreVertical className="w-4 h-4" />
            </button>
            
            {showMenu && (
              <div className="absolute right-0 mt-2 w-48 bg-surface-light border border-white/10 rounded-xl shadow-xl overflow-hidden z-50">
                <button 
                  onClick={handleTrigger} 
                  disabled={isTriggering}
                  className="w-full text-left px-4 py-2.5 text-sm text-white hover:bg-white/5 flex items-center gap-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Rocket className="w-4 h-4 text-primary" /> 
                  {isTriggering ? 'Triggering...' : 'Trigger Build'}
                </button>
              </div>
            )}
          </div>
        </div>
        
        <div className="flex items-center gap-3 mb-6 relative z-10">
          <span className={`text-xs px-2.5 py-1 rounded-full border flex items-center gap-1.5 font-medium ${statusColors[project.status] || statusColors.ACTIVE}`}>
            {project.status === 'BUILDING' && <Activity className="w-3 h-3" />}
            {project.status || 'UNKNOWN'}
          </span>
          <div className="flex items-center gap-1.5 text-xs text-text-muted bg-white/5 px-2.5 py-1 rounded-full border border-white/5">
            <GitBranch className="w-3 h-3" />
            <span className="truncate max-w-[100px]">{project.defaultBranch || project.branch || 'main'}</span>
          </div>
        </div>
      </div>
      
      <div className="pt-4 border-t border-white/5 flex items-center justify-between relative z-10">
        <span className="text-xs text-text-muted">Updated {formatDate(project.updatedAt)}</span>
        <button className="text-xs font-medium text-white hover:text-primary transition-colors flex items-center gap-1.5 group/btn">
          View App <Globe className="w-3 h-3 group-hover/btn:scale-110 transition-transform" />
        </button>
      </div>
    </div>
  );
}
