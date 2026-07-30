import Sidebar from "../../components/ui/Sidebar";
import Topbar from "../../components/ui/Topbar";

export default function AppLayout({ children }) {
  return (
    <div className="flex h-screen bg-background overflow-hidden text-text-main font-sans selection:bg-primary/30">
      <Sidebar />
      <div className="flex-1 flex flex-col h-full overflow-hidden relative z-0">
        {/* Subtle background glow */}
        <div className="absolute top-0 left-0 right-0 h-[500px] bg-primary/5 rounded-full blur-[120px] -z-10 pointer-events-none" />
        <Topbar />
        <main className="flex-1 overflow-y-auto p-6 md:p-8 z-10 scrollbar-hide">
          <div className="max-w-7xl mx-auto h-full">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
