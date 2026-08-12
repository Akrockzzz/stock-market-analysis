package com.stock.analysis.enums;

public enum ScoringCategory {
    BUSINESS_UNDERSTANDING("Business Model & Industry Understandability", 2.5),
    REVENUE_GROWTH("Consistent Revenue Growth (5-Yr CAGR)", 2.5),
    PROFIT_MARGINS("Operating & Net Margins Stability", 2.5),
    ROE_ROCE("ROE & ROCE Performance (> 15%)", 2.5),
    BALANCE_SHEET_DEBT("Debt-to-Equity (< 0.5 or Cash Rich)", 2.5),
    CASH_FLOW_CONVERSION("Operating Cash Flow / Net Profit Ratio", 2.5),
    CAPITAL_ALLOCATION("Reinvestment Quality & Dividend Discipline", 2.5),
    PROMOTER_INTEGRITY("Management Integrity & Governance Track Record", 2.5),
    PROMOTER_HOLDING_PLEDGE("Promoter Holding (>50%) & Low/Zero Pledge", 2.5),
    INSTITUTIONAL_INTEREST("FII & DII Institutional Holdings", 2.0),
    MOAT_COMPETITIVE_EDGE("Economic Moat & Pricing Power", 2.5),
    WORKING_CAPITAL_EFFICIENCY("Working Capital & Receivables Management", 2.0),
    VALUATION_MULTIPLES("P/E, P/B, EV/EBITDA Fair Value Multiples", 2.5),
    INTRINSIC_VALUE_MARGIN_SAFETY("DCF / Intrinsic Value Margin of Safety", 2.5),
    CONTINGENT_LIABILITIES("Contingent Liabilities & Auditor Notes Check", 2.0),
    GOVERNANCE_RED_FLAGS("Governance Red Flags (RPT, Dilution, Restatements)", 2.5),
    TECHNICAL_TREND_MOVING_AVERAGES("Moving Average Alignment (20/50/200 EMA)", 2.0),
    MOMENTUM_INDICATORS("RSI & MACD Technical Momentum", 2.0),
    VOLUME_PRICE_ACTION("Volume Breakouts & Support/Resistance", 2.0),
    RISK_REWARD_RATIO("Risk-to-Reward Ratio (> 1:2.5)", 2.0),
    POSITION_SIZING_PORTFOLIO("Portfolio Position Sizing & Sector Risk Cap", 2.0),
    THESIS_EXIT_CRITERIA("Clear Investment Thesis & Pre-defined Exit Rules", 2.0);

    private final String displayName;
    private final double weight;

    ScoringCategory(String displayName, double weight) {
        this.displayName = displayName;
        this.weight = weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getWeight() {
        return weight;
    }
}
