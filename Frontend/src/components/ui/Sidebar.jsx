import { Link, useLocation } from "react-router-dom";
import { LayoutDashboard, FolderKanban, Rocket, Activity, Settings, Cloud } from "lucide-react";

const NAV_ITEMS = [
  { name: "Dashboard", path: "/dashboard", icon: LayoutDashboard },
  { name: "Projects", path: "/projects", icon: FolderKanban },
  { name: "Deployments", path: "/deployments", icon: Rocket },
  { name: "Metrics", path: "/metrics", icon: Activity },
  { name: "Settings", path: "/settings", icon: Settings },
];

export default function Sidebar() {
  const location = useLocation();

  return (
    <aside className="w-64 flex-shrink-0 border-r border-white/5 bg-surface/40 backdrop-blur-xl h-full flex flex-col transition-all duration-300">
      <div className="h-16 flex items-center px-6 border-b border-white/5">
        <Link to="/" className="flex items-center gap-3 group">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary to-blue-400 flex items-center justify-center shadow-lg shadow-primary/20 group-hover:shadow-primary/40 transition-all duration-300">
            <Cloud className="w-5 h-5 text-white" />
          </div>
          <span className="font-bold text-lg tracking-wide text-transparent bg-clip-text bg-gradient-to-r from-white to-white/70">
            Nimbus
          </span>
        </Link>
      </div>

      <nav className="flex-1 py-6 px-4 space-y-1.5 overflow-y-auto">
        {NAV_ITEMS.map((item) => {
          const isActive = location.pathname.startsWith(item.path);
          const Icon = item.icon;
          return (
            <Link
              key={item.name}
              to={item.path}
              className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group relative overflow-hidden ${
                isActive 
                  ? "text-white bg-primary/10 font-medium" 
                  : "text-text-muted hover:text-white hover:bg-surface-hover/50"
              }`}
            >
              {isActive && (
                <span className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r-full shadow-[0_0_10px_rgba(59,130,246,0.8)]" />
              )}
              <Icon className={`w-5 h-5 transition-transform duration-300 ${isActive ? "text-primary" : "group-hover:scale-110"}`} />
              {item.name}
            </Link>
          );
        })}
      </nav>
      
      <div className="p-4 border-t border-white/5">
        <div className="bg-surface-hover/30 border border-white/5 rounded-xl p-4 backdrop-blur-sm">
          <h4 className="text-sm font-semibold text-white mb-1">Nimbus Pro</h4>
          <p className="text-xs text-text-muted mb-3 leading-relaxed">Upgrade for unlimited deployments and custom domains.</p>
          <button className="w-full py-2 bg-white/5 hover:bg-primary/20 text-white text-xs font-medium rounded-lg border border-white/10 hover:border-primary/50 transition-all duration-300">
            Upgrade Now
          </button>
        </div>
      </div>
    </aside>
  );
}
