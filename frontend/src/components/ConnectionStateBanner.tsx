'use client';

import React from 'react';
import { ConnectionStatus } from '@/types';
import { Wifi, WifiOff, Clock, ShieldAlert, RefreshCw } from 'lucide-react';

interface Props {
  status: ConnectionStatus | null;
}

export default function ConnectionStateBanner({ status }: Props) {
  const state = status?.state || 'NOT_CONNECTED';
  const message = status?.message || 'Disconnected from backend websocket';

  const getBadgeStyle = () => {
    if (!status) {
      return {
        container: 'bg-slate-900/60 border-slate-700/40 text-slate-400',
        icon: <RefreshCw className="w-4 h-4 text-slate-400 animate-spin" />,
        title: 'CONNECTING...',
        color: 'bg-slate-600',
      };
    }
    switch (state) {
      case 'LIVE':
        return {
          container: 'bg-emerald-950/60 border-emerald-500/40 text-emerald-300',
          icon: <Wifi className="w-4 h-4 text-emerald-400 animate-pulse" />,
          title: 'LIVE MARKET FEED',
          color: 'bg-emerald-500',
        };
      case 'HISTORICAL_ONLY':
        return {
          container: 'bg-amber-950/60 border-amber-500/40 text-amber-300',
          icon: <Clock className="w-4 h-4 text-amber-400" />,
          title: 'MARKET CLOSED — EOD / HISTORICAL MODE',
          color: 'bg-amber-500',
        };
      case 'NOT_CONNECTED':
      default:
        return {
          container: 'bg-rose-950/60 border-rose-500/40 text-rose-300',
          icon: <WifiOff className="w-4 h-4 text-rose-400" />,
          title: 'NOT CONNECTED',
          color: 'bg-rose-500',
        };
    }
  };

  const style = getBadgeStyle();

  return (
    <div className={`w-full px-4 py-2.5 rounded-lg border backdrop-blur-md flex flex-wrap items-center justify-between gap-3 text-xs font-mono shadow-lg transition-all ${style.container}`}>
      <div className="flex items-center space-x-2.5">
        <div className="relative flex items-center justify-center">
          {style.icon}
          {state === 'LIVE' && (
            <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          )}
        </div>
        <span className="font-bold tracking-wider uppercase px-2 py-0.5 rounded bg-black/40 border border-white/10">
          {style.title}
        </span>
        <span className="text-slate-300 opacity-90 hidden md:inline">{message}</span>
      </div>

      <div className="flex items-center space-x-4">
        <div className="flex items-center space-x-1.5 text-slate-400">
          <ShieldAlert className="w-3.5 h-3.5 text-slate-400" />
          <span>Honest Data Guarantee: No Synthetic Fallbacks</span>
        </div>
        <div className="text-slate-400 border-l border-white/10 pl-3">
          Broker: <span className="text-slate-200 font-semibold">Upstox Developer V3 API</span>
        </div>
      </div>
    </div>
  );
}
