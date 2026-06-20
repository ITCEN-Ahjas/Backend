import styles from './LodgingSectionHeader.module.css';

export default function LodgingSectionHeader({ count }) {
  return (
    <div className={styles.header}>
      <div>
        <p className={styles.eyebrow}>숙박</p>
        <h2 className={styles.title}>충북 숙박 정보</h2>
      </div>

      <p className={styles.count}>전체 {count}개</p>
    </div>
  );
}
