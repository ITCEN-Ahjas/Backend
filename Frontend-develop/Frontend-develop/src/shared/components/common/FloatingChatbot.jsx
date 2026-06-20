import { useEffect, useRef, useState, useCallback } from 'react';
import { sendChatMessage, fetchSuggestedQuestions } from '../../../api/chatApi';

const SAMPLES = [
  '단양 당일치기 여행지 추천해줘',
  '제천 가볼 만한 곳 알려줘',
  '청주에서 아이랑 가기 좋은 곳은?',
];

const ChatIcon = () => (
  <svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path
      d="M11 1.5C5.753 1.5 1.5 5.477 1.5 10.375c0 2.674 1.22 5.07 3.148 6.69L3.5 20.5l4.23-1.406A10.1 10.1 0 0011 19.25c5.247 0 9.5-3.977 9.5-8.875S16.247 1.5 11 1.5z"
      fill="white"
      stroke="white"
      strokeWidth="0.5"
    />
  </svg>
);

const SendIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M16 9L2 2l3 7-3 7 14-7z" fill="white" />
  </svg>
);

export default function FloatingChatbot() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([
    { role: 'ai', text: '안녕하세요! 충북 여행 AI입니다.\n궁금한 여행 정보를 편하게 물어보세요 😊' },
  ]);
  const [suggested, setSuggested] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);

  const [pos, setPos] = useState({ x: window.innerWidth - 80, y: window.innerHeight - 80 });
  const [panelSize, setPanelSize] = useState({ width: 380, height: 360 });
  const dragging = useRef(false);
  const offset = useRef({ x: 0, y: 0 });
  const hasDragged = useRef(false);
  const resizing = useRef(false);
  const resizeStart = useRef({ x: 0, y: 0, w: 380, h: 360 });
  const bottomRef = useRef(null);
  const inputRef = useRef(null);

  useEffect(() => {
    if (open) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
      setTimeout(() => inputRef.current?.focus(), 100);
    }
  }, [messages, open]);

  const onMouseDown = useCallback(e => {
    dragging.current = true;
    hasDragged.current = false;
    offset.current = { x: e.clientX - pos.x, y: e.clientY - pos.y };
    e.preventDefault();
  }, [pos]);

  const onResizeMouseDown = useCallback(e => {
    e.stopPropagation();
    resizing.current = true;
    resizeStart.current = {
      x: e.clientX, y: e.clientY,
      w: panelSize.width, h: panelSize.height,
    };
    e.preventDefault();
  }, [panelSize]);

  useEffect(() => {
    const onMouseMove = e => {
      if (dragging.current) {
        hasDragged.current = true;
        setPos({ x: e.clientX - offset.current.x, y: e.clientY - offset.current.y });
      }
      if (resizing.current) {
        const dx = resizeStart.current.x - e.clientX;
        const dy = resizeStart.current.y - e.clientY;
        setPanelSize({
          width: Math.max(300, Math.min(600, resizeStart.current.w + dx)),
          height: Math.max(280, Math.min(600, resizeStart.current.h + dy)),
        });
      }
    };
    const onMouseUp = () => {
      dragging.current = false;
      resizing.current = false;
    };
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
    };
  }, []);

  const handleToggle = () => {
    if (!hasDragged.current) setOpen(p => !p);
  };

  const buildHistory = msgs =>
    msgs
      .filter((m, i) => !(m.role === 'ai' && i === 0))
      .map(m => ({ role: m.role === 'user' ? 'user' : 'assistant', content: m.text }));

  const handleSend = async question => {
    const text = (question ?? input).trim();
    if (!text || loading) return;
    setMessages(p => [...p, { role: 'user', text }]);
    setInput('');
    setSuggested([]);
    setLoading(true);
    try {
      const history = buildHistory(messages);
      const res = await sendChatMessage(text, history);
      const aiText = res.reply;
      setMessages(p => [...p, { role: 'ai', text: aiText }]);
      const questions = await fetchSuggestedQuestions(text, aiText);
      setSuggested(questions);
    } catch {
      setMessages(p => [
        ...p,
        { role: 'ai', text: '일시적인 오류가 발생했어요.\n잠시 후 다시 시도해 주세요.' },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <style>{`
        @keyframes dotBounce {
          0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
          40% { transform: translateY(-5px); opacity: 1; }
        }
        @keyframes panelIn {
          from { opacity: 0; transform: translateY(16px) scale(0.96); }
          to   { opacity: 1; transform: translateY(0)  scale(1); }
        }
        @keyframes msgIn {
          from { opacity: 0; transform: translateY(6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
        .cb-sample:hover  { background: #f1f5f9 !important; transform: translateX(2px); }
        .cb-suggest:hover { background: #d1fae5 !important; transform: translateX(2px); }
        .cb-send:hover:not(:disabled) { background: #059669 !important; transform: scale(1.06); }
        .cb-close:hover { background: rgba(255,255,255,0.15) !important; }
        .cb-fab:hover { box-shadow: 0 10px 32px rgba(0,0,0,0.4) !important; transform: scale(1.07) !important; }
        .cb-msg { animation: msgIn 0.2s ease; }
        .cb-scroll::-webkit-scrollbar { width: 4px; }
        .cb-scroll::-webkit-scrollbar-track { background: transparent; }
        .cb-scroll::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 4px; }
      `}</style>

      <div style={{ position: 'fixed', left: pos.x, top: pos.y, zIndex: 9999, transform: 'translate(-50%,-50%)' }}>

        {/* ── 패널 ── */}
        {open && (
          <div style={{
            position: 'absolute',
            bottom: 80,
            right: 0,
            width: `min(${panelSize.width}px, 90vw)`,
            borderRadius: 24,
            background: '#fff',
            boxShadow: '0 32px 80px rgba(0,0,0,0.14), 0 2px 8px rgba(0,0,0,0.06)',
            border: '1px solid #e8ecf1',
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            animation: 'panelIn 0.22s cubic-bezier(0.34,1.56,0.64,1)',
          }}>
            {/* 리사이즈 핸들 */}
            <div
              onMouseDown={onResizeMouseDown}
              title="드래그해서 크기 조절"
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: 20,
                height: 20,
                cursor: 'nw-resize',
                zIndex: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                <path d="M1 9L9 1M1 5L5 1M5 9L9 5" stroke="#cbd5e1" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
            </div>

            {/* 헤더 */}
            <div style={{
              padding: '16px 18px',
              background: '#0f172a',
              display: 'flex',
              alignItems: 'center',
              gap: 12,
            }}>
              <div style={{
                width: 40, height: 40, borderRadius: 12, flexShrink: 0,
                background: 'linear-gradient(135deg,#10b981,#059669)',
                display: 'grid', placeItems: 'center',
                boxShadow: '0 4px 14px rgba(16,185,129,0.35)',
              }}>
                <ChatIcon />
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <p style={{ margin: 0, fontSize: 14, fontWeight: 700, color: '#fff', letterSpacing: '-0.2px' }}>
                  충북 여행 AI
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 3 }}>
                  <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#10b981', boxShadow: '0 0 5px #10b981' }} />
                  <span style={{ fontSize: 11, color: 'rgba(255,255,255,0.4)' }}>지금 바로 답변 가능</span>
                </div>
              </div>
              <button className="cb-close" onClick={() => setOpen(false)} style={{
                background: 'rgba(255,255,255,0.08)',
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: 10, width: 32, height: 32,
                display: 'grid', placeItems: 'center',
                cursor: 'pointer', color: 'rgba(255,255,255,0.55)',
                fontSize: 13, flexShrink: 0,
                transition: 'background 0.15s',
              }}>✕</button>
            </div>

            {/* 메시지 */}
            <div className="cb-scroll" style={{
              height: panelSize.height,
              overflowY: 'auto',
              background: '#f8fafc',
              padding: '18px 14px',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
            }}>
              {messages.map((msg, i) => (
                <div key={i} className="cb-msg" style={{
                  display: 'flex',
                  flexDirection: msg.role === 'user' ? 'row-reverse' : 'row',
                  alignItems: 'flex-end',
                  gap: 8,
                }}>
                  {msg.role === 'ai' && (
                    <div style={{
                      width: 30, height: 30, borderRadius: 10, flexShrink: 0,
                      background: 'linear-gradient(135deg,#10b981,#059669)',
                      display: 'grid', placeItems: 'center',
                      boxShadow: '0 2px 8px rgba(16,185,129,0.25)',
                    }}>
                      <ChatIcon />
                    </div>
                  )}
                  <div style={{
                    maxWidth: '72%',
                    padding: '11px 14px',
                    borderRadius: msg.role === 'user' ? '18px 4px 18px 18px' : '4px 18px 18px 18px',
                    background: msg.role === 'user' ? '#0f172a' : '#fff',
                    color: msg.role === 'user' ? '#f8fafc' : '#1e293b',
                    fontSize: 13.5,
                    lineHeight: 1.7,
                    fontWeight: 400,
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-word',
                    boxShadow: msg.role === 'ai'
                      ? '0 2px 10px rgba(0,0,0,0.07)'
                      : '0 2px 10px rgba(15,23,42,0.2)',
                  }}>
                    {msg.text}
                  </div>
                </div>
              ))}

              {/* 로딩 */}
              {loading && (
                <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
                  <div style={{
                    width: 30, height: 30, borderRadius: 10, flexShrink: 0,
                    background: 'linear-gradient(135deg,#10b981,#059669)',
                    display: 'grid', placeItems: 'center',
                  }}>
                    <ChatIcon />
                  </div>
                  <div style={{
                    background: '#fff', borderRadius: '4px 18px 18px 18px',
                    padding: '14px 18px', display: 'flex', gap: 5, alignItems: 'center',
                    boxShadow: '0 2px 10px rgba(0,0,0,0.07)',
                  }}>
                    {[0, 1, 2].map(i => (
                      <div key={i} style={{
                        width: 7, height: 7, borderRadius: '50%', background: '#10b981',
                        animation: `dotBounce 1.1s ease-in-out ${i * 0.16}s infinite`,
                      }} />
                    ))}
                  </div>
                </div>
              )}

              {/* 샘플 질문 */}
              {messages.length <= 1 && !loading && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 7, marginTop: 4 }}>
                  <span style={{ fontSize: 11, fontWeight: 600, color: '#94a3b8', paddingLeft: 2, letterSpacing: '0.2px' }}>
                    이런 질문은 어떠세요?
                  </span>
                  {SAMPLES.map(q => (
                    <button key={q} className="cb-sample" onClick={() => handleSend(q)} style={{
                      background: '#fff',
                      border: '1.5px solid #e2e8f0',
                      borderRadius: 13,
                      padding: '11px 15px',
                      fontSize: 13,
                      fontWeight: 500,
                      textAlign: 'left',
                      cursor: 'pointer',
                      color: '#334155',
                      transition: 'all 0.15s ease',
                      boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
                    }}>
                      {q}
                    </button>
                  ))}
                </div>
              )}

              {/* 추천 질문 */}
              {suggested.length > 0 && !loading && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 7, marginTop: 4 }}>
                  <span style={{ fontSize: 11, fontWeight: 600, color: '#10b981', paddingLeft: 2, letterSpacing: '0.2px' }}>
                    💡 이런 것도 궁금하지 않으세요?
                  </span>
                  {suggested.map(q => (
                    <button key={q} className="cb-suggest" onClick={() => handleSend(q)} style={{
                      background: '#f0fdf9',
                      border: '1.5px solid #a7f3d0',
                      borderRadius: 13,
                      padding: '11px 15px',
                      fontSize: 13,
                      fontWeight: 500,
                      textAlign: 'left',
                      cursor: 'pointer',
                      color: '#065f46',
                      transition: 'all 0.15s ease',
                      boxShadow: '0 1px 4px rgba(16,185,129,0.08)',
                    }}>
                      {q}
                    </button>
                  ))}
                </div>
              )}

              <div ref={bottomRef} />
            </div>

            {/* 입력 영역 */}
            <div style={{
              padding: '12px 14px',
              background: '#fff',
              borderTop: '1px solid #f1f5f9',
              display: 'flex',
              gap: 8,
              alignItems: 'flex-end',
            }}>
              <input
                ref={inputRef}
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && !e.shiftKey && handleSend()}
                disabled={loading}
                placeholder="궁금한 점을 입력하세요..."
                style={{
                  flex: 1,
                  background: '#f1f5f9',
                  border: '1.5px solid transparent',
                  borderRadius: 14,
                  padding: '11px 16px',
                  fontSize: 13.5,
                  color: '#1e293b',
                  outline: 'none',
                  resize: 'none',
                  lineHeight: 1.5,
                  transition: 'border 0.2s, background 0.2s',
                }}
                onFocus={e => {
                  e.target.style.borderColor = '#10b981';
                  e.target.style.background = '#fff';
                }}
                onBlur={e => {
                  e.target.style.borderColor = 'transparent';
                  e.target.style.background = '#f1f5f9';
                }}
              />
              <button
                className="cb-send"
                onClick={() => handleSend()}
                disabled={loading || !input.trim()}
                style={{
                  width: 44, height: 44, borderRadius: 14, flexShrink: 0,
                  background: loading || !input.trim() ? '#e2e8f0' : '#10b981',
                  border: 'none',
                  display: 'grid', placeItems: 'center',
                  cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: !loading && input.trim() ? '0 4px 14px rgba(16,185,129,0.3)' : 'none',
                }}
              >
                <SendIcon />
              </button>
            </div>
          </div>
        )}

        {/* ── FAB 버튼 ── */}
        <div
          className="cb-fab"
          onMouseDown={onMouseDown}
          onClick={handleToggle}
          style={{
            width: 64, height: 64, borderRadius: '50%',
            background: '#0f172a',
            display: 'grid', placeItems: 'center',
            cursor: 'grab',
            boxShadow: '0 6px 24px rgba(0,0,0,0.28)',
            userSelect: 'none',
            transition: 'all 0.2s ease',
            border: '2px solid rgba(255,255,255,0.07)',
          }}
        >
          {open
            ? <span style={{ fontSize: 18, color: 'rgba(255,255,255,0.75)', lineHeight: 1 }}>✕</span>
            : <ChatIcon />
          }
        </div>
      </div>
    </>
  );
}
