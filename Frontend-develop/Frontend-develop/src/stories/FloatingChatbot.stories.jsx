import FloatingChatbot from "../shared/components/FloatingChatbot";

export default {
  title: "Shared/FloatingChatbot",
  component: FloatingChatbot,
  parameters: {
    layout: "fullscreen",
    docs: {
      description: {
        component:
          "모든 페이지 우측 하단에 고정 노출되는 플로팅 챗봇입니다. 백엔드 연결 전까지는 fallback 응답을 제공합니다.",
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
        story: "한국어 챗봇입니다. 우측 하단 버튼을 눌러 열 수 있습니다.",
      },
    },
  },
};
