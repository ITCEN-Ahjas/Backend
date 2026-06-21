import styles from './LodgingFilterPanel.module.css';

function FilterRow({ label, options, selected, onChange }) {
  return (
    <div className={styles.filterRow}>
      <span className={styles.filterLabel}>{label}</span>

      <div className={styles.filterOptions}>
        {options.map(option => {
          const active = selected === option;

          return (
            <button
              key={option}
              type="button"
              onClick={() => onChange(option)}
              className={`${styles.filterButton} ${active ? styles.filterButtonActivePurple : ''}`}
            >
              {option}
            </button>
          );
        })}
      </div>
    </div>
  );
}

export default function LodgingFilterPanel({
  keyword,
  regionOptions,
  selectedRegion,
  onKeywordChange,
  onRegionChange,
  onReset,
}) {
  return (
    <section className={styles.panel} aria-label="숙박 검색 필터">
      <div className={styles.searchRow}>
        <form
          onSubmit={event => {
            event.preventDefault();
          }}
          className={styles.searchForm}
        >
          <div className={styles.searchBox}>
            <span className={styles.searchIcon}>⌕</span>

            <input
              type="text"
              value={keyword}
              onChange={event => onKeywordChange(event.target.value)}
              placeholder="숙소명, 지역 또는 키워드를 검색하세요"
              className={styles.searchInput}
            />
          </div>
        </form>

        <div className={styles.actionGroup}>
          <button type="button" onClick={onReset} className={styles.resetButton}>
            초기화
          </button>
        </div>
      </div>

      <div className={styles.filterGroup}>
        <FilterRow
          label="지역"
          options={regionOptions}
          selected={selectedRegion}
          onChange={onRegionChange}
        />
      </div>
    </section>
  );
}
