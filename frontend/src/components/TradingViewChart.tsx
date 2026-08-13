'use client';

import React, { useEffect, useRef } from 'react';
import { createChart, ColorType, IChartApi, ISeriesApi, CandlestickData, Time } from 'lightweight-charts';
import { Candle, Technicals } from '@/types';

interface Props {
  candles: Candle[];
  technicals: Technicals | null;
  symbol: string;
}

export default function TradingViewChart({ candles, technicals, symbol }: Props) {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);

  useEffect(() => {
    if (!chartContainerRef.current || !candles || candles.length === 0) return;

    // Clear previous chart DOM nodes if any
    chartContainerRef.current.innerHTML = '';

    const handleResize = () => {
      if (chartRef.current && chartContainerRef.current) {
        chartRef.current.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    };

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: '#0B1329' },
        textColor: '#94A3B8',
      },
      grid: {
        vertLines: { color: 'rgba(51, 65, 85, 0.25)' },
        horzLines: { color: 'rgba(51, 65, 85, 0.25)' },
      },
      width: chartContainerRef.current.clientWidth,
      height: 420,
      timeScale: {
        borderColor: '#1E293B',
        timeVisible: true,
      },
    });

    chartRef.current = chart;

    const candlestickSeries = chart.addCandlestickSeries({
      upColor: '#10B981',
      downColor: '#EF4444',
      borderVisible: false,
      wickUpColor: '#10B981',
      wickDownColor: '#EF4444',
    });

    const formattedData: CandlestickData<Time>[] = candles.map((c) => {
      const dateVal = new Date(c.timestamp);
      const timeString = Math.floor(dateVal.getTime() / 1000) as Time;
      return {
        time: timeString,
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close,
      };
    });

    candlestickSeries.setData(formattedData);
    chart.timeScale().fitContent();

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
    };
  }, [candles, symbol]);

  const latestCandle = candles && candles.length > 0 ? candles[candles.length - 1] : null;
  const prevCandle = candles && candles.length > 1 ? candles[candles.length - 2] : null;
  const priceChange = latestCandle && prevCandle ? latestCandle.close - prevCandle.close : 0;
  const priceChangePct = prevCandle ? (priceChange / prevCandle.close) * 100 : 0;

  return (
    <div className="w-full bg-slate-900/90 border border-slate-800 rounded-xl p-5 shadow-2xl backdrop-blur-md">
      {/* Symbol Title & Live Summary Row */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-4 border-b border-slate-800/80 pb-4">
        <div>
          <div className="flex items-center gap-3">
            <h2 className="text-2xl font-black tracking-tight text-white">{symbol}</h2>
            <span className="text-xs px-2.5 py-1 rounded-md bg-blue-950/80 text-blue-400 border border-blue-800/80 font-mono font-bold">
              NSE Equity • 1D
            </span>
            {latestCandle && (
              <div className="flex items-baseline gap-2 font-mono">
                <span className="text-xl font-bold text-white">₹{latestCandle.close.toFixed(2)}</span>
                <span className={`text-xs font-bold ${priceChange >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                  {priceChange >= 0 ? '+' : ''}{priceChange.toFixed(2)} ({priceChangePct >= 0 ? '+' : ''}{priceChangePct.toFixed(2)}%)
                </span>
              </div>
            )}
          </div>
          <p className="text-xs text-slate-400 font-mono mt-1">
            TradingView Lightweight Charts • Multi-Timeframe Technical Feed
          </p>
        </div>

        {technicals && (
          <div className="flex flex-wrap items-center gap-2.5 text-xs font-mono">
            {technicals.ema20 && (
              <div className="px-3 py-1.5 rounded-lg bg-amber-950/40 text-amber-300 border border-amber-800/60 shadow-sm">
                <span className="opacity-75">EMA 20:</span> <strong className="font-bold">₹{technicals.ema20}</strong>
              </div>
            )}
            {technicals.ema50 && (
              <div className="px-3 py-1.5 rounded-lg bg-blue-950/40 text-blue-300 border border-blue-800/60 shadow-sm">
                <span className="opacity-75">EMA 50:</span> <strong className="font-bold">₹{technicals.ema50}</strong>
              </div>
            )}
            {technicals.rsi14 && (
              <div className={`px-3 py-1.5 rounded-lg border font-bold shadow-sm ${
                technicals.rsi14 > 70 ? 'bg-rose-950/80 text-rose-300 border-rose-800' :
                technicals.rsi14 < 30 ? 'bg-emerald-950/80 text-emerald-300 border-emerald-800' :
                'bg-slate-950 text-purple-300 border-slate-800'
              }`}>
                RSI (14): {technicals.rsi14} {technicals.rsi14 > 70 ? '(Overbought)' : technicals.rsi14 < 30 ? '(Oversold)' : ''}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Chart Canvas or Loading Skeleton */}
      {(!candles || candles.length === 0) ? (
        <div className="w-full h-[420px] bg-slate-950/60 border border-slate-800/80 rounded-lg flex flex-col items-center justify-center p-6 text-center">
          <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
          <p className="text-slate-300 font-mono text-sm font-semibold">Loading real-time candlestick stream for {symbol}...</p>
          <p className="text-xs text-slate-500 font-mono mt-1">Connecting to Upstox REST & WebSocket Feed API</p>
        </div>
      ) : (
        <div ref={chartContainerRef} className="w-full rounded-lg overflow-hidden border border-slate-800/80 shadow-inner" />
      )}

      {/* Pivot Points & Key Levels Card */}
      {technicals && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3 mt-4 pt-4 border-t border-slate-800/80 text-xs font-mono">
          <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800">
            <span className="text-slate-400 block text-[11px]">PIVOT POINT</span>
            <span className="font-bold text-slate-100 text-sm">₹{technicals.pivotPoint ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800">
            <span className="text-rose-400 block text-[11px]">RESISTANCE 1 (R1)</span>
            <span className="font-bold text-rose-300 text-sm">₹{technicals.resistance1 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800">
            <span className="text-rose-400 block text-[11px]">RESISTANCE 2 (R2)</span>
            <span className="font-bold text-rose-300 text-sm">₹{technicals.resistance2 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800">
            <span className="text-emerald-400 block text-[11px]">SUPPORT 1 (S1)</span>
            <span className="font-bold text-emerald-300 text-sm">₹{technicals.support1 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/80 p-3 rounded-lg border border-slate-800">
            <span className="text-emerald-400 block text-[11px]">SUPPORT 2 (S2)</span>
            <span className="font-bold text-emerald-300 text-sm">₹{technicals.support2 ?? 'N/A'}</span>
          </div>
        </div>
      )}
    </div>
  );
}
