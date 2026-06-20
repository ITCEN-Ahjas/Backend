import { BrowserRouter, Route, Routes } from 'react-router-dom';
import ClothingPage from './pages/clothing/ClothingPage';
import CoursePage from './pages/course/CoursePage';
import FestivalDetailPage from './pages/festival/FestivalDetailPage';
import FestivalPage from './pages/festival/FestivalPage';
import GuidePage from './pages/guide/GuidePage';
import LodgingDetailPage from './pages/lodging/LodgingDetailPage';
import LodgingPage from './pages/lodging/LodgingPage';
import MainPage from './pages/main/MainPage';
import MapPage from './pages/map/MapPage';
import FloatingChatbot from './shared/components/common/FloatingChatbot';
import PageLayout from './shared/layout/PageLayout';

export default function App() {
  return (
    <BrowserRouter>
      <PageLayout>
        <Routes>
          <Route path="/" element={<MainPage />} />
          <Route path="/guide" element={<GuidePage />} />
          <Route path="/map" element={<MapPage />} />
          <Route path="/festival" element={<FestivalPage />} />
          <Route path="/festival/:contentId" element={<FestivalDetailPage />} />
          <Route path="/lodging" element={<LodgingPage />} />
          <Route path="/lodging/:contentId" element={<LodgingDetailPage />} />
          <Route path="/course" element={<CoursePage />} />
          <Route path="/clothing" element={<ClothingPage />} />
        </Routes>
      </PageLayout>

      <FloatingChatbot />
    </BrowserRouter>
  );
}
