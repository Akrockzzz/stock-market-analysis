# 📈 Real-Time Stock Market Analysis Platform

A production-grade, high-performance stock market analysis platform engineered with **Java 17/21 & Spring Boot 3.3** on the backend, **MySQL 8.0** for relational persistence, and **Next.js 14 / React 18 / TradingView Lightweight Charts** on the frontend. The platform integrates directly with the official **Upstox Developer API (Protobuf V3 Feed & REST APIs)** to ingest and analyze licensed Indian equity and F&O market data (NSE & BSE).

---

## 📋 Table of Contents

- [Vision \& Core Capabilities](#-vision--core-capabilities)
- [Key Features \& Interactive Panels](#-key-features--interactive-panels)
  - [1. Real-Time Stock Search \& Autocomplete](#1-real-time-stock-search--autocomplete)
  - [2. Live Market Watchlist with Persistence](#2-live-market-watchlist-with-persistence)
  - [3. Interactive Technical Charts (TradingView)](#3-interactive-technical-charts-tradingview)
  - [4. Option Chain Matrix \& Max Pain](#4-option-chain-matrix--max-pain)
  - [5. Fundamental Analysis \& Key Ratios](#5-fundamental-analysis--key-ratios)
  - [6. 22-Category Institutional Scorecard (50 Points)](#6-22-category-institutional-scorecard-50-points)
  - [7. Transparent Connection State Banner](#7-transparent-connection-state-banner)
- [Strict Zero-Mock Design Principles](#-strict-zero-mock-design-principles)
- [Technology Stack](#-technology-stack)
- [System Architecture \& Ingestion Pipeline](#-system-architecture--ingestion-pipeline)
  - [Two-Tier LRU Subscription Manager](#two-tier-lru-subscription-manager)
  - [Protobuf V3 Binary Decoding](#protobuf-v3-binary-decoding)
- [Project Directory Structure](#-project-directory-structure)
- [Database Schema \& Persistence](#-database-schema--persistence)
- [Upstox API Integration \& Token Setup](#-upstox-api-integration--token-setup)
  - [Step 1: Create Developer App](#step-1-create-developer-app)
  - [Step 2: Environment Variables Configuration](#step-2-environment-variables-configuration)
  - [Step 3: Dynamic OAuth Exchange](#step-3-dynamic-oauth-exchange)
- [🚀 Step-by-Step Execution Guide](#-step-by-step-execution-guide)
  - [Prerequisites](#prerequisites)
  - [1. Setup MySQL Database](#1-setup-mysql-database)
  - [2. Start Spring Boot Backend](#2-start-spring-boot-backend)
  - [3. Start Next.js Frontend](#3-start-nextjs-frontend)
- [🔌 REST API Reference](#-rest-api-reference)
- [🧪 Automated Testing \& Verification](#-automated-testing--verification)
- [⚖️ SEBI Compliance \& Legal Disclaimer](#-sebi-compliance--legal-disclaimer)

---

## 🎯 Vision & Core Capabilities

This platform is designed as an institutional-grade decision-support ecosystem that replaces fragmented tools (NSE India, Screener.in, TradingView, and broker terminals) into a single cohesive, high-speed interface:

1. **Sub-Second Live Market Ingestion**: Streams binary Protobuf V3 market quotes (LTP, bid/ask depth, daily volume, Open Interest).
2. **Deterministic Technical Engine**: Mathematical calculation of 20/50/200 Exponential Moving Averages (EMA), RSI (14), MACD (12, 26, 9), and Floor Pivot Support/Resistance levels.
3. **Derivatives Analytics**: Real-time Put-Call Ratio (PCR), At-The-Money (ATM) strike calculation, Implied Volatilities (IV), and Max Pain minimization.
4. **Fundamental Evaluation**: Automated extraction of TTM revenues, net profit, EBITDA margins, return metrics (ROE, ROCE), leverage (Debt-to-Equity), and shareholding distribution (Promoters, FIIs, DIIs, Pledge %).
5. **Structured 22-Category Scorecard**: Converts quantitative metrics into an objective 50-point investment scoring framework with thesis documentation and exit criteria.

---

## 🖥️ Key Features & Interactive Panels

### 1. Real-Time Stock Search & Autocomplete
- Searches the full NSE Equity universe (~2,500+ instruments) in real time with debounced backend queries (`/api/instruments/search`).
- Shows company names, ISIN identifiers, and exchange tags.
- Selecting any symbol instantly triggers live WebSocket subscription and loads all technical, derivative, and fundamental metrics.

### 2. Live Market Watchlist with Persistence
- Fast-access top bar with one-click stock switching.
- Pin or unpin symbols dynamically.
- State is synchronized with browser `localStorage`, ensuring personalized watchlists persist across sessions.

### 3. Interactive Technical Charts (TradingView)
- Powered by `lightweight-charts` with smooth dark mode styling.
- Renders historical OHLCV candlestick series with customizable intervals (`1d`, `1m`, `5m`, `15m`).
- Overlays EMA 20 (Blue), EMA 50 (Orange), and EMA 200 (Purple) with interactive indicator toggles.
- Real-time technical summary badge (RSI momentum, MACD histogram, and Pivot Support/Resistance).

### 4. Option Chain Matrix & Max Pain
- Analyzes upcoming Tuesday (NSE) or Thursday (BSE) expiry contracts.
- Displays Call OI, Put OI, Strike Prices, Call/Put LTP, and Implied Volatility (IV).
- Automatically calculates and highlights the **Max Pain Strike** (the price where option writers experience minimum cumulative loss) and **Put-Call Ratio (PCR)**.

### 5. Fundamental Analysis & Key Ratios
- Financial health metrics: 5-Year Revenue CAGR, Net Margins, EBITDA Margins.
- Capital efficiency: Return on Equity (ROE) & Return on Capital Employed (ROCE).
- Solvency: Debt-to-Equity ratio.
- Ownership: Promoter holding, Promoter pledge %, FII %, and DII % ownership.
- Governance risk flag detection.

### 6. 22-Category Institutional Scorecard (50 Points)
- 22 distinct investment categories across Growth, Quality, Valuation, Financial Health, and Technicals.
- **Auto-Suggest Engine**: Automatically seeds baseline scores based on live fundamentals and indicators (`GET /api/scorecard/{symbol}/auto-suggest`).
- Interactive scoring sliders (0.0 to 5.0 rating per category).
- Automatic recommendation classification:
  - **40.0 - 50.0**: `STRONG BUY CANDIDATE`
  - **30.0 - 39.5**: `MODERATE BUY / ACCUMULATE`
  - **20.0 - 29.5**: `NEUTRAL / HOLD`
  - **< 20.0**: `AVOID / HIGH RISK`
- Persistent Investment Thesis and Exit Rules worksheet saved to MySQL database (`POST /api/scorecard/{symbol}`).

### 7. Transparent Connection State Banner
- Explicit real-time feed banner showing exact system connection states:
  - 🟢 **`LIVE`**: Active sub-second Protobuf WebSocket stream during NSE market hours (09:15 - 15:30 IST).
  - 🟡 **`HISTORICAL_ONLY`**: Outside market hours or stream idle; serving verified EOD database records.
  - 🔴 **`NOT_CONNECTED`**: Access token required or network unavailable.

---

## 🛡️ Strict Zero-Mock Design Principles

1. **No Synthetic Data Generation**: All random-walk candle generators, mock option chains, and fake fundamental generators have been completely eliminated.
2. **Honest-Fail Architecture**: When Upstox API credentials, tokens, or network requests fail, the application throws a typed `MarketDataUnavailableException` (HTTP 404/500), clearly alerting the user instead of displaying synthetic charts.
3. **Database-API Interval Mapping**: Consistent translation between database storage (`"1d"`, `"1m"`) and Upstox REST URL paths (`"day"`, `"1minute"`), ensuring all backfilled data matches follow-up queries.
4. **ISIN-Based Instrument Keys**: Accurate Upstox instrument key lookup via `InstrumentRepository` for guaranteed REST API resolution.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Java 17/21, Spring Boot 3.3.2, Spring Data JPA, Spring WebSocket (STOMP) |
| **Data Ingestion** | Upstox API V2 / V3, Google Protobuf (Protobuf Java 3.25.3), Java-WebSocket 1.5.6 |
| **Resilience & Caching** | Resilience4j CircuitBreaker, Caffeine / Concurrent In-Memory Cache |
| **Relational Database** | MySQL 8.0, Hibernate ORM |
| **Frontend Framework** | Next.js 14 (App Router), React 18, TypeScript |
| **Styling & Icons** | Tailwind CSS, Lucide React |
| **Charting Library** | TradingView Lightweight Charts (`lightweight-charts`) |

---

## 🏗️ System Architecture & Ingestion Pipeline

```
                                 ┌───────────────────────────────┐
                                 │   Upstox Developer Servers    │
                                 │ (V3 Protobuf WS & V2 REST API)│
                                 └──────────────┬────────────────┘
                                                │
                          ┌─────────────────────┴─────────────────────┐
                          │                                           │
                          ▼ (Binary Protobuf Feed)                    ▼ (JSON REST APIs)
             ┌───────────────────────────┐               ┌───────────────────────────┐
             │  UpstoxWebSocketStreamer  │               │ Ingestion & Sync Services │
             │  - Protobuf V3 Unmarshal  │               │ - HistoricalDataSync      │
             │  - SpotPriceCacheService  │               │ - OptionChainSync         │
             │  - 2-Tier LRU Sub Manager │               │ - FundamentalsSync        │
             └─────────────┬─────────────┘               │ - InstrumentMasterSync    │
                           │                             └─────────────┬─────────────┘
                           │                                           │
                           ▼                                           ▼
             ┌───────────────────────────────────────────────────────────────┐
             │                     Spring Boot Service Layer                 │
             │  - TechnicalAnalysisService   - OptionChainAnalysisService    │
             │  - ScorecardEvaluationService - ScorecardAutoSuggestService   │
             └───────────────────────────────┬───────────────────────────────┘
                                             │
                          ┌──────────────────┴──────────────────┐
                          │                                     │
                          ▼                                     ▼
             ┌────────────────────────┐            ┌────────────────────────┐
             │    MySQL 8 Database    │            │   Next.js 14 Client    │
             │  - instruments         │            │  - TradingView Charts  │
             │  - candles             │◄───────────┤  - Option Chain Matrix │
             │  - option_chain_snaps  │ (REST APIs)│  - 22-Cat Scorecard    │
             │  - fundamentals        │            │  - Live Watchlist      │
             │  - stock_scorecards    │            │  - Real-Time Search    │
             └────────────────────────┘            └────────────────────────┘
```

### Two-Tier LRU Subscription Manager
To optimize Upstox WebSocket bandwidth:
- **Tier 1 (Spot Watchlist)**: Maintains up to 2,500 active stock tokens with a 15-minute idle eviction window.
- **Tier 2 (Option Chain Strikes)**: Subscribes up to 40 active strike contracts per symbol with a 2-minute LRU eviction window.

### Protobuf V3 Binary Decoding
Payloads received from `wss://api.upstox.com/v3/feed/market-data-feed` are parsed directly into Java objects using classes compiled from [MarketDataFeedV3.proto](file:///f:/Stock%20Market%20Analysis/backend/src/main/proto/MarketDataFeedV3.proto).

---

## 📁 Project Directory Structure

```
Stock Market Analysis/
├── backend/                               # Spring Boot Application
│   ├── pom.xml                            # Maven dependencies & Protobuf compiler plugin
│   ├── src/main/proto/
│   │   └── MarketDataFeedV3.proto         # Official Upstox Protobuf V3 schema
│   ├── src/main/resources/
│   │   └── application.yml                # Database, Upstox, & Resilience4j configuration
│   └── src/main/java/com/stock/analysis/
│       ├── controller/                    # REST API Controllers (Analysis, Market, Watchlist, Scorecard)
│       ├── dto/                           # Data Transfer Objects
│       ├── engine/                        # Technical, Option Chain, & Scorecard Engines
│       ├── enums/                         # ConnectionState, Exchange (NSE_EQ, NSE_FO)
│       ├── exception/                     # GlobalExceptionHandler & MarketDataUnavailableException
│       ├── ingestion/                     # UpstoxWebSocketStreamer & LruSubscriptionManager
│       ├── model/                         # JPA Entities (Candle, Tick, Instrument, Fundamentals, etc.)
│       ├── repository/                    # Spring Data JPA Repositories
│       ├── service/                       # Historical, Option Chain, Fundamentals, & Sync Services
│       └── util/                          # MarketHoursUtil, ExpiryUtil
│
├── frontend/                              # Next.js 14 Web Application
│   ├── package.json                       # Dependencies (lightweight-charts, lucide-react, etc.)
│   ├── tsconfig.json                      # TypeScript configuration
│   └── src/
│       ├── app/
│       │   ├── layout.tsx                 # Root application layout
│       │   ├── page.tsx                   # Main Dashboard component
│       │   └── api/auth/upstox/callback/  # OAuth Callback Route Handler
│       ├── components/                    # Modular UI components
│       │   ├── ConnectionStateBanner.tsx  # Live stream status banner
│       │   ├── TradingViewChart.tsx       # Interactive candlestick & indicator chart
│       │   ├── OptionChainMatrix.tsx      # Derivatives & Max Pain analysis table
│       │   ├── FundamentalsPanel.tsx      # Financial statements & ratio breakdown
│       │   └── ScorecardWorksheet.tsx     # 22-category checklist & thesis worksheet
│       └── types/                         # TypeScript interfaces & definitions
│
├── .env.example                           # Template for Upstox API keys
└── README.md                              # Project documentation
```

---

## 🔑 Upstox API Integration & Token Setup

### Step 1: Create Developer App
1. Log in to the [Upstox Developer Console](https://upstox.com/developer/).
2. Click **Manage API Apps** ➔ **+ App** (Create New App).
3. Set **Redirect URL**: `http://localhost:3000/api/auth/upstox/callback`.
4. Copy your **API Key** and **API Secret**.

### Step 2: Environment Variables Configuration
Create a `.env` file in the root directory:

```bash
UPSTOX_API_KEY=your_upstox_api_key
UPSTOX_API_SECRET=your_upstox_api_secret
UPSTOX_REDIRECT_URI=http://localhost:3000/api/auth/upstox/callback
UPSTOX_ACCESS_TOKEN=your_generated_access_token
```

### Step 3: Dynamic OAuth Exchange
When logging in via Upstox OAuth:
1. Upstox redirects to `http://localhost:3000/api/auth/upstox/callback?code=...`.
2. The Next.js route handler exchanges the `code` for an `access_token` and pushes it to backend `POST /api/auth/upstox/token`.
3. The backend dynamically updates `UpstoxWebSocketStreamer` in-memory without needing a server restart.

---

## 🚀 Step-by-Step Execution Guide

### Prerequisites
- **Java**: JDK 17 or 21 (`java -version`)
- **Maven**: Apache Maven 3.8+ (`mvn -v`)
- **Node.js**: Node.js 18+ & NPM (`node -v`, `npm -v`)
- **MySQL**: MySQL Server 8.0+ running on port `3306`

---

### 1. Setup MySQL Database
Ensure MySQL is running on port `3306`:
```sql
CREATE DATABASE IF NOT EXISTS stockdb;
```
Verify `backend/src/main/resources/application.yml` has your MySQL password:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stockdb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata
    username: root
    password: your_mysql_password
```

---

### 2. Start Spring Boot Backend
Open a terminal in the project directory:
```powershell
cd backend
mvn spring-boot:run
```
- **Backend API**: `http://localhost:8080`
- **Stream Status**: `http://localhost:8080/api/market/status`

---

### 3. Start Next.js Frontend
Open a second terminal in the project directory:
```powershell
cd frontend
npm install
npm run dev
```
- **Web App**: Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 🔌 REST API Reference

| HTTP Method | Endpoint | Description | Query / Body Params |
|---|---|---|---|
| `GET` | `/api/market/status` | Current live stream connection state (`LIVE`, `HISTORICAL_ONLY`, `NOT_CONNECTED`) | None |
| `GET` | `/api/instruments/search` | Search NSE stock and index universe with autocomplete | `?query=TATAMOTORS` |
| `POST` | `/api/watchlist/{symbol}/subscribe` | Subscribes stock to live Tier 1 WebSocket feed | None |
| `POST` | `/api/watchlist/{symbol}/subscribe-option-chain` | Subscribes stock option strikes to Tier 2 WebSocket feed | None |
| `DELETE` | `/api/watchlist/{symbol}/unsubscribe` | Unsubscribes stock from live stream | None |
| `GET` | `/api/market/ticks/{symbol}` | Returns latest live tick (LTP, Volume, OI) | None |
| `GET` | `/api/market/candles/{symbol}` | Retrieves historical OHLCV candle records | `?interval=1d` |
| `GET` | `/api/analysis/technicals/{symbol}` | Calculates EMAs, RSI(14), MACD, and Pivot points | None |
| `GET` | `/api/analysis/option-chain/{symbol}` | Analyzes option chain matrix, PCR ratio, & Max Pain | `?spotPrice=...&expiry=...` |
| `GET` | `/api/fundamentals/{symbol}` | Retrieves financial statements, return ratios, & shareholding | None |
| `GET` | `/api/scorecard/{symbol}/auto-suggest` | Generates baseline ratings for the 22 scorecard categories | None |
| `POST` | `/api/scorecard/{symbol}` | Saves custom scorecard ratings, thesis, and exit rules to DB | JSON payload |
| `POST` | `/api/auth/upstox/token` | Updates active runtime access token and triggers WS reconnect | `{"accessToken":"..."}` |

---

## 🧪 Automated Testing & Verification

The backend includes a comprehensive JUnit 5 test suite covering mathematical engines and edge cases:

```powershell
cd backend
mvn test
```

### Test Suite Coverage:
- **`TechnicalAnalysisServiceTest`**: Verifies EMA (20/50/200), RSI (14), MACD, and Pivot point formulas.
- **`OptionChainAnalysisServiceTest`**: Validates Put-Call Ratio (PCR), ATM strike selection, and Max Pain minimization.
- **`ScorecardEvaluationServiceTest`**: Validates weight aggregations and recommendation threshold bands.

---

## ⚖️ SEBI Compliance & Legal Disclaimer

> [!CAUTION]
> **Legal Disclaimer:** This software application is built strictly as a personal decision-support framework and educational market analysis platform. It **does not** provide SEBI-registered investment advice, stock tips, or automated trading recommendations. Financial markets involve substantial risk of loss. Always conduct independent research and consult a SEBI-registered financial advisor before making investment decisions.
