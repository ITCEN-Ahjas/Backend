const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

async function requestJson(url, errorMessage, options) {
  const response = await fetch(url, options);

  if (!response.ok) {
    throw new Error(errorMessage);
  }

  return response.json();
}

function createAccommodationQuery({
  page = 1,
  size = 10,
  region = '전체',
  category = '전체',
  keyword = '',
} = {}) {
  const params = new URLSearchParams();

  params.append('page', String(page));
  params.append('size', String(size));

  if (region !== '전체') {
    const trimmedRegion = String(region).trim();
    if (trimmedRegion) {
      params.append('region', trimmedRegion);
    }
  }

  if (category !== '전체') {
    const trimmedCategory = String(category).trim();
    if (trimmedCategory) {
      params.append('category', trimmedCategory);
    }
  }

  const trimmedKeyword = String(keyword ?? '').trim();
  if (trimmedKeyword) {
    params.append('keyword', trimmedKeyword);
  }

  return params.toString() ? `?${params.toString()}` : '';
}

export async function fetchAccommodationList({
  page = 1,
  size = 10,
  region = '전체',
  category = '전체',
  keyword = '',
  signal,
} = {}) {
  const query = createAccommodationQuery({ page, size, region, category, keyword });
  const url = `${API_BASE_URL}/api/accommodations${query}`;
  return requestJson(url, '숙박 목록 조회에 실패했습니다.', { signal });
}

export async function fetchAccommodationDetail(contentId, { signal } = {}) {
  if (!contentId) {
    throw new Error('contentId가 필요합니다.');
  }

  const url = `${API_BASE_URL}/api/accommodations/${encodeURIComponent(contentId)}`;
  return requestJson(url, '숙박 상세 조회에 실패했습니다.', { signal });
}

// 챗봇 응답은 아직 별도 API가 없어 더미로 유지합니다.
export const sendChatMessage = async question => {
  await new Promise(resolve => setTimeout(resolve, 1000));

  if (question.includes('단양')) {
    return {
      answer:
        '단양에는 도담삼봉, 만천하스카이워크, 패러글라이딩이 유명해요. 감성 스테이를 추천드립니다.',
      sources: ['단양 관광지 데이터', 'Tripadvisor 리뷰'],
    };
  }

  if (question.includes('제천')) {
    return {
      answer: '제천은 청풍호 케이블카와 의림지가 대표 명소예요. 청풍호 리조트를 추천드립니다.',
      sources: ['제천 관광지 데이터', 'Tripadvisor 리뷰'],
    };
  }

  if (question.includes('숙소') || question.includes('숙박')) {
    return {
      answer: '충북 곳곳의 숙소 정보를 충북 숙박 페이지에서 비교해보실 수 있어요.',
      sources: ['충북 숙박 데이터'],
    };
  }

  return {
    answer: '충북 여행지, 숙소, 맛집, 체험 코스에 대해 질문해 주세요.',
    sources: ['충북 관광 데이터'],
  };
};
