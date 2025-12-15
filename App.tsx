import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Plus, Settings, CreditCard, Bell, Info, ArrowLeft, Languages } from 'lucide-react';
import { App as CapacitorApp } from '@capacitor/app';
import { Subscription, AppSettings, Language } from './types';
import * as storage from './services/storageService';
import SubscriptionForm from './components/SubscriptionForm';
import SubscriptionCard from './components/SubscriptionCard';
import { getTranslation } from './utils/translations';

type View = 'list' | 'settings';

const App: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [settings, setSettings] = useState<AppSettings>({ notificationsEnabled: false, language: 'en' });
  const [currentView, setCurrentView] = useState<View>('list');
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingSub, setEditingSub] = useState<Subscription | null>(null);

  // Refs to access latest state inside the event listener closure
  const stateRef = useRef({ currentView, isFormOpen });

  // Load initial data
  useEffect(() => {
    setSubscriptions(storage.getSubscriptions());
    setSettings(storage.getSettings());
  }, []);

  // Sync state to ref for back button listener
  useEffect(() => {
    stateRef.current = { currentView, isFormOpen };
  }, [currentView, isFormOpen]);

  // Handle Hardware Back Button (Android)
  useEffect(() => {
    const handleBackButton = async () => {
      CapacitorApp.addListener('backButton', ({ canGoBack }) => {
        const { currentView, isFormOpen } = stateRef.current;

        if (isFormOpen) {
          // If modal is open, close it
          setIsFormOpen(false);
          setEditingSub(null);
        } else if (currentView === 'settings') {
          // If in settings, go back to list
          setCurrentView('list');
        } else {
          // If on main screen, exit app
          CapacitorApp.exitApp();
        }
      });
    };

    handleBackButton();

    return () => {
      CapacitorApp.removeAllListeners();
    };
  }, []);

  // Gesture handling for Swipe Back
  const touchStartX = useRef<number | null>(null);
  const minSwipeDistance = 50; // px

  const onTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.targetTouches[0].clientX;
  };

  const onTouchEnd = (e: React.TouchEvent) => {
    if (!touchStartX.current) return;
    const touchEndX = e.changedTouches[0].clientX;
    
    const distance = touchEndX - touchStartX.current;
    
    // If swipe right (distance > min)
    if (distance > minSwipeDistance) {
      setCurrentView('list');
    }
    
    touchStartX.current = null;
  };

  const t = getTranslation(settings.language);

  // Sort: Closest date first
  const sortedSubscriptions = useMemo(() => {
    return [...subscriptions].sort((a, b) => {
      return new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime();
    });
  }, [subscriptions]);

  // Calculate stats for header
  const monthlyCosts = useMemo(() => {
    return subscriptions.reduce((acc, sub) => {
      const amount = sub.cycle === 'Monthly' ? sub.price : sub.price / 12;
      const currency = sub.currency || 'USD'; // Fallback
      
      if (currency === 'CNY') {
        acc.CNY += amount;
      } else {
        acc.USD += amount;
      }
      return acc;
    }, { CNY: 0, USD: 0 });
  }, [subscriptions]);

  const totalDisplay = useMemo(() => {
    const parts = [];
    if (monthlyCosts.CNY > 0) parts.push(`¥${monthlyCosts.CNY.toFixed(0)}`);
    if (monthlyCosts.USD > 0) parts.push(`$${monthlyCosts.USD.toFixed(0)}`);
    
    if (parts.length === 0) return `~ ${settings.language === 'zh' ? '¥0' : '$0'}`;
    return `~ ${parts.join(' + ')}`;
  }, [monthlyCosts, settings.language]);

  const handleSaveSubscription = (subData: Omit<Subscription, 'id' | 'createdAt'>) => {
    let newSubs: Subscription[];
    
    if (editingSub) {
      newSubs = subscriptions.map(s => 
        s.id === editingSub.id 
          ? { ...s, ...subData } 
          : s
      );
    } else {
      const newSub: Subscription = {
        id: crypto.randomUUID(),
        createdAt: Date.now(),
        ...subData
      };
      newSubs = [...subscriptions, newSub];
    }

    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    setIsFormOpen(false);
    setEditingSub(null);
  };

  const handleDeleteSubscription = (id: string) => {
    const newSubs = subscriptions.filter(s => s.id !== id);
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    setIsFormOpen(false);
    setEditingSub(null);
  };

  const handleToggleNotification = () => {
    const newSettings = { ...settings, notificationsEnabled: !settings.notificationsEnabled };
    setSettings(newSettings);
    storage.saveSettings(newSettings);

    if (newSettings.notificationsEnabled) {
      if ('Notification' in window && Notification.permission !== 'granted') {
        Notification.requestPermission();
      }
    }
  };

  const handleLanguageChange = (lang: Language) => {
    const newSettings = { ...settings, language: lang };
    setSettings(newSettings);
    storage.saveSettings(newSettings);
  };

  // Check for today's subscriptions to simulate notification
  useEffect(() => {
    if (!settings.notificationsEnabled) return;

    const todayStr = new Date().toISOString().split('T')[0];
    const dueToday = subscriptions.filter(s => s.nextBillingDate === todayStr);

    if (dueToday.length > 0) {
      if ('Notification' in window && Notification.permission === 'granted') {
         // Notification logic
      }
    }
  }, [subscriptions, settings.notificationsEnabled]);

  return (
    <div className="min-h-screen bg-surface pb-24 text-gray-900 font-sans selection:bg-primary-200">
      
      {/* Top Bar */}
      <header className="sticky top-0 z-10 bg-surface/90 backdrop-blur-md border-b border-gray-100 transition-all">
        <div className="max-w-5xl mx-auto px-5 py-4 flex justify-between items-center">
          {currentView === 'settings' ? (
             <button onClick={() => setCurrentView('list')} className="p-2 -ml-2 rounded-full hover:bg-gray-100">
               <ArrowLeft className="w-6 h-6 text-gray-700" />
             </button>
          ) : (
            <div className="flex flex-col">
              <h1 className="text-2xl font-bold bg-gradient-to-br from-primary-700 to-primary-500 bg-clip-text text-transparent">
                {t.appName}
              </h1>
              <span className="text-xs text-gray-500 font-medium">
                {totalDisplay} {t.totalMonthly}
              </span>
            </div>
          )}
          
          {currentView === 'list' && (
            <button 
              onClick={() => setCurrentView('settings')}
              className="p-3 rounded-full hover:bg-surface-variant text-gray-600 active:scale-90 transition-transform"
            >
              <Settings className="w-6 h-6" />
            </button>
          )}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-5xl mx-auto px-4 pt-6 transition-all duration-300">
        {currentView === 'settings' ? (
          <div 
            className="space-y-6 max-w-xl mx-auto animate-in slide-in-from-right-4 duration-300 min-h-[80vh]"
            onTouchStart={onTouchStart}
            onTouchEnd={onTouchEnd}
          >
            <h2 className="text-2xl font-bold px-1">{t.settings}</h2>
            
            {/* Language Settings */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-50 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="bg-indigo-50 p-3 rounded-2xl text-indigo-600">
                  <Languages className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-semibold text-lg">{t.language}</h3>
                  <p className="text-sm text-gray-500">{t.languageDesc}</p>
                </div>
              </div>
              <div className="flex bg-gray-100 rounded-xl p-1">
                <button 
                  onClick={() => handleLanguageChange('en')}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${settings.language === 'en' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'}`}
                >
                  EN
                </button>
                <button 
                  onClick={() => handleLanguageChange('zh')}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${settings.language === 'zh' ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500'}`}
                >
                  中文
                </button>
              </div>
            </div>

            {/* Notification Settings */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-50 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="bg-primary-50 p-3 rounded-2xl text-primary-600">
                  <Bell className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-semibold text-lg">{t.notifications}</h3>
                  <p className="text-sm text-gray-500">{t.notificationsDesc}</p>
                </div>
              </div>
              <button 
                onClick={handleToggleNotification}
                className={`w-14 h-8 rounded-full transition-colors relative ${settings.notificationsEnabled ? 'bg-primary-600' : 'bg-gray-200'}`}
              >
                <div className={`absolute top-1 w-6 h-6 rounded-full bg-white shadow-sm transition-transform ${settings.notificationsEnabled ? 'left-7' : 'left-1'}`} />
              </button>
            </div>

            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-50">
               <div className="flex items-center gap-4 mb-4">
                <div className="bg-gray-50 p-3 rounded-2xl text-gray-600">
                  <Info className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-semibold text-lg">{t.about}</h3>
                  <p className="text-sm text-gray-500">SubRadar MVP v1.3.1</p>
                </div>
              </div>
              <p className="text-sm text-gray-400 leading-relaxed">
                {t.aboutDesc}
              </p>
            </div>
          </div>
        ) : (
          <div className="animate-in fade-in duration-300">
            {subscriptions.length === 0 ? (
              <div className="flex flex-col items-center justify-center mt-20 text-center space-y-4 opacity-60">
                <div className="w-24 h-24 bg-surface-variant rounded-full flex items-center justify-center mb-4">
                  <CreditCard className="w-10 h-10 text-gray-400" />
                </div>
                <h3 className="text-xl font-semibold text-gray-700">{t.noSubsTitle}</h3>
                <p className="text-gray-500 max-w-[200px]">{t.noSubsDesc}</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {sortedSubscriptions.map((sub) => (
                  <SubscriptionCard 
                    key={sub.id} 
                    subscription={sub} 
                    onClick={(s) => {
                      setEditingSub(s);
                      setIsFormOpen(true);
                    }} 
                    t={t}
                    language={settings.language}
                  />
                ))}
              </div>
            )}
          </div>
        )}
      </main>

      {/* FAB - Add Button */}
      {currentView === 'list' && (
        <button
          onClick={() => {
            setEditingSub(null);
            setIsFormOpen(true);
          }}
          className="fixed bottom-8 right-6 w-16 h-16 bg-primary-600 text-white rounded-[20px] shadow-xl shadow-primary-200/50 flex items-center justify-center hover:bg-primary-700 active:scale-95 transition-all z-20"
          aria-label="Add Subscription"
        >
          <Plus className="w-8 h-8" strokeWidth={2.5} />
        </button>
      )}

      {/* Add/Edit Modal */}
      {isFormOpen && (
        <SubscriptionForm 
          initialData={editingSub}
          onSave={handleSaveSubscription} 
          onCancel={() => {
            setIsFormOpen(false);
            setEditingSub(null);
          }}
          onDelete={handleDeleteSubscription}
          t={t}
        />
      )}
    </div>
  );
};

export default App;