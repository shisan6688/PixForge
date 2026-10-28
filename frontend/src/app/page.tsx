"use client";

import Link from "next/link";
import { ArrowRight, ChevronRight, Search, Sparkles, WandSparkles } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { Template } from "@/lib/types";

export default function HomePage() {
  const [items, setItems] = useState<Template[]>([]);
  const [categories, setCategories] = useState<string[]>([]);
  const [category, setCategory] = useState("全部模板");
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    api.listTemplates(undefined, controller.signal).then(data => { setItems(data.items); setCategories(data.categories); setError(""); }).catch(err => { if (!controller.signal.aborted) setError(err.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [retry]);
  const filtered = useMemo(() => items.filter(item => (category === "全部模板" || item.category === category) && item.title.includes(query.trim())), [items, category, query]);
  return <div className="page-container home-page">
    <div className="breadcrumb">创作工作台 <ChevronRight size={14}/> 探索模板</div>
    <section className="hero"><div className="hero-copy"><span className="eyebrow"><Sparkles size={15}/> CREATE SOMETHING WONDERFUL</span><h1>每一张照片<br/>都有<span>新的可能</span></h1><p>精选创意模板，让灵感轻松落地。<br/>从一张照片开始，创造你的专属作品。</p><a className="hero-button" href="#templates">开始探索 <ArrowRight size={18}/></a></div><div className="hero-art" aria-hidden="true"><div className="hero-circle hero-circle-one"/><div className="hero-circle hero-circle-two"/><div className="hero-photo hero-photo-back"/><div className="hero-photo hero-photo-front"/><div className="hero-star hero-star-one">✦</div><div className="hero-star hero-star-two">✳</div><div className="hero-art-tag"><WandSparkles size={15}/> 灵感即刻成真</div></div></section>
    <section id="templates" className="template-section"><div className="section-title-row"><div><div className="section-overline">EXPLORE TEMPLATES</div><h2>探索灵感模板 <span className="title-spark">✳</span></h2><p>找到喜欢的风格，开启你的下一次创作</p></div><span className="template-count">{items.length} 个精选模板</span></div>
      <div className="filter-row"><div className="category-tabs" role="group" aria-label="模板分类">{["全部模板", ...categories].map(item => <button key={item} className={category === item ? "selected" : ""} onClick={() => setCategory(item)}>{item}</button>)}</div><label className="search-box"><Search size={17}/><input aria-label="搜索模板" placeholder="搜索模板..." value={query} onChange={event => setQuery(event.target.value)}/></label></div>
      {loading ? <div className="state-panel">正在加载模板…</div> : error ? <div className="state-panel"><p>{error}</p><button className="text-button" onClick={() => { setLoading(true); setRetry(value => value + 1); }}>重新加载</button></div> : filtered.length === 0 ? <div className="state-panel"><p>没有找到符合条件的模板</p><button className="text-button" onClick={() => { setCategory("全部模板"); setQuery(""); }}>清除筛选</button></div> : <div className="template-grid">{filtered.map(template => <Link className="template-card" href={`/templates/${template.id}`} key={template.id}><div className={`card-image${template.id === "travel-postcard" ? " postcard" : ""}`} style={{ backgroundColor: template.accent }}><img src={template.coverUrl} alt={`${template.title}模板封面`}/>{template.badge && <span className="card-badge">{template.badge}</span>}<span className="card-overlay"><span>立即制作 <ArrowRight size={17}/></span></span></div><div className="card-content"><span className="card-category">{template.category}</span><div className="card-title-row"><h3>{template.title}</h3><ArrowRight size={18}/></div><p>{template.description}</p></div></Link>)}</div>}
    </section><footer className="page-footer">用想象力，让每个瞬间闪闪发光 ✦</footer>
  </div>;
}
