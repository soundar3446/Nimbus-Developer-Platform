import { GitBranch, Globe, MoreVertical, Activity } from "lucide-react";

export default function ProjectCard({ project }) {
  const statusColors = {
    ACTIVE: "bg-emerald-500/20 text-emerald-400 border-emerald-500/30",
    BUILDING: "bg-blue-500/20 text-blue-400 border-blue-500/30 animate-pulse",
    FAILED: "bg-rose-500/20 text-rose-400 border-rose-500/30",
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
          <button className="text-text-muted hover:text-white p-1 rounded-md hover:bg-white/5 transition-colors">
            <MoreVertical className="w-4 h-4" />
          </button>
        </div>
        
        <div className="flex items-center gap-3 mb-6 relative z-10">
          <span className={`text-xs px-2.5 py-1 rounded-full border flex items-center gap-1.5 font-medium ${statusColors[project.status]}`}>
            {project.status === 'BUILDING' && <Activity className="w-3 h-3" />}
            {project.status}
          </span>
          <div className="flex items-center gap-1.5 text-xs text-text-muted bg-white/5 px-2.5 py-1 rounded-full border border-white/5">
            <GitBranch className="w-3 h-3" />
            <span className="truncate max-w-[100px]">{project.branch}</span>
          </div>
        </div>
      </div>
      
      <div className="pt-4 border-t border-white/5 flex items-center justify-between relative z-10">
        <span className="text-xs text-text-muted">Updated {project.updatedAt}</span>
        <button className="text-xs font-medium text-white hover:text-primary transition-colors flex items-center gap-1.5 group/btn">
          View App <Globe className="w-3 h-3 group-hover/btn:scale-110 transition-transform" />
        </button>
      </div>
    </div>
  );
}
