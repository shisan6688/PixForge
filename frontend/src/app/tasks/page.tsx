"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ArrowRight, Check, ChevronRight, Clock3, CircleAlert, LoaderCircle, RefreshCw, Sparkles, WandSparkles, X } from "lucide-react";
import { Suspense, useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { mergeTasks } from "@/lib/task-state";
import { isActive, ProductionTask, STAGE_LABELS, STAGES } from "@/lib/types";

function TaskPageContent() {
  const search = useSearchParams();
  const created = search.get("created");
  const [tasks, setTasks] = useState<ProductionTask[]>([]);
  const [loading, setLoading] = useState(true);
  const [queryError, setQueryError] = useState("");
  const [filter, setFilter] = useState<"ALL" | "ACTIVE" | "DONE">("ALL");
  const [revision, setRevision] = useState(0);
  const requests = useRef<AbortController | null>(null);
  const generation = useRef(0);
  const refresh = useCallback(async (initial = false) => {
    requests.current?.abort();
    const controller = new AbortController(); requests.current = controller;
    const requestGeneration = generation.current;
    try {
      const result = await api.listTasks(controller.signal);
      if (!controller.signal.aborted && requestGeneration === generation.current) { setTasks(previous => initial ? result : mergeTasks(previous, result)); setQueryError(""); }
    } catch (err) {
      if (!controller.signal.aborted && requestGeneration === generation.current) setQueryError(err instanceof Error ? err.message : "查询失败，请重试。");
    } finally { if (!controller.signal.aborted && requestGeneration === generation.current) setLoading(false); }
  }, []);
  useEffect(() => {
    const reset = () => { generation.current += 1; requests.current?.abort(); setTasks([]); setLoading(true); setQueryError(""); setRevision(value => value + 1); };
    window.addEventListener("pixforge:user-changed", reset);
    return () => { generation.current += 1; requests.current?.abort(); window.removeEventListener("pixforge:user-changed", reset); };
  }, []);
  useEffect(() => { void refresh(true); return () => requests.current?.abort(); }, [refresh, revision]);
  const activeCount = tasks.filter(isActive).length;
  useEffect(() => {
    if (activeCount === 0) return;
    const timer = window.setInterval(() => { void refresh(); }, 1500);
    return () => window.clearInterval(timer);
  }, [activeCount, refresh]);
  const visible = tasks.filter(task => filter === "ALL" || (filter === "ACTIVE" ? isActive(task) : !isActive(task)));
  return <div className="page-container tasks-page"><div className="breadcrumb"><Link href="/">创作工作台</Link><ChevronRight size={14}/> 我的制作</div><div className="tasks-heading"><div><span className="eyebrow light"><Sparkles size={14}/> MY CREATIONS</span><h1>我的制作</h1><p>每一份灵感，都值得被好好收藏。</p></div><Link href="/" className="primary-button">探索更多模板 <ArrowRight size={17}/></Link></div>
    {created && <div className="accepted-banner"><span><Check size={18}/></span><div><strong>制作任务已受理</strong><p>你可以继续浏览，作品进度会在这里持续更新。</p></div></div>}
    <div className="task-toolbar"><div className="task-tabs" role="group" aria-label="任务筛选"><button className={filter === "ALL" ? "selected" : ""} onClick={() => setFilter("ALL")}>全部作品 <span>{tasks.length}</span></button><button className={filter === "ACTIVE" ? "selected" : ""} onClick={() => setFilter("ACTIVE")}>制作中 <span>{activeCount}</span></button><button className={filter === "DONE" ? "selected" : ""} onClick={() => setFilter("DONE")}>已完成 <span>{tasks.length - activeCount}</span></button></div><button className="refresh-button" onClick={() => void refresh()}><RefreshCw size={16}/> 刷新状态</button></div>
    {queryError && <div className="query-error" role="alert"><CircleAlert size={18}/><span>查询暂时不可用：{queryError}。已有记录仍会保留。</span><button onClick={() => void refresh()}>重新查询</button></div>}
    {loading ? <div className="state-panel">正在加载制作记录…</div> : visible.length === 0 ? <div className="empty-tasks"><span><WandSparkles size={32}/></span><h2>{tasks.length === 0 ? "还没有制作记录" : "这里还没有作品"}</h2><p>{tasks.length === 0 ? "从一个喜欢的模板开始，创造你的第一份作品吧。" : "切换筛选条件，看看其他作品。"}</p><Link href="/" className="primary-button">探索模板 <ArrowRight size={17}/></Link></div> : <div className="task-list">{visible.map(task => <TaskCard task={task} key={task.id}/>)}</div>}
  </div>;
}

function TaskCard({ task }: { task: ProductionTask }) {
  const statusText = { QUEUED: "等待处理", RUNNING: "制作中", SUCCEEDED: "制作完成", FAILED: "制作失败" }[task.status];
  const formatted = new Intl.DateTimeFormat("zh-CN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(task.createdAt));
  return <article className="task-card"><div className="task-cover"><img src={task.templateCoverUrl} alt=""/></div><div className="task-body"><div className="task-top"><div><div className="task-date"><Clock3 size={14}/> {formatted}</div><h2>{task.templateTitle}</h2></div><span className={`status-pill status-${task.status.toLowerCase()}`}>{task.status === "RUNNING" ? <LoaderCircle size={14} className="spin"/> : task.status === "SUCCEEDED" ? <Check size={14}/> : task.status === "FAILED" ? <X size={14}/> : <Clock3 size={14}/>} {statusText}</span></div><div className="stage-list">{STAGES.map((stage, index) => { const step = task.steps.find(item => item.stage === stage); const state = step?.status ?? "PENDING"; return <div className={`stage stage-${state.toLowerCase()}`} key={stage}><span className="stage-dot">{state === "SUCCEEDED" ? <Check size={12}/> : state === "FAILED" ? <X size={12}/> : index + 1}</span><span>{STAGE_LABELS[stage]}</span>{index < STAGES.length - 1 && <span className="stage-line"/>}</div>; })}</div>{task.failureMessage && <p className="task-failure"><CircleAlert size={15}/>{task.failureMessage}</p>}{task.status === "SUCCEEDED" && task.resultUrl && <a className="result-link" href={task.resultUrl} target="_blank" rel="noreferrer">查看作品 <ArrowRight size={16}/></a>}{isActive(task) && <p className="task-progress-note">{task.status === "QUEUED" ? "任务已进入队列，等待开始…" : `正在${STAGE_LABELS[task.currentStage ?? "CHECK_ASSETS"]}，请稍候…`}</p>}</div></article>;
}

export default function TasksPage() { return <Suspense fallback={<div className="page-container"><div className="state-panel">正在加载制作记录…</div></div>}><TaskPageContent/></Suspense>; }
