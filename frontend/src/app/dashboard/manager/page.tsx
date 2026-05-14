'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { apiFetch } from '@/lib/api';
import Link from 'next/link';
import toast from 'react-hot-toast';
import {
  FileText, Clock, CheckCircle, UserCheck,
  ArrowRight, AlertCircle, RefreshCw, Target, Award, TrendingUp
} from 'lucide-react';

const STATUS_COLORS: Record<string, string> = {
  DRAFT: 'bg-gray-500/10 text-gray-400 border border-gray-500/20',
  PENDING: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20',
  IN_PROGRESS: 'bg-blue-500/10 text-blue-400 border border-blue-500/20',
  AWAITING_DOCS: 'bg-orange-500/10 text-orange-400 border border-orange-500/20',
  COMPLETED: 'bg-green-500/10 text-green-400 border border-green-500/20',
  CANCELLED: 'bg-red-500/10 text-red-400 border border-red-500/20',
};
const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon', PENDING: 'En attente', IN_PROGRESS: 'En cours',
  AWAITING_DOCS: 'Docs manquants', COMPLETED: 'Terminé', CANCELLED: 'Annulé',
};

const PROGRESS_STEPS = [
  { key: 'DRAFT', label: 'Reçu', step: 1 },
  { key: 'IN_PROGRESS', label: 'En cours', step: 2 },
  { key: 'PENDING', label: 'Révision', step: 3 },
  { key: 'COMPLETED', label: 'Livré', step: 4 },
];



export default function ManagerDashboard() {
  useAuth(['MANAGER', 'ADMIN']);
  const [audits, setAudits] = useState<any[]>([]);
  const [auditors, setAuditors] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'attribution' | 'suivi' | 'validation'>('attribution');
  const [assigning, setAssigning] = useState<string | null>(null);
  const [changingStatus, setChangingStatus] = useState<string | null>(null);

  useEffect(() => { loadData(); }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [aRes, audRes] = await Promise.all([
        apiFetch('/api/audits?size=100'),
        apiFetch('/api/users?role=AUDITOR&size=100'),
      ]);
      setAudits(aRes?.content ?? (Array.isArray(aRes) ? aRes : []));
      setAuditors(audRes?.content ?? (Array.isArray(audRes) ? audRes : []));
    } catch (e: any) { toast.error(e.message); }
    finally { setLoading(false); }
  };

  const assignAuditor = async (auditId: string, auditorId: string) => {
    if (!auditorId) return;
    setAssigning(auditId);
    try {
      await apiFetch(`/api/audits/${auditId}/assign`, { method: 'POST', body: JSON.stringify({ auditorId }) });
      toast.success('Auditeur assigné avec succès');
      loadData();
    } catch (e: any) { toast.error(e.message); }
    finally { setAssigning(null); }
  };

  const validateAudit = async (auditId: string, newStatus: string) => {
    setChangingStatus(auditId);
    try {
      await apiFetch(`/api/audits/${auditId}/status/${newStatus}`, { method: 'PATCH' });
      toast.success(newStatus === 'COMPLETED' ? '✅ Rapport validé et livré au client !' : 'Statut mis à jour');
      loadData();
    } catch (e: any) { toast.error(e.message); }
    finally { setChangingStatus(null); }
  };

  const unassigned = audits.filter(a => !a.auditorName);
  const inProgress  = audits.filter(a => a.status === 'IN_PROGRESS');
  const pending     = audits.filter(a => a.status === 'PENDING');
  const completed   = audits.filter(a => a.status === 'COMPLETED');



  const globalCompletionRate = audits.length > 0
    ? Math.round((completed.length / audits.length) * 100) : 0;

  const kpi = [
    { label: 'Total Dossiers',    value: audits.length,      icon: FileText,    color: 'text-blue-400',   bg: 'bg-blue-500/10' },
    { label: 'Non Assignés',      value: unassigned.length,  icon: AlertCircle, color: 'text-orange-400', bg: 'bg-orange-500/10' },
    { label: 'En révision',       value: pending.length,     icon: Clock,       color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
    { label: 'Taux de livraison', value: `${globalCompletionRate}%`, icon: TrendingUp, color: 'text-green-400', bg: 'bg-green-500/10' },
  ];

  const tabs = [
    { key: 'attribution', label: 'Attribution', icon: UserCheck },
    { key: 'validation',  label: 'Validation Qualité', icon: CheckCircle },
  ] as const;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <div className="h-10 w-10 rounded-xl bg-blue-500/10 flex items-center justify-center">
              <Target className="h-5 w-5 text-blue-400" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--foreground)]">Centre de Pilotage Manager</h1>
          </div>
          <p className="text-[var(--muted-foreground)] text-sm">
            Attribution · Suivi performance · Validation qualité des rapports
          </p>
        </div>
        <button onClick={loadData}
          className="flex items-center gap-2 text-[var(--muted-foreground)] hover:text-[var(--foreground)] text-sm glass px-3 py-2 rounded-xl transition-all">
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} /> Actualiser
        </button>
      </div>

      {/* KPIs */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {kpi.map(s => (
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
      <div className="flex gap-1 glass rounded-xl p-1 w-fit flex-wrap">
        {tabs.map(t => (
          <button key={t.key} onClick={() => setActiveTab(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
              activeTab === t.key
                ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)]'
            }`}>
            <t.icon className="h-4 w-4" /> {t.label}
          </button>
        ))}
      </div>

      {/* === ATTRIBUTION === */}
      {activeTab === 'attribution' && (
        <div className="glass rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-[var(--border)] flex items-center justify-between">
            <div>
              <h2 className="font-semibold text-[var(--foreground)]">Attribution Intelligente des Dossiers</h2>
              <p className="text-xs text-[var(--muted-foreground)] mt-0.5">Assignez un auditeur expert à chaque mission client</p>
            </div>
            {unassigned.length > 0 && (
              <span className="text-xs text-orange-400 bg-orange-500/10 border border-orange-500/20 px-2.5 py-1 rounded-lg animate-pulse">
                ⚠ {unassigned.length} dossier(s) sans auditeur
              </span>
            )}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-[var(--border)] bg-[var(--muted)]/30">
                  {['Dossier', 'Client', 'Statut', 'Auditeur actuel', 'Affecter / Réaffecter', 'Actions'].map(h => (
                    <th key={h} className="text-left px-6 py-3 text-xs font-semibold text-[var(--muted-foreground)] uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan={6} className="text-center py-12 text-[var(--muted-foreground)]">Chargement...</td></tr>
                ) : audits.length === 0 ? (
                  <tr><td colSpan={6} className="text-center py-12 text-[var(--muted-foreground)]">Aucun audit</td></tr>
                ) : audits.map(a => (
                  <tr key={a.id} className={`border-b border-[var(--border)]/50 hover:bg-[var(--muted)]/30 transition-colors ${!a.auditorName ? 'bg-orange-500/5' : ''}`}>
                    <td className="px-6 py-4">
                      <p className="font-medium text-[var(--foreground)]">{a.title}</p>
                      <p className="text-xs text-[var(--muted-foreground)] mt-0.5">
                        {a.deadline ? new Date(a.deadline).toLocaleDateString('fr-FR') : 'Sans échéance'}
                      </p>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="h-7 w-7 rounded-full bg-blue-500/20 flex items-center justify-center text-xs font-bold text-blue-400">
                          {a.clientName?.[0] || '?'}
                        </div>
                        <span className="text-[var(--foreground)] text-sm">{a.clientName}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-0.5 rounded-lg text-xs font-medium ${STATUS_COLORS[a.status] || ''}`}>
                        {STATUS_LABELS[a.status] || a.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {a.auditorName ? (
                        <div className="flex items-center gap-2">
                          <div className="h-7 w-7 rounded-full bg-green-500/20 flex items-center justify-center text-xs font-bold text-green-400">
                            {a.auditorName[0]}
                          </div>
                          <span className="text-[var(--foreground)] text-sm">{a.auditorName}</span>
                        </div>
                      ) : (
                        <span className="text-orange-400 text-xs flex items-center gap-1">
                          <AlertCircle className="h-3 w-3" /> Non assigné
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <select defaultValue="" onChange={e => assignAuditor(a.id, e.target.value)}
                          disabled={assigning === a.id}
                          className="bg-[var(--muted)] border border-[var(--border)] text-[var(--foreground)] rounded-xl px-3 py-1.5 text-xs outline-none focus:border-blue-500 transition-all min-w-[140px]">
                          <option value="">{a.auditorName ? 'Réaffecter…' : 'Choisir…'}</option>
                          {auditors.map(aud => (
                            <option key={aud.id} value={aud.id}>{aud.fullName}</option>
                          ))}
                        </select>
                        {assigning === a.id && <span className="h-4 w-4 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin" />}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Link href={`/audit/${a.id}`} className="text-blue-400 hover:text-blue-300 text-xs font-medium hover:underline inline-flex items-center gap-1">
                        Ouvrir <ArrowRight className="h-3 w-3" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}



      {/* === VALIDATION QUALITÉ === */}
      {activeTab === 'validation' && (
        <div className="space-y-5">
          <div className="glass rounded-2xl p-5 border border-yellow-500/20">
            <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2 mb-1">
              <CheckCircle className="h-5 w-5 text-yellow-400" /> Rapports en attente de validation qualité
            </h2>
            <p className="text-xs text-[var(--muted-foreground)] mb-5">
              Ces dossiers sont en statut &quot;En révision&quot; — validez ou renvoyez-les en correction avant livraison officielle au client.
            </p>
            {pending.length === 0 ? (
              <div className="py-10 text-center space-y-3">
                <CheckCircle className="h-12 w-12 mx-auto text-green-400/30" />
                <p className="text-[var(--muted-foreground)]">Aucun rapport en attente de validation</p>
                <p className="text-xs text-[var(--muted-foreground)]/60">Tous les rapports ont été validés ou sont encore en cours d&apos;analyse</p>
              </div>
            ) : (
              <div className="space-y-3">
                {pending.map(a => (
                  <div key={a.id} className="glass rounded-xl p-5 border border-[var(--border)] flex items-center gap-4 flex-wrap">
                    <div className="flex-1 min-w-0">
                      <p className="font-semibold text-[var(--foreground)]">{a.title}</p>
                      <p className="text-xs text-[var(--muted-foreground)] mt-0.5">
                        Client: {a.clientName} · Auditeur: {a.auditorName || 'Non assigné'}
                      </p>
                      {a.deadline && (
                        <p className="text-xs text-orange-400 mt-0.5">
                          ⏰ Échéance : {new Date(a.deadline).toLocaleDateString('fr-FR')}
                        </p>
                      )}
                    </div>
                    <div className="flex items-center gap-2 flex-shrink-0 flex-wrap">
                      <Link href={`/audit/${a.id}`}
                        className="flex items-center gap-1.5 px-3 py-2 glass border border-[var(--border)] rounded-xl text-xs text-[var(--foreground)] hover:border-blue-500 transition-all">
                        <ArrowRight className="h-3.5 w-3.5" /> Consulter le rapport
                      </Link>
                      <button
                        onClick={() => validateAudit(a.id, 'IN_PROGRESS')}
                        disabled={changingStatus === a.id}
                        className="px-3 py-2 rounded-xl text-xs font-medium bg-orange-500/10 text-orange-400 border border-orange-500/20 hover:bg-orange-500/20 transition-all disabled:opacity-50">
                        ↩ Renvoyer en correction
                      </button>
                      <button
                        onClick={() => validateAudit(a.id, 'COMPLETED')}
                        disabled={changingStatus === a.id}
                        className="px-4 py-2 rounded-xl text-xs font-semibold bg-gradient-to-r from-green-600 to-emerald-600 text-white hover:opacity-90 transition-all shadow-lg shadow-green-500/20 disabled:opacity-50 flex items-center gap-1.5">
                        {changingStatus === a.id
                          ? <span className="h-3.5 w-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                          : <CheckCircle className="h-3.5 w-3.5" />
                        }
                        Valider & Livrer
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Recently validated */}
          {completed.length > 0 && (
            <div className="glass rounded-2xl p-5 border border-green-500/20">
              <h2 className="font-semibold text-green-400 flex items-center gap-2 mb-3">
                <Award className="h-5 w-5" /> Rapports validés et livrés ({completed.length})
              </h2>
              <div className="space-y-2">
                {completed.slice(0, 5).map(a => (
                  <div key={a.id} className="flex items-center justify-between gap-3 px-4 py-3 bg-green-500/5 rounded-xl border border-green-500/10">
                    <div>
                      <p className="text-sm font-medium text-[var(--foreground)]">{a.title}</p>
                      <p className="text-xs text-[var(--muted-foreground)]">Client: {a.clientName} · Auditeur: {a.auditorName}</p>
                    </div>
                    <span className="text-xs font-medium text-green-400 bg-green-500/10 px-2.5 py-1 rounded-lg border border-green-500/20">
                      ✓ Livré
                    </span>
                  </div>
                ))}
                {completed.length > 5 && (
                  <p className="text-xs text-center text-[var(--muted-foreground)] pt-1">
                    +{completed.length - 5} autres rapports livrés
                  </p>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
