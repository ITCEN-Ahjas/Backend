import ExperienceCard from '../../../../shared/components/common/ExperienceCard';
import styles from './FestivalGrid.module.css';

function getCardBadges(item) {
  return [
    item.category ? { label: item.category, variant: 'category' } : null,
    item.themeCategory && item.themeCategory !== item.category
      ? { label: item.themeCategory, variant: 'dark' }
      : null,
  ].filter(Boolean);
}

export default function FestivalGrid({ festivals, loading, errorMessage, onClickDetail }) {
  if (loading) {
    return (
      <div className={styles.stateBox}>
        <p>축제 정보를 불러오는 중입니다.</p>
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className={styles.stateBox}>
        <p>{errorMessage}</p>
      </div>
    );
  }

  if (festivals.length === 0) {
    return (
      <div className={styles.stateBox}>
        <p>조건에 맞는 축제·체험 정보가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className={styles.grid}>
      {festivals.map(item => (
        <ExperienceCard
          key={item.id}
          imageUrl={item.imageUrl}
          title={item.title}
          region={item.region}
          description={item.description}
          descriptionLabel={item.descriptionLabel}
          subInfo={item.subInfo}
          subInfoLabel={item.subInfoLabel}
          badges={getCardBadges(item)}
          onClick={() => onClickDetail(item)}
        />
      ))}
    </div>
  );
}
