import { CheckCircle2, XCircle, Loader2 } from "lucide-react";

export default function ActivityFeed({ activities }) {
  return (
    <div className="bg-surface/30 backdrop-blur-md border border-white/5 rounded-2xl p-6 relative overflow-hidden">
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-purple-500/5 rounded-full blur-[100px] pointer-events-none" />
      
      <h3 className="text-lg font-semibold text-white mb-6 flex items-center gap-2 relative z-10">
        Recent Activity
        <span className="flex h-2 w-2 rounded-full bg-primary animate-pulse shadow-[0_0_8px_rgba(59,130,246,0.8)]" />
      </h3>
      
      <div className="space-y-6 relative before:absolute before:inset-0 before:ml-4 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-[1px] before:bg-gradient-to-b before:from-white/10 before:to-transparent z-10">
        {activities.map((activity) => (
          <div key={activity.id} className="relative flex items-center justify-between md:justify-normal md:odd:flex-row-reverse group">
            <div className="flex items-center justify-center w-8 h-8 rounded-full border border-white/10 bg-surface shrink-0 md:order-1 md:group-odd:-translate-x-1/2 md:group-even:translate-x-1/2 shadow-lg relative z-10">
              {activity.status === 'success' && <CheckCircle2 className="w-4 h-4 text-emerald-400" />}
              {activity.status === 'failed' && <XCircle className="w-4 h-4 text-rose-400" />}
              {activity.status === 'building' && <Loader2 className="w-4 h-4 text-blue-400 animate-spin" />}
            </div>
            
            <div className="w-[calc(100%-4rem)] md:w-[calc(50%-2.5rem)] p-4 rounded-xl border border-white/5 bg-white/[0.02] backdrop-blur-sm group-hover:bg-white/5 group-hover:border-white/10 transition-all duration-300 shadow-sm">
              <div className="flex items-center justify-between mb-1.5">
                <h4 className="text-sm font-semibold text-white tracking-wide">{activity.title}</h4>
                <time className="text-[11px] font-medium text-text-muted bg-white/5 px-2 py-0.5 rounded-md">{activity.time}</time>
              </div>
              <p className="text-xs text-text-muted leading-relaxed">{activity.description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
