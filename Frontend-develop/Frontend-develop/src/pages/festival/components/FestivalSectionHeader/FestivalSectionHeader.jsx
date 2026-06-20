import styles from './FestivalSectionHeader.module.css';

export default function FestivalSectionHeader({ count, hasKnownTotal = true }) {
  return (
    <div className={styles.header}>
      <div>
        <p className={styles.eyebrow}>체험·축제</p>
        <h2 className={styles.title}>충북 축제·체험</h2>
      </div>

      <p className={styles.count}>{hasKnownTotal ? '전체' : '현재'} {count}개</p>
    </div>
  );
}
