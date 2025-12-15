import { Subscription, AppSettings, Language } from '../types';

const SUBS_KEY = 'subradar_subscriptions';
const SETTINGS_KEY = 'subradar_settings';

export const getSubscriptions = (): Subscription[] => {
  try {
    const data = localStorage.getItem(SUBS_KEY);
    if (!data) return [];
    
    const parsed = JSON.parse(data);
    
    // Migration: Add currency 'USD' to legacy data that doesn't have it
    // We default legacy data to USD because the previous version only supported $ display.
    return parsed.map((item: any) => ({
      ...item,
      currency: item.currency || 'USD' 
    }));
  } catch (e) {
    console.error('Failed to load subscriptions', e);
    return [];
  }
};

export const saveSubscriptions = (subs: Subscription[]): void => {
  try {
    localStorage.setItem(SUBS_KEY, JSON.stringify(subs));
  } catch (e) {
    console.error('Failed to save subscriptions', e);
  }
};

const detectLanguage = (): Language => {
  if (typeof navigator !== 'undefined' && navigator.language) {
    return navigator.language.startsWith('zh') ? 'zh' : 'en';
  }
  return 'en';
};

export const getSettings = (): AppSettings => {
  try {
    const data = localStorage.getItem(SETTINGS_KEY);
    if (data) {
      const parsed = JSON.parse(data);
      // Ensure language property exists for older saves
      if (!parsed.language) {
        parsed.language = detectLanguage();
      }
      // Ensure theme property exists
      if (!parsed.theme) {
        parsed.theme = 'auto';
      }
      return parsed;
    }
    return { 
      notificationsEnabled: false,
      language: detectLanguage(),
      theme: 'auto'
    };
  } catch (e) {
    return { 
      notificationsEnabled: false,
      language: detectLanguage(),
      theme: 'auto'
    };
  }
};

export const saveSettings = (settings: AppSettings): void => {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
};