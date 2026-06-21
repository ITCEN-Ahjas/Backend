import { useEffect, useMemo, useReducer, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchAccommodationList } from '../../api/lodgingApi';
import LodgingFilterPanel from './components/LodgingFilterPanel';
import LodgingGrid from './components/LodgingGrid';
import LodgingHero from './components/LodgingHero';
import LodgingPagination from './components/LodgingPagination';
import LodgingSectionHeader from './components/LodgingSectionHeader';
import styles from './LodgingPage.module.css';

const PAGE_SIZE = 9;

const CHUNGBUK_REGIONS = [
  '청주',
  '충주',
  '제천',
  '보은',
  '옥천',
  '영동',
  '증평',
  '진천',
  '괴산',
  '음성',
  '단양',
];

function pickArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.items)) return payload.items;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data)) return payload.data;
  return [];
}

function pickTotalCount(payload, fallbackCount) {
  return payload?.totalCount ?? payload?.total ?? fallbackCount;
}

function normalizeAccommodation(item) {
  const region = String(item.region ?? '').trim() || '충북';

  return {
    id: String(item.id ?? item.contentId ?? item.contentid ?? item.title),
    contentId: String(item.id ?? item.contentId ?? item.contentid ?? ''),
    title: item.title ?? '이름 없는 숙소',
    region,
    category: item.category ?? item.cat3 ?? item.cat2 ?? '숙박',
    description: item.address ?? '',
    descriptionLabel: '주소',
    subInfo: item.tel ?? '',
    subInfoLabel: '문의',
    imageUrl: item.imageUrl ?? '',
    tel: item.tel ?? '',
    mapX: item.mapX ?? '',
    mapY: item.mapY ?? '',
    address: item.address ?? '',
  };
}

const initialRequestState = {
  lodgings: [],
  totalCount: 0,
  loading: true,
  errorMessage: '',
};

function lodgingRequestReducer(state, action) {
  switch (action.type) {
    case 'start':
      return {
        ...state,
        loading: true,
        errorMessage: '',
      };

    case 'success':
      return {
        lodgings: action.payload.items,
        totalCount: action.payload.totalCount,
        loading: false,
        errorMessage: '',
      };

    case 'error':
      return {
        lodgings: [],
        totalCount: 0,
        loading: false,
        errorMessage: action.payload,
      };

    default:
      return state;
  }
}

export default function LodgingPage() {
  const navigate = useNavigate();

  const [keyword, setKeyword] = useState('');
  const [selectedRegion, setSelectedRegion] = useState('전체');
  const [currentPage, setCurrentPage] = useState(1);

  const [{ lodgings, totalCount, loading, errorMessage }, dispatch] = useReducer(
    lodgingRequestReducer,
    initialRequestState,
  );

  const regionOptions = useMemo(() => ['전체', ...CHUNGBUK_REGIONS], []);

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE));

  useEffect(() => {
    const controller = new AbortController();

    dispatch({ type: 'start' });

    fetchAccommodationList({
      page: currentPage,
      size: PAGE_SIZE,
      region: selectedRegion,
      keyword,
      signal: controller.signal,
    })
      .then(payload => {
        if (controller.signal.aborted) return;

        const items = pickArray(payload).map(normalizeAccommodation);

        dispatch({
          type: 'success',
          payload: {
            items,
            totalCount: pickTotalCount(payload, items.length),
          },
        });
      })
      .catch(error => {
        if (error.name === 'AbortError') return;

        dispatch({
          type: 'error',
          payload: error.message || '숙박 목록 조회에 실패했습니다.',
        });
      });

    return () => {
      controller.abort();
    };
  }, [currentPage, selectedRegion, keyword]);

  const resetFilters = () => {
    setKeyword('');
    setSelectedRegion('전체');
    setCurrentPage(1);
  };

  const handleClickDetail = item => {
    const contentId = item.contentId || item.id;

    navigate(`/lodging/${encodeURIComponent(contentId)}`, {
      state: { lodging: item },
    });
  };

  return (
    <div className={styles.page}>
      <LodgingHero />

      <main className={styles.content}>
        <LodgingFilterPanel
          keyword={keyword}
          regionOptions={regionOptions}
          selectedRegion={selectedRegion}
          onKeywordChange={value => {
            setKeyword(value);
            setCurrentPage(1);
          }}
          onRegionChange={value => {
            setSelectedRegion(value);
            setCurrentPage(1);
          }}
          onReset={resetFilters}
        />

        <LodgingSectionHeader count={totalCount} />

        <LodgingGrid
          lodgings={lodgings}
          loading={loading}
          errorMessage={errorMessage}
          onClickDetail={handleClickDetail}
        />

        <LodgingPagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </main>
    </div>
  );
}
