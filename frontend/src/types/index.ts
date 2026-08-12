export type ConnectionState = 'LIVE' | 'HISTORICAL_ONLY' | 'NOT_CONNECTED';

export interface ConnectionStatus {
  source: string;
  state: ConnectionState;
  message: string;
  lastHeartbeat: string;
}

export interface Tick {
  symbol: string;
  instrumentKey?: string;
  ltp: number;
  open?: number;
  high?: number;
  low?: number;
  close?: number;
  volume?: number;
  openInterest?: number;
  bid?: number;
  ask?: number;
  timestamp?: string;
}

export interface Candle {
  symbol: string;
  intervalName: string;
  timestamp: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface Technicals {
  symbol: string;
  lastPrice: number;
  ema20?: number;
  ema50?: number;
  ema200?: number;
  rsi14?: number;
  macdLine?: number;
  macdSignal?: number;
  macdHistogram?: number;
  pivotPoint?: number;
  resistance1?: number;
  resistance2?: number;
  support1?: number;
  support2?: number;
}

export interface StrikeData {
  strikePrice: number;
  callOi: number;
  putOi: number;
  callLtp?: number;
  putLtp?: number;
  callIv?: number;
  putIv?: number;
}

export interface OptionChainData {
  underlyingSymbol: string;
  spotPrice: number;
  atmStrike: number;
  totalCallOi: number;
  totalPutOi: number;
  putCallRatio: number;
  maxPainStrike: number;
  strikes: StrikeData[];
}

export interface Fundamentals {
  symbol: string;
  period: string;
  revenueInCr: number;
  revenueCagr5Yr: number;
  netProfitInCr: number;
  ebitdaMarginPct: number;
  netMarginPct: number;
  roePct: number;
  rocePct: number;
  debtToEquity: number;
  ocfInCr: number;
  fcfInCr: number;
  ocfToNetProfitRatio: number;
  peRatio: number;
  pbRatio: number;
  evEbitda: number;
  promoterHoldingPct: number;
  promoterPledgePct: number;
  fiiHoldingPct: number;
  diiHoldingPct: number;
  governanceFlagged: boolean;
}

export interface Scorecard {
  symbol: string;
  categoryScoresJson: string;
  totalScore: number;
  maxPossibleScore: number;
  recommendationBand: string;
  investmentThesis?: string;
  exitCriteria?: string;
  userNotes?: string;
  updatedAt?: string;
}
