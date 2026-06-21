import ExperienceCard from '../../../../shared/components/common/ExperienceCard';
import styles from './LodgingGrid.module.css';

function getCardBadges(item) {
  return [item.category ? { label: item.category, variant: 'category' } : null].filter(Boolean);
}

export default function LodgingGrid({ lodgings, loading, errorMessage, onClickDetail }) {
  if (loading) {
    return (
      <div className={styles.stateBox}>
        <p>숙박 정보를 불러오는 중입니다.</p>
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

  if (lodgings.length === 0) {
    return (
      <div className={styles.stateBox}>
        <p>조건에 맞는 숙박 정보가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className={styles.grid}>
      {lodgings.map(item => (
        <ExperienceCard
          key={item.id}
          imageUrl={item.imageUrl}
          title={item.title}
          region={item.region}
          description={item.description}
          descriptionLabel={item.descriptionLabel}
          subInfo={item.subInfo}
          subInfoLabel={item.subInfoLabel}
          tel={item.tel}
          badges={getCardBadges(item)}
          onClick={() => onClickDetail(item)}
        />
      ))}
    </div>
  );
}
