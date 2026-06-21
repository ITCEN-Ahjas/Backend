const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000';

export async function sendChatMessage(message, history = []) {
  const response = await fetch(`${API_BASE_URL}/api/v1/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, history }),
  });
  if (!response.ok) throw new Error('챗봇 응답에 실패했습니다.');
  return response.json();
}

export async function fetchSuggestedQuestions(message, reply) {
  const response = await fetch(`${API_BASE_URL}/api/v1/chat/suggest`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, reply }),
  });
  if (!response.ok) return [];
  const data = await response.json();
  return data.questions ?? [];
}
