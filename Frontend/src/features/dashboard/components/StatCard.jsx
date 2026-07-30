export default function StatCard({ title, value, icon: Icon, trend, trendValue, colorClass }) {
  return (
    <div className="bg-surface/40 backdrop-blur-xl border border-white/5 rounded-2xl p-6 hover:border-white/10 transition-all duration-300 group">
      <div className="flex items-start justify-between mb-4">
        <div className={`p-3 rounded-xl bg-gradient-to-br ${colorClass} bg-opacity-10 backdrop-blur-md border border-white/5 shadow-inner`}>
          <Icon className="w-5 h-5 text-white" />
        </div>
        {trend && (
          <span className={`text-xs font-medium px-2 py-1 rounded-full ${trend === 'up' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'}`}>
            {trend === 'up' ? '+' : '-'}{trendValue}
          </span>
        )}
      </div>
      <div>
        <h3 className="text-text-muted text-sm font-medium mb-1">{title}</h3>
        <p className="text-3xl font-bold text-white tracking-tight">{value}</p>
      </div>
    </div>
  );
}
