import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, MessageSquare, User, Clock, Hash, Calendar, ExternalLink, ArrowLeft, ChevronDown, AlertCircle, Zap, HelpCircle, MessageCircle } from 'lucide-react';
import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const getTypeStyles = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG':
            return { bg: 'bg-red-500/10', text: 'text-red-400', border: 'border-red-500/20', gradient: 'from-red-500/20 to-red-600/10' };
        case 'FEATURE_REQUEST':
            return { bg: 'bg-purple-500/10', text: 'text-purple-400', border: 'border-purple-500/20', gradient: 'from-purple-500/20 to-purple-600/10' };
        case 'SUPPORT':
            return { bg: 'bg-green-500/10', text: 'text-green-400', border: 'border-green-500/20', gradient: 'from-green-500/20 to-green-600/10' };
        case 'QUESTION':
            return { bg: 'bg-blue-500/10', text: 'text-blue-400', border: 'border-blue-500/20', gradient: 'from-blue-500/20 to-blue-600/10' };
        default:
            return { bg: 'bg-slate-500/10', text: 'text-slate-400', border: 'border-slate-500/20', gradient: 'from-slate-500/20 to-slate-600/10' };
    }
};

const getTypeIcon = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG': return <AlertCircle size={20} />;
        case 'FEATURE_REQUEST': return <Zap size={20} />;
        case 'SUPPORT': return <MessageCircle size={20} />;
        case 'QUESTION': return <HelpCircle size={20} />;
        default: return <MessageSquare size={20} />;
    }
};

const getTypeLabel = (type) => {
    switch (type?.toUpperCase()) {
        case 'BUG': return '🐛 BUG REPORT';
        case 'FEATURE_REQUEST': return '✨ FEATURE REQUEST';
        case 'SUPPORT': return '❓ SUPPORT REQUEST';
        case 'QUESTION': return '💬 QUESTION';
        default: return type;
    }
};

const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const getTimeAgo = (timestamp) => {
    const now = new Date();
    const date = new Date(timestamp);
    const seconds = Math.floor((now - date) / 1000);

    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)} hours ago`;
    const days = Math.floor(seconds / 86400);
    return days === 1 ? '1 day ago' : `${days} days ago`;
};

export const TicketDetailModal = ({ ticket, onClose }) => {
    const [ticketDetails, setTicketDetails] = useState(null);
    const [loading, setLoading] = useState(true);
    const [showMetadata, setShowMetadata] = useState(true);

    const styles = getTypeStyles(ticket.type);

    useEffect(() => {
        const fetchTicketDetails = async () => {
            try {
                setLoading(true);
                const response = await axios.get(`${API_BASE}/tickets/${ticket.id}`);
                console.log('Ticket details:', response.data);
                setTicketDetails(response.data);
            } catch (error) {
                console.error('Error fetching ticket details:', error);
            } finally {
                setLoading(false);
            }
        };

        if (ticket?.id) {
            fetchTicketDetails();
        }
    }, [ticket?.id]);

    // Close on Escape key
    useEffect(() => {
        const handleEscape = (e) => {
            if (e.key === 'Escape') onClose();
        };
        window.addEventListener('keydown', handleEscape);
        return () => window.removeEventListener('keydown', handleEscape);
    }, [onClose]);

    // Prevent body scroll when modal is open
    useEffect(() => {
        document.body.style.overflow = 'hidden';
        return () => {
            document.body.style.overflow = 'unset';
        };
    }, []);

    return (
        <AnimatePresence>
            {ticket && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    className="fixed inset-0 bg-slate-900 z-50 overflow-hidden"
                >
                    {/* Background Gradient */}
                    <div className={`absolute inset-0 bg-gradient-to-br ${styles.gradient} opacity-30`} />

                    {/* Header */}
                    <motion.header
                        initial={{ y: -20, opacity: 0 }}
                        animate={{ y: 0, opacity: 1 }}
                        transition={{ delay: 0.1 }}
                        className="relative border-b border-slate-800 bg-slate-900/80 backdrop-blur-xl"
                    >
                        <div className="max-w-7xl mx-auto px-6 py-4">
                            <div className="flex items-center justify-between">
                                <div className="flex items-center gap-4">
                                    <button
                                        onClick={onClose}
                                        className="p-2 rounded-lg hover:bg-slate-800 transition-colors text-slate-400 hover:text-slate-200 flex items-center gap-2"
                                    >
                                        <ArrowLeft size={20} />
                                        <span className="text-sm font-medium">Back</span>
                                    </button>

                                    <div className="h-8 w-px bg-slate-700" />

                                    <div className="flex items-center gap-3">
                                        <div className={`p-2 rounded-lg ${styles.bg} border ${styles.border}`}>
                                            {getTypeIcon(ticket.type)}
                                        </div>
                                        <div>
                                            <div className="flex items-center gap-2">
                                                <span className={`text-xs font-bold uppercase tracking-wider ${styles.text}`}>
                                                    {getTypeLabel(ticket.type)}
                                                </span>
                                                <span className="text-xs text-slate-500 font-mono">
                                                    #{ticket.id.substring(0, 8)}
                                                </span>
                                            </div>
                                            <p className="text-xs text-slate-500">
                                                Created {getTimeAgo(ticket.timestamp)}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                <div className="flex items-center gap-3">
                                    <button
                                        onClick={() => setShowMetadata(!showMetadata)}
                                        className="px-3 py-2 rounded-lg hover:bg-slate-800 transition-colors text-slate-400 hover:text-slate-200 flex items-center gap-2 text-sm"
                                    >
                                        <span>Details</span>
                                        <ChevronDown size={16} className={`transition-transform ${showMetadata ? 'rotate-180' : ''}`} />
                                    </button>

                                    <button
                                        className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg font-medium transition-colors flex items-center gap-2 text-sm shadow-lg shadow-indigo-500/20"
                                    >
                                        <ExternalLink size={16} />
                                        Open in Slack
                                    </button>
                                </div>
                            </div>
                        </div>

                        {/* Expandable Metadata */}
                        <AnimatePresence>
                            {showMetadata && (
                                <motion.div
                                    initial={{ height: 0, opacity: 0 }}
                                    animate={{ height: 'auto', opacity: 1 }}
                                    exit={{ height: 0, opacity: 0 }}
                                    transition={{ duration: 0.2 }}
                                    className="overflow-hidden border-t border-slate-800 bg-slate-800/50"
                                >
                                    <div className="max-w-7xl mx-auto px-6 py-4">
                                        <div className="grid grid-cols-4 gap-6">
                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-lg bg-slate-700/50">
                                                    <Calendar size={16} className="text-slate-400" />
                                                </div>
                                                <div>
                                                    <p className="text-xs text-slate-500 uppercase font-medium">Created</p>
                                                    <p className="text-sm text-slate-300">{formatDate(ticket.timestamp)}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-lg bg-slate-700/50">
                                                    <Clock size={16} className="text-slate-400" />
                                                </div>
                                                <div>
                                                    <p className="text-xs text-slate-500 uppercase font-medium">Last Updated</p>
                                                    <p className="text-sm text-slate-300">{getTimeAgo(ticket.timestamp)}</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-lg bg-slate-700/50">
                                                    <MessageSquare size={16} className="text-slate-400" />
                                                </div>
                                                <div>
                                                    <p className="text-xs text-slate-500 uppercase font-medium">Messages</p>
                                                    <p className="text-sm text-slate-300">{ticket.messageCount || 0} messages</p>
                                                </div>
                                            </div>

                                            <div className="flex items-center gap-3">
                                                <div className="p-2 rounded-lg bg-slate-700/50">
                                                    <User size={16} className="text-slate-400" />
                                                </div>
                                                <div>
                                                    <p className="text-xs text-slate-500 uppercase font-medium">Reporter</p>
                                                    <p className="text-sm text-slate-300">{ticket.customerName || 'Customer'}</p>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </motion.header>

                    {/* Main Content */}
                    <main className="relative overflow-y-auto" style={{ height: showMetadata ? 'calc(100vh - 90px)' : 'calc(100vh - 80px)' }}>
                        <div className="max-w-7xl mx-auto px-6 py-8">
                            <div className="grid grid-cols-5 gap-8 min-h-full">

                                {/* Left Column - Title & Summary */}
                                <motion.div
                                    initial={{ x: -20, opacity: 0 }}
                                    animate={{ x: 0, opacity: 1 }}
                                    transition={{ delay: 0.2 }}
                                    className="col-span-2 flex flex-col"
                                >
                                    <div className="bg-slate-800/30 backdrop-blur-sm border border-slate-700 rounded-xl p-6 flex-1">
                                        <h2 className="text-3xl font-bold text-slate-100 leading-tight mb-4">
                                            {ticket.title}
                                        </h2>

                                        <div className="space-y-4 text-slate-400">
                                            <div>
                                                <h3 className="text-xs uppercase font-semibold text-slate-500 mb-2">Description</h3>
                                                <p className="text-sm leading-relaxed">
                                                    This ticket contains {ticket.messageCount || 0} message{ticket.messageCount !== 1 ? 's' : ''} from the customer
                                                    regarding a {ticket.type?.toLowerCase().replace('_', ' ')} issue.
                                                </p>
                                            </div>

                                            <div>
                                                <h3 className="text-xs uppercase font-semibold text-slate-500 mb-2">Status</h3>
                                                <div className="flex items-center gap-2">
                                                    <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full text-xs font-medium">
                                                        OPEN
                                                    </span>
                                                    <span className={`px-3 py-1 ${styles.bg} ${styles.text} border ${styles.border} rounded-full text-xs font-medium`}>
                                                        {ticket.type?.replace('_', ' ')}
                                                    </span>
                                                </div>
                                            </div>

                                            <div>
                                                <h3 className="text-xs uppercase font-semibold text-slate-500 mb-2">Priority</h3>
                                                <p className="text-sm">
                                                    {ticket.type === 'BUG' ? 'High Priority' :
                                                        ticket.type === 'FEATURE_REQUEST' ? 'Medium Priority' : 'Normal Priority'}
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </motion.div>

                                {/* Right Column - Messages */}
                                <motion.div
                                    initial={{ x: 20, opacity: 0 }}
                                    animate={{ x: 0, opacity: 1 }}
                                    transition={{ delay: 0.3 }}
                                    className="col-span-3 flex flex-col"
                                >
                                    <div className="bg-slate-800/30 backdrop-blur-sm border border-slate-700 rounded-xl flex flex-col h-full overflow-hidden">
                                        {/* Messages Header */}
                                        <div className="border-b border-slate-700 p-4">
                                            <div className="flex items-center justify-between">
                                                <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-2">
                                                    <MessageSquare size={16} />
                                                    Conversation Timeline
                                                </h3>
                                                <span className="text-xs text-slate-500">
                                                    {ticketDetails?.messages?.length || 0} messages
                                                </span>
                                            </div>
                                        </div>

                                        {/* Messages List - Scrollable */}
                                        <div className="flex-1 overflow-y-auto p-6 space-y-4">
                                            {loading ? (
                                                <div className="flex items-center justify-center h-full">
                                                    <div className="text-center">
                                                        <div className="w-16 h-16 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin mx-auto mb-4" />
                                                        <p className="text-slate-400 font-medium">Loading conversation...</p>
                                                        <p className="text-slate-600 text-sm mt-1">Please wait</p>
                                                    </div>
                                                </div>
                                            ) : ticketDetails?.messages && ticketDetails.messages.length > 0 ? (
                                                [...ticketDetails.messages].reverse().map((message, idx) => (
                                                    <MessageBubble
                                                        key={message.id || idx}
                                                        message={message}
                                                        isFirst={idx === ticketDetails.messages.length - 1}
                                                        isLast={idx === 0}
                                                    />
                                                ))
                                            ) : (
                                                <div className="flex items-center justify-center h-full">
                                                    <div className="text-center">
                                                        <div className="w-20 h-20 bg-slate-800 rounded-full flex items-center justify-center mx-auto mb-4">
                                                            <MessageSquare className="w-10 h-10 text-slate-600" />
                                                        </div>
                                                        <p className="text-slate-500 font-medium text-lg">No messages yet</p>
                                                        <p className="text-slate-600 text-sm mt-2">Messages will appear here once available</p>
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </motion.div>

                            </div>
                        </div>
                    </main>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

const MessageBubble = ({ message, isFirst, isLast }) => {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="relative"
        >
            {/* Timeline connector */}
            {!isLast && (
                <div className="absolute left-5 top-14 bottom-0 w-px bg-slate-700" />
            )}

            <div className="flex gap-4">
                {/* Avatar with timeline dot */}
                <div className="relative flex-shrink-0">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-600/30 to-purple-600/30 border-2 border-slate-700 flex items-center justify-center relative z-10 bg-slate-900">
                        <User size={18} className="text-indigo-400" />
                    </div>
                </div>

                {/* Message Content */}
                <div className="flex-1 min-w-0">
                    <div className={`bg-slate-800/50 border rounded-xl p-4 ${isFirst ? 'border-indigo-500/30 shadow-lg shadow-indigo-500/10' :
                            isLast ? 'border-emerald-500/30 shadow-lg shadow-emerald-500/10' :
                                'border-slate-700/50'
                        }`}>
                        {/* Header */}
                        <div className="flex items-start justify-between mb-3">
                            <div>
                                <p className="text-sm font-semibold text-slate-200">
                                    {message.user || message.slackUser || 'Customer'}
                                </p>
                                <p className="text-xs text-slate-500 mt-0.5">
                                    {formatDate(message.slackMessageTime || message.createdAt)}
                                </p>
                            </div>
                            <div className="flex items-center gap-1.5 text-xs text-slate-500 bg-slate-800/50 px-2 py-1 rounded border border-slate-700/50">
                                {message.channel}
                            </div>
                        </div>

                        {/* Message Text */}
                        <div className="text-slate-300 leading-relaxed text-sm">
                            {message.text || message.slackText}
                        </div>

                        {/* Thread & Badge */}
                        <div className="flex items-center gap-2 mt-3 pt-3 border-t border-slate-700/50">
                            {message.threadTs && (
                                <span className="flex items-center gap-1.5 text-xs text-indigo-400">
                                    <MessageSquare size={12} />
                                    Thread reply
                                </span>
                            )}

                            {isFirst && (
                                <span className="ml-auto px-2 py-0.5 bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 rounded text-xs font-medium">
                                    First message
                                </span>
                            )}

                            {isLast && (
                                <span className="ml-auto px-2 py-0.5 bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 rounded text-xs font-medium">
                                    Latest message
                                </span>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </motion.div>
    );
};