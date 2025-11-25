import React, { useState } from 'react';
import { useWebSocket } from './Hooks/useWebSocket';
import { TicketCard } from './Components/TicketCard';
import { Activity, Wifi, WifiOff, CheckCheck, Loader2, X } from 'lucide-react';
import { TicketDetailModal } from './Components/TicketDetailModal';

function App() {
  const { tickets, isConnected, isLoading } = useWebSocket();
  const [filter, setFilter] = useState("ALL");
  const [dismissedTickets, setDismissedTickets] = useState(new Set());
  const [selectedTicket, setSelectedTicket] = useState(null);

  // Apply filter
  const filteredTickets =
    filter === "ALL"
      ? tickets
      : tickets.filter(t => t.type === filter);

  // Stats
  const activeBugs = tickets.filter(t => t.type === 'BUG').length;
  const features = tickets.filter(t => t.type === 'FEATURE_REQUEST').length;
  const support = tickets.filter(t => t.type === 'SUPPORT').length;
  const questions = tickets.filter(t => t.type === 'QUESTION').length;

  // Unread count
  const unreadCount = tickets.filter(t =>
    (t.isNew || t.isUpdated) && !dismissedTickets.has(t.id)
  ).length;

  const handleDismissAll = () => {
    const allTicketIds = tickets.map(t => t.id);
    setDismissedTickets(new Set(allTicketIds));
  };

  const clearFilter = () => setFilter("ALL");

  return (
    <div className="min-h-screen bg-slate-900 text-slate-200 font-sans selection:bg-indigo-500/30">

      {/* Header */}
      <header className="fixed top-0 w-full z-50 bg-slate-900/80 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-5xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="bp-1.5 rounded-md">
              {/* <Activity className="text-white" size={20} /> */}
              <img
                src="/nixo.png"
                alt="Nixo Logo"
                className="h-8 w-auto object-contain rounded-md"
              />
            </div>
            <h1 className="font-bold text-xl tracking-tight text-white">
              Nixo <span className="text-slate-500 font-normal">FDE Dashboard</span>
            </h1>
          </div>

          <div className="flex items-center gap-3">
            {/* Dismiss All Button */}
            {!isLoading && unreadCount > 0 && (
              <button
                onClick={handleDismissAll}
                className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 hover:bg-indigo-500/20 transition-colors"
              >
                <CheckCheck size={14} />
                Dismiss All ({unreadCount})
              </button>
            )}

            {/* Connection Status */}
            <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium border ${isConnected
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
              : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
              }`}>
              {isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}
              {isConnected ? 'SYSTEM ONLINE' : 'DISCONNECTED'}
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="pt-24 pb-12 max-w-5xl mx-auto px-6">

        {/* Stats Row */}
        <div className="grid grid-cols-4 gap-4 mb-8">
          <StatsCard
            label="Active Conversations"
            value={isLoading ? '-' : tickets.length}
            isLoading={isLoading}
            isActive={filter === "ALL"}
            onClick={() => setFilter("ALL")}
          />
          <StatsCard
            label="Potential Bugs"
            value={isLoading ? '-' : activeBugs}
            type="bug"
            isLoading={isLoading}
            isActive={filter === "BUG"}
            onClick={() => setFilter("BUG")}
          />
          <StatsCard
            label="Feature Requests"
            value={isLoading ? '-' : features}
            type="feature"
            isLoading={isLoading}
            isActive={filter === "FEATURE_REQUEST"}
            onClick={() => setFilter("FEATURE_REQUEST")}
          />
          <StatsCard
            label="Support & Questions"
            value={isLoading ? '-' : (support + questions)}
            type="support"
            isLoading={isLoading}
            isActive={filter === "SUPPORT" || filter === "QUESTION"}
            onClick={() => setFilter("SUPPORT")}
          />
        </div>

        {/* Filter Buttons */}
        <div className="flex items-center gap-2 mb-4">
          {[
            { label: "All", value: "ALL", count: tickets.length },
            { label: "🐛 Bugs", value: "BUG", count: activeBugs },
            { label: "✨ Features", value: "FEATURE_REQUEST", count: features },
            { label: "❓ Support", value: "SUPPORT", count: support },
            { label: "💬 Questions", value: "QUESTION", count: questions },
          ].map(btn => (
            <button
              key={btn.value}
              onClick={() => setFilter(btn.value)}
              disabled={isLoading}
              className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-all disabled:opacity-50 disabled:cursor-not-allowed
                ${filter === btn.value
                  ? "bg-indigo-600/20 text-indigo-300 border-indigo-600/40 shadow-lg shadow-indigo-500/10"
                  : "bg-slate-800 text-slate-400 border-slate-700 hover:bg-slate-700/50 hover:text-slate-300"}
              `}
            >
              {btn.label}
              {!isLoading && btn.count > 0 && (
                <span className={`ml-1.5 px-1.5 py-0.5 rounded text-[10px] font-bold ${filter === btn.value
                  ? 'bg-indigo-500 text-white'
                  : 'bg-slate-700 text-slate-400'
                  }`}>
                  {btn.count}
                </span>
              )}
            </button>
          ))}

          {/* Clear Filter */}
          {filter !== "ALL" && !isLoading && (
            <button
              onClick={clearFilter}
              className="ml-auto px-3 py-1.5 rounded-md text-sm font-medium bg-slate-800 text-slate-400 border border-slate-700 hover:bg-slate-700/50 transition-colors flex items-center gap-1.5"
            >
              <X size={14} />
              Clear Filter
            </button>
          )}
        </div>

        {/* Filter Banner */}
        {!isLoading && (
          <div className="mb-6">
            <div className="px-3 py-2 rounded-md bg-slate-800/60 border border-slate-700 text-xs text-slate-400 flex items-center justify-between">
              <span>
                {filter === "ALL" ? (
                  <>Showing <span className="text-slate-300 font-medium">all {tickets.length} tickets</span></>
                ) : (
                  <>Filtered by <span className="text-indigo-300 font-medium">{filter.replace("_", " ")}</span> • <span className="text-slate-300 font-medium">{filteredTickets.length} results</span></>
                )}
              </span>
              {unreadCount > 0 && (
                <span className="px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 text-[10px] font-bold">
                  {unreadCount} UNREAD
                </span>
              )}
            </div>
          </div>
        )}

        {/* Tickets Feed */}
        <div className="space-y-2">
          <h2 className="text-slate-400 text-sm uppercase font-semibold tracking-wider mb-4 flex items-center gap-2">
            Current Updates
            {filter !== "ALL" && !isLoading && (
              <span className="text-indigo-400 bg-indigo-400/10 border border-indigo-400/20 px-2 py-0.5 rounded text-[10px] font-medium">
                FILTER ACTIVE
              </span>
            )}
          </h2>

          {/* Loading State */}
          {isLoading ? (
            <div className="flex flex-col items-center justify-center py-20 border-2 border-dashed border-slate-800 rounded-xl">
              <Loader2 className="w-12 h-12 text-indigo-500 animate-spin mb-4" />
              <p className="text-slate-400 text-lg font-medium">Loading tickets...</p>
              <p className="text-slate-600 text-sm mt-2">Fetching data from backend</p>
            </div>
          ) : filteredTickets.length === 0 ? (
            <div className="text-center py-20 border-2 border-dashed border-slate-800 rounded-xl">
              {tickets.length === 0 ? (
                <>
                  <div className="w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center mx-auto mb-4">
                    <Activity className="text-slate-600" size={28} />
                  </div>
                  <p className="text-slate-500 text-lg font-medium">No tickets yet</p>
                  <p className="text-slate-600 text-sm mt-2">Waiting for Slack events...</p>
                  <p className="text-slate-700 text-xs mt-1">Send a message in your Slack channel to see it here</p>
                </>
              ) : (
                <>
                  <div className="w-16 h-16 bg-slate-800 rounded-full flex items-center justify-center mx-auto mb-4">
                    <X className="text-slate-600" size={28} />
                  </div>
                  <p className="text-slate-500 text-lg font-medium">No tickets in this category</p>
                  <p className="text-slate-600 text-sm mt-2">Try selecting a different filter</p>
                  <button
                    onClick={clearFilter}
                    className="mt-4 px-4 py-2 bg-indigo-600/20 text-indigo-300 border border-indigo-600/40 rounded-md text-sm font-medium hover:bg-indigo-600/30 transition-colors"
                  >
                    Show All Tickets
                  </button>
                </>
              )}
            </div>
          ) : (
            <div className={`transition-opacity duration-200 ${filter !== "ALL" ? "opacity-95" : "opacity-100"}`}>
              {filteredTickets.map((ticket) => (
                <TicketCard
                  key={ticket.id}
                  ticket={ticket}
                  isNew={ticket.isNew && !dismissedTickets.has(ticket.id)}
                  isUpdated={ticket.isUpdated && !dismissedTickets.has(ticket.id)}
                  onViewDetails={() => setSelectedTicket(ticket)} // Pass handler
                />
              ))}
            </div>
          )}
        </div>
      </main>
      {/* Global Detail Modal */}
      {selectedTicket && (
        <TicketDetailModal
          ticket={selectedTicket}
          onClose={() => setSelectedTicket(null)}
        />
      )}
    </div>
  );
}

const StatsCard = ({ label, value, type, isLoading, isActive, onClick }) => (
  <button
    onClick={onClick}
    disabled={isLoading}
    className={`bg-slate-800 border p-4 rounded-lg text-left transition-all hover:border-indigo-600/40 disabled:cursor-not-allowed ${isActive
      ? 'border-indigo-600/40 shadow-lg shadow-indigo-500/10'
      : 'border-slate-700'
      }`}
  >
    <p className="text-slate-400 text-xs uppercase font-medium mb-1">{label}</p>
    {isLoading ? (
      <div className="flex items-center gap-2">
        <Loader2 className="w-5 h-5 text-slate-600 animate-spin" />
        <div className="h-8 w-12 bg-slate-700/50 rounded animate-pulse" />
      </div>
    ) : (
      <p className="text-3xl font-bold text-white">{value}</p>
    )}
  </button>
);

export default App;