import { useState, useEffect } from 'react';
import { apiFetch } from '@/lib/api';
import toast from 'react-hot-toast';

export function useAuditDetail(id: string, user: any) {
  const [audit, setAudit] = useState<any>(null);
  const [docs, setDocs] = useState<any[]>([]);
  const [aiResult, setAiResult] = useState<any>(null);
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [analyzing, setAnalyzing] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const [auditors, setAuditors] = useState<any[]>([]);
  const [selectedAuditor, setSelectedAuditor] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [finalReportFile, setFinalReportFile] = useState<File | null>(null);
  const [uploadingReport, setUploadingReport] = useState(false);
  const [finalReport, setFinalReport] = useState<any | null>(null);
  const [reportRecord, setReportRecord] = useState<any | null>(null);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewing, setReviewing] = useState(false);

  useEffect(() => {
    if (user) loadAudit();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, user]);

  const loadAudit = async () => {
    try {
      const [a, d] = await Promise.all([
        apiFetch(`/api/audits/${id}`),
        apiFetch(`/api/documents/audit/${id}`),
      ]);
      setAudit(a);
      setDocs(Array.isArray(d) ? d : []);
      const report = (Array.isArray(d) ? d : []).find((doc: any) => doc.fileName?.toLowerCase().startsWith('rapport_final_'));
      setFinalReport(report || null);

      fetchAdditionalData();
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  const fetchAdditionalData = async () => {
    try {
      const rr = await apiFetch(`/api/reports/audit/${id}`);
      setReportRecord(rr);
    } catch { setReportRecord(null); }

    try {
      const ai = await apiFetch(`/api/ai/result/${id}`);
      setAiResult(ai);
    } catch { setAiResult(null); }

    if (user?.role === 'MANAGER' || user?.role === 'ADMIN') {
      try {
        const uRes = await apiFetch('/api/users?role=AUDITOR&size=100');
        setAuditors(uRes?.content || uRes || []);
      } catch { setAuditors([]); }
    }
  };

  const assignAuditor = async () => {
    if (!selectedAuditor) return;
    setAssigning(true);
    try {
      await apiFetch(`/api/audits/${id}/assign`, {
        method: 'POST',
        body: JSON.stringify({ auditorId: selectedAuditor, managerId: user?.id }),
      });
      toast.success('Auditeur assigné avec succès');
      setSelectedAuditor('');
      loadAudit();
    } catch (e: any) { toast.error(e.message); }
    finally { setAssigning(false); }
  };

  const triggerAiAnalysis = async () => {
    setAnalyzing(true);
    try {
      await apiFetch(`/api/ai/analyze/${id}`, { method: 'POST' });
      toast.success("L'analyse IA a été lancée en tâche de fond ! Cela prend quelques secondes...");
      pollAiResult();
    } catch(e: any) {
      toast.error("Erreur lancement IA: " + e.message);
      setAnalyzing(false);
    }
  };

  const pollAiResult = () => {
    let attempts = 0;
    const interval = setInterval(async () => {
      attempts++;
      try {
        const ai = await apiFetch(`/api/ai/result/${id}`);
        if (ai && ai.summary) {
          setAiResult(ai);
          setAnalyzing(false);
          clearInterval(interval);
          toast.success("Analyse IA terminée et affichée !");
        }
      } catch(e) { /* ignore */ }

      if (attempts > 10) {
        clearInterval(interval);
        setAnalyzing(false);
        toast.error("L'IA prend plus de temps que prévu. Réessayez d'actualiser la page plus tard.");
      }
    }, 3000);
  };

  const uploadDoc = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) return;
    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    try {
      await executeUpload(formData);
      toast.success('Document téléchargé avec succès');
      setFile(null);
      loadAudit();
    } catch (e: any) { toast.error(e.message); }
    finally { setUploading(false); }
  };

  const uploadFinalReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!finalReportFile) return;
    setUploadingReport(true);
    
    const renamedFile = new File([finalReportFile], `rapport_final_${finalReportFile.name}`, { type: finalReportFile.type });
    const formData = new FormData();
    formData.append('file', renamedFile);
    
    try {
      await executeUpload(formData);
      toast.success('✅ Rapport final soumis avec succès !');
      setFinalReportFile(null);
      
      try {
        await apiFetch(`/api/reports/submit/${id}`, {
          method: 'POST',
          body: JSON.stringify({
            documentFileKey: `rapport_final_${finalReportFile.name}`,
            documentFileName: finalReportFile.name,
          }),
        });
      } catch { /* ignore */ }
      
      loadAudit();
    } catch (e: any) { toast.error(e.message); }
    finally { setUploadingReport(false); }
  };

  const executeUpload = async (formData: FormData) => {
    const token = JSON.parse(sessionStorage.getItem('audit-auth-storage') || '{}')?.state?.token;
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    const res = await fetch(`${apiUrl}/api/documents/upload?auditId=${id}`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body: formData,
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: `Erreur ${res.status}` }));
      throw new Error(err.message || `Erreur ${res.status}`);
    }
  };

  const reviewReport = async (decision: 'APPROVE' | 'REJECT' | 'REVISION') => {
    setReviewing(true);
    try {
      await apiFetch(`/api/reports/review/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ decision, comment: reviewComment }),
      });
      const label = decision === 'APPROVE' ? '✅ Rapport approuvé — Client notifié !' : decision === 'REJECT' ? '❌ Rapport refusé' : '🔄 Révision demandée à l\'auditeur';
      toast.success(label);
      setReviewComment('');
      loadAudit();
    } catch (e: any) { toast.error(e.message); }
    finally { setReviewing(false); }
  };

  const changeStatus = async (status: string) => {
    if (!status) return;
    setChangingStatus(true);
    try {
      await apiFetch(`/api/audits/${id}/status/${status}`, { method: 'PATCH' });
      toast.success('Statut mis à jour');
      loadAudit();
    } catch (e: any) { toast.error(e.message); }
    finally { setChangingStatus(false); }
  };

  const generateReport = async () => {
    try {
      await apiFetch(`/api/reports/generate/${id}`, { method: 'POST' });
      toast.success('Génération du rapport initiée');
    } catch (e: any) { toast.error(e.message); }
  };

  const deleteDoc = async (docId: string, docName: string) => {
    if (!window.confirm(`Supprimer le document "${docName}" ? Cette action est irréversible.`)) return;
    try {
      await apiFetch(`/api/documents/${docId}`, { method: 'DELETE' });
      toast.success('Document supprimé');
      loadAudit();
    } catch (e: any) { toast.error('Erreur suppression : ' + e.message); }
  };

  return {
    audit, docs, aiResult, file, uploading, analyzing, changingStatus, auditors,
    selectedAuditor, assigning, finalReportFile, uploadingReport, finalReport,
    reportRecord, reviewComment, reviewing,
    setFile, setFinalReportFile, setReviewComment, setSelectedAuditor,
    assignAuditor, triggerAiAnalysis, uploadDoc, uploadFinalReport,
    reviewReport, changeStatus, generateReport, deleteDoc
  };
}
