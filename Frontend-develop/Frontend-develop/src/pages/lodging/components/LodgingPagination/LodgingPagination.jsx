import styles from './LodgingPagination.module.css';

const PAGE_WINDOW_SIZE = 10;

export default function LodgingPagination({ currentPage, totalPages, onPageChange }) {
  const windowStart = Math.floor((currentPage - 1) / PAGE_WINDOW_SIZE) * PAGE_WINDOW_SIZE + 1;
  const windowEnd = Math.min(totalPages, windowStart + PAGE_WINDOW_SIZE - 1);
  const pages = Array.from(
    { length: windowEnd - windowStart + 1 },
    (_, index) => windowStart + index,
  );

  return (
    <nav className={styles.pagination} aria-label="숙박 목록 페이지">
      <button
        type="button"
        className={styles.pageButton}
        onClick={() => onPageChange(Math.max(1, windowStart - PAGE_WINDOW_SIZE))}
        disabled={windowStart === 1}
        aria-label="이전 페이지 묶음"
      >
        ‹
      </button>

      {pages.map(page => (
        <button
          key={page}
          type="button"
          onClick={() => onPageChange(page)}
          className={`${styles.pageButton} ${currentPage === page ? styles.pageButtonActive : ''}`}
          aria-current={currentPage === page ? 'page' : undefined}
        >
          {page}
        </button>
      ))}

      <button
        type="button"
        className={styles.pageButton}
        onClick={() => onPageChange(Math.min(totalPages, windowStart + PAGE_WINDOW_SIZE))}
        disabled={windowEnd === totalPages}
        aria-label="다음 페이지 묶음"
      >
        ›
      </button>
    </nav>
  );
}
