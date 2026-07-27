import AppLayout from "../app/layouts/AppLayout";
import StatCard from "../features/dashboard/components/StatCard";
import ProjectCard from "../features/dashboard/components/ProjectCard";
import ActivityFeed from "../features/dashboard/components/ActivityFeed";
import { Server, Rocket, Activity, Cpu } from "lucide-react";

// Dummy Data for Preview
const MOCK_PROJECTS = [
  { id: 1, name: "Nimbus API", subdomain: "api", status: "ACTIVE", branch: "main", updatedAt: "2m ago" },
  { id: 2, name: "Web Client", subdomain: "web", status: "BUILDING", branch: "feat/dashboard", updatedAt: "Just now" },
  { id: 3, name: "Auth Service", subdomain: "auth", status: "FAILED", branch: "main", updatedAt: "1h ago" },
  { id: 4, name: "Data Pipeline", subdomain: "data", status: "ACTIVE", branch: "develop", updatedAt: "3d ago" },
];

const MOCK_ACTIVITIES = [
  { id: 1, title: "Deployment Started", description: "Web Client is building on feat/dashboard. Extracting base layers.", time: "Just now", status: "building" },
  { id: 2, title: "Deployment Successful", description: "Nimbus API deployed successfully to production namespace.", time: "2m ago", status: "success" },
  { id: 3, title: "Deployment Failed", description: "Auth Service failed during container build phase. Exit code 1.", time: "1h ago", status: "failed" },
];

export default function Dashboard() {
  return (
    <AppLayout>
      <div className="mb-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
        <h1 className="text-2xl md:text-3xl font-bold text-white mb-2 tracking-tight">Overview</h1>
        <p className="text-text-muted text-sm font-medium">Welcome back, Soundar. Here is what's happening with your apps today.</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 mb-10 animate-in fade-in slide-in-from-bottom-6 duration-700 delay-100 fill-mode-both">
        <StatCard title="Total Projects" value="12" icon={Server} trend="up" trendValue="2" colorClass="from-blue-500 to-cyan-500" />
        <StatCard title="Active Deployments" value="34" icon={Rocket} trend="up" trendValue="12%" colorClass="from-purple-500 to-pink-500" />
        <StatCard title="Avg. Build Time" value="1m 24s" icon={Activity} trend="down" trendValue="14s" colorClass="from-emerald-500 to-teal-500" />
        <StatCard title="Cluster Health" value="98.9%" icon={Cpu} colorClass="from-amber-500 to-orange-500" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 md:gap-8 pb-10">
        {/* Projects Grid */}
        <div className="xl:col-span-2 space-y-6 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200 fill-mode-both">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Recent Projects</h2>
            <button className="text-sm font-medium text-primary hover:text-white transition-colors bg-primary/10 px-3 py-1.5 rounded-lg border border-primary/20 hover:border-primary/50 hover:bg-primary/20">
              View All Projects
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
            {MOCK_PROJECTS.map(project => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        </div>

        {/* Activity Feed */}
        <div className="animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300 fill-mode-both">
          <ActivityFeed activities={MOCK_ACTIVITIES} />
        </div>
      </div>
    </AppLayout>
  );
}
