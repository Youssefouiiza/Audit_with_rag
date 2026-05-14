'use client';

import Link from 'next/link';
import { 
  ArrowRight, Database, Globe,
  Shield, User
} from 'lucide-react';

const zelligePattern = `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0l30 30-30 30L0 30z' fill='none' stroke='%23ffffff' stroke-width='1' stroke-opacity='0.2'/%3E%3Ccircle cx='30' cy='30' r='3' fill='%23ffffff' fill-opacity='0.2'/%3E%3Cpath d='M15 15l30 30M15 45l30-30' stroke='%23ffffff' stroke-width='0.5' stroke-opacity='0.1'/%3E%3C/svg%3E")`;

export default function AuditProHomepage() {
  return (
    <div className="min-h-screen bg-[#0a111a] text-slate-100 font-sans selection:bg-[#185FA5]/30 overflow-x-hidden relative flex flex-col">
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes fadeUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes float {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-10px); }
        }
        @keyframes pulse-dot {
          0% { box-shadow: 0 0 0 0 rgba(29, 158, 117, 0.7); }
          70% { box-shadow: 0 0 0 10px rgba(29, 158, 117, 0); }
          100% { box-shadow: 0 0 0 0 rgba(29, 158, 117, 0); }
        }
        .animate-fade-up { animation: fadeUp 0.6s ease-out forwards; opacity: 0; }
        .animate-float { animation: float 6s ease-in-out infinite; }
        .animate-float-delay-1 { animation: float 6s ease-in-out 1s infinite; }
        .animate-float-delay-2 { animation: float 6s ease-in-out 2s infinite; }
        .animate-pulse-dot { animation: pulse-dot 2s infinite; }
      `}} />

      {/* Zellige Top Accent Bar */}
      <div className="h-1.5 w-full flex fixed top-0 z-50">
        <div className="flex-1 bg-[#185FA5]" />
        <div className="flex-1 bg-[#BA7517]" />
        <div className="flex-1 bg-[#1D9E75]" />
      </div>

      {/* Sticky Navigation */}
      <nav className="fixed top-1.5 inset-x-0 z-40 bg-[#0a111a]/80 backdrop-blur-md border-b border-white/5">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xl font-bold tracking-tight text-white">
              AuditPro <span className="text-[#185FA5]">AI</span>
            </span>
          </div>
          <div className="flex items-center gap-5">
            <Link href="/login" className="flex items-center gap-2 text-slate-300 hover:text-white transition-colors">
              <User className="h-5 w-5" />
              <span className="text-sm font-medium">Sign In</span>
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="relative pt-32 pb-16 overflow-hidden flex-1 flex items-center">
        {/* Hero Background Image */}
        <div className="absolute inset-0 z-0">
          <img
            src="/hero-bg.jpg"
            alt=""
            aria-hidden="true"
            className="w-full h-full object-cover object-right-top"
          />
          {/* Dark gradient overlay — heavy on left (text side), lighter on right (image side) */}
          <div className="absolute inset-0 bg-gradient-to-r from-[#0a111a] via-[#0a111a]/88 to-[#0a111a]/60" />
          {/* Bottom fade to match page background */}
          <div className="absolute inset-x-0 bottom-0 h-32 bg-gradient-to-t from-[#0a111a] to-transparent" />
        </div>

        {/* Zellige Watermark */}
        <div className="absolute inset-0 z-[1] pointer-events-none" style={{ backgroundImage: zelligePattern, opacity: 0.03 }} />
        
        <div className="max-w-7xl mx-auto px-6 relative z-10 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center w-full">
          
          {/* Hero Left */}
          <div className="animate-fade-up">
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-emerald-900/30 border border-[#1D9E75]/30 mb-6">
              <div className="w-2 h-2 rounded-full bg-[#1D9E75] animate-pulse-dot" />
              <span className="text-xs font-medium text-[#1D9E75] tracking-wide">Plateforme d'audit intelligente · Maroc & International</span>
            </div>
            
            <h1 className="text-5xl lg:text-6xl font-bold text-white tracking-tight leading-[1.1] mb-6">
              L'audit d'entreprise,<br/>
              propulsé par <span className="text-[#185FA5]">l'intelligence artificielle</span>
            </h1>
            
            <p className="text-lg text-slate-400 mb-8 max-w-xl leading-relaxed">
              Sécurisez la conformité CGNC et IFRS de vos clients. Analysez des milliers de documents en quelques secondes grâce à notre modèle IA multilingue (Arabe, Français, Anglais).
            </p>
            
            <div className="flex flex-wrap items-center gap-4 mb-10">
              <Link href="/dashboard" className="px-6 py-3 rounded-lg bg-[#185FA5] hover:bg-[#185FA5]/90 text-white font-medium transition-colors flex items-center gap-2">
                Explore Now <ArrowRight className="h-4 w-4" />
              </Link>
            </div>

            <div className="flex items-center gap-3">
              <span className="px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs text-slate-300 flex items-center gap-1.5"><Database className="h-3 w-3 text-[#1D9E75]"/> RAG Online</span>
              <span className="px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs text-slate-300 flex items-center gap-1.5"><Globe className="h-3 w-3 text-[#185FA5]"/> Ar/Fr/En</span>
              <span className="px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs text-slate-300 flex items-center gap-1.5"><Shield className="h-3 w-3 text-[#BA7517]"/> CGNC / IFRS</span>
            </div>
          </div>

          {/* Hero Right: Floating Cards */}
          <div className="relative h-[500px] hidden lg:block">
            {/* Person Cards */}
            <div className="absolute top-10 right-20 bg-[#162032] border border-white/10 p-4 rounded-xl shadow-2xl flex items-center gap-4 animate-float z-20 w-64 backdrop-blur-sm">
              <div className="w-10 h-10 rounded-full bg-[#185FA5]/20 flex items-center justify-center text-[#185FA5]"><User className="h-5 w-5"/></div>
              <div>
                <p className="text-sm font-bold text-white">Youssef A.</p>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-[#BA7517]/20 text-[#BA7517]">Directeur Audit</span>
              </div>
            </div>

            <div className="absolute top-40 right-4 bg-[#162032] border border-white/10 p-4 rounded-xl shadow-2xl flex items-center gap-4 animate-float-delay-1 z-10 w-64 backdrop-blur-sm">
              <div className="w-10 h-10 rounded-full bg-[#1D9E75]/20 flex items-center justify-center text-[#1D9E75]"><User className="h-5 w-5"/></div>
              <div>
                <p className="text-sm font-bold text-white">Fatima Z.</p>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-[#1D9E75]/20 text-[#1D9E75]">Manager Senior</span>
              </div>
            </div>

            <div className="absolute top-72 right-24 bg-[#162032] border border-white/10 p-4 rounded-xl shadow-2xl flex items-center gap-4 animate-float-delay-2 z-20 w-64 backdrop-blur-sm">
              <div className="w-10 h-10 rounded-full bg-purple-500/20 flex items-center justify-center text-purple-400"><User className="h-5 w-5"/></div>
              <div>
                <p className="text-sm font-bold text-white">Karim B.</p>
                <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-purple-500/20 text-purple-400">Auditeur Senior</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/5 bg-[#06090e] pt-12 pb-8 mt-auto">
        <div className="max-w-7xl mx-auto px-6 text-center">
          <div className="flex justify-center gap-1.5 mb-6">
            <div className="w-4 h-4 bg-[#185FA5] rounded-sm transform rotate-45"/>
            <div className="w-4 h-4 bg-[#BA7517] rounded-sm transform rotate-45"/>
            <div className="w-4 h-4 bg-[#1D9E75] rounded-sm transform rotate-45"/>
            <div className="w-4 h-4 bg-slate-200 rounded-sm transform rotate-45"/>
          </div>
          <p className="text-lg font-bold text-white mb-2">AuditPro AI</p>
          <p className="text-sm text-slate-500 mb-8 max-w-sm mx-auto">Plateforme de gestion d'audit d'entreprise propulsée par l'intelligence artificielle.</p>
          <div className="flex items-center justify-center gap-6 text-sm text-slate-400">
            <a href="#" className="hover:text-white transition-colors">Documentation</a>
            <a href="#" className="hover:text-white transition-colors">API</a>
            <a href="#" className="hover:text-white transition-colors">Support</a>
            <a href="#" className="hover:text-white transition-colors">Confidentialité</a>
          </div>
          <p className="text-xs text-slate-600 mt-12">&copy; {new Date().getFullYear()} AuditPro SaaS. Tous droits réservés.</p>
        </div>
      </footer>
    </div>
  );
}
