"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Clock3, Images, Menu, PanelLeftClose, Sparkles, X } from "lucide-react";
import { ReactNode, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { User } from "@/lib/types";

export default function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [mobileOpen, setMobileOpen] = useState(false);
  useEffect(() => { api.getSession().then(setUser).catch(() => undefined); }, []);
  async function switchUser(key: "A" | "B") {
    if (key === user?.demoKey) return;
    setUser(await api.switchUser(key));
    window.dispatchEvent(new Event("pixforge:user-changed"));
    router.refresh();
  }
  const links = [
    { href: "/", label: "探索模板", icon: Images },
    { href: "/tasks", label: "我的制作", icon: Clock3 },
  ];
  return <div className="app-frame">
    <button className="mobile-menu icon-button" aria-label="打开导航" onClick={() => setMobileOpen(true)}><Menu size={21} /></button>
    {mobileOpen && <button className="mobile-scrim" aria-label="关闭导航" onClick={() => setMobileOpen(false)} />}
    <aside className={`sidebar ${mobileOpen ? "sidebar-open" : ""}`}>
      <div className="sidebar-head"><Link href="/" className="brand" onClick={() => setMobileOpen(false)}><span className="brand-mark"><Sparkles size={22} strokeWidth={2.4}/></span><span>Pix<span className="brand-accent">Forge</span></span></Link><button className="mobile-close icon-button" aria-label="关闭导航" onClick={() => setMobileOpen(false)}><X size={19}/></button></div>
      <div className="sidebar-caption">创作工作台</div>
      <nav className="main-nav" aria-label="主导航">{links.map(({ href, label, icon: Icon }) => <Link key={href} href={href} onClick={() => setMobileOpen(false)} className={`nav-link ${pathname === href || (href === "/" && pathname.startsWith("/templates")) ? "active" : ""}`}><Icon size={19}/><span>{label}</span>{href === "/tasks" && <span className="nav-arrow">›</span>}</Link>)}</nav>
      <div className="sidebar-bottom"><div className="sidebar-tip"><span className="tip-icon"><Sparkles size={17}/></span><strong>灵感，从这里开始</strong><p>挑选喜欢的模板，上传照片，轻松开启创作。</p><Link href="/" onClick={() => setMobileOpen(false)}>发现模板 <span>↗</span></Link></div><div className="sidebar-footer"><PanelLeftClose size={16}/> PixForge · 创意素材工作台</div></div>
    </aside>
    <div className="main-area"><header className="topbar"><div className="topbar-spacer"/><div className="topbar-actions"><span className="demo-chip"><span/> 演示模式</span><label className="user-picker"><span className="avatar">{user?.demoKey ?? "A"}</span><select aria-label="切换演示用户" value={user?.demoKey ?? "A"} onChange={event => void switchUser(event.target.value as "A" | "B")}><option value="A">演示用户 A</option><option value="B">演示用户 B</option></select></label></div></header><main>{children}</main></div>
  </div>;
}
