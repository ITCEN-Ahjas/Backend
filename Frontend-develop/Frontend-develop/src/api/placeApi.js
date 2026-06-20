const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const PLACE_CARD_IMAGE_WIDTH = 320;

export const PLACE_CATEGORIES = [
  { value: 'ALL', label: '전체' },
  { value: 'TOURIST_ATTRACTION', label: '관광지' },
  { value: 'RESTAURANT', label: '음식점' },
  { value: 'SHOPPING', label: '쇼핑' },
];

function createPlaceQuery({
  keyword = '',
  category = 'ALL',
  size = 10,
  pageToken,
} = {}) {
  const params = new URLSearchParams({
    category,
    size: String(size),
  });
  const normalizedKeyword = String(keyword ?? '').trim();
  const normalizedPageToken = String(pageToken ?? '').trim();

  if (normalizedKeyword) {
    params.set('keyword', normalizedKeyword);
  }

  if (normalizedPageToken) {
    params.set('pageToken', normalizedPageToken);
  }

  return params.toString();
}

async function parseErrorResponse(response) {
  try {
    const errorBody = await response.json();
    return errorBody.message || '장소 검색 요청에 실패했습니다.';
  } catch {
    return '장소 검색 요청에 실패했습니다.';
  }
}

function createPlacePhotoUrl(photoName) {
  const normalizedPhotoName = String(photoName ?? '').trim();

  if (!normalizedPhotoName) {
    return '';
  }

  const params = new URLSearchParams({
    name: normalizedPhotoName,
    maxWidthPx: String(PLACE_CARD_IMAGE_WIDTH),
  });

  return `${API_BASE_URL}/api/places/photo?${params.toString()}`;
}

function normalizePlace(place) {
  if (!place) {
    return place;
  }

  return {
    ...place,
    imageUrl:
      place.imageUrl ||
      place.photoUrl ||
      place.thumbnailUrl ||
      createPlacePhotoUrl(place.photoName),
  };
}

export async function fetchPlaces({
  keyword = '',
  category = 'ALL',
  size = 10,
  pageToken,
  signal,
} = {}) {
  const query = createPlaceQuery({ keyword, category, size, pageToken });
  const response = await fetch(`${API_BASE_URL}/api/places?${query}`, { signal });

  if (!response.ok) {
    throw new Error(await parseErrorResponse(response));
  }

  const data = await response.json();

  return {
    items: Array.isArray(data.items) ? data.items.map(normalizePlace) : [],
    size: Number(data.size) || 0,
    nextPageToken: data.nextPageToken || null,
  };
}
