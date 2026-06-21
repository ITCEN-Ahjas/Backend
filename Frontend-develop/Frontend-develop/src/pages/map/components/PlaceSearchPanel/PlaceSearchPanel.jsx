import { PLACE_CATEGORIES } from '../../../../api/placeApi';
import styles from './PlaceSearchPanel.module.css';

export default function PlaceSearchPanel({
  keyword,
  category,
  isLoading,
  onKeywordChange,
  onCategoryChange,
  onSubmit,
}) {
  return (
    <section className={styles.panel} aria-labelledby="place-search-title">
      <div className={styles.heading}>
        <p className={styles.eyebrow}>CHUNGBUK MAP</p>
        <h1 id="place-search-title" className={styles.title}>
          충북 지도 검색
        </h1>
        <p className={styles.description}>
          관광지, 음식점, 쇼핑 장소를 검색하고 원하는 목적지를 찾아보세요.
        </p>
      </div>

      <form className={styles.searchForm} onSubmit={onSubmit}>
        <label className={styles.searchLabel}>
          <span className={styles.visuallyHidden}>장소 검색어</span>
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="m21 21-4.35-4.35m2.35-5.65a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z" />
          </svg>
          <input
            type="search"
            value={keyword}
            onChange={event => onKeywordChange(event.target.value)}
            placeholder="장소, 관광지, 음식점 검색"
          />
        </label>
        <button type="submit" className={styles.searchButton} disabled={isLoading}>
          검색
        </button>
      </form>

      <div className={styles.categories} aria-label="장소 카테고리">
        {PLACE_CATEGORIES.map(item => (
          <button
            key={item.value}
            type="button"
            className={`${styles.categoryButton} ${
              category === item.value ? styles.categoryButtonActive : ''
            }`}
            aria-pressed={category === item.value}
            disabled={isLoading}
            onClick={() => onCategoryChange(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </section>
  );
}
