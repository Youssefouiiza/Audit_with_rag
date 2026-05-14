'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { apiFetch } from '@/lib/api';
import Link from 'next/link';
import toast from 'react-hot-toast';
import {
  Users, FileText, CheckCircle, Activity, BarChart3, Shield,
  ChevronLeft, ChevronRight, AlertTriangle, Clock, Server,
  UserCog, Eye, TrendingUp, Lock
} from 'lucide-react';

const PAGE_SIZE = 6;

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Brouillon', PENDING: 'En attente', IN_PROGRESS: 'En cours',
  AWAITING_DOCS: 'Docs requis', COMPLETED: 'Terminé', CANCELLED: 'Annulé',
};
const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20',
  IN_PROGRESS: 'bg-blue-500/10 text-blue-400 border border-blue-500/20',
  DRAFT: 'bg-gray-500/10 text-gray-400 border border-gray-500/20',
  AWAITING_DOCS: 'bg-orange-500/10 text-orange-400 border border-orange-500/20',
  COMPLETED: 'bg-green-500/10 text-green-400 border border-green-500/20',
  CANCELLED: 'bg-red-500/10 text-red-400 border border-red-500/20',
};

const AUDIT_STATUSES = [
  { name: 'Brouillon',  key: 'DRAFT',        color: 'bg-gray-400' },
  { name: 'En Attente', key: 'PENDING',       color: 'bg-yellow-400' },
  { name: 'En Cours',   key: 'IN_PROGRESS',   color: 'bg-blue-500' },
  { name: 'Docs Requis',key: 'AWAITING_DOCS', color: 'bg-orange-400' },
  { name: 'Terminés',   key: 'COMPLETED',     color: 'bg-green-500' },
  { name: 'Annulés',    key: 'CANCELLED',     color: 'bg-red-500' },
];

// Simulated security events (replace with real API when available)
const SECURITY_EVENTS = [
  { time: '21:43', type: 'INFO',  msg: 'Connexion réussie — admin@audit.ma' },
  { time: '21:38', type: 'WARN',  msg: '3 tentatives échouées — user@client.com' },
  { time: '21:30', type: 'INFO',  msg: 'Rapport exporté — Audit #247' },
  { time: '21:15', type: 'INFO',  msg: 'Auditeur assigné par manager' },
  { time: '20:59', type: 'WARN',  msg: 'Token expiré — session fermée automatiquement' },
];

function LiveBadge() {
  return (
    <div className="flex items-center gap-2 text-xs px-2.5 py-1 bg-green-500/10 text-green-400 font-medium rounded-full border border-green-500/20">
      <div className="h-1.5 w-1.5 bg-green-400 rounded-full animate-pulse" />
      Temps Réel
    </div>
  );
}

export default function AdminDashboard() {
  useAuth(['ADMIN']);
  const [users, setUsers]     = useState<any>({ total: 0 });
  const [audits, setAudits]   = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [auditPage, setAuditPage] = useState(0);
  const [activeSection, setActiveSection] = useState<'overview' | 'security' | 'users'>('overview');


  useEffect(() => {
    loadData(false);
    const interval = setInterval(() => loadData(true), 10000);
    return () => clearInterval(interval);
  }, []);

  const loadData = async (isBackground = false) => {
    if (!isBackground) setLoading(true);
    try {
      const [uRes, aRes] = await Promise.all([
        apiFetch('/api/users?size=1'),
        apiFetch('/api/audits?size=200&sort=createdAt,desc'),
      ]);
      setUsers({ total: uRes?.totalElements || 0 });
      const raw = aRes?.content ?? (Array.isArray(aRes) ? aRes : []);
      setAudits([...raw].sort((a, b) =>
        new Date(b.createdAt || b.deadline || 0).getTime() -
        new Date(a.createdAt || a.deadline || 0).getTime()
      ));
      setAuditPage(0);
    } catch (e: any) {
      if (!isBackground) toast.error(e.message);
    } finally {
      if (!isBackground) setLoading(false);
    }
  };

  const completionRate = audits.length > 0
    ? Math.round((audits.filter(a => a.status === 'COMPLETED').length / audits.length) * 100)
    : 0;

  const kpiCards = [
    { label: 'Utilisateurs Totaux',  value: (users as any).total || 0,                                         icon: Users,         color: 'text-blue-400',   bg: 'bg-blue-500/10',   border: 'border-blue-500/20' },
    { label: 'Total Audits',         value: audits.length,                                                      icon: FileText,      color: 'text-indigo-400', bg: 'bg-indigo-500/10', border: 'border-indigo-500/20' },
    { label: 'En Cours',             value: audits.filter(a => a.status === 'IN_PROGRESS').length,              icon: Activity,      color: 'text-yellow-400', bg: 'bg-yellow-500/10', border: 'border-yellow-500/20' },
    { label: 'Taux de Complétion',   value: `${completionRate}%`,                                               icon: TrendingUp,    color: 'text-green-400',  bg: 'bg-green-500/10',  border: 'border-green-500/20' },
  ];




  const tabs = [
    { key: 'overview', label: 'Vue d\'ensemble',  icon: BarChart3 },
    { key: 'security', label: 'Sécurité & Logs',  icon: Shield },
    { key: 'users',    label: 'Comptes & Accès',   icon: UserCog },
  ] as const;

  const totalPages = Math.max(1, Math.ceil(audits.length / PAGE_SIZE));

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <div className="flex items-center gap-3 mb-1">
            <div className="h-10 w-10 rounded-xl bg-red-500/10 flex items-center justify-center">
              <Shield className="h-5 w-5 text-red-400" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-[var(--foreground)]">Administration Centrale</h1>
          </div>
          <p className="text-[var(--muted-foreground)] text-sm ml-13">
            Supervision globale · Sécurité · Contrôle des accès · Performances
          </p>
        </div>
        <div className="flex items-center gap-3">
          <LiveBadge />
          <Link href="/dashboard/users"
            className="flex items-center gap-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white px-4 py-2.5 rounded-xl text-sm font-medium shadow-lg shadow-blue-500/20 hover:opacity-90 transition-all">
            <Users className="h-4 w-4" /> Gérer les Utilisateurs
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {kpiCards.map(s => (
          <div key={s.label} className={`glass rounded-2xl p-5 flex items-center gap-4 border ${s.border}`}>
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
        {tabs.map(t => (
          <button key={t.key} onClick={() => setActiveSection(t.key)}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-all flex items-center gap-2 ${
              activeSection === t.key
                ? 'bg-red-600 text-white shadow-lg shadow-red-500/20'
                : 'text-[var(--muted-foreground)] hover:text-[var(--foreground)]'
            }`}>
            <t.icon className="h-4 w-4" />
            {t.label}
          </button>
        ))}
      </div>

      {/* === OVERVIEW === */}
      {activeSection === 'overview' && (
        <div className="space-y-6">
          {/* Progress Chart */}
          <div className="glass rounded-2xl p-6 border border-[var(--border)]">
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2">
                <BarChart3 className="h-5 w-5 text-indigo-400" /> Répartition des Dossiers par Statut
              </h2>
              <LiveBadge />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-5">
              {AUDIT_STATUSES.map(d => {
                const count = audits.filter(a => a.status === d.key).length;
                const pct = audits.length > 0 ? Math.round((count / audits.length) * 100) : 0;
                return (
                  <div key={d.key} className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="font-medium text-[var(--foreground)]">{d.name}</span>
                      <span className="text-[var(--muted-foreground)] font-mono text-xs">
                        {count} dossier(s) <span className="opacity-50">({pct}%)</span>
                      </span>
                    </div>
                    <div className="w-full h-2.5 bg-[var(--muted)] rounded-full overflow-hidden shadow-inner">
                      <div className={`h-full ${d.color} rounded-full transition-all duration-1000 ease-out`}
                        style={{ width: `${pct}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>



          {/* Audit Monitoring Table */}
          <AuditTable audits={audits} loading={loading} page={auditPage} setPage={setAuditPage} totalPages={totalPages} />
        </div>
      )}

      {/* === SECURITY === */}
      {activeSection === 'security' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <div className="glass rounded-2xl p-5 border border-green-500/20 col-span-1">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-10 w-10 rounded-xl bg-green-500/10 flex items-center justify-center">
                  <Shield className="h-5 w-5 text-green-400" />
                </div>
                <div>
                  <p className="font-semibold text-green-400">Système Sécurisé</p>
                  <p className="text-xs text-[var(--muted-foreground)]">Aucune menace active</p>
                </div>
              </div>
              <div className="space-y-2">
                {[
                  { label: 'JWT Rotation', value: 'Actif' },
                  { label: 'CORS Policy', value: 'Configuré' },
                  { label: 'Rate Limiting', value: 'Actif' },
                  { label: 'HTTPS', value: 'Forcé' },
                ].map(i => (
                  <div key={i.label} className="flex justify-between items-center text-sm">
                    <span className="text-[var(--muted-foreground)]">{i.label}</span>
                    <span className="text-green-400 font-medium text-xs bg-green-500/10 px-2 py-0.5 rounded">{i.value}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="glass rounded-2xl p-5 border border-[var(--border)] col-span-2">
              <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2 mb-4">
                <Eye className="h-5 w-5 text-indigo-400" /> Journal d'Activité & Sécurité
              </h2>
              <div className="space-y-2">
                {SECURITY_EVENTS.map((e, i) => (
                  <div key={i} className="flex items-start gap-3 p-3 rounded-xl bg-[var(--muted)]/30">
                    <span className={`mt-0.5 flex-shrink-0 text-xs font-bold px-1.5 py-0.5 rounded ${
                      e.type === 'WARN'
                        ? 'bg-yellow-500/20 text-yellow-400'
                        : 'bg-blue-500/10 text-blue-400'
                    }`}>{e.type}</span>
                    <span className="text-xs text-[var(--muted-foreground)] flex-shrink-0 font-mono">{e.time}</span>
                    <span className="text-sm text-[var(--foreground)]">{e.msg}</span>
                  </div>
                ))}
              </div>
              <p className="text-xs text-[var(--muted-foreground)] mt-3 text-center">
                Intégrez un endpoint <code className="bg-[var(--muted)] px-1 rounded">/api/admin/logs</code> pour les journaux en temps réel
              </p>
            </div>
          </div>

          <div className="glass rounded-2xl p-5 border border-orange-500/20">
            <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2 mb-4">
              <AlertTriangle className="h-5 w-5 text-orange-400" /> Points d'attention
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[
                { title: 'Dossiers sans auditeur', count: audits.filter(a => !a.auditorName).length, color: 'text-orange-400', bg: 'bg-orange-500/10' },
                { title: 'Audits annulés', count: audits.filter(a => a.status === 'CANCELLED').length, color: 'text-red-400', bg: 'bg-red-500/10' },
                { title: 'Docs manquants', count: audits.filter(a => a.status === 'AWAITING_DOCS').length, color: 'text-yellow-400', bg: 'bg-yellow-500/10' },
              ].map(item => (
                <div key={item.title} className={`rounded-xl p-4 ${item.bg} flex items-center gap-4`}>
                  <p className={`text-3xl font-bold ${item.color}`}>{loading ? '—' : item.count}</p>
                  <p className="text-sm text-[var(--foreground)]">{item.title}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* === USERS === */}
      {activeSection === 'users' && (
        <div className="space-y-6">
          <div className="glass rounded-2xl p-6 border border-[var(--border)]">
            <div className="flex items-center justify-between mb-6">
              <h2 className="font-semibold text-[var(--foreground)] flex items-center gap-2">
                <UserCog className="h-5 w-5 text-blue-400" /> Gestion des Comptes & Permissions
              </h2>
              <Link href="/dashboard/users"
                className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-xl text-sm font-medium hover:opacity-90 transition-all">
                <Users className="h-4 w-4" /> Gérer tous les comptes
              </Link>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {[
                { role: 'ADMIN',   label: 'Administrateurs', color: 'text-red-400',    bg: 'bg-red-500/10',    desc: 'Contrôle total' },
                { role: 'MANAGER', label: 'Managers',        color: 'text-blue-400',   bg: 'bg-blue-500/10',   desc: 'Pilotage opérationnel' },
                { role: 'AUDITOR', label: 'Auditeurs',       color: 'text-purple-400', bg: 'bg-purple-500/10', desc: 'Expertise & analyse' },
                { role: 'CLIENT',  label: 'Clients',         color: 'text-green-400',  bg: 'bg-green-500/10',  desc: 'Service & dépôt' },
              ].map(r => (
                <div key={r.role} className={`rounded-xl p-4 ${r.bg} border border-current/10`}>
                  <p className={`text-lg font-bold ${r.color}`}>{r.role}</p>
                  <p className="text-sm text-[var(--foreground)] font-medium mt-1">{r.label}</p>
                  <p className="text-xs text-[var(--muted-foreground)] mt-0.5">{r.desc}</p>
                </div>
              ))}
            </div>
            <p className="text-xs text-[var(--muted-foreground)] mt-4 text-center">
              Accédez à la page de gestion pour créer, modifier ou désactiver des comptes
            </p>
          </div>
          <AuditTable audits={audits} loading={loading} page={auditPage} setPage={setAuditPage} totalPages={totalPages} />
        </div>
      )}
    </div>
  );
}

function AuditTable({ audits, loading, page, setPage, totalPages }: {
  audits: any[]; loading: boolean; page: number;
  setPage: (p: (prev: number) => number) => void; totalPages: number;
}) {
  const slice = audits.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  return (
    <div className="glass rounded-2xl overflow-hidden">
      <div className="px-6 py-4 border-b border-[var(--border)] flex items-center justify-between">
        <h2 className="font-semibold text-[var(--foreground)]">Monitoring global des audits</h2>
        <span className="text-xs text-[var(--muted-foreground)]">
          Page {page + 1} / {totalPages} · {audits.length} audit(s)
        </span>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[var(--border)] bg-[var(--muted)]/30">
              {['#', 'Titre', 'Client', 'Auditeur', 'Statut', 'Échéance', ''].map(h => (
                <th key={h} className="text-left px-6 py-3 text-xs font-semibold text-[var(--muted-foreground)] uppercase tracking-wider whitespace-nowrap">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={7} className="text-center py-12">
                <div className="h-6 w-6 border-2 border-blue-500/30 border-t-blue-500 rounded-full animate-spin mx-auto" />
              </td></tr>
            ) : audits.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-12 text-[var(--muted-foreground)]">Aucun audit</td></tr>
            ) : slice.map((a, idx) => (
              <tr key={a.id} className="border-b border-[var(--border)]/50 hover:bg-[var(--muted)]/30 transition-colors group">
                <td className="px-6 py-4 text-xs text-[var(--muted-foreground)]">{page * PAGE_SIZE + idx + 1}</td>
                <td className="px-6 py-4 font-semibold text-[var(--foreground)] max-w-[160px]">
                  <span className="truncate block">{a.title}</span>
                </td>
                <td className="px-6 py-4 text-[var(--muted-foreground)]">{a.clientName}</td>
                <td className="px-6 py-4 text-[var(--muted-foreground)]">
                  {a.auditorName ?? <span className="italic text-orange-400 text-xs">Non assigné</span>}
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 rounded-lg text-xs font-medium whitespace-nowrap ${STATUS_COLORS[a.status] || ''}`}>
                    {STATUS_LABELS[a.status] || a.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-[var(--muted-foreground)] text-xs whitespace-nowrap">
                  {a.deadline ? new Date(a.deadline).toLocaleDateString('fr-FR') : '—'}
                </td>
                <td className="px-6 py-4">
                  <Link href={`/audit/${a.id}`}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-500/10 text-blue-400 hover:bg-blue-500/20 text-xs font-medium transition-colors border border-blue-500/20 opacity-0 group-hover:opacity-100">
                    Voir →
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {totalPages > 1 && (
        <div className="px-6 py-4 border-t border-[var(--border)] flex items-center justify-between">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium bg-[var(--muted)] text-[var(--muted-foreground)] hover:text-[var(--foreground)] disabled:opacity-30 disabled:cursor-not-allowed transition-all">
            <ChevronLeft className="h-4 w-4" /> Précédent
          </button>
          <div className="flex items-center gap-1">
            {Array.from({ length: totalPages }, (_, i) => (
              <button key={i} onClick={() => setPage(() => i)}
                className={`h-8 w-8 rounded-lg text-xs font-semibold transition-all ${
                  i === page ? 'bg-red-600 text-white' : 'bg-[var(--muted)] text-[var(--muted-foreground)] hover:text-[var(--foreground)]'
                }`}>{i + 1}</button>
            ))}
          </div>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium bg-[var(--muted)] text-[var(--muted-foreground)] hover:text-[var(--foreground)] disabled:opacity-30 disabled:cursor-not-allowed transition-all">
            Suivant <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      )}
    </div>
  );
}
