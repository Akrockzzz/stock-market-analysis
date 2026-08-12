'use client';

import React from 'react';
import { Fundamentals } from '@/types';
import { ShieldCheck, ShieldAlert, Award, PieChart, Activity } from 'lucide-react';

interface Props {
  fundamentals: Fundamentals | null;
}

export default function FundamentalsPanel({ fundamentals }: Props) {
  if (!fundamentals) {
    return (
      <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-8 text-center font-mono text-slate-400">
        No Fundamentals data loaded for this symbol.
      </div>
    );
  }

  return (
    <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl font-mono text-slate-200">
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6 border-b border-slate-800 pb-4">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            <Activity className="w-5 h-5 text-emerald-400" />
            <span>Fundamental Analysis & Ratios ({fundamentals.period})</span>
          </h2>
          <p className="text-xs text-slate-400">Upstox Fundamentals API Integration</p>
        </div>

        {fundamentals.governanceFlagged ? (
          <div className="px-3 py-1.5 rounded-lg bg-rose-950/80 border border-rose-800 text-rose-300 text-xs font-bold flex items-center gap-1.5">
            <ShieldAlert className="w-4 h-4 text-rose-400" />
            <span>GOVERNANCE RED-FLAGS DETECTED</span>
          </div>
        ) : (
          <div className="px-3 py-1.5 rounded-lg bg-emerald-950/80 border border-emerald-800 text-emerald-300 text-xs font-bold flex items-center gap-1.5">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <span>CLEAN AUDIT & GOVERNANCE</span>
          </div>
        )}
      </div>

      {/* Grid of Key Ratios */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">REVENUE (5-YR CAGR)</span>
          <span className="text-lg font-bold text-slate-100">₹{fundamentals.revenueInCr} Cr</span>
          <span className="text-xs text-emerald-400 block mt-1">+{fundamentals.revenueCagr5Yr}% CAGR</span>
        </div>

        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">NET PROFIT & MARGIN</span>
          <span className="text-lg font-bold text-slate-100">₹{fundamentals.netProfitInCr} Cr</span>
          <span className="text-xs text-slate-400 block mt-1">NPM: {fundamentals.netMarginPct}%</span>
        </div>

        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">RETURN RATIOS</span>
          <span className="text-lg font-bold text-emerald-400">ROE: {fundamentals.roePct}%</span>
          <span className="text-xs text-blue-400 block mt-1">ROCE: {fundamentals.rocePct}%</span>
        </div>

        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">DEBT-TO-EQUITY</span>
          <span className={`text-lg font-bold ${fundamentals.debtToEquity < 0.5 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {fundamentals.debtToEquity}
          </span>
          <span className="text-xs text-slate-400 block mt-1">
            {fundamentals.debtToEquity < 0.5 ? 'Low Debt' : 'High Leverage'}
          </span>
        </div>
      </div>

      {/* Shareholding & Valuation Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-slate-950 p-4 rounded-lg border border-slate-800">
          <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3 flex items-center gap-1.5">
            <PieChart className="w-4 h-4 text-purple-400" />
            Shareholding Pattern & Pledge
          </h3>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">Promoter Holding</span>
              <span className="font-bold text-slate-200">{fundamentals.promoterHoldingPct}%</span>
            </div>
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">Promoter Pledge %</span>
              <span className={`font-bold ${fundamentals.promoterPledgePct === 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                {fundamentals.promoterPledgePct}% {fundamentals.promoterPledgePct === 0 && '(Zero Pledge)'}
              </span>
            </div>
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">FII Holding</span>
              <span className="font-bold text-blue-400">{fundamentals.fiiHoldingPct}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">DII Holding</span>
              <span className="font-bold text-purple-400">{fundamentals.diiHoldingPct}%</span>
            </div>
          </div>
        </div>

        <div className="bg-slate-950 p-4 rounded-lg border border-slate-800">
          <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3 flex items-center gap-1.5">
            <Award className="w-4 h-4 text-amber-400" />
            Valuation Multiples & Cash Flow
          </h3>
          <div className="space-y-2 text-xs">
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">P/E Ratio</span>
              <span className="font-bold text-slate-200">{fundamentals.peRatio}x</span>
            </div>
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">P/B Ratio</span>
              <span className="font-bold text-slate-200">{fundamentals.pbRatio}x</span>
            </div>
            <div className="flex justify-between border-b border-slate-800 pb-1">
              <span className="text-slate-400">Operating Cash Flow (OCF)</span>
              <span className="font-bold text-emerald-400">₹{fundamentals.ocfInCr} Cr</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">OCF / Net Profit Ratio</span>
              <span className={`font-bold ${fundamentals.ocfToNetProfitRatio >= 0.8 ? 'text-emerald-400' : 'text-amber-400'}`}>
                {fundamentals.ocfToNetProfitRatio}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
