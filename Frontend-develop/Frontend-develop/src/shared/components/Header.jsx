import { NavLink } from 'react-router-dom';
import styles from './Header.module.css';

const NAV_ITEMS = [
  { label: '사용설명서', path: '/guide' },
  { label: '지도', path: '/map' },
  { label: '체험·축제', path: '/festival' },
  { label: '숙박', path: '/lodging' },
  { label: 'AI 코스 추천', path: '/course' },
  { label: 'AI 옷차림 추천', path: '/clothing' },
];

export default function Header() {
  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <NavLink to="/" className={styles.logoLink} aria-label="메인으로 이동">
          <img src="/images/Logo3.png" alt="충북 AI Tourism Guide" className={styles.logoImage} />
        </NavLink>

        <nav className={styles.nav} aria-label="주요 메뉴">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </header>
  );
}
