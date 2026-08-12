import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "Real-Time Stock Market Analysis Platform",
  description: "Upstox Developer API V3 Market Streamer & SEBI-Compliant 22-Category Stock Scoring Platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full dark">
      <body className={`${inter.className} min-h-full flex flex-col bg-slate-950 text-slate-100`}>
        {children}
      </body>
    </html>
  );
}
