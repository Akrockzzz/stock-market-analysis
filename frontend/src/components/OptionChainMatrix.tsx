'use client';

import React from 'react';
import { OptionChainData } from '@/types';
import { Target, TrendingUp, AlertTriangle } from 'lucide-react';

interface Props {
  data: OptionChainData | null;
}

export default function OptionChainMatrix({ data }: Props) {
  if (!data) {
    return (
      <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-8 text-center font-mono text-slate-400">
        No Option Chain data loaded.
      </div>
    );
  }

  const getPcrColor = (pcr: number) => {
    if (pcr > 1.2) return 'text-emerald-400 border-emerald-800 bg-emerald-950/60';
    if (pcr < 0.7) return 'text-rose-400 border-rose-800 bg-rose-950/60';
    return 'text-amber-400 border-amber-800 bg-amber-950/60';
  };

  const maxCallOi = Math.max(...data.strikes.map((s) => s.callOi || 1));
  const maxPutOi = Math.max(...data.strikes.map((s) => s.putOi || 1));

  return (
    <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-xl font-mono">
      {/* Option Analytics Header */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">SPOT PRICE</span>
          <span className="text-xl font-bold text-slate-100">
            ₹{data.spotPrice.toLocaleString('en-IN')}
          </span>
          <span className="text-[10px] text-slate-500 block mt-1">ATM: {data.atmStrike}</span>
        </div>

        <div className={`p-3.5 rounded-lg border ${getPcrColor(data.putCallRatio)}`}>
          <span className="text-xs text-slate-400 block mb-1">PUT-CALL RATIO (PCR)</span>
          <div className="flex items-center justify-between">
            <span className="text-xl font-bold">{data.putCallRatio}</span>
            <TrendingUp className="w-5 h-5 opacity-80" />
          </div>
          <span className="text-[10px] opacity-80 block mt-1">
            {data.putCallRatio > 1.2 ? 'Bullish Sentiment' : data.putCallRatio < 0.7 ? 'Bearish Sentiment' : 'Neutral Zone'}
          </span>
        </div>

        <div className="bg-purple-950/40 p-3.5 rounded-lg border border-purple-800/60 text-purple-200">
          <span className="text-xs text-purple-400 block mb-1">MAX PAIN STRIKE</span>
          <div className="flex items-center justify-between">
            <span className="text-xl font-bold text-purple-300">₹{data.maxPainStrike}</span>
            <Target className="w-5 h-5 text-purple-400" />
          </div>
          <span className="text-[10px] text-purple-400 block mt-1">Option Seller Expiry Target</span>
        </div>

        <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800">
          <span className="text-xs text-slate-400 block mb-1">TOTAL OPEN INTEREST</span>
          <div className="text-xs flex justify-between mt-1 text-slate-300">
            <span>CE: <strong className="text-rose-400">{(data.totalCallOi / 100000).toFixed(2)}L</strong></span>
            <span>PE: <strong className="text-emerald-400">{(data.totalPutOi / 100000).toFixed(2)}L</strong></span>
          </div>
        </div>
      </div>

      {/* Option Strike Matrix Table */}
      <div className="overflow-x-auto rounded-lg border border-slate-800">
        <table className="w-full text-xs text-left text-slate-300">
          <thead className="text-slate-400 bg-slate-950 border-b border-slate-800 uppercase tracking-wider text-[11px]">
            <tr>
              <th className="py-3 px-3 text-right text-rose-400">Call OI</th>
              <th className="py-3 px-3 text-right text-rose-400">Call IV</th>
              <th className="py-3 px-3 text-right text-rose-400">Call LTP</th>
              <th className="py-3 px-4 text-center font-bold text-slate-200 bg-slate-900 border-x border-slate-800">Strike Price</th>
              <th className="py-3 px-3 text-left text-emerald-400">Put LTP</th>
              <th className="py-3 px-3 text-left text-emerald-400">Put IV</th>
              <th className="py-3 px-3 text-left text-emerald-400">Put OI</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {data.strikes.map((s) => {
              const isAtm = s.strikePrice === data.atmStrike;
              const isMaxPain = s.strikePrice === data.maxPainStrike;

              const callOiBarWidth = Math.min(100, (s.callOi / maxCallOi) * 100);
              const putOiBarWidth = Math.min(100, (s.putOi / maxPutOi) * 100);

              return (
                <tr
                  key={s.strikePrice}
                  className={`hover:bg-slate-850 transition-colors ${
                    isAtm ? 'bg-blue-950/40 border-y border-blue-600/40' : ''
                  }`}
                >
                  {/* Call OI Bar */}
                  <td className="py-2.5 px-3 text-right relative">
                    <div
                      className="absolute right-0 top-1 bottom-1 bg-rose-500/20 rounded-l"
                      style={{ width: `${callOiBarWidth}%` }}
                    />
                    <span className="relative z-10 text-rose-300 font-semibold">
                      {(s.callOi / 1000).toFixed(1)}k
                    </span>
                  </td>
                  <td className="py-2.5 px-3 text-right text-slate-400">{s.callIv?.toFixed(1)}%</td>
                  <td className="py-2.5 px-3 text-right text-rose-300">₹{s.callLtp?.toFixed(1)}</td>

                  {/* Strike Price */}
                  <td className={`py-2.5 px-4 text-center font-bold border-x border-slate-800 ${
                    isAtm ? 'text-blue-400 bg-blue-950/80 font-black' : isMaxPain ? 'text-purple-300 bg-purple-950/50' : 'text-slate-100 bg-slate-950/50'
                  }`}>
                    {s.strikePrice}
                    {isAtm && <span className="block text-[9px] text-blue-400">ATM</span>}
                    {isMaxPain && !isAtm && <span className="block text-[9px] text-purple-400">MAX PAIN</span>}
                  </td>

                  <td className="py-2.5 px-3 text-left text-emerald-300">₹{s.putLtp?.toFixed(1)}</td>
                  <td className="py-2.5 px-3 text-left text-slate-400">{s.putIv?.toFixed(1)}%</td>

                  {/* Put OI Bar */}
                  <td className="py-2.5 px-3 text-left relative">
                    <div
                      className="absolute left-0 top-1 bottom-1 bg-emerald-500/20 rounded-r"
                      style={{ width: `${putOiBarWidth}%` }}
                    />
                    <span className="relative z-10 text-emerald-300 font-semibold">
                      {(s.putOi / 1000).toFixed(1)}k
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
