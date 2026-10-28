"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, Check, ChevronRight, CircleAlert, CloudUpload, ImagePlus, RotateCcw, Sparkles, Trash2, WandSparkles } from "lucide-react";
import { ChangeEvent, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";
import { Asset, Stage, Template } from "@/lib/types";

type Slot = { file: File | null; asset: Asset | null; preview: string | null; progress: number; status: "empty" | "uploading" | "ready" | "error"; error: string };
const emptySlot = (): Slot => ({ file: null, asset: null, preview: null, progress: 0, status: "empty", error: "" });

export default function TemplateDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [template, setTemplate] = useState<Template | null>(null);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [failAtStage, setFailAtStage] = useState<Stage | "">("");
  const [stepDelay, setStepDelay] = useState(2000);
  const [retry, setRetry] = useState(0);
  const uploadControllers = useRef<Map<number, AbortController>>(new Map());
  useEffect(() => {
    const controller = new AbortController();
    api.getTemplate(id, controller.signal).then(item => { setTemplate(item); setSlots(Array.from({ length: item.requiredImageCount }, emptySlot)); setError(""); }).catch(err => { if (!controller.signal.aborted) setError(err.message); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    const controllers = uploadControllers.current;
    return () => { controller.abort(); controllers.forEach(value => value.abort()); };
  }, [id, retry]);
  function updateSlot(index: number, change: Partial<Slot>) { setSlots(previous => previous.map((slot, i) => i === index ? { ...slot, ...change } : slot)); }
  async function selectFile(index: number, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]; event.target.value = "";
    if (!file || !template) return;
    uploadControllers.current.get(index)?.abort();
    if (slots[index]?.preview?.startsWith("blob:")) URL.revokeObjectURL(slots[index].preview);
    if (!template.acceptedMimeTypes.includes(file.type)) { updateSlot(index, { ...emptySlot(), status: "error", error: "图片格式不支持，请选择 JPG、PNG 或模板允许的格式。" }); return; }
    if (file.size > template.maxFileSizeBytes) { updateSlot(index, { ...emptySlot(), status: "error", error: `图片不能超过 ${Math.round(template.maxFileSizeBytes / 1024 / 1024)} MB。` }); return; }
    const preview = URL.createObjectURL(file);
    const controller = new AbortController(); uploadControllers.current.set(index, controller);
    updateSlot(index, { file, asset: null, preview, progress: 0, status: "uploading", error: "" });
    try { const asset = await api.uploadAsset(file, progress => updateSlot(index, { progress }), controller.signal); if (!controller.signal.aborted) updateSlot(index, { asset, progress: 100, status: "ready" }); }
    catch (err) { if (!controller.signal.aborted) updateSlot(index, { asset: null, status: "error", error: err instanceof Error ? err.message : "上传失败" }); }
  }
  function removeFile(index: number) { uploadControllers.current.get(index)?.abort(); if (slots[index]?.preview?.startsWith("blob:")) URL.revokeObjectURL(slots[index].preview); updateSlot(index, emptySlot()); }
  async function submit() {
    if (!template || submitting || slots.some(slot => slot.status !== "ready" || !slot.asset)) return;
    setSubmitting(true); setSubmitError("");
    try { const task = await api.createTask(template, slots.map(slot => slot.asset!.id), { failAtStage: failAtStage || null, stepDelayMs: stepDelay }); router.push(`/tasks?created=${task.id}`); }
    catch (err) { setSubmitError(err instanceof Error ? err.message : "提交失败，请稍后重试。"); setSubmitting(false); }
  }
  if (loading) return <div className="page-container"><div className="state-panel">正在加载模板…</div></div>;
  if (error || !template) return <div className="page-container"><div className="state-panel"><CircleAlert size={28}/><p>{error || "模板不存在"}</p><button className="text-button" onClick={() => { setLoading(true); setRetry(value => value + 1); }}>重新加载</button><Link href="/">返回模板列表</Link></div></div>;
  const ready = slots.length === template.requiredImageCount && slots.every(slot => slot.status === "ready");
  return <div className="page-container detail-page"><div className="breadcrumb"><Link href="/">探索模板</Link><ChevronRight size={14}/>{template.category}<ChevronRight size={14}/>{template.title}</div><Link className="back-link" href="/"><ArrowLeft size={17}/> 返回模板列表</Link><div className="detail-heading"><div><span className="eyebrow light"><Sparkles size={14}/> CREATE WITH PIXFORGE</span><h1>{template.title}</h1><p>{template.description}</p></div><span className="detail-category">{template.category}</span></div><div className="detail-layout"><div className="detail-main"><div className="preview-card"><div className="panel-heading"><div><span className="panel-icon lavender"><WandSparkles size={18}/></span><h2>效果预览</h2></div><span>示例效果</span></div><div className="preview-image"><img src={template.resultSampleUrl} alt={`${template.title}示例效果`}/></div><p className="preview-note">效果仅供参考，实际作品将根据你上传的素材生成。</p></div><div className="how-card"><div className="panel-heading"><div><span className="panel-icon peach"><Sparkles size={18}/></span><h2>制作流程</h2></div></div><div className="how-steps"><div><span>01</span><strong>上传素材</strong><p>选择符合要求的图片</p></div><ChevronRight size={18}/><div><span>02</span><strong>提交制作</strong><p>创建专属制作任务</p></div><ChevronRight size={18}/><div><span>03</span><strong>查看成果</strong><p>随时追踪进度与结果</p></div></div></div></div><div className="upload-panel"><div className="panel-heading"><div><span className="panel-icon purple"><CloudUpload size={18}/></span><h2>上传素材</h2></div><span>{slots.filter(slot => slot.status === "ready").length} / {slots.length} 已就绪</span></div><p className="upload-intro">请上传 {template.requiredImageCount} 张图片，开始制作你的作品。</p><div className="slot-list">{slots.map((slot, index) => <div className={`upload-slot ${slot.status}`} key={index}>{slot.preview ? <div className="slot-preview"><img src={slot.preview} alt={`素材 ${index + 1} 预览`}/></div> : <span className="slot-icon"><ImagePlus size={24}/></span>}<div className="slot-info"><strong>素材 {index + 1}</strong><span>{slot.file ? slot.file.name : "点击选择图片"}</span>{slot.status === "uploading" && <div className="progress-track"><div style={{ width: `${slot.progress}%` }}/></div>}{slot.status === "ready" && <small className="success-text"><Check size={13}/> 上传完成</small>}{slot.status === "error" && <small className="error-text">{slot.error}</small>}</div><div className="slot-actions"><label className="slot-file-label">{slot.file ? <RotateCcw size={17}/> : <CloudUpload size={17}/>}<span className="sr-only">{slot.file ? "替换" : "上传"}素材 {index + 1}</span><input type="file" accept={template.acceptedMimeTypes.join(",")} onChange={event => void selectFile(index, event)}/></label>{slot.file && <button className="icon-button" aria-label={`移除素材 ${index + 1}`} onClick={() => removeFile(index)}><Trash2 size={17}/></button>}</div></div>)}</div><div className="upload-rules"><CircleAlert size={16}/><span>支持 {template.acceptedMimeTypes.map(type => type.split("/")[1].toUpperCase()).join(" / ")}，每张不超过 {Math.round(template.maxFileSizeBytes / 1024 / 1024)} MB</span></div><details className="simulation-control"><summary>演示设置 <span>调整耗时 / 模拟失败</span></summary><label>每阶段耗时<select value={stepDelay} onChange={event => setStepDelay(Number(event.target.value))}><option value={1000}>1 秒</option><option value={2000}>2 秒</option><option value={4000}>4 秒</option><option value={8000}>8 秒</option></select></label><label>失败阶段<select value={failAtStage} onChange={event => setFailAtStage(event.target.value as Stage | "")}><option value="">不模拟失败</option><option value="CHECK_ASSETS">素材检查</option><option value="GENERATE">模拟生成</option><option value="FINALIZE">结果整理</option></select></label></details>{submitError && <div className="inline-error" role="alert">{submitError}</div>}<button className="primary-button submit-button" disabled={!ready || submitting} onClick={() => void submit()}>{submitting ? "正在提交…" : "开始制作"}<ArrowRight size={18}/></button><p className="submit-hint">提交后可在「我的制作」中查看进度，离开页面也不会丢失记录。</p></div></div></div>;
}
