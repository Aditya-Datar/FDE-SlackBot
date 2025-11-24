import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MessageSquare, AlertCircle, Zap, HelpCircle, MessageCircle, Bell, X } from 'lucide-react';
import { TicketDetailModal } from './TicketDetailModal';

const getTypeStyles = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG':
            return 'bg-red-500/10 text-red-400 border-red-500/20';
        case 'FEATURE_REQUEST':
            return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
        case 'SUPPORT':
            return 'bg-green-500/10 text-green-400 border-green-500/20';
        case 'QUESTION':
            return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
        default:
            return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    }
};

const getIcon = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG': return <AlertCircle size={16} />;
        case 'FEATURE_REQUEST': return <Zap size={16} />;
        case 'SUPPORT': return <MessageCircle size={16} />;
        case 'QUESTION': return <HelpCircle size={16} />;
        default: return <MessageSquare size={16} />;
    }
};

const getTypeLabel = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG': return '🐛 BUG';
        case 'FEATURE_REQUEST': return '✨ FEATURE';
        case 'SUPPORT': return '❓ SUPPORT';
        case 'QUESTION': return '💬 QUESTION';
        default: return type;
    }
};

const getTimeAgo = (timestamp) => {
    const now = new Date();
    const date = new Date(timestamp);
    const seconds = Math.floor((now - date) / 1000);

    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
};

export const TicketCard = ({ ticket, isNew = false, isUpdated = false, onViewDetails }) => {
    const [showNewBadge, setShowNewBadge] = useState(isNew);
    const [showUpdatedBadge, setShowUpdatedBadge] = useState(false);
    const [prevMessageCount, setPrevMessageCount] = useState(ticket.messageCount || 0);
    const [showDetailModal, setShowDetailModal] = useState(false);

    // Handle NEW badge
    useEffect(() => {
        if (isNew) {
            setShowNewBadge(true);
        }
    }, [isNew]);

    // Handle UPDATED badge - detect message count changes
    useEffect(() => {
        const currentCount = ticket.messageCount || 0;

        if (currentCount > prevMessageCount && prevMessageCount > 0) {
            setShowUpdatedBadge(true);
            setPrevMessageCount(currentCount);
        } else if (prevMessageCount === 0) {
            setPrevMessageCount(currentCount);
        }
    }, [ticket.messageCount, prevMessageCount]);

    // Also handle isUpdated prop from parent
    useEffect(() => {
        if (isUpdated) {
            setShowUpdatedBadge(true);
        }
    }, [isUpdated]);

    const showBadge = showNewBadge || showUpdatedBadge;
    const badgeType = showNewBadge ? 'NEW' : 'UPDATED';
    const badgeColor = showNewBadge ? 'bg-emerald-500' : 'bg-blue-500';

    const handleDismissBadge = (e) => {
        e.stopPropagation();
        setShowNewBadge(false);
        setShowUpdatedBadge(false);
    };

    const handleViewDetails = (e) => {
        e.stopPropagation();
        if (onViewDetails) {
            onViewDetails(ticket);
        }
    };

    return (
        <>
            <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{
                    opacity: 1,
                    y: 0
                }}
                transition={{
                    duration: 0.2,
                    ease: "easeOut"
                }}
                className={`relative bg-slate-800/50 backdrop-blur-sm border rounded-xl p-5 mb-4 shadow-lg hover:border-slate-600 transition-colors duration-200 cursor-pointer ${showUpdatedBadge
                        ? 'border-blue-500 shadow-blue-500/20 shadow-xl'
                        : 'border-slate-700'
                    }`}
                onClick={handleViewDetails}
            >
                {/* Click-to-Dismiss Badge */}
                <AnimatePresence>
                    {showBadge && (
                        <motion.button
                            initial={{ scale: 0, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0, opacity: 0 }}
                            transition={{
                                type: "spring",
                                stiffness: 500,
                                damping: 30
                            }}
                            onClick={handleDismissBadge}
                            className={`absolute -top-2 -right-2 px-3 py-1 rounded-full text-xs font-bold flex items-center gap-1.5 ${badgeColor} text-white ${showUpdatedBadge ? 'animate-pulse' : ''
                                } hover:brightness-110 active:scale-95 transition-all group shadow-lg`}
                            title="Click to dismiss"
                        >
                            <Bell size={10} />
                            {badgeType}
                            <X size={12} className="opacity-0 group-hover:opacity-100 transition-opacity" />
                        </motion.button>
                    )}
                </AnimatePresence>

                <div className="flex justify-between items-start mb-3">
                    <div className="flex gap-3 items-center">
                        <div className={`p-2 rounded-lg border ${getTypeStyles(ticket.type)}`}>
                            {getIcon(ticket.type)}
                        </div>
                        <div>
                            <h3 className="text-slate-100 font-semibold text-lg leading-tight">
                                {ticket.title || "New Customer Issue"}
                            </h3>
                            <div className="flex items-center gap-2 mt-1">
                                <span className="text-slate-400 text-xs uppercase tracking-wider font-medium">
                                    #{ticket.id.toString().substring(0, 8)}
                                </span>
                                <span className={`text-xs px-2 py-0.5 rounded ${getTypeStyles(ticket.type)}`}>
                                    {getTypeLabel(ticket.type)}
                                </span>
                                {/* Message Count Badge - Simple fade animation */}
                                {ticket.messageCount > 0 && (
                                    <motion.span
                                        key={ticket.messageCount}
                                        initial={{ opacity: 0.5 }}
                                        animate={{ opacity: 1 }}
                                        transition={{ duration: 0.3 }}
                                        className="text-xs px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-400 border border-indigo-500/30"
                                    >
                                        {ticket.messageCount} msg{ticket.messageCount !== 1 ? 's' : ''}
                                    </motion.span>
                                )}
                            </div>
                        </div>
                    </div>
                    <span className="text-xs text-slate-500 font-mono">
                        {getTimeAgo(ticket.timestamp)}
                    </span>
                </div>

                {/* Message Preview - No shrinking animation */}
                {ticket.messages && ticket.messages.length > 0 && (
                    <div className="space-y-2 mt-4 pl-2 border-l-2 border-slate-700">
                        {ticket.messages.slice(0, 2).map((msg, idx) => (
                            <div key={idx} className="text-slate-300 text-sm py-1">
                                <span className="text-slate-500 text-xs block mb-0.5">
                                    {msg.sender || "Customer"}:
                                </span>
                                {msg.content}
                            </div>
                        ))}
                        {ticket.messages.length > 2 && (
                            <p className="text-xs text-slate-500 italic">
                                +{ticket.messages.length - 2} more messages
                            </p>
                        )}
                    </div>
                )}

                <div className="mt-4 flex justify-between items-center">
                    <span className="text-xs text-slate-500">
                        {ticket.customerName || 'Customer'}
                    </span>
                    <button className="text-xs flex items-center gap-1 text-indigo-400 hover:text-indigo-300 transition-colors group">
                        <MessageSquare size={12} className="group-hover:scale-110 transition-transform" />
                        View Details
                    </button>
                </div>
            </motion.div>
            {/* Detail Modal */}
            {showDetailModal && (
                <TicketDetailModal
                    ticket={ticket}
                    onClose={() => setShowDetailModal(false)}
                />
            )}
        </>
    );
};