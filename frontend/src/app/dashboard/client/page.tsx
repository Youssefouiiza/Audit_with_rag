'use client';

import { useState, useEffect, useRef } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { apiFetch } from '@/lib/api';
import { useAuthStore } from '@/store/useAuthStore';
import Link from 'next/link';
import toast from 'react-hot-toast';
import {
  FileText, Clock, CheckCircle, Plus, X, ArrowRight,
  Upload, Paperclip, AlertCircle, Download, Shield,
  MessageSquare, PackageCheck, Send
} from 'lucide-react';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';

const STATUS_COLORS: Record<string, string> = {
  DRAFT:        'bg-gray-500/10 text-gray-400 border border-gray-500/20',
  PENDING:      'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20',
  IN_PROGRESS:  'bg-blue-500/10 text-blue-400 border border-blue-500/20',
  AWAITING_DOCS:'bg-orange-500/10 text-orange-400 border border-orange-500/20',
  COMPLETED:    'bg-green-500/10 text-green-400 border border-green-500/20',
  CANCELLED:    'bg-red-500/10 text-red-400 border border-red-500/20',
};
const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Reçu',  PENDING: 'En attente', IN_PROGRESS: 'En analyse',
  AWAITING_DOCS: 'Docs manquants', COMPLETED: 'Rapport prêt', CANCELLED: 'Annulé',
};

// Visual step tracker for client-facing status
const STEPS = [
  { key: 'DRAFT',         label: 'Dossier reçu',          icon: FileText,     color: 'text-gray-400',   bg: 'bg-gray-500/10' },
  { key: 'IN_PROGRESS',   label: 'Analyse en cours',       icon: Clock,        color: 'text-blue-400',   bg: 'bg-blue-500/10' },
  { key: 'PENDING',       label: 'Vérification finale',    icon: Shield,       color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
  { key: 'COMPLETED',     label: 'Rapport certifié',       icon: PackageCheck, color: 'text-green-400',  bg: 'bg-green-500/10' },
];

function AuditStepTracker({ status }: { status: string }) {
  const currentIdx = STEPS.findIndex(s => s.key === status);
  const idx = currentIdx >= 0 ? currentIdx : 0;
  return (
    <div className="flex items-start gap-0 mt-3">
      {STEPS.map((s, i) => {
        const done   = i < idx;
        const active = i === idx;
        const Icon   = s.icon;
        return (
          <div key={s.key} className="flex flex-col items-center flex-1">
            <div className="flex items-center w-full">
              <div className={`h-7 w-7 rounded-full flex items-center justify-center flex-shrink-0 transition-all ${
                done   ? 'bg-green-500 text-white' :
                active ? s.bg + ' ring-2 ring-offset-1 ring-offset-[var(--card)] ring-current ' + s.color :
                         'bg-[var(--muted)] text-[var(--muted-foreground)]'
              }`}>
                {done ? <CheckCircle className="h-4 w-4" /> : <Icon className="h-3.5 w-3.5" />}
              </div>
              {i < STEPS.length - 1 && (
                <div className={`h-0.5 flex-1 mx-1 transition-all ${done ? 'bg-green-500' : 'bg-[var(--border)]'}`} />
              )}
            </div>
            <p className={`text-xs mt-1.5 text-center leading-tight ${
              active ? s.color + ' font-semibold' : done ? 'text-green-400' : 'text-[var(--muted-foreground)]'
            }`} style={{ maxWidth: 64 }}>{s.label}</p>
          </div>
        );
      })}
    </div>
  );
}

export default function ClientDashboard() {
  useAuth(['CLIENT']);
  const { user } = useAuthStore();
  const [audits, setAudits]       = useState<any[]>([]);
  const [showForm, setShowForm]   = useState(false);
  const [loading, setLoading]     = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [formData, setFormData]   = useState({ title: '', description: '', deadline: '' });
  const [files, setFiles]         = useState<File[]>([]);
  const [dragOver, setDragOver]   = useState(false);
  const fileInputRef              = useRef<HTMLInputElement>(null);

  useEffect(() => { loadAudits(); }, []);

  const loadAudits = async () => {
    setLoading(true);
    try {
      const data = await apiFetch('/api/audits/mine?size=100');
      setAudits(data?.content ?? (Array.isArray(data) ? data : []));
    } catch (e: any) { toast.error(e.message); }
    finally { setLoading(false); }
  };

  const handleFileDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    setFiles(prev => [...prev, ...Array.from(e.dataTransfer.files)]);
  };

  const removeFile = (idx: number) => setFiles(prev => prev.filter((_, i) => i !== idx));

  const formatBytes = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const createAudit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const audit = await apiFetch('/api/audits', {
        method: 'POST',
        body: JSON.stringify({ ...formData }),
      });
      if (files.length > 0 && audit?.id) {
        for (const file of files) {
          const fd = new FormData();
          fd.append('file', file);
          await apiFetch(`/api/documents/upload?auditId=${audit.id}`, {
            method: 'POST', body: fd, isFormData: true,
          }).catch(err => toast.error(`"${file.name}" : ${err.message}`));
        }
      }
      toast.success(`✅ Demande soumise avec ${files.length} document(s) !`);
      setFormData({ title: '', description: '', deadline: '' });
      setFiles([]);
      setShowForm(false);
      loadAudits();
    } catch (e: any) { toast.error(e.message); }
    finally { setSubmitting(false); }
  };

  const completed = audits.filter(a => a.status === 'COMPLETED');
  const pending   = audits.filter(a => ['DRAFT','PENDING','IN_PROGRESS','AWAITING_DOCS'].includes(a.status));

  const stats = [
    { label: 'Total dossiers',    value: audits.length,   icon: FileText,    color: 'text-blue-400',   bg: 'bg-blue-500/10' },
    { label: 'En traitement',     value: pending.length,  icon: Clock,       color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
    { label: 'Docs manquants',    value: audits.filter(a => a.status === 'AWAITING_DOCS').length, icon: AlertCircle, color: 'text-orange-400', bg: 'bg-orange-500/10' },
    { label: 'Rapports certifiés',value: completed.length,icon: PackageCheck,color: 'text-green-400',  bg: 'bg-green-500/10' },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <div className="h-10 w-10 rounded-xl bg-green-500/10 flex items-center justify-center">
              <Shield className="h-5 w-5 text-green-400" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--foreground)]">
              Bonjour, {user?.fullName?.split(' ')[0] || 'Client'} 👋
            </h1>
          </div>
          <p className="text-[var(--muted-foreground)] text-sm">
            Dépôt sécurisé · Suivi en temps réel · Récupération de vos rapports certifiés
          </p>
        </div>
        <button onClick={() => setShowForm(v => !v)}
          className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-lg shadow-blue-500/20 hover:opacity-90 transition-all">
          {showForm ? <X className="h-4 w-4" /> : <Plus className="h-4 w-4" />}
          {showForm ? 'Annuler' : 'Nouvelle demande'}
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

      {/* === FORM === */}
      {showForm && (
        <div className="glass rounded-2xl p-6 space-y-5 border border-blue-500/20">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-xl bg-blue-500/10 flex items-center justify-center">
              <Send className="h-5 w-5 text-blue-400" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-[var(--foreground)]">Nouvelle demande d&apos;audit</h2>
              <p className="text-xs text-[var(--muted-foreground)]">Remplissez le formulaire et joignez vos documents financiers</p>
            </div>
          </div>

          <form onSubmit={createAudit} className="space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="audit-title" className="block text-xs font-medium text-[var(--muted-foreground)] mb-1.5">Titre de la mission *</label>
                <input id="audit-title" type="text" required placeholder="ex: Audit comptable exercice 2025"
                  value={formData.title}
                  onChange={e => setFormData({ ...formData, title: e.target.value })}
                  className="w-full bg-[var(--muted)] border border-[var(--border)] text-[var(--foreground)] rounded-xl px-3 py-2.5 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 transition-all" />
              </div>
              <div>
                <label htmlFor="audit-deadline" className="block text-xs font-medium text-[var(--muted-foreground)] mb-1.5">Échéance souhaitée</label>
                <div className="relative">
                  <DatePicker
                    id="audit-deadline"
                    selected={formData.deadline ? new Date(formData.deadline) : null}
                    onChange={(date: Date | null) => {
                      if (date) {
                        setFormData({ ...formData, deadline: `${date.toLocaleDateString('en-CA')}T23:59:59` });
                      } else {
                        setFormData({ ...formData, deadline: '' });
                      }
                    }}
                    minDate={new Date()}
                    dateFormat="dd/MM/yyyy"
                    placeholderText="🗓️ Sélectionner une date"
                    className="w-full bg-[var(--muted)] border border-[var(--border)] text-[var(--foreground)] rounded-xl px-3 py-2.5 text-sm outline-none focus:border-blue-500 transition-all cursor-pointer"
                    wrapperClassName="w-full"
                  />
                </div>
              </div>
            </div>

            <div>
              <label htmlFor="audit-description" className="block text-xs font-medium text-[var(--muted-foreground)] mb-1.5">Description & objectifs *</label>
              <textarea id="audit-description" required placeholder="Décrivez le périmètre, les exercices concernés, les problématiques…"
                value={formData.description}
                onChange={e => setFormData({ ...formData, description: e.target.value })}
                className="w-full bg-[var(--muted)] border border-[var(--border)] text-[var(--foreground)] rounded-xl px-3 py-2.5 text-sm outline-none focus:border-blue-500 transition-all resize-none h-24" />
            </div>

            {/* Upload zone */}
            <div>
              <label htmlFor="audit-file-upload" className="block text-xs font-medium text-[var(--muted-foreground)] mb-2">Documents financiers</label>
              <div className="mb-3 bg-orange-500/10 border border-orange-500/20 rounded-xl p-4">
                <div className="flex gap-3">
                  <AlertCircle className="h-5 w-5 text-orange-400 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="text-sm font-semibold text-orange-400 mb-1">Documents requis pour l&apos;analyse</p>
                    <p className="text-xs text-[var(--muted-foreground)]">
                      Bilan & Compte de résultat · Grand Livre · Balance Générale · Rapprochements bancaires
                    </p>
                    <a href="/templates/Formulaire_Dossier_Audit.doc" download
                      className="mt-2 text-xs inline-flex items-center gap-1.5 text-blue-400 hover:text-blue-300 underline">
                      <FileText className="h-3.5 w-3.5" /> Télécharger le formulaire modèle (Word)
                    </a>
                  </div>
                </div>
              </div>
              <div
                role="button"
                tabIndex={0}
                onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInputRef.current?.click(); } }}
                onDragOver={e => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleFileDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all ${
                  dragOver ? 'border-blue-500 bg-blue-500/10' : 'border-[var(--border)] hover:border-blue-500/50 hover:bg-[var(--muted)]/50'
                }`}>
                <Upload className="h-8 w-8 mx-auto mb-2 text-[var(--muted-foreground)]" />
                <p className="text-sm text-[var(--muted-foreground)]">
                  Glissez vos fichiers ici ou <span className="text-blue-400 font-medium">cliquez pour sélectionner</span>
                </p>
                <p className="text-xs text-[var(--muted-foreground)]/60 mt-1">PDF, Excel, Word, CSV acceptés</p>
                <input ref={fileInputRef} id="audit-file-upload" type="file" multiple accept=".pdf,.xlsx,.xls,.doc,.docx,.csv,.txt"
                  className="hidden" onChange={e => setFiles(prev => [...prev, ...Array.from(e.target.files || [])])} />
              </div>
              {files.length > 0 && (
                <div className="mt-3 space-y-2">
                  {files.map((f, i) => (
                    <div key={i} className="flex items-center gap-3 bg-[var(--muted)] rounded-xl px-3 py-2.5">
                      <Paperclip className="h-4 w-4 text-blue-400 flex-shrink-0" />
                      <span className="text-sm text-[var(--foreground)] flex-1 truncate">{f.name}</span>
                      <span className="text-xs text-[var(--muted-foreground)]">{formatBytes(f.size)}</span>
                      <button type="button" onClick={() => removeFile(i)}
                        className="text-[var(--muted-foreground)] hover:text-red-400 transition-colors">
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="flex items-center justify-between pt-2">
              <p className="text-xs text-[var(--muted-foreground)]">
                {files.length > 0 ? `${files.length} document(s) joint(s)` : 'Aucun document joint'}
              </p>
              <button type="submit" disabled={submitting}
                className="bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-6 py-2.5 rounded-xl text-sm font-medium hover:opacity-90 transition-all shadow-lg shadow-blue-500/20 disabled:opacity-50 flex items-center gap-2">
                {submitting ? (
                  <><span className="h-4 w-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Envoi…</>
                ) : (
                  <><Upload className="h-4 w-4" /> Soumettre</>
                )}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* === RAPPORTS CERTIFIÉS === */}
      {completed.length > 0 && (
        <div className="glass rounded-2xl overflow-hidden border border-green-500/20">
          <div className="px-6 py-4 border-b border-green-500/20 flex items-center gap-3">
            <PackageCheck className="h-5 w-5 text-green-400" />
            <h2 className="font-semibold text-green-400">Rapports Certifiés — Disponibles</h2>
            <span className="ml-auto text-xs bg-green-500/10 text-green-400 px-2.5 py-1 rounded-lg border border-green-500/20">
              {completed.length} rapport(s)
            </span>
          </div>
          <div className="divide-y divide-[var(--border)]/50">
            {completed.map(a => (
              <div key={a.id} className="px-6 py-5 hover:bg-green-500/5 transition-colors flex items-center gap-4 flex-wrap">
                <div className="h-10 w-10 rounded-xl bg-green-500/10 flex items-center justify-center flex-shrink-0">
                  <CheckCircle className="h-5 w-5 text-green-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-[var(--foreground)] truncate">{a.title}</p>
                  <p className="text-xs text-[var(--muted-foreground)] mt-0.5">
                    Auditeur : {a.auditorName || '—'} · Livré le {a.deadline ? new Date(a.deadline).toLocaleDateString('fr-FR') : '—'}
                  </p>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <Link href={`/audit/${a.id}`}
                    className="flex items-center gap-1.5 px-3 py-2 glass border border-[var(--border)] rounded-xl text-xs text-[var(--foreground)] hover:border-green-500 transition-all">
                    <ArrowRight className="h-3.5 w-3.5" /> Consulter
                  </Link>
                  <a href={`http://localhost:8000/report/${a.id}/download`} target="_blank" rel="noreferrer"
                    className="flex items-center gap-1.5 px-3 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-xl text-xs font-semibold hover:opacity-90 transition-all shadow-md shadow-green-500/20">
                    <Download className="h-3.5 w-3.5" /> Télécharger
                  </a>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* === DOSSIERS EN COURS === */}
      <div className="glass rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-[var(--border)] flex items-center justify-between">
          <h2 className="font-semibold text-[var(--foreground)]">Suivi de mes dossiers en traitement</h2>
          <span className="text-xs text-[var(--muted-foreground)]">{pending.length} en cours</span>
        </div>
        {loading ? (
          <div className="py-20 text-center text-[var(--muted-foreground)]">Chargement…</div>
        ) : audits.length === 0 ? (
          <div className="py-20 text-center space-y-3">
            <FileText className="h-12 w-12 mx-auto text-[var(--muted-foreground)]/40" />
            <p className="text-[var(--muted-foreground)]">Aucun audit soumis pour le moment</p>
            <button onClick={() => setShowForm(true)} className="text-blue-400 text-sm hover:underline">
              Soumettre votre première demande →
            </button>
          </div>
        ) : pending.length === 0 ? (
          <div className="px-6 py-8 text-center text-[var(--muted-foreground)] text-sm">
            Tous vos dossiers ont été traités ✅
          </div>
        ) : (
          <div className="divide-y divide-[var(--border)]/50">
            {pending.map(a => (
              <div key={a.id} className="px-6 py-5 hover:bg-[var(--muted)]/20 transition-colors">
                <div className="flex items-start justify-between gap-4 flex-wrap">
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-[var(--foreground)] truncate">{a.title}</p>
                    <p className="text-xs text-[var(--muted-foreground)] mt-0.5">
                      Auditeur : {a.auditorName ?? 'En attente d\'assignation'} ·{' '}
                      {a.deadline ? new Date(a.deadline).toLocaleDateString('fr-FR') : 'Pas d\'échéance'}
                    </p>
                    {a.status === 'AWAITING_DOCS' && (
                      <p className="text-xs text-orange-400 mt-1 flex items-center gap-1">
                        <AlertCircle className="h-3 w-3" /> Des documents supplémentaires sont requis
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-2 flex-shrink-0">
                    <span className={`px-2.5 py-1 rounded-lg text-xs font-medium ${STATUS_COLORS[a.status] || ''}`}>
                      {STATUS_LABELS[a.status] || a.status}
                    </span>
                    <Link href={`/audit/${a.id}`}
                      className="flex items-center gap-1 text-blue-400 hover:text-blue-300 text-xs font-medium glass px-3 py-1.5 rounded-lg transition-all">
                      Détails <ArrowRight className="h-3 w-3" />
                    </Link>
                    <Link href="/chat"
                      className="flex items-center gap-1 text-[var(--muted-foreground)] hover:text-[var(--foreground)] text-xs glass px-3 py-1.5 rounded-lg transition-all">
                      <MessageSquare className="h-3.5 w-3.5" />
                    </Link>
                  </div>
                </div>
                {/* Visual step tracker */}
                <AuditStepTracker status={a.status} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
