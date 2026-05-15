'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { apiFetch } from '@/lib/api';
import { useAuthStore } from '@/store/useAuthStore';
import Link from 'next/link';
import toast from 'react-hot-toast';
import {
  FileText, Clock, CheckCircle, Activity, Brain,
  ArrowRight, ChevronRight, Loader2, RefreshCw,
  Search, Microscope, Scale, Zap, BookOpen, AlertTriangle
} from 'lucide-react';

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-500/10 text-gray-400 border border-gray-500/20',
  PENDING: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20',
  IN_PROGRESS: 'bg-blue-500/10 text-blue-400 border border-blue-500/20',
  AWAITING_DOCS: 'bg-orange-500/10 text-orange-400 border border-orange-500/20',
  COMPLETED: 'bg-green-500/10 text-green-400 border border-green-500/20',
  CANCELLED: 'bg-red-500/10 text-red-400 border border-red-500/20',
};

const WORK_STAGES = [
  { id: 'DRAFT',       label: 'Reçu',      description: 'Dossier reçu, prêt à démarrer', color: 'text-gray-400',   dotColor: 'bg-gray-400',   next: 'IN_PROGRESS' },
  { id: 'IN_PROGRESS', label: 'En analyse', description: 'Analyse documentaire en cours', color: 'text-blue-400',   dotColor: 'bg-blue-400',   next: 'PENDING' },
  { id: 'PENDING',     label: 'Révision',   description: 'Soumis au manager pour validation', color: 'text-yellow-400', dotColor: 'bg-yellow-400', next: 'COMPLETED' },
  { id: 'COMPLETED',   label: 'Livré',      description: 'Rapport finalisé et livré',    color: 'text-green-400',  dotColor: 'bg-green-500',  next: null },
];

// Auditor's specialized tools
const TOOLS = [
  {
    icon: Brain,
    color: 'text-purple-400', bg: 'bg-purple-500/10', border: 'border-purple-500/20',
    title: 'Assistant RAG Juridique',
    desc: 'Analyse IA de conformité avec le droit marocain — 10 domaines juridiques',
    badge: 'IA',
    badgeColor: 'bg-purple-500/20 text-purple-300',
  },
  {
    icon: Microscope,
    color: 'text-blue-400', bg: 'bg-blue-500/10', border: 'border-blue-500/20',
    title: 'Analyse Documentaire',
    desc: 'Extraction et comparaison automatique des données comptables',
    badge: 'OCR',
    badgeColor: 'bg-blue-500/20 text-blue-300',
  },
  {
    icon: Scale,
    color: 'text-indigo-400', bg: 'bg-indigo-500/10', border: 'border-indigo-500/20',
    title: 'Conformité Réglementaire',
    desc: 'Vérification automatique des obligations légales et fiscales',
    badge: 'Auto',
    badgeColor: 'bg-indigo-500/20 text-indigo-300',
  },
  {
    icon: AlertTriangle,
    color: 'text-orange-400', bg: 'bg-orange-500/10', border: 'border-orange-500/20',
    title: 'Détection d\'Anomalies',
    desc: 'Identification des irrégularités comptables et signaux de risque',
    badge: 'Smart',
    badgeColor: 'bg-orange-500/20 text-orange-300',
  },
];

function StageProgress({ status, auditId, onUpdate }: { status: string; auditId: string; onUpdate: () => void }) {
  const [updating, setUpdating] = useState(false);
  const currentIdx = WORK_STAGES.findIndex(s => s.id === status);
  const current = WORK_STAGES[currentIdx];
  const next = current?.next ? WORK_STAGES.find(s => s.id === current.next) : null;

  const advance = async () => {
    if (!next) return;
    setUpdating(true);
    try {
      await apiFetch(`/api/audits/${auditId}/status/${next.id}`, { method: 'PATCH' });
      toast.success(`Avancement → ${next.label}`);
      onUpdate();
    } catch (e: any) { toast.error(e.message); }
    finally { setUpdating(false); }
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-0">
        {WORK_STAGES.map((s, i) => {
          const done = i <= currentIdx;
          const active = i === currentIdx;
          return (
            <div key={s.id} className="flex items-center flex-1">
              <div className={`h-2 w-2 rounded-full flex-shrink-0 transition-all ${done ? s.dotColor : 'bg-[var(--border)]'} ${active ? 'ring-2 ring-offset-1 ring-offset-[var(--card)] ' + s.dotColor : ''}`} />
              {i < WORK_STAGES.length - 1 && (
                <div className={`h-0.5 flex-1 transition-all ${i < currentIdx ? 'bg-blue-500' : 'bg-[var(--border)]'}`} />
              )}
            </div>
          );
        })}
      </div>
      <div className="flex justify-between">
        {WORK_STAGES.map((s, i) => (
          <span key={s.id} className={`text-xs ${i === currentIdx ? s.color + ' font-semibold' : 'text-[var(--muted-foreground)]'}`}
            style={{ width: '25%', textAlign: i === 0 ? 'left' : i === WORK_STAGES.length - 1 ? 'right' : 'center' }}>
            {s.label}
          </span>
        ))}
      </div>
      {next && (
        <button onClick={advance} disabled={updating}
          className="flex items-center gap-1.5 text-xs font-medium text-blue-400 hover:text-blue-300 transition-colors disabled:opacity-50">
          {updating ? <Loader2 className="h-3 w-3 animate-spin" /> : <ChevronRight className="h-3 w-3" />}
          Passer à : {next.label}
        </button>
      )}
    </div>
  );
}

export default function AuditorDashboard() {
  useAuth(['AUDITOR']);
  const { user } = useAuthStore();
  const [audits, setAudits] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedAudit, setSelectedAudit] = useState<any | null>(null);
  const [aiSummary, setAiSummary] = useState<string | null>(null);
  const [loadingAi, setLoadingAi] = useState(false);
  const [activeTab, setActiveTab] = useState<'dossiers' | 'outils'>('dossiers');
  const [searchQ, setSearchQ] = useState('');

  const loadAudits = () => {
    setLoading(true);
    apiFetch('/api/audits/mine?size=100')
      .then(data => setAudits(data?.content ?? (Array.isArray(data) ? data : [])))
      .catch(e => toast.error(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadAudits(); }, []);

  const loadAiSummary = async (auditId: string) => {
    setLoadingAi(true);
    setAiSummary(null);
    try {
      const res = await apiFetch(`/api/ai/result/${auditId}`);
      setAiSummary(res?.summary || 'Aucune analyse disponible. Lancez une analyse depuis la page du dossier.');
    } catch {
      setAiSummary('Aucune analyse IA. Ouvrez le dossier et lancez l\'analyse RAG.');
    } finally { setLoadingAi(false); }
  };

  const triggerAi = async (auditId: string) => {
    setLoadingAi(true);
    let attempts = 0;
    try {
      await apiFetch(`/api/ai/analyze/${auditId}`, { method: 'POST' });
      toast.success('Analyse lancée — l\'IA traite les documents…');
      const interval = setInterval(async () => {
        attempts++;
        try {
          const res = await apiFetch(`/api/ai/result/${auditId}`);
          if (res?.summary) {
            setAiSummary(res.summary);
            setLoadingAi(false);
            clearInterval(interval);
            toast.success('Analyse terminée ✅');
          }
        } catch { }
        if (attempts > 12) {
          clearInterval(interval);
          setLoadingAi(false);
          setAiSummary('Analyse très longue. Revenez vérifier dans quelques instants.');
        }
      }, 3000);
    } catch (e: any) {
      toast.error(e.message);
      setLoadingAi(false);
    }
  };

  const filtered = audits.filter(a =>
    !searchQ || a.title?.toLowerCase().includes(searchQ.toLowerCase()) ||
    a.clientName?.toLowerCase().includes(searchQ.toLowerCase())
  );

  const stats = [
    { label: 'Assignés',     value: audits.length, icon: FileText, color: 'text-blue-400', bg: 'bg-blue-500/10' },
    { label: 'À démarrer',   value: audits.filter(a => a.status === 'DRAFT').length, icon: Clock, color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
    { label: 'En analyse',   value: audits.filter(a => a.status === 'IN_PROGRESS').length, icon: Activity, color: 'text-indigo-400', bg: 'bg-indigo-500/10' },
    { label: 'Livrés',       value: audits.filter(a => a.status === 'COMPLETED').length, icon: CheckCircle, color: 'text-green-400', bg: 'bg-green-500/10' },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <div className="h-10 w-10 rounded-xl bg-purple-500/10 flex items-center justify-center">
              <Microscope className="h-5 w-5 text-purple-400" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--foreground)]">
              Laboratoire d&apos;Investigation — {user?.fullName?.split(' ')[0] || 'Auditeur'}
            </h1>
          </div>
          <p className="text-[var(--muted-foreground)] text-sm">
            Analyse documentaire · Conformité réglementaire · Assistant IA RAG juridique marocain
          </p>
        </div>
        <button onClick={loadAudits}
          className="flex items-center gap-2 text-[var(--muted-foreground)] hover:text-[var(--foreground)] text-sm glass px-3 py-2 rounded-xl transition-all">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Actualiser
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map(s => (
          <div key={s.label} className="glass rounded-2xl p-5 flex items-center gap-4">
            <div className={`h-12 w-12 rounded-xl ${s.bg} flex items-center justify-center flex-shrink-0`}>
              <s.icon className={`h-6 w-6 ${s.color}`} />
            </div>
            <div>
              <p className="text-2xl font-bold text-[var(--foreground)]">{loading ? '—' : s.value}</p>
              <p className="text-xs text-[var(--muted-foreground)]">{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="flex gap-1 glass rounded-xl p-1 w-fit">
        {[
          { key: 'dossiers', label: 'Mes Dossiers', icon: FileText },
          { key: 'outils', label: 'Outils Métier', icon: Zap },
        ].map((t: any) => (
          <button key={t.key} onClick={() => setActiveTab(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
              activeTab === t.key
                ? 'bg-purple-600 text-white shadow-lg shadow-purple-500/20'
                : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)]'
            }`}>
            <t.icon className="h-4 w-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* === DOSSIERS === */}
      {activeTab === 'dossiers' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* List */}
          <div className="lg:col-span-5 glass rounded-2xl overflow-hidden flex flex-col">
            <div className="px-5 py-4 border-b border-[var(--border)] space-y-3">
              <h2 className="font-semibold text-[var(--foreground)]">Dossiers Assignés</h2>
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-[var(--muted-foreground)]" />
                <input
                  type="text"
                  placeholder="Rechercher un dossier…"
                  value={searchQ}
                  onChange={e => setSearchQ(e.target.value)}
                  className="w-full bg-[var(--muted)] border border-[var(--border)] text-[var(--foreground)] rounded-xl pl-9 pr-3 py-2 text-sm outline-none focus:border-purple-500 transition-all"
                />
              </div>
            </div>
            {loading ? (
              <div className="py-20 text-center text-[var(--muted-foreground)]">Chargement…</div>
            ) : filtered.length === 0 ? (
              <div className="py-20 text-center space-y-3">
                <FileText className="h-12 w-12 mx-auto text-[var(--muted-foreground)]/40" />
                <p className="text-[var(--muted-foreground)]">
                  {searchQ ? 'Aucun résultat' : 'Aucun dossier assigné'}
                </p>
                <p className="text-xs text-[var(--muted-foreground)]/60">Le manager vous assignera bientôt des missions</p>
              </div>
            ) : (
              <div className="divide-y divide-[var(--border)]/50 overflow-y-auto">
                {filtered.map(a => (
                  <div key={a.id}
                    onClick={() => { setSelectedAudit(a); setAiSummary(null); }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        setSelectedAudit(a);
                        setAiSummary(null);
                      }
                    }}
                    role="button"
                    tabIndex={0}
                    className={`px-5 py-4 cursor-pointer transition-colors hover:bg-[var(--muted)]/30 ${selectedAudit?.id === a.id ? 'bg-purple-500/10 border-l-2 border-purple-500' : ''}`}>
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-[var(--foreground)] text-sm truncate">{a.title}</p>
                        <p className="text-xs text-[var(--muted-foreground)] mt-0.5">Client: {a.clientName}</p>
                      </div>
                      <span className={`px-2 py-0.5 rounded-lg text-xs font-medium flex-shrink-0 ${STATUS_COLORS[a.status] || ''}`}>
                        {WORK_STAGES.find(s => s.id === a.status)?.label || a.status}
                      </span>
                    </div>
                    <StageProgress status={a.status} auditId={a.id} onUpdate={loadAudits} />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Detail Panel */}
          <div className="lg:col-span-7 space-y-4">
            {!selectedAudit ? (
              <div className="glass rounded-2xl py-20 text-center">
                <Microscope className="h-12 w-12 mx-auto text-[var(--muted-foreground)]/40 mb-3" />
                <p className="text-[var(--muted-foreground)]">Sélectionnez un dossier pour démarrer l&apos;investigation</p>
              </div>
            ) : (
              <>
                {/* Dossier Info */}
                <div className="glass rounded-2xl p-6 space-y-4">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="text-lg font-semibold text-[var(--foreground)]">{selectedAudit.title}</h3>
                      <p className="text-xs text-[var(--muted-foreground)] mt-1">Client: <span className="text-[var(--foreground)]">{selectedAudit.clientName}</span></p>
                    </div>
                    <Link href={`/audit/${selectedAudit.id}`}
                      className="flex items-center gap-1 text-purple-400 hover:text-purple-300 text-xs font-medium glass px-3 py-1.5 rounded-lg transition-all border border-purple-500/20">
                      Ouvrir le dossier <ArrowRight className="h-3 w-3" />
                    </Link>
                  </div>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    <div>
                      <p className="text-[var(--muted-foreground)] text-xs mb-1">Statut</p>
                      <span className={`px-2.5 py-1 rounded-lg text-xs font-medium ${STATUS_COLORS[selectedAudit.status] || ''}`}>
                        {WORK_STAGES.find(s => s.id === selectedAudit.status)?.label || selectedAudit.status}
                      </span>
                    </div>
                    <div>
                      <p className="text-[var(--muted-foreground)] text-xs mb-1">Échéance</p>
                      <p className="text-[var(--foreground)] text-xs">
                        {selectedAudit.deadline ? new Date(selectedAudit.deadline).toLocaleDateString('fr-FR') : 'Non définie'}
                      </p>
                    </div>
                  </div>
                  {selectedAudit.description && (
                    <p className="text-sm text-[var(--foreground)] bg-[var(--muted)]/40 rounded-xl p-3">{selectedAudit.description}</p>
                  )}
                  <div className="bg-[var(--muted)]/50 rounded-xl p-4">
                    <p className="text-xs font-semibold text-[var(--muted-foreground)] uppercase tracking-wider mb-3">Avancement de l&apos;investigation</p>
                    <StageProgress status={selectedAudit.status} auditId={selectedAudit.id} onUpdate={() => {
                      loadAudits();
                      const stage = WORK_STAGES.find(s => s.id === selectedAudit.status);
                      if (stage?.next) setSelectedAudit((prev: any) => ({ ...prev, status: stage.next }));
                    }} />
                    <div className="mt-4 space-y-2">
                      {WORK_STAGES.map((s, i) => {
                        const currentIdx = WORK_STAGES.findIndex(x => x.id === selectedAudit.status);
                        const done = i <= currentIdx;
                        return (
                          <div key={s.id} className={`flex items-center gap-3 text-xs ${done ? '' : 'opacity-40'}`}>
                            <div className={`h-2 w-2 rounded-full flex-shrink-0 ${done ? s.dotColor : 'bg-[var(--border)]'}`} />
                            <span className={done ? s.color : 'text-[var(--muted-foreground)]'}>{s.label}</span>
                            <span className="text-[var(--muted-foreground)]">—</span>
                            <span className="text-[var(--muted-foreground)]">{s.description}</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>

                {/* AI Analysis Panel */}
                <div className="glass rounded-2xl p-6 space-y-4 border border-purple-500/20">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="h-9 w-9 rounded-xl bg-purple-500/10 flex items-center justify-center">
                        <Brain className="h-5 w-5 text-purple-400" />
                      </div>
                      <div>
                        <h3 className="font-semibold text-[var(--foreground)]">Analyse IA RAG Juridique</h3>
                        <p className="text-xs text-[var(--muted-foreground)]">Conformité droit marocain — détection de non-conformités</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <button onClick={() => loadAiSummary(selectedAudit.id)} disabled={loadingAi}
                        className="text-xs text-[var(--muted-foreground)] glass px-3 py-1.5 rounded-lg hover:text-[var(--foreground)] transition-all disabled:opacity-50">
                        Voir résultat
                      </button>
                      <button onClick={() => triggerAi(selectedAudit.id)} disabled={loadingAi}
                        className="flex items-center gap-2 bg-gradient-to-r from-purple-600 to-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-medium hover:opacity-90 transition-all shadow-lg shadow-purple-500/20 disabled:opacity-50">
                        {loadingAi ? <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Analyse…</> : <><Brain className="h-3.5 w-3.5" /> Lancer l&apos;analyse</>}
                      </button>
                    </div>
                  </div>
                  {aiSummary ? (
                    <div className="bg-[var(--muted)]/50 rounded-xl p-4">
                      <p className="text-sm text-[var(--foreground)] whitespace-pre-wrap leading-relaxed">{aiSummary}</p>
                    </div>
                  ) : (
                    <div className="py-8 text-center space-y-2">
                      <Brain className="h-10 w-10 mx-auto text-purple-400/30" />
                      <p className="text-[var(--muted-foreground)] text-sm">
                        {loadingAi ? 'Analyse en cours, veuillez patienter…' : 'Lancez l\'analyse pour détecter les non-conformités'}
                      </p>
                    </div>
                  )}
                  <Link href={`/audit/${selectedAudit.id}/analyse`}
                    className="flex items-center justify-center gap-2 w-full py-2.5 border border-purple-500/30 text-purple-400 rounded-xl text-sm font-medium hover:bg-purple-500/10 transition-all">
                    <BookOpen className="h-4 w-4" /> Ouvrir l&apos;interface RAG complète →
                  </Link>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* === OUTILS MÉTIER === */}
      {activeTab === 'outils' && (
        <div className="space-y-6">
          <div className="glass rounded-2xl p-6 border border-purple-500/20">
            <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2 mb-2">
              <Zap className="h-5 w-5 text-purple-400" /> Outils d&apos;investigation spécialisés
            </h2>
            <p className="text-xs text-[var(--muted-foreground)] mb-6">
              Votre laboratoire d&apos;audit assisté par intelligence artificielle — sélectionnez un dossier et utilisez ces outils pour une investigation approfondie.
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {TOOLS.map(tool => (
                <div key={tool.title} className={`rounded-2xl p-5 border ${tool.border} ${tool.bg} flex items-start gap-4 group hover:scale-[1.01] transition-all cursor-default`}>
                  <div className={`h-11 w-11 rounded-xl bg-[var(--background)]/50 flex items-center justify-center flex-shrink-0`}>
                    <tool.icon className={`h-6 w-6 ${tool.color}`} />
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <p className="font-semibold text-[var(--foreground)]">{tool.title}</p>
                      <span className={`text-xs px-2 py-0.5 rounded font-medium ${tool.badgeColor}`}>{tool.badge}</span>
                    </div>
                    <p className="text-xs text-[var(--muted-foreground)]">{tool.desc}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Quick access to dossiers needing action */}
          {audits.filter(a => a.status === 'DRAFT' || a.status === 'IN_PROGRESS').length > 0 && (
            <div className="glass rounded-2xl p-5 border border-[var(--border)]">
              <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2 mb-4">
                <AlertTriangle className="h-5 w-5 text-orange-400" /> Dossiers nécessitant votre action
              </h2>
              <div className="space-y-2">
                {audits.filter(a => a.status === 'DRAFT' || a.status === 'IN_PROGRESS').map(a => (
                  <div key={a.id} className="flex items-center justify-between gap-3 px-4 py-3 bg-[var(--muted)]/30 rounded-xl">
                    <div>
                      <p className="text-sm font-medium text-[var(--foreground)]">{a.title}</p>
                      <p className="text-xs text-[var(--muted-foreground)]">Client: {a.clientName}</p>
                    </div>
                    <Link href={`/audit/${a.id}`}
                      className="flex items-center gap-1.5 px-3 py-2 rounded-xl text-xs font-medium bg-purple-500/10 text-purple-400 border border-purple-500/20 hover:bg-purple-500/20 transition-all">
                      Ouvrir <ArrowRight className="h-3 w-3" />
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
