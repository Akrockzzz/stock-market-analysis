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

    const handleResize = () => {
      if (chartRef.current && chartContainerRef.current) {
        chartRef.current.applyOptions({ width: chartContainerRef.current.clientWidth });
      }
    };

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: '#0F172A' },
        textColor: '#94A3B8',
      },
      grid: {
        vertLines: { color: 'rgba(51, 65, 85, 0.3)' },
        horzLines: { color: 'rgba(51, 65, 85, 0.3)' },
      },
      width: chartContainerRef.current.clientWidth,
      height: 420,
      timeScale: {
        borderColor: '#334155',
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
      // Parse ISO timestamp or UNIX timestamp
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

    window.addEventListener('resize', handleResize);

    return () => {
      window.removeEventListener('resize', handleResize);
      chart.remove();
    };
  }, [candles]);

  return (
    <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-xl">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-3 border-b border-slate-800 pb-3">
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            <span>{symbol}</span>
            <span className="text-xs px-2 py-0.5 rounded bg-blue-950 text-blue-400 border border-blue-800 font-mono">
              1D Candlestick
            </span>
          </h2>
          <p className="text-xs text-slate-400 font-mono">
            TradingView Lightweight Chart Stream
          </p>
        </div>

        {technicals && (
          <div className="flex flex-wrap items-center gap-3 text-xs font-mono">
            {technicals.ema20 && (
              <span className="px-2 py-1 rounded bg-slate-800 text-amber-400 border border-slate-700">
                EMA 20: {technicals.ema20}
              </span>
            )}
            {technicals.ema50 && (
              <span className="px-2 py-1 rounded bg-slate-800 text-blue-400 border border-slate-700">
                EMA 50: {technicals.ema50}
              </span>
            )}
            {technicals.rsi14 && (
              <span className={`px-2 py-1 rounded border font-semibold ${
                technicals.rsi14 > 70 ? 'bg-rose-950 text-rose-400 border-rose-800' :
                technicals.rsi14 < 30 ? 'bg-emerald-950 text-emerald-400 border-emerald-800' :
                'bg-slate-800 text-purple-400 border-slate-700'
              }`}>
                RSI (14): {technicals.rsi14}
              </span>
            )}
          </div>
        )}
      </div>

      <div ref={chartContainerRef} className="w-full rounded-lg overflow-hidden" />

      {technicals && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-2 mt-3 pt-3 border-t border-slate-800 text-xs font-mono text-slate-300">
          <div className="bg-slate-950/60 p-2 rounded border border-slate-800">
            <span className="text-slate-500 block">Pivot Point</span>
            <span className="font-bold text-slate-200">{technicals.pivotPoint ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/60 p-2 rounded border border-slate-800">
            <span className="text-rose-500 block">Resistance 1</span>
            <span className="font-bold text-rose-400">{technicals.resistance1 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/60 p-2 rounded border border-slate-800">
            <span className="text-rose-500 block">Resistance 2</span>
            <span className="font-bold text-rose-400">{technicals.resistance2 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/60 p-2 rounded border border-slate-800">
            <span className="text-emerald-500 block">Support 1</span>
            <span className="font-bold text-emerald-400">{technicals.support1 ?? 'N/A'}</span>
          </div>
          <div className="bg-slate-950/60 p-2 rounded border border-slate-800">
            <span className="text-emerald-500 block">Support 2</span>
            <span className="font-bold text-emerald-400">{technicals.support2 ?? 'N/A'}</span>
          </div>
        </div>
      )}
    </div>
  );
}
