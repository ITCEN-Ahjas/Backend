import { useEffect, useMemo, useReducer } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { fetchAccommodationDetail } from '../../api/lodgingApi';
import styles from './LodgingDetailPage.module.css';

const DEFAULT_IMAGE_TEXT = 'CHUNGBUK STAY';

function isEmptyText(value) {
  const text = String(value ?? '').trim();

  return !text || text === '-' || text === '0' || text === '없음' || text === '정보 없음';
}

function pickText(...values) {
  const value = values.find(item => !isEmptyText(item));
  return String(value ?? '').trim();
}

function formatFee(min, max) {
  const minFee = Number(min) || 0;
  const maxFee = Number(max) || 0;

  if (!minFee && !maxFee) return '';
  if (minFee && maxFee && minFee !== maxFee) {
    return `${minFee.toLocaleString()}원 ~ ${maxFee.toLocaleString()}원`;
  }

  return `${(minFee || maxFee).toLocaleString()}원~`;
}

function buildGoogleMapsDirectionsUrl(item) {
  const mapX = String(item?.mapX ?? '').trim();
  const mapY = String(item?.mapY ?? '').trim();

  if (!mapX || !mapY) return '';

  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(`${mapY},${mapX}`)}`;
}

function normalizeHomepageUrl(homepage) {
  const trimmed = String(homepage ?? '').trim();
  if (!trimmed) return '';
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) return trimmed;
  if (trimmed.startsWith('www.')) return `https://${trimmed}`;
  return trimmed;
}

function getLodgingDetail(rawDetail, fallbackItem) {
  const detail = rawDetail ?? {};
  const fallback = fallbackItem ?? {};

  const title = pickText(detail.title, fallback.title, '숙박 상세');
  const region = pickText(detail.region, fallback.region, '충북');
  const category = pickText(detail.category, fallback.category, '숙박');
  const address = pickText(detail.address, fallback.address);
  const imageUrls = Array.isArray(detail.imageUrls) ? detail.imageUrls.filter(Boolean) : [];
  const imageUrl = pickText(detail.imageUrl, imageUrls[0], fallback.imageUrl);

  return {
    id: pickText(detail.id, detail.contentId, fallback.contentId, fallback.id),
    title,
    region,
    category,
    address,
    imageUrl,
    imageUrls,
    tel: pickText(detail.tel, fallback.tel),
    homepage: normalizeHomepageUrl(pickText(detail.homepage)),
    overview: pickText(detail.overview, detail.description),
    description: pickText(detail.description, detail.overview),
    checkInTime: pickText(detail.checkInTime),
    checkOutTime: pickText(detail.checkOutTime),
    parking: pickText(detail.parking),
    cookingAvailable: pickText(detail.cookingAvailable),
    roomCount: pickText(detail.roomCount),
    infoCenter: pickText(detail.infoCenter),
    rooms: Array.isArray(detail.rooms) ? detail.rooms : [],
    mapX: detail.mapX ?? '',
    mapY: detail.mapY ?? '',
  };
}

function makeInfoItem(icon, label, value) {
  if (isEmptyText(value)) return null;
  return { icon, label, value };
}

function buildKeyInfo(detail) {
  if (!detail) return [];

  return [
    makeInfoItem('◷', '체크인', detail.checkInTime),
    makeInfoItem('◷', '체크아웃', detail.checkOutTime),
    makeInfoItem('▤', '주차', detail.parking),
    makeInfoItem('◎', '취사 가능 여부', detail.cookingAvailable),
    makeInfoItem('▣', '객실 수', detail.roomCount),
    makeInfoItem('☎', '문의', detail.infoCenter || detail.tel),
  ].filter(Boolean);
}

function detailReducer(state, action) {
  switch (action.type) {
    case 'start':
      return { detail: null, loading: true, errorMessage: '' };
    case 'success':
      return { detail: action.payload, loading: false, errorMessage: '' };
    case 'error':
      return { detail: null, loading: false, errorMessage: action.payload };
    default:
      return state;
  }
}

export default function LodgingDetailPage() {
  const { contentId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const fallbackItem = location.state?.lodging;

  const [state, dispatch] = useReducer(detailReducer, {
    detail: null,
    loading: true,
    errorMessage: '',
  });

  useEffect(() => {
    const controller = new AbortController();

    dispatch({ type: 'start' });

    fetchAccommodationDetail(contentId, { signal: controller.signal })
      .then(detail => {
        if (controller.signal.aborted) return;

        dispatch({ type: 'success', payload: getLodgingDetail(detail, fallbackItem) });
      })
      .catch(error => {
        if (error.name === 'AbortError') return;

        if (fallbackItem) {
          dispatch({ type: 'success', payload: getLodgingDetail(null, fallbackItem) });
          return;
        }

        dispatch({
          type: 'error',
          payload: error.message || '숙박 상세 조회에 실패했습니다.',
        });
      });

    return () => {
      controller.abort();
    };
  }, [contentId, fallbackItem]);

  const detail = state.detail;

  const mapUrl = useMemo(() => buildGoogleMapsDirectionsUrl(detail), [detail]);
  const keyInfo = useMemo(() => buildKeyInfo(detail), [detail]);

  if (state.loading) {
    return (
      <div className={styles.page}>
        <div className={styles.stateBox}>숙박 상세 정보를 불러오는 중입니다.</div>
      </div>
    );
  }

  if (state.errorMessage) {
    return (
      <div className={styles.page}>
        <div className={styles.stateBox}>{state.errorMessage}</div>
      </div>
    );
  }

  if (!detail) {
    return (
      <div className={styles.page}>
        <div className={styles.stateBox}>숙박 상세 정보가 없습니다.</div>
      </div>
    );
  }

  const introText = pickText(detail.description, detail.overview) || '소개 정보가 없습니다.';

  return (
    <div className={styles.page}>
      <div className={styles.breadcrumb}>
        <Link to="/">홈</Link>
        <span>›</span>
        <Link to="/lodging">숙박</Link>
        <span>›</span>
        <span>상세</span>
      </div>

      <section className={styles.heroSection}>
        <div className={styles.heroImageBox}>
          {detail.imageUrl ? (
            <img src={detail.imageUrl} alt={detail.title} className={styles.heroImage} />
          ) : (
            <div className={styles.heroFallback}>{DEFAULT_IMAGE_TEXT}</div>
          )}
        </div>

        <div className={styles.heroContent}>
          <div className={styles.badgeRow}>
            {detail.category && <span className={styles.categoryBadge}>{detail.category}</span>}
          </div>

          <h1>{detail.title}</h1>
          <p className={styles.regionText}>{detail.region} 숙박 정보</p>
          <p className={styles.heroIntroText}>{introText}</p>
        </div>
      </section>

      <section className={styles.section}>
        <h2>주요 정보</h2>

        {keyInfo.length > 0 ? (
          <div className={styles.infoGrid}>
            {keyInfo.map(item => (
              <div key={`${item.label}-${item.value}`} className={styles.infoItem}>
                <span aria-hidden="true" className={styles.infoIcon}>
                  {item.icon}
                </span>
                <strong>{item.label}</strong>
                <p>{item.value}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className={styles.emptyInfoText}>제공된 상세 정보가 없습니다.</p>
        )}
      </section>

      <section className={styles.section}>
        <h2>객실 정보</h2>

        {detail.rooms.length > 0 ? (
          <div className={styles.roomGrid}>
            {detail.rooms.map((room, index) => {
              const fee = formatFee(room.offSeasonMinFee, room.offSeasonMaxFee);
              const peakFee = formatFee(room.peakSeasonMinFee, room.peakSeasonMaxFee);

              return (
                <div key={`${room.roomTitle}-${index}`} className={styles.roomCard}>
                  <h3>{room.roomTitle || `객실 ${index + 1}`}</h3>

                  <div className={styles.roomMetaList}>
                    {room.roomSize && <span>면적 {room.roomSize}</span>}
                    {room.baseCount && (
                      <span>
                        기준 {room.baseCount}인{room.maxCount ? ` / 최대 ${room.maxCount}인` : ''}
                      </span>
                    )}
                    {room.bathFacility && <span>욕실: {room.bathFacility}</span>}
                  </div>

                  {fee && <p className={styles.roomFee}>비수기 {fee}</p>}
                  {peakFee && <p className={styles.roomFee}>성수기 {peakFee}</p>}
                </div>
              );
            })}
          </div>
        ) : (
          <p className={styles.emptyInfoText}>등록된 객실 정보가 없습니다.</p>
        )}
      </section>

      <section id="location" className={styles.section}>
        <h2>위치 안내</h2>

        <div className={styles.locationBox}>
          <div className={styles.mapPreview}>
            <span className={styles.mapPin} aria-hidden="true">
              ◆
            </span>
          </div>

          <div className={styles.locationContent}>
            <strong>주소</strong>

            <p>Google Maps 연동 예정</p>

            {detail.address && <p className={styles.addressText}>{detail.address}</p>}

            {mapUrl && (
              <a
                href={mapUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.outlineButton}
              >
                Google Maps에서 길찾기
                <span aria-hidden="true">↗</span>
              </a>
            )}
          </div>
        </div>
      </section>

      <div className={styles.bottomActions}>
        <button type="button" onClick={() => navigate('/lodging')} className={styles.backButton}>
          ‹ 목록으로 돌아가기
        </button>

        {detail.homepage && (
          <a
            href={detail.homepage}
            target="_blank"
            rel="noopener noreferrer"
            className={styles.primaryButton}
          >
            공식 홈페이지 바로가기
            <span aria-hidden="true">↗</span>
          </a>
        )}
      </div>
    </div>
  );
}
