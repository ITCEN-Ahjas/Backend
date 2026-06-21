import Header from "../shared/components/Header";
import { MemoryRouter, Route, Routes } from "react-router-dom";

const withRouter = (path) => (Story) => (
  <MemoryRouter initialEntries={[path]}>
    <Routes>
      <Route path="*" element={<Story />} />
    </Routes>
  </MemoryRouter>
);

export default {
  title: "Shared/Header",
  component: Header,
  decorators: [withRouter("/lodging")],
  parameters: {
    layout: "fullscreen",
    docs: {
      description: {
        component:
          "모든 페이지에서 공통으로 사용하는 헤더 컴포넌트입니다. lang props와 현재 경로에 따라 활성 탭이 변경됩니다.",
      },
    },
  },
  argTypes: {
    lang: {
      control: { type: "radio" },
      options: ["ko", "en"],
      description: "언어 설정",
      table: { category: "Props" },
    },
  },
};

export const Korean = {
  args: { lang: "ko" },
  parameters: {
    docs: {
      description: {
        story: "한국어 헤더입니다. 숙박 탭이 활성화된 상태입니다.",
      },
    },
  },
};

export const English = {
  args: { lang: "en" },
  parameters: {
    docs: { description: { story: "영어 헤더입니다." } },
  },
};
