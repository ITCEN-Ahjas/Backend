import styles from './PlaceResultList.module.css';

const CATEGORY_META = {
  관광지: { icon: '景', tone: 'purple' },
  음식점: { icon: '味', tone: 'green' },
  쇼핑: { icon: '店', tone: 'pink' },
  전체: { icon: '地', tone: 'blue' },
};

function getPlaceMeta(place) {
  return CATEGORY_META[place.category] || CATEGORY_META.전체;
}

function formatRatingCount(count) {
  if (!Number.isFinite(count)) {
    return null;
  }

  return count.toLocaleString('ko-KR');
}

function getPlaceImageUrl(place) {
  return String(
    place?.imageUrl ||
      place?.photoUrl ||
      place?.thumbnailUrl ||
      '',
  ).trim();
}

function handleThumbnailError(event) {
  const image = event.currentTarget;
  const fallback = image.nextElementSibling;

  image.hidden = true;

  if (fallback) {
    fallback.hidden = false;
  }
}

export default function PlaceResultList({
  places,
  totalPlaceCount,
  selectedPlaceId,
  isLoading,
  errorMessage,
  hasSearched,
  hasMoreResults,
  onSelectPlace,
  onRetry,
  onLoadMore,
}) {
  return (
    <section className={styles.panel} aria-labelledby="place-result-title">
      <div className={styles.header}>
        <div>
          <p className={styles.kicker}>SEARCH RESULT</p>
          <h2 id="place-result-title">검색 결과</h2>
        </div>
        {!isLoading && !errorMessage && (
          <span className={styles.resultCount}>{totalPlaceCount}개 장소</span>
        )}
      </div>

      {isLoading && places.length === 0 && (
        <div className={styles.state} role="status">
          <span className={styles.spinner} aria-hidden="true" />
          <strong>장소를 검색하고 있습니다.</strong>
        </div>
      )}

      {errorMessage && places.length === 0 && (
        <div className={styles.state} role="alert">
          <span className={styles.errorIcon} aria-hidden="true">
            !
          </span>
          <strong>검색 결과를 불러오지 못했습니다.</strong>
          <p>{errorMessage}</p>
          <button type="button" className={styles.retryButton} onClick={onRetry}>
            다시 시도
          </button>
        </div>
      )}

      {!isLoading && !errorMessage && places.length === 0 && (
        <div className={styles.state}>
          <span className={styles.emptyIcon} aria-hidden="true">
            ⌖
          </span>
          <strong>{hasSearched ? '검색 결과가 없습니다.' : '장소를 검색해보세요.'}</strong>
          <p>
            {hasSearched
              ? '다른 검색어나 카테고리를 선택해주세요.'
              : '검색 결과가 이 영역에 표시됩니다.'}
          </p>
        </div>
      )}

      {places.length > 0 && (
        <>
          <ul className={styles.list}>
            {places.map((place, index) => {
              const meta = getPlaceMeta(place);
              const ratingCount = formatRatingCount(place.userRatingCount);

              return (
                <li key={`${place.placeId}-${index}`}>
                  <button
                    id={`place-result-${place.placeId}`}
                    type="button"
                    className={`${styles.item} ${
                      selectedPlaceId === place.placeId ? styles.itemSelected : ''
                    }`}
                    aria-pressed={selectedPlaceId === place.placeId}
                    onClick={() => onSelectPlace(place)}
                  >
                    <div className={styles.thumbnail} aria-hidden="true">
                      {getPlaceImageUrl(place) && (
                        <img
                          src={getPlaceImageUrl(place)}
                          alt=""
                          loading="lazy"
                          onError={handleThumbnailError}
                        />
                      )}
                      <span
                        className={`${styles.thumbnailFallback} ${styles[meta.tone]}`}
                        hidden={Boolean(getPlaceImageUrl(place))}
                      >
                        {meta.icon}
                      </span>
                    </div>

                    <div className={styles.info}>
                      <div className={styles.metaRow}>
                        <span className={styles.category}>{place.category || '장소'}</span>
                        {Number.isFinite(place.rating) && (
                          <span className={styles.rating}>
                            <span aria-hidden="true">★</span>
                            {place.rating.toFixed(1)}
                            {ratingCount && <small>({ratingCount})</small>}
                          </span>
                        )}
                      </div>

                      <h3>{place.name || '이름 없는 장소'}</h3>
                      <p className={styles.type}>
                        {place.primaryTypeName || place.primaryType || '장소 정보'}
                      </p>
                      <p className={styles.address}>{place.address || '주소 정보 없음'}</p>
                    </div>

                    <span className={styles.selectGuide}>
                      {selectedPlaceId === place.placeId ? '목적지 선택됨' : '지도에서 보기'}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>

          {errorMessage && (
            <div className={styles.inlineError} role="alert">
              <span>{errorMessage}</span>
              <button type="button" onClick={onLoadMore}>
                다시 시도
              </button>
            </div>
          )}

          {hasMoreResults && !errorMessage && (
            <button
              type="button"
              className={styles.loadMoreButton}
              onClick={onLoadMore}
            >
              더 많은 결과 보기
            </button>
          )}
        </>
      )}
    </section>
  );
}
