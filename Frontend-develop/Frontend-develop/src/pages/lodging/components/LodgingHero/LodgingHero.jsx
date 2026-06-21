import styles from './LodgingHero.module.css';

export default function LodgingHero() {
  return (
    <section className={styles.hero}>
      <div className={styles.inner}>
        <div className={styles.text}>
          <h1 className={styles.title}>충북 숙박</h1>
          <p className={styles.description}>충북 곳곳의 숙소 정보를 한눈에 비교해 보세요.</p>
          <p className={styles.subDescription}>Find places to stay in Chungbuk.</p>
        </div>
      </div>
    </section>
  );
}
