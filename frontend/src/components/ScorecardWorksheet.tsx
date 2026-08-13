'use client';

import React, { useState } from 'react';
import { Scorecard } from '@/types';
import { CheckCircle2, AlertOctagon, Save, Calculator, HelpCircle, Sparkles } from 'lucide-react';

interface Props {
  symbol: string;
  scorecard: Scorecard | null;
  onSave: (ratings: Record<string, number>, thesis: string, exitRules: string, notes: string) => void;
}

const CATEGORIES = [
  { id: 'BUSINESS_UNDERSTANDING', name: '1. Business Model & Industry Understandability', weight: 2.5 },
  { id: 'REVENUE_GROWTH', name: '2. Consistent Revenue Growth (5-Yr CAGR >12%)', weight: 2.5 },
  { id: 'PROFIT_MARGINS', name: '3. Operating & Net Margins Stability', weight: 2.5 },
  { id: 'ROE_ROCE', name: '4. ROE & ROCE Performance (>15%)', weight: 2.5 },
  { id: 'BALANCE_SHEET_DEBT', name: '5. Debt-to-Equity (<0.5 or Cash Rich)', weight: 2.5 },
  { id: 'CASH_FLOW_CONVERSION', name: '6. Operating Cash Flow / Net Profit Ratio', weight: 2.5 },
  { id: 'CAPITAL_ALLOCATION', name: '7. Reinvestment Quality & Dividend Discipline', weight: 2.5 },
  { id: 'PROMOTER_INTEGRITY', name: '8. Management Integrity & Governance Track Record', weight: 2.5 },
  { id: 'PROMOTER_HOLDING_PLEDGE', name: '9. Promoter Holding (>50%) & Low/Zero Pledge', weight: 2.5 },
  { id: 'INSTITUTIONAL_INTEREST', name: '10. FII & DII Institutional Holdings', weight: 2.0 },
  { id: 'MOAT_COMPETITIVE_EDGE', name: '11. Economic Moat & Pricing Power', weight: 2.5 },
  { id: 'WORKING_CAPITAL_EFFICIENCY', name: '12. Working Capital & Receivables Management', weight: 2.0 },
  { id: 'VALUATION_MULTIPLES', name: '13. P/E, P/B, EV/EBITDA Fair Value Multiples', weight: 2.5 },
  { id: 'INTRINSIC_VALUE_MARGIN_SAFETY', name: '14. DCF / Intrinsic Value Margin of Safety', weight: 2.5 },
  { id: 'CONTINGENT_LIABILITIES', name: '15. Contingent Liabilities & Auditor Notes Check', weight: 2.0 },
  { id: 'GOVERNANCE_RED_FLAGS', name: '16. Governance Red Flags (RPT, Dilution, Restatements)', weight: 2.5 },
  { id: 'TECHNICAL_TREND_MOVING_AVERAGES', name: '17. Moving Average Alignment (20/50/200 EMA)', weight: 2.0 },
  { id: 'MOMENTUM_INDICATORS', name: '18. RSI & MACD Technical Momentum', weight: 2.0 },
  { id: 'VOLUME_PRICE_ACTION', name: '19. Volume Breakouts & Support/Resistance', weight: 2.0 },
  { id: 'RISK_REWARD_RATIO', name: '20. Risk-to-Reward Ratio (> 1:2.5)', weight: 2.0 },
  { id: 'POSITION_SIZING_PORTFOLIO', name: '21. Portfolio Position Sizing & Sector Risk Cap', weight: 2.0 },
  { id: 'THESIS_EXIT_CRITERIA', name: '22. Clear Investment Thesis & Pre-defined Exit Rules', weight: 2.0 },
];

export default function ScorecardWorksheet({ symbol, scorecard, onSave }: Props) {
  const initialRatings: Record<string, number> = {};
  CATEGORIES.forEach((c) => {
    initialRatings[c.id] = 4.0; // Default rating 4/5
  });

  const [ratings, setRatings] = useState<Record<string, number>>(initialRatings);
  const [thesis, setThesis] = useState(scorecard?.investmentThesis || '');
  const [exitRules, setExitRules] = useState(scorecard?.exitCriteria || '');
  const [notes, setNotes] = useState(scorecard?.userNotes || '');

  // Calculate live total score (out of 50.0)
  const calculateTotalScore = () => {
    let total = 0.0;
    let maxWeights = 0.0;
    CATEGORIES.forEach((c) => {
      const rating = ratings[c.id] || 0;
      total += (rating / 5.0) * c.weight;
      maxWeights += c.weight;
    });
    return Math.round((total / maxWeights) * 50.0 * 100.0) / 100.0;
  };

  const currentScore = calculateTotalScore();

  const getBand = (score: number) => {
    if (score >= 40.0) return { text: 'STRONG BUY CANDIDATE (40-50 pts)', style: 'bg-emerald-950 text-emerald-400 border-emerald-800' };
    if (score >= 30.0) return { text: 'HOLD / ACCUMULATE (30-39 pts)', style: 'bg-amber-950 text-amber-400 border-amber-800' };
    return { text: 'HIGH RISK / AVOID (<30 pts)', style: 'bg-rose-950 text-rose-400 border-rose-800' };
  };

  const bandInfo = getBand(currentScore);

  const handleRatingChange = (id: string, val: number) => {
    setRatings((prev) => ({ ...prev, [id]: val }));
  };

  return (
    <div className="w-full bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-2xl font-mono text-slate-200">
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6 border-b border-slate-800 pb-4">
        <div>
          <h2 className="text-xl font-bold text-slate-100 flex items-center gap-2">
            <Calculator className="w-6 h-6 text-blue-400" />
            <span>22-Category Stock Scoring Worksheet</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            SEBI-compliant structured analysis framework for symbol: <span className="text-blue-400 font-bold">{symbol}</span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={async () => {
              try {
                const res = await fetch(`http://localhost:8080/api/scorecard/${symbol}/auto-suggest`);
                if (res.ok) {
                  const suggested = await res.json();
                  setRatings((prev) => ({ ...prev, ...suggested }));
                }
              } catch (err) {
                console.error('Failed to fetch auto-suggest scorecard:', err);
              }
            }}
            className="px-4 py-2 rounded-lg bg-gradient-to-r from-amber-600 to-orange-600 hover:from-amber-500 hover:to-orange-500 text-white font-bold text-xs flex items-center space-x-1.5 shadow-lg transition-all"
            title="Auto-evaluate scorecard based on real-time fundamentals & technical indicators"
          >
            <Sparkles className="w-4 h-4" />
            <span>⚡ Auto-Evaluate with Real Data</span>
          </button>

          <div className={`px-4 py-2 rounded-lg border font-bold text-sm shadow-inner flex items-center space-x-2 ${bandInfo.style}`}>
            <CheckCircle2 className="w-4 h-4" />
            <span>{currentScore} / 50.0 PTS</span>
            <span className="opacity-60 text-xs border-l border-white/20 pl-2 ml-2">{bandInfo.text}</span>
          </div>

          <button
            onClick={() => onSave(ratings, thesis, exitRules, notes)}
            className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs flex items-center space-x-2 shadow-lg transition-all"
          >
            <Save className="w-4 h-4" />
            <span>Save Scorecard</span>
          </button>
        </div>
      </div>

      {/* Categories Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        {CATEGORIES.map((cat) => {
          const rating = ratings[cat.id] || 0;
          return (
            <div
              key={cat.id}
              className="bg-slate-950 p-3.5 rounded-lg border border-slate-800 hover:border-slate-700 transition-all flex flex-col justify-between"
            >
              <div className="flex items-start justify-between gap-2 mb-2">
                <span className="text-xs font-semibold text-slate-200 leading-snug">{cat.name}</span>
                <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-900 text-slate-400 border border-slate-800 whitespace-nowrap">
                  Wt: {cat.weight}
                </span>
              </div>

              <div className="flex items-center justify-between gap-3 mt-1">
                <input
                  type="range"
                  min="0"
                  max="5"
                  step="0.5"
                  value={rating}
                  onChange={(e) => handleRatingChange(cat.id, parseFloat(e.target.value))}
                  className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-500"
                />
                <span className="text-xs font-bold text-blue-400 w-8 text-right">
                  {rating.toFixed(1)}/5
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Thesis & Exit Rules Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 border-t border-slate-800 pt-6">
        <div>
          <label className="text-xs font-bold text-slate-300 block mb-2">
            Investment Thesis ("Why am I buying?")
          </label>
          <textarea
            value={thesis}
            onChange={(e) => setThesis(e.target.value)}
            placeholder="Document your clear qualitative thesis, pricing power, moat, and catalyst..."
            rows={4}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-slate-200 focus:border-blue-500 focus:outline-none font-mono"
          />
        </div>

        <div>
          <label className="text-xs font-bold text-slate-300 block mb-2">
            Exit Rules & Pre-defined Stop Loss ("What would make me sell?")
          </label>
          <textarea
            value={exitRules}
            onChange={(e) => setExitRules(e.target.value)}
            placeholder="e.g. Sell if revenue growth drops below 10% for 2 quarters or price breaks 200 EMA..."
            rows={4}
            className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs text-slate-200 focus:border-blue-500 focus:outline-none font-mono"
          />
        </div>
      </div>
    </div>
  );
}
