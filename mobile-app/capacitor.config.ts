import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.deli.courier',
  appName: 'Deli',
  webDir: 'www',
  server: {
    // For local development — point to the dev server so hot-reload works on device
    // Comment this out for production builds
    url: 'http://10.0.2.2:8100',  // Android emulator host IP
    cleartext: true,
  },
  plugins: {
    Geolocation: {
      // Request background location for GPS streaming while delivering
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
    SplashScreen: {
      launchAutoHide: true,
      backgroundColor: '#0f0f0f',
      showSpinner: false,
    },
    StatusBar: {
      style: 'Dark',
      backgroundColor: '#0f0f0f',
    },
  },
};

export default config;
