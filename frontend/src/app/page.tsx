'use client';

import React, { useEffect, useState } from 'react';
import ConnectionStateBanner from '@/components/ConnectionStateBanner';
import TradingViewChart from '@/components/TradingViewChart';
import OptionChainMatrix from '@/components/OptionChainMatrix';
import FundamentalsPanel from '@/components/FundamentalsPanel';
import ScorecardWorksheet from '@/components/ScorecardWorksheet';
import { ConnectionStatus, Candle, Technicals, OptionChainData, Fundamentals, Scorecard, Tick } from '@/types';
import { LineChart, PieChart, Calculator, Layers, Search, RefreshCw } from 'lucide-react';

const POPULAR_SYMBOLS = ['RELIANCE', 'TCS', 'INFY', 'HDFCBANK', 'NIFTY'];

export default function Dashboard() {
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE');
  const [activeTab, setActiveTab] = useState<'chart' | 'option-chain' | 'fundamentals' | 'scorecard'>('chart');
  
  const [status, setStatus] = useState<ConnectionStatus | null>({
    source: 'UPSTOX_V3_FEED',
    state: 'HISTORICAL_ONLY',
    message: 'Backend API online. Displaying verified historical data.',
    lastHeartbeat: new Date().toISOString(),
  });

  const [liveTick, setLiveTick] = useState<Tick | null>(null);
  const [candles, setCandles] = useState<Candle[]>([]);
  const [technicals, setTechnicals] = useState<Technicals | null>(null);
  const [optionChain, setOptionChain] = useState<OptionChainData | null>(null);
  const [fundamentals, setFundamentals] = useState<Fundamentals | null>(null);
  const [scorecard, setScorecard] = useState<Scorecard | null>(null);
  const [loading, setLoading] = useState(false);

  // Fetch backend market data
  const loadSymbolData = async (sym: string) => {
    setLoading(true);
    try {
      // 1. Fetch Technicals & Candles
      const techRes = await fetch(`http://localhost:8080/api/analysis/technicals/${sym}`);
      if (techRes.ok) {
        const techData = await techRes.json();
        setTechnicals(techData);
      } else {
        setTechnicals(null);
      }

      const candleRes = await fetch(`http://localhost:8080/api/market/candles/${sym}`);
      if (candleRes.ok) {
        const candleData = await candleRes.json();
        setCandles(candleData);
      } else {
        setCandles([]);
      }

      // 2. Fetch Option Chain
      const optRes = await fetch(`http://localhost:8080/api/analysis/option-chain/${sym}`);
      if (optRes.ok) {
        const optData = await optRes.json();
        setOptionChain(optData);
      } else {
        setOptionChain(null);
      }

      // 3. Fetch Fundamentals
      const fundRes = await fetch(`http://localhost:8080/api/fundamentals/${sym}`);
      if (fundRes.ok) {
        const fundData = await fundRes.json();
        setFundamentals(fundData);
      } else {
        setFundamentals(null);
      }

      // 4. Fetch Scorecard
      const scoreRes = await fetch(`http://localhost:8080/api/scorecard/${sym}`);
      if (scoreRes.ok) {
        const scoreData = await scoreRes.json();
        setScorecard(scoreData);
      } else {
        setScorecard(null);
      }

      // 5. Fetch Connection Status
      const statusRes = await fetch(`http://localhost:8080/api/market/status`);
      if (statusRes.ok) {
        const statusData = await statusRes.json();
        setStatus(statusData);
      }
    } catch (err) {
      console.error('Error fetching backend data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSymbolData(selectedSymbol);
  }, [selectedSymbol]);

  const handleSaveScorecard = async (
    ratings: Record<string, number>,
    thesis: string,
    exitRules: string,
    notes: string
  ) => {
    try {
      const res = await fetch(`http://localhost:8080/api/scorecard/${selectedSymbol}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          categoryRatings: ratings,
          investmentThesis: thesis,
          exitCriteria: exitRules,
          userNotes: notes,
        }),
      });
      if (res.ok) {
        const saved = await res.json();
        setScorecard(saved);
        alert(`Scorecard saved successfully for ${selectedSymbol}! Total score: ${saved.totalScore}/50.0`);
      }
    } catch (err) {
      console.error('Failed to save scorecard:', err);
    }
  };

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 p-4 md:p-6 space-y-6 font-sans">
      {/* Top Header & Connection Banner */}
      <div className="space-y-3">
        <header className="flex flex-wrap items-center justify-between gap-4 bg-slate-900/80 border border-slate-800 p-4 rounded-xl backdrop-blur-md">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-lg bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center shadow-lg font-black text-lg">
              ₹
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white flex items-center gap-2">
                <span>Real-Time Stock Market Analysis Platform</span>
              </h1>
              <p className="text-xs text-slate-400 font-mono">
                Upstox V3 Feed • Technicals • Option Chain • 22-Category Scorecard
              </p>
            </div>
          </div>

          {/* Symbol Selector Bar */}
          <div className="flex items-center space-x-2 font-mono text-xs">
            <span className="text-slate-400 font-semibold hidden sm:inline">Watchlist:</span>
            {POPULAR_SYMBOLS.map((sym) => (
              <button
                key={sym}
                onClick={() => setSelectedSymbol(sym)}
                className={`px-3 py-1.5 rounded-lg border font-bold transition-all ${
                  selectedSymbol === sym
                    ? 'bg-blue-600 text-white border-blue-500 shadow-md'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700 hover:text-slate-200'
                }`}
              >
                {sym}
              </button>
            ))}
            <button
              onClick={() => loadSymbolData(selectedSymbol)}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
              title="Refresh Data"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>
        </header>

        <ConnectionStateBanner status={status} />
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-800 pb-2 font-mono text-xs">
        <button
          onClick={() => setActiveTab('chart')}
          className={`px-4 py-2 rounded-lg font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'chart'
              ? 'bg-blue-600 text-white shadow-lg'
              : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <LineChart className="w-4 h-4" />
          <span>Technical Chart & Indicators</span>
        </button>

        <button
          onClick={() => setActiveTab('option-chain')}
          className={`px-4 py-2 rounded-lg font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'option-chain'
              ? 'bg-purple-600 text-white shadow-lg'
              : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Option Chain & Max Pain</span>
        </button>

        <button
          onClick={() => setActiveTab('fundamentals')}
          className={`px-4 py-2 rounded-lg font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'fundamentals'
              ? 'bg-emerald-600 text-white shadow-lg'
              : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <PieChart className="w-4 h-4" />
          <span>Fundamentals & Ratios</span>
        </button>

        <button
          onClick={() => setActiveTab('scorecard')}
          className={`px-4 py-2 rounded-lg font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'scorecard'
              ? 'bg-amber-600 text-white shadow-lg'
              : 'bg-slate-900 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <Calculator className="w-4 h-4" />
          <span>22-Category Scorecard</span>
        </button>
      </div>

      {/* Tab Content Panels */}
      <div className="transition-all">
        {activeTab === 'chart' && (
          <TradingViewChart candles={candles} technicals={technicals} symbol={selectedSymbol} />
        )}

        {activeTab === 'option-chain' && (
          <OptionChainMatrix data={optionChain} />
        )}

        {activeTab === 'fundamentals' && (
          <FundamentalsPanel fundamentals={fundamentals} />
        )}

        {activeTab === 'scorecard' && (
          <ScorecardWorksheet
            symbol={selectedSymbol}
            scorecard={scorecard}
            onSave={handleSaveScorecard}
          />
        )}
      </div>
    </main>
  );
}
