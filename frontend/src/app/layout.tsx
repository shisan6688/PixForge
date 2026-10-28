import type { Metadata } from "next";
import AppShell from "@/components/app-shell";
import "./globals.css";

export const metadata: Metadata = { title: "PixForge · 创意素材工作台", description: "探索模板，制作属于你的创意图片。" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="zh-CN" data-scroll-behavior="smooth"><body><AppShell>{children}</AppShell></body></html>;
}
