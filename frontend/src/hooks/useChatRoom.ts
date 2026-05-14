import { useState, useEffect, useRef } from 'react';
import { apiFetch } from '@/lib/api';
import toast from 'react-hot-toast';

export function useChatRoom(rawAuditId: string | null, user: any) {
  const [messages, setMessages] = useState<any[]>([]);
  const [room, setRoom] = useState<any | null>(null);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [myAudits, setMyAudits] = useState<any[]>([]);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!rawAuditId) {
      loadMyAudits();
    }
  }, [rawAuditId]);

  const loadMyAudits = async () => {
    try {
      const res = await apiFetch('/api/audits/mine?size=100');
      setMyAudits(res?.content || []);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const loadRoom = async () => {
    setLoading(true);
    try {
      if (!rawAuditId) {
        toast.error('Aucun audit spécifié pour ce chat');
        setLoading(false);
        return;
      }
      const r = await apiFetch(`/api/chat/room/audit/${rawAuditId}`);
      setRoom(r);
      if (r?.id) {
        await loadMessages(r.id);
      }
    } catch (e: any) {
      toast.error('Impossible de charger le salon : ' + e.message);
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async (roomId: string) => {
    try {
      const res = await apiFetch(`/api/chat/rooms/${roomId}/messages?size=100&sort=createdAt,asc`);
      let msgs = res?.content ?? (Array.isArray(res) ? res : []);
      msgs.sort((a: any, b: any) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      setMessages(msgs);
    } catch { /* ignore */ }
  };

  useEffect(() => {
    if (!room?.id) return;
    const interval = setInterval(() => {
      loadMessages(room.id);
    }, 5000);
    return () => clearInterval(interval);
  }, [room?.id]);

  useEffect(() => {
    loadRoom();
  }, [rawAuditId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim() || !room?.id) return;
    setSending(true);

    const tempMsg = {
      id: `temp-${Date.now()}`,
      senderId: user?.id,
      senderName: user?.fullName,
      content: inputText,
      createdAt: new Date().toISOString(),
      messageType: 'TEXT',
    };
    setMessages(prev => [...prev, tempMsg]);
    const textToSend = inputText;
    setInputText('');

    try {
      const token = JSON.parse(sessionStorage.getItem('audit-auth-storage') || '{}')?.state?.token;
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      const res = await fetch(`${apiUrl}/api/chat/rooms/${room.id}/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({ roomId: room.id, content: textToSend, messageType: 'TEXT' }),
      });
      if (res.ok) {
        const saved = await res.json();
        setMessages(prev => prev.map(m => m.id === tempMsg.id ? saved : m));
      } else {
        setMessages(prev => prev.filter(m => m.id !== tempMsg.id));
        toast.error('Envoi échoué');
      }
    } catch {
      setMessages(prev => prev.filter(m => m.id !== tempMsg.id));
      toast.error('Erreur de connexion');
    } finally {
      setSending(false);
    }
  };

  return {
    messages, room, inputText, setInputText, loading, sending, myAudits, messagesEndRef,
    handleSend, loadRoom
  };
}
