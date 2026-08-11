import AppLayout from "../app/layouts/AppLayout";
import StatCard from "../features/dashboard/components/StatCard";
import ProjectCard from "../features/dashboard/components/ProjectCard";
import ActivityFeed from "../features/dashboard/components/ActivityFeed";
import CreateProjectModal from "../features/dashboard/components/CreateProjectModal";
import { Server, Rocket, Activity, Cpu, GitBranch, Plus } from "lucide-react";
import { useState } from "react";
import { useProjects } from "../hooks/useProjects";
import { useAllProjectDeployments } from "../hooks/useDeployments";
import { useGithubProfile, useGithubLoginUrl } from "../hooks/useGitHub";

export default function Dashboard() {
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const { projects, isLoading: projectsLoading } = useProjects();
  const { data: allDeployments, isLoading: deploymentsLoading } = useAllProjectDeployments(projects);
  const { data: githubProfile, isLoading: githubLoading } = useGithubProfile();
  const { data: loginUrlResponse } = useGithubLoginUrl();

  const githubConnected = !!githubProfile?.data;

  const activeDeploymentsCount = allDeployments.filter(d => d.status === 'RUNNING').length;
  
  const completedBuilds = allDeployments.filter(d => d.durationMs != null && d.durationMs > 0);
  const totalBuildTimeMs = completedBuilds.reduce((acc, d) => acc + d.durationMs, 0);
  const avgBuildTimeMs = completedBuilds.length > 0 ? (totalBuildTimeMs / completedBuilds.length) : 0;
  
  const formatBuildTime = (ms) => {
    if (!ms) return "0s";
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes > 0) return `${minutes}m ${seconds}s`;
    return `${seconds}s`;
  };

  const sortedDeployments = [...allDeployments].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  const recentActivities = sortedDeployments.slice(0, 5).map(dep => {
      const projectName = projects.find(p => p.uuid === dep.projectUuid)?.name || 'Unknown Project';
      
      let mappedStatus = 'building';
      if (dep.status === 'RUNNING') mappedStatus = 'success';
      if (dep.status === 'FAILED') mappedStatus = 'failed';
      if (dep.status === 'STOPPED') mappedStatus = 'failed';

      return {
          id: dep.id,
          title: `Deployment ${dep.status}`,
          description: `${projectName} deployment on ${dep.branch || 'main'} branch.`,
          time: new Date(dep.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }),
          status: mappedStatus
      };
  });

  return (
    <AppLayout>
      <div className="mb-8 animate-in fade-in slide-in-from-bottom-4 duration-700 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-bold text-white mb-2 tracking-tight">Overview</h1>
          <p className="text-text-muted text-sm font-medium">Welcome back, Soundar. Here is what's happening with your apps today.</p>
        </div>
        <button 
          onClick={() => setIsCreateModalOpen(true)}
          className="flex items-center gap-2 bg-primary hover:bg-primary-hover text-white px-4 py-2 rounded-lg font-medium transition-colors shadow-lg shadow-primary/20"
        >
          <Plus className="w-4 h-4" /> New Project
        </button>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-6 mb-10 animate-in fade-in slide-in-from-bottom-6 duration-700 delay-100 fill-mode-both">
        <StatCard title="Total Projects" value={projectsLoading ? "-" : projects.length} icon={Server} trend="up" trendValue="" colorClass="from-blue-500 to-cyan-500" />
        <StatCard title="Active Deployments" value={deploymentsLoading ? "-" : activeDeploymentsCount} icon={Rocket} trend="up" trendValue="" colorClass="from-purple-500 to-pink-500" />
        <StatCard title="Avg. Build Time" value={deploymentsLoading ? "-" : formatBuildTime(avgBuildTimeMs)} icon={Activity} trend="down" trendValue="" colorClass="from-emerald-500 to-teal-500" />
        <StatCard title="Cluster Health" value="98.9%" icon={Cpu} colorClass="from-amber-500 to-orange-500" />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 md:gap-8 pb-10">
        {/* Projects Grid */}
        <div className="xl:col-span-2 space-y-6 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-200 fill-mode-both">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Recent Projects</h2>
            <div className="flex items-center gap-3">
              {!githubLoading && !githubConnected && loginUrlResponse?.data && (
                <a 
                  href={loginUrlResponse.data.authorizationUrl} 
                  className="text-sm font-medium text-white transition-colors bg-[#24292e]/80 hover:bg-[#24292e] px-3 py-1.5 rounded-lg border border-white/10 hover:border-white/20 flex items-center gap-2"
                >
                  <GitBranch className="w-4 h-4" /> Connect GitHub
                </a>
              )}
              <button className="text-sm font-medium text-primary hover:text-white transition-colors bg-primary/10 px-3 py-1.5 rounded-lg border border-primary/20 hover:border-primary/50 hover:bg-primary/20">
                View All Projects
              </button>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6">
            {projectsLoading ? (
               <div className="text-text-muted col-span-2">Loading projects...</div>
            ) : projects.length === 0 ? (
               <div className="text-text-muted col-span-2">No projects found. Create one to get started.</div>
            ) : (
              projects.map(project => (
                <ProjectCard key={project.id || project.uuid} project={project} />
              ))
            )}
          </div>
        </div>

        {/* Activity Feed */}
        <div className="animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300 fill-mode-both">
          {deploymentsLoading ? (
             <div className="text-text-muted">Loading activities...</div>
          ) : recentActivities.length === 0 ? (
             <div className="text-text-muted">No recent activity found.</div>
          ) : (
            <ActivityFeed activities={recentActivities} />
          )}
        </div>
      </div>
      
      <CreateProjectModal 
        isOpen={isCreateModalOpen} 
        onClose={() => setIsCreateModalOpen(false)} 
      />
    </AppLayout>
  );
}
