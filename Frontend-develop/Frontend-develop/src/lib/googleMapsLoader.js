const GOOGLE_MAPS_SCRIPT_ID = 'google-maps-javascript-api';
const GOOGLE_MAPS_CALLBACK = '__initChungbukGoogleMaps';

let googleMapsPromise;

function createGoogleMapsScript(apiKey) {
  const params = new URLSearchParams({
    key: apiKey,
    callback: GOOGLE_MAPS_CALLBACK,
    loading: 'async',
    v: 'weekly',
    language: 'ko',
    region: 'KR',
  });

  const script = document.createElement('script');
  script.id = GOOGLE_MAPS_SCRIPT_ID;
  script.src = `https://maps.googleapis.com/maps/api/js?${params.toString()}`;
  script.async = true;
  script.defer = true;

  return script;
}

export function loadGoogleMaps() {
  if (window.google?.maps?.importLibrary) {
    return Promise.resolve(window.google.maps);
  }

  if (googleMapsPromise) {
    return googleMapsPromise;
  }

  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim();

  if (!apiKey) {
    return Promise.reject(
      new Error('VITE_GOOGLE_MAPS_API_KEY 환경변수가 설정되지 않았습니다.'),
    );
  }

  googleMapsPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(GOOGLE_MAPS_SCRIPT_ID);

    window[GOOGLE_MAPS_CALLBACK] = () => {
      delete window[GOOGLE_MAPS_CALLBACK];
      resolve(window.google.maps);
    };

    if (existingScript) {
      existingScript.addEventListener(
        'error',
        () => reject(new Error('Google Maps 스크립트를 불러오지 못했습니다.')),
        { once: true },
      );
      return;
    }

    const script = createGoogleMapsScript(apiKey);
    script.addEventListener(
      'error',
      () => {
        delete window[GOOGLE_MAPS_CALLBACK];
        googleMapsPromise = undefined;
        reject(new Error('Google Maps 스크립트를 불러오지 못했습니다.'));
      },
      { once: true },
    );

    document.head.appendChild(script);
  });

  return googleMapsPromise;
}

export async function importGoogleMapsLibrary(libraryName) {
  const maps = await loadGoogleMaps();
  return maps.importLibrary(libraryName);
}
