'use client';

import React, { useEffect, useState, useRef } from 'react';
import ConnectionStateBanner from '@/components/ConnectionStateBanner';
import TradingViewChart from '@/components/TradingViewChart';
import OptionChainMatrix from '@/components/OptionChainMatrix';
import FundamentalsPanel from '@/components/FundamentalsPanel';
import ScorecardWorksheet from '@/components/ScorecardWorksheet';
import { ConnectionStatus, Candle, Technicals, OptionChainData, Fundamentals, Scorecard, Tick } from '@/types';
import { LineChart, PieChart, Calculator, Layers, Search, RefreshCw, Plus, X, Check, TrendingUp } from 'lucide-react';
import { Client as StompClient } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface SearchResult {
  symbol: string;
  name: string;
  exchange: string;
  instrumentKey: string;
  instrumentType: string;
}

const DEFAULT_WATCHLIST = ['RELIANCE', 'TCS', 'INFY', 'HDFCBANK', 'TATAMOTORS', 'SBIN', 'NIFTY'];

export default function Dashboard() {
  const [selectedSymbol, setSelectedSymbol] = useState('RELIANCE');
  const [activeTab, setActiveTab] = useState<'chart' | 'option-chain' | 'fundamentals' | 'scorecard'>('chart');
  
  const [watchlist, setWatchlist] = useState<string[]>(DEFAULT_WATCHLIST);

  // Load Watchlist from LocalStorage on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem('stock_analysis_watchlist');
      if (saved) {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setWatchlist(parsed);
        }
      }
    } catch (err) {
      console.error('Failed to load watchlist from localStorage:', err);
    }
  }, []);

  // Save Watchlist to LocalStorage on updates
  useEffect(() => {
    try {
      localStorage.setItem('stock_analysis_watchlist', JSON.stringify(watchlist));
    } catch (err) {
      console.error('Failed to save watchlist to localStorage:', err);
    }
  }, [watchlist]);
  
  // Real-time Stock Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  const [status, setStatus] = useState<ConnectionStatus | null>(null);

  const [candles, setCandles] = useState<Candle[]>([]);
  const [technicals, setTechnicals] = useState<Technicals | null>(null);
  const [optionChain, setOptionChain] = useState<OptionChainData | null>(null);
  const [fundamentals, setFundamentals] = useState<Fundamentals | null>(null);
  const [scorecard, setScorecard] = useState<Scorecard | null>(null);
  const [loading, setLoading] = useState(false);

  // Close search dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // STOMP WebSocket: subscribe to real-time connection status & live tick updates from backend
  useEffect(() => {
    let stompClient: StompClient | null = null;
    try {
      stompClient = new StompClient({
        webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
        reconnectDelay: 5000,
        onConnect: () => {
          // Subscribe to live connection state changes
          stompClient?.subscribe('/topic/status', (msg) => {
            try {
              const statusUpdate: ConnectionStatus = JSON.parse(msg.body);
              setStatus(statusUpdate);
            } catch (e) {
              console.error('Failed to parse /topic/status message:', e);
            }
          });
        },
        onStompError: (frame) => {
          console.warn('STOMP error:', frame.headers['message']);
        },
      });
      stompClient.activate();
    } catch (err) {
      console.error('Failed to initialize STOMP WebSocket client:', err);
    }
    return () => {
      stompClient?.deactivate();
    };
  }, []);


  // Real-Time Stock Search Query API
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      setIsDropdownOpen(false);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearching(true);
      try {
        const res = await fetch(`http://localhost:8080/api/instruments/search?query=${encodeURIComponent(searchQuery)}`);
        if (res.ok) {
          const data = await res.json();
          setSearchResults(data);
          setIsDropdownOpen(true);
        }
      } catch (err) {
        console.error('Failed to search instruments:', err);
      } finally {
        setIsSearching(false);
      }
    }, 250);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Subscribe selected symbol to live WebSocket stream
  const subscribeToLiveStream = async (sym: string) => {
    try {
      await fetch(`http://localhost:8080/api/watchlist/${encodeURIComponent(sym.toUpperCase())}/subscribe`, {
        method: 'POST',
      });
    } catch (err) {
      console.error('Failed to subscribe symbol to live stream:', err);
    }
  };

  // Fetch backend market data in fast parallel calls
  const loadSymbolData = async (sym: string) => {
    const upperSym = sym.toUpperCase();
    setLoading(true);

    try {
      const [techRes, candleRes, optRes, fundRes, scoreRes, statusRes] = await Promise.allSettled([
        fetch(`http://localhost:8080/api/analysis/technicals/${upperSym}`),
        fetch(`http://localhost:8080/api/market/candles/${upperSym}`),
        fetch(`http://localhost:8080/api/analysis/option-chain/${upperSym}`),
        fetch(`http://localhost:8080/api/fundamentals/${upperSym}`),
        fetch(`http://localhost:8080/api/scorecard/${upperSym}`),
        fetch(`http://localhost:8080/api/market/status`),
      ]);

      if (techRes.status === 'fulfilled' && techRes.value.ok) setTechnicals(await techRes.value.json());
      else setTechnicals(null);

      if (candleRes.status === 'fulfilled' && candleRes.value.ok) setCandles(await candleRes.value.json());
      else setCandles([]);

      if (optRes.status === 'fulfilled' && optRes.value.ok) setOptionChain(await optRes.value.json());
      else setOptionChain(null);

      if (fundRes.status === 'fulfilled' && fundRes.value.ok) setFundamentals(await fundRes.value.json());
      else setFundamentals(null);

      if (scoreRes.status === 'fulfilled' && scoreRes.value.ok) setScorecard(await scoreRes.value.json());
      else setScorecard(null);

      if (statusRes.status === 'fulfilled' && statusRes.value.ok) setStatus(await statusRes.value.json());
    } catch (err) {
      console.error('Error loading market data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSymbolData(selectedSymbol);
    subscribeToLiveStream(selectedSymbol);
  }, [selectedSymbol]);

  const handleSelectSymbol = (sym: string) => {
    const upper = sym.toUpperCase();
    setSelectedSymbol(upper);
    subscribeToLiveStream(upper);
    if (!watchlist.includes(upper)) {
      setWatchlist((prev) => [...prev, upper]);
    }
    setSearchQuery('');
    setIsDropdownOpen(false);
  };

  const handleToggleWatchlist = (sym: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const upper = sym.toUpperCase();
    if (watchlist.includes(upper)) {
      if (watchlist.length > 1) {
        setWatchlist((prev) => prev.filter((s) => s !== upper));
        if (selectedSymbol === upper) {
          setSelectedSymbol(watchlist.find((s) => s !== upper) || 'RELIANCE');
        }
      }
    } else {
      setWatchlist((prev) => [...prev, upper]);
    }
  };

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
      {/* Header & Real-Time Search Bar */}
      <div className="space-y-4">
        <header className="flex flex-wrap items-center justify-between gap-4 bg-slate-900/90 border border-slate-800 p-4 md:p-5 rounded-2xl backdrop-blur-xl shadow-2xl">
          {/* Logo & Title */}
          <div className="flex items-center space-x-3.5">
            <div className="w-11 h-11 rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-lg font-black text-xl text-white">
              ₹
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-black tracking-tight text-white flex items-center gap-2.5">
                <span>Real-Time Stock Market Analysis</span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-950 text-emerald-400 border border-emerald-800 font-mono font-bold tracking-normal">
                  PRO ENGINE
                </span>
              </h1>
              <p className="text-xs text-slate-400 font-mono">
                Upstox V3 Market Stream • Technical Charts • Option Analytics • 22-Category Scorecard
              </p>
            </div>
          </div>

          {/* Real-Time Autocomplete Search Bar */}
          <div ref={searchRef} className="relative w-full md:w-80 lg:w-96">
            <div className="relative flex items-center">
              <Search className="w-4 h-4 absolute left-3.5 text-slate-400 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => searchQuery.trim() && setIsDropdownOpen(true)}
                placeholder="Search stocks, indices, NSE symbols (e.g. TATAMOTORS, SBIN)..."
                className="w-full pl-10 pr-10 py-2.5 bg-slate-950/90 border border-slate-700/80 rounded-xl text-xs text-slate-100 placeholder-slate-400 font-mono focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all shadow-inner"
              />
              {searchQuery ? (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 text-slate-400 hover:text-slate-200"
                >
                  <X className="w-4 h-4" />
                </button>
              ) : isSearching ? (
                <RefreshCw className="w-4 h-4 absolute right-3 text-blue-400 animate-spin" />
              ) : null}
            </div>

            {/* Autocomplete Dropdown */}
            {isDropdownOpen && searchResults.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-2 bg-slate-900 border border-slate-700/80 rounded-xl shadow-2xl max-h-72 overflow-y-auto z-50 divide-y divide-slate-800/60 font-mono">
                {searchResults.map((item) => {
                  const isSelected = selectedSymbol === item.symbol;
                  const isPinned = watchlist.includes(item.symbol);
                  return (
                    <div
                      key={item.instrumentKey || item.symbol}
                      onClick={() => handleSelectSymbol(item.symbol)}
                      className={`p-3 flex items-center justify-between hover:bg-blue-950/60 cursor-pointer transition-colors ${
                        isSelected ? 'bg-blue-950/40 border-l-4 border-blue-500' : ''
                      }`}
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-white">{item.symbol}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                            {item.exchange || 'NSE_EQ'}
                          </span>
                        </div>
                        <p className="text-[11px] text-slate-400 truncate max-w-[220px]">{item.name || item.symbol}</p>
                      </div>

                      <button
                        onClick={(e) => handleToggleWatchlist(item.symbol, e)}
                        className={`p-1.5 rounded-lg border text-xs flex items-center gap-1 transition-all ${
                          isPinned
                            ? 'bg-blue-950 text-blue-400 border-blue-800 hover:bg-rose-950 hover:text-rose-400 hover:border-rose-800'
                            : 'bg-slate-800 text-slate-400 border-slate-700 hover:text-slate-200'
                        }`}
                        title={isPinned ? 'Remove from Watchlist' : 'Add to Watchlist'}
                      >
                        {isPinned ? <Check className="w-3.5 h-3.5" /> : <Plus className="w-3.5 h-3.5" />}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </header>

        {/* Live Watchlist Bar */}
        <div className="flex items-center justify-between gap-3 bg-slate-900/60 border border-slate-800 p-2.5 rounded-xl font-mono text-xs overflow-x-auto">
          <div className="flex items-center space-x-2 min-w-max">
            <span className="text-slate-400 font-bold px-1 flex items-center gap-1.5">
              <TrendingUp className="w-3.5 h-3.5 text-blue-400" />
              Watchlist:
            </span>
            {watchlist.map((sym) => {
              const isSelected = selectedSymbol === sym;
              return (
                <div
                  key={sym}
                  onClick={() => setSelectedSymbol(sym)}
                  className={`px-3 py-1.5 rounded-lg border font-bold flex items-center space-x-2 cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-blue-600 text-white border-blue-500 shadow-md scale-105'
                      : 'bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700 hover:bg-slate-900'
                  }`}
                >
                  <span>{sym}</span>
                  {watchlist.length > 1 && (
                    <button
                      onClick={(e) => handleToggleWatchlist(sym, e)}
                      className="opacity-50 hover:opacity-100 hover:text-rose-400 ml-1"
                      title="Remove from Watchlist"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  )}
                </div>
              );
            })}
          </div>

          <button
            onClick={() => loadSymbolData(selectedSymbol)}
            className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 font-bold flex items-center space-x-1 shrink-0"
            title="Refresh All Data"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline text-[11px]">Sync Data</span>
          </button>
        </div>

        <ConnectionStateBanner status={status} />
      </div>

      {/* Navigation Tabs */}
      <div className="flex items-center space-x-2 border-b border-slate-800/80 pb-2 font-mono text-xs overflow-x-auto">
        <button
          onClick={() => setActiveTab('chart')}
          className={`px-4 py-2.5 rounded-xl font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'chart'
              ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/30'
              : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <LineChart className="w-4 h-4" />
          <span>Technical Chart & Indicators</span>
        </button>

        <button
          onClick={() => setActiveTab('option-chain')}
          className={`px-4 py-2.5 rounded-xl font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'option-chain'
              ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/30'
              : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Option Chain & Max Pain</span>
        </button>

        <button
          onClick={() => setActiveTab('fundamentals')}
          className={`px-4 py-2.5 rounded-xl font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'fundamentals'
              ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-600/30'
              : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
          }`}
        >
          <PieChart className="w-4 h-4" />
          <span>Fundamentals & Ratios</span>
        </button>

        <button
          onClick={() => setActiveTab('scorecard')}
          className={`px-4 py-2.5 rounded-xl font-bold flex items-center space-x-2 transition-all ${
            activeTab === 'scorecard'
              ? 'bg-amber-600 text-white shadow-lg shadow-amber-600/30'
              : 'bg-slate-900/80 text-slate-400 hover:text-slate-200 border border-slate-800'
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
          <OptionChainMatrix data={optionChain} onSync={() => loadSymbolData(selectedSymbol)} />
        )}

        {activeTab === 'fundamentals' && (
          <FundamentalsPanel fundamentals={fundamentals} onSync={() => loadSymbolData(selectedSymbol)} />
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
