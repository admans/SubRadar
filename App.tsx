import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Plus, Settings, CreditCard, Bell, Info, ArrowLeft, Languages, Search, X } from 'lucide-react';
import { App as CapacitorApp } from '@capacitor/app';
import { LocalNotifications } from '@capacitor/local-notifications';
import { Subscription, AppSettings, Language, BillingCycle } from './types';
import * as storage from './services/storageService';
import SubscriptionForm from './components/SubscriptionForm';
import SubscriptionCard from './components/SubscriptionCard';
import { getTranslation } from './utils/translations';

// Helper to generate numeric ID from string UUID for LocalNotifications
const hashCode = (str: string) => {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash;
  }
  return Math.abs(hash);
};

// Helper for date calculations
const addCycleToDate = (date: Date, cycle: BillingCycle, customDuration?: number, customUnit?: string) => {
  const d = new Date(date);
  if (cycle === BillingCycle.Monthly) {
    d.setMonth(d.getMonth() + 1);
  } else if (cycle === BillingCycle.Quarterly) {
    d.setMonth(d.getMonth() + 3);
  } else if (cycle === BillingCycle.Yearly) {
    d.setFullYear(d.getFullYear() + 1);
  } else if (cycle === BillingCycle.Custom && customDuration && customUnit) {
    if (customUnit === 'day') d.setDate(d.getDate() + customDuration);
    if (customUnit === 'week') d.setDate(d.getDate() + (customDuration * 7));
    if (customUnit === 'month') d.setMonth(d.getMonth() + customDuration);
    if (customUnit === 'year') d.setFullYear(d.getFullYear() + customDuration);
  }
  return d;
};

const App: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [settings, setSettings] = useState<AppSettings>({ notificationsEnabled: false, language: 'en' });
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [editingSub, setEditingSub] = useState<Subscription | null>(null);
  
  // Search State
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Gesture State
  const [dragX, setDragX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const touchStartX = useRef<number | null>(null);
  const settingsContentRef = useRef<HTMLDivElement>(null);

  const stateRef = useRef({ isSettingsOpen, isFormOpen, isDeleteConfirmOpen, isSearchOpen });

  // 1. Initialize Data & Auto-Renew Logic
  useEffect(() => {
    const loadedSubs = storage.getSubscriptions();
    const loadedSettings = storage.getSettings();
    setSettings(loadedSettings);

    // Run Auto-Renew Check
    const { updatedSubs, hasChanges } = checkAutoRenewals(loadedSubs);
    
    setSubscriptions(updatedSubs);
    if (hasChanges) {
      storage.saveSubscriptions(updatedSubs);
      if (loadedSettings.notificationsEnabled) {
        scheduleNotifications(updatedSubs);
      }
    } else {
      if (loadedSettings.notificationsEnabled) {
        scheduleNotifications(updatedSubs);
      }
    }
  }, []);

  useEffect(() => {
    stateRef.current = { isSettingsOpen, isFormOpen, isDeleteConfirmOpen, isSearchOpen };
  }, [isSettingsOpen, isFormOpen, isDeleteConfirmOpen, isSearchOpen]);

  // Hardware Back Button
  useEffect(() => {
    const handleBackButton = async () => {
      CapacitorApp.addListener('backButton', ({ canGoBack }) => {
        const { isSettingsOpen, isFormOpen, isDeleteConfirmOpen, isSearchOpen } = stateRef.current;

        if (isDeleteConfirmOpen) {
          setIsDeleteConfirmOpen(false);
        } else if (isFormOpen) {
          setIsFormOpen(false);
          setEditingSub(null);
        } else if (isSettingsOpen) {
          setIsSettingsOpen(false);
          setDragX(0);
        } else if (isSearchOpen) {
          setIsSearchOpen(false);
          setSearchQuery('');
        } else {
          CapacitorApp.exitApp();
        }
      });
    };
    handleBackButton();
    return () => { CapacitorApp.removeAllListeners(); };
  }, []);

  // --- Auto Renew Logic ---
  const checkAutoRenewals = (subs: Subscription[]) => {
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000;
    const localTodayStr = (new Date(now.getTime() - offset)).toISOString().slice(0, 10);

    let hasChanges = false;

    const updatedSubs = subs.map(sub => {
      // If balance insufficient or undefined, skip
      if (sub.accountBalance === undefined || sub.accountBalance < sub.price) {
        return sub;
      }

      // If not due yet, skip
      if (sub.nextBillingDate > localTodayStr) {
        return sub;
      }

      // Start Auto-Renew Loop
      let currentSub = { ...sub };
      // Safety limit
      for (let i = 0; i < 60; i++) {
        if (currentSub.nextBillingDate > localTodayStr) break;
        if (currentSub.accountBalance < currentSub.price) break;

        // 1. Deduct Balance
        currentSub.accountBalance = Number((currentSub.accountBalance - currentSub.price).toFixed(2));
        
        // 2. Advance Date (using shared helper)
        const d = new Date(currentSub.nextBillingDate);
        const newDate = addCycleToDate(d, currentSub.cycle, currentSub.customCycleDuration, currentSub.customCycleUnit);
        currentSub.nextBillingDate = newDate.toISOString().split('T')[0];
        
        hasChanges = true;
      }
      return currentSub;
    });

    return { updatedSubs, hasChanges };
  };

  // --- Notification Logic ---
  const scheduleNotifications = async (subs: Subscription[]) => {
    try {
      await LocalNotifications.cancel({ notifications: subs.map(s => ({ id: hashCode(s.id) })) });
      
      const notifications = subs.map(sub => {
        const date = new Date(sub.nextBillingDate);
        date.setHours(9, 0, 0, 0); 
        
        if (date.getTime() < Date.now()) return null;

        return {
          id: hashCode(sub.id),
          title: t.appName,
          body: t.dueToday.replace('Today', sub.name) + `: ${sub.name}`,
          schedule: { at: date },
          sound: undefined,
          smallIcon: 'ic_stat_icon_config_sample',
          extra: { subId: sub.id }
        };
      }).filter(n => n !== null);

      if (notifications.length > 0) {
        // @ts-ignore
        await LocalNotifications.schedule({ notifications });
      }
    } catch (e) {
      console.error("Failed to schedule notifications", e);
    }
  };

  const handleToggleNotification = async () => {
    if (settings.notificationsEnabled) {
      const newSettings = { ...settings, notificationsEnabled: false };
      setSettings(newSettings);
      storage.saveSettings(newSettings);
      LocalNotifications.cancel({ notifications: subscriptions.map(s => ({ id: hashCode(s.id) })) });
      return;
    }

    try {
      const result = await LocalNotifications.requestPermissions();
      if (result.display === 'granted') {
        const newSettings = { ...settings, notificationsEnabled: true };
        setSettings(newSettings);
        storage.saveSettings(newSettings);
        scheduleNotifications(subscriptions);
      } else {
        const newSettings = { ...settings, notificationsEnabled: false };
        setSettings(newSettings);
        storage.saveSettings(newSettings);
        alert('Permission denied. Please enable notifications in system settings.');
      }
    } catch (e) {
      const newSettings = { ...settings, notificationsEnabled: false };
      setSettings(newSettings);
    }
  };

  // --- Gesture Logic ---
  const onTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.targetTouches[0].clientX;
    setIsDragging(true);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return;
    const currentX = e.targetTouches[0].clientX;
    const delta = currentX - touchStartX.current;
    if (delta > 0) setDragX(delta);
  };

  const onTouchEnd = () => {
    setIsDragging(false);
    touchStartX.current = null;
    const threshold = window.innerWidth * 0.3;
    if (dragX > threshold) {
      setIsSettingsOpen(false);
      setDragX(0);
    } else {
      setDragX(0);
    }
  };

  const t = getTranslation(settings.language);

  // Sorting & Filtering
  const sortedSubscriptions = useMemo(() => {
    let filtered = subscriptions;
    if (searchQuery.trim()) {
      const lowerQuery = searchQuery.toLowerCase();
      filtered = subscriptions.filter(s => s.name.toLowerCase().includes(lowerQuery));
    }

    return [...filtered].sort((a, b) => {
      return new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime();
    });
  }, [subscriptions, searchQuery]);

  const monthlyCosts = useMemo(() => {
    return subscriptions.reduce((acc, sub) => {
      let monthlyPrice = 0;
      if (sub.cycle === BillingCycle.Monthly) monthlyPrice = sub.price;
      else if (sub.cycle === BillingCycle.Quarterly) monthlyPrice = sub.price / 3;
      else if (sub.cycle === BillingCycle.Yearly) monthlyPrice = sub.price / 12;
      else if (sub.cycle === BillingCycle.Custom && sub.customCycleDuration && sub.customCycleUnit) {
        // Approximate calculation for custom cycles
        let days = 30; // default divisor
        if (sub.customCycleUnit === 'day') days = sub.customCycleDuration;
        if (sub.customCycleUnit === 'week') days = sub.customCycleDuration * 7;
        if (sub.customCycleUnit === 'month') days = sub.customCycleDuration * 30;
        if (sub.customCycleUnit === 'year') days = sub.customCycleDuration * 365;
        monthlyPrice = (sub.price / days) * 30;
      } else {
        monthlyPrice = sub.price; // Fallback
      }

      const currency = sub.currency || 'USD';
      if (currency === 'CNY') acc.CNY += monthlyPrice;
      else acc.USD += monthlyPrice;
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
      newSubs = subscriptions.map(s => s.id === editingSub.id ? { ...s, ...subData } : s);
    } else {
      newSubs = [...subscriptions, { id: crypto.randomUUID(), createdAt: Date.now(), ...subData }];
    }
    
    const { updatedSubs } = checkAutoRenewals(newSubs);
    
    setSubscriptions(updatedSubs);
    storage.saveSubscriptions(updatedSubs);
    
    if (settings.notificationsEnabled) {
      scheduleNotifications(updatedSubs);
    }

    setIsFormOpen(false);
    setEditingSub(null);
    setIsDeleteConfirmOpen(false);
  };

  const handleDeleteSubscription = (id: string) => {
    const newSubs = subscriptions.filter(s => s.id !== id);
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    
    if (settings.notificationsEnabled) {
      LocalNotifications.cancel({ notifications: [{ id: hashCode(id) }] });
    }

    setIsFormOpen(false);
    setEditingSub(null);
    setIsDeleteConfirmOpen(false);
  };

  const handleRenewSubscription = (sub: Subscription) => {
    const currentDate = new Date(sub.nextBillingDate);
    const newDate = addCycleToDate(currentDate, sub.cycle, sub.customCycleDuration, sub.customCycleUnit);
    const newDateStr = newDate.toISOString().split('T')[0];

    let newBalance = sub.accountBalance;
    if (typeof sub.accountBalance === 'number' && sub.accountBalance >= sub.price) {
      newBalance = Number((sub.accountBalance - sub.price).toFixed(2));
    }

    const updatedSub = { ...sub, nextBillingDate: newDateStr, accountBalance: newBalance };
    const newSubs = subscriptions.map(s => s.id === sub.id ? updatedSub : s);
    
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    
    if (settings.notificationsEnabled) {
      scheduleNotifications(newSubs);
    }
  };

  const handleLanguageChange = (lang: Language) => {
    const newSettings = { ...settings, language: lang };
    setSettings(newSettings);
    storage.saveSettings(newSettings);
  };

  const closeForm = () => {
    setIsFormOpen(false);
    setEditingSub(null);
    setIsDeleteConfirmOpen(false);
  };

  const closeSearch = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsSearchOpen(false);
    setSearchQuery('');
  };

  return (
    <div className="min-h-screen bg-surface text-gray-900 font-sans selection:bg-primary-200 overflow-hidden relative">
      
      {/* ================= MAIN LIST VIEW ================= */}
      <div 
        className={`h-full transition-all duration-300 ${isSettingsOpen ? 'scale-[0.92] opacity-50 bg-gray-100 rounded-3xl overflow-hidden cursor-pointer' : ''}`}
        style={{ transformOrigin: 'center top' }}
        onClick={() => isSettingsOpen && setIsSettingsOpen(false)}
      >
        <header className="sticky top-0 z-10 bg-surface/90 backdrop-blur-md border-b border-gray-100">
          <div className="max-w-5xl mx-auto px-5 py-4 flex justify-between items-center h-[72px]">
            
            {/* Conditional Header Content: Title vs Search Bar */}
            {!isSearchOpen ? (
              <>
                <div className="flex flex-col animate-in fade-in duration-200">
                  <h1 className="text-2xl font-bold bg-gradient-to-br from-primary-700 to-primary-500 bg-clip-text text-transparent">
                    {t.appName}
                  </h1>
                  <span className="text-xs text-gray-500 font-medium">
                    {totalDisplay} {t.totalMonthly}
                  </span>
                </div>
                
                <div className="flex items-center gap-2">
                   <button 
                    onClick={(e) => { e.stopPropagation(); setIsSearchOpen(true); }}
                    className="p-3 rounded-full hover:bg-surface-variant text-gray-600 active:scale-90 transition-transform"
                  >
                    <Search className="w-6 h-6" />
                  </button>
                  <button 
                    onClick={(e) => { e.stopPropagation(); setIsSettingsOpen(true); }}
                    className="p-3 rounded-full hover:bg-surface-variant text-gray-600 active:scale-90 transition-transform"
                  >
                    <Settings className="w-6 h-6" />
                  </button>
                </div>
              </>
            ) : (
              <div className="flex-1 flex items-center gap-2 animate-in slide-in-from-right-4 duration-200">
                <div className="flex-1 relative">
                  <input 
                    autoFocus
                    type="text"
                    placeholder={t.searchPlaceholder}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full bg-gray-100 rounded-xl py-2.5 pl-10 pr-4 text-base focus:outline-none focus:ring-2 focus:ring-primary-400"
                  />
                  <Search className="w-5 h-5 text-gray-400 absolute left-3 top-2.5" />
                </div>
                <button 
                  onClick={closeSearch}
                  className="p-2 rounded-full hover:bg-surface-variant text-gray-600"
                >
                   <X className="w-6 h-6" />
                </button>
              </div>
            )}
          </div>
        </header>

        <main className="max-w-5xl mx-auto px-4 pt-6 pb-24">
          <div className="animate-in fade-in duration-300">
            {subscriptions.length === 0 ? (
              <div className="flex flex-col items-center justify-center mt-20 text-center space-y-4 opacity-60">
                <div className="w-24 h-24 bg-surface-variant rounded-full flex items-center justify-center mb-4">
                  <CreditCard className="w-10 h-10 text-gray-400" />
                </div>
                <h3 className="text-xl font-semibold text-gray-700">{t.noSubsTitle}</h3>
                <p className="text-gray-500 max-w-[200px]">{t.noSubsDesc}</p>
              </div>
            ) : sortedSubscriptions.length === 0 && searchQuery ? (
               <div className="flex flex-col items-center justify-center mt-20 text-center space-y-4 opacity-60">
                <Search className="w-12 h-12 text-gray-300" />
                <p className="text-gray-500">No results found for "{searchQuery}"</p>
               </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {sortedSubscriptions.map((sub) => (
                  <SubscriptionCard 
                    key={sub.id} 
                    subscription={sub} 
                    onClick={(s) => { setEditingSub(s); setIsFormOpen(true); }} 
                    onRenew={handleRenewSubscription}
                    t={t}
                    language={settings.language}
                  />
                ))}
              </div>
            )}
          </div>
        </main>
      </div>
      
      {/* Floating Action Button */}
      <button
        onClick={(e) => { e.stopPropagation(); setEditingSub(null); setIsFormOpen(true); }}
        className="fixed bottom-8 right-6 w-16 h-16 bg-primary-600 text-white rounded-[20px] shadow-xl shadow-primary-200/50 flex items-center justify-center hover:bg-primary-700 active:scale-95 transition-all z-20"
      >
        <Plus className="w-8 h-8" strokeWidth={2.5} />
      </button>

      {/* ================= SETTINGS OVERLAY ================= */}
      <div 
        className="fixed inset-0 z-30 bg-surface flex flex-col shadow-2xl"
        ref={settingsContentRef}
        onTouchStart={onTouchStart}
        onTouchMove={onTouchMove}
        onTouchEnd={onTouchEnd}
        style={{
          transform: isSettingsOpen ? `translateX(${Math.max(0, dragX)}px)` : 'translateX(100%)',
          transition: isDragging ? 'none' : 'transform 0.35s cubic-bezier(0.32, 0.72, 0, 1)',
        }}
      >
        {/* Settings Header */}
        <div className="sticky top-0 z-10 bg-surface/90 backdrop-blur-md border-b border-gray-100 shrink-0">
          <div className="max-w-5xl mx-auto px-5 py-4 flex items-center gap-3 h-[72px]">
             <button onClick={() => setIsSettingsOpen(false)} className="p-2 -ml-2 rounded-full hover:bg-gray-100">
               <ArrowLeft className="w-6 h-6 text-gray-700" />
             </button>
             <h1 className="text-xl font-bold text-gray-800">{t.settings}</h1>
          </div>
        </div>

        {/* Settings Content */}
        <div className="flex-1 overflow-y-auto">
          <div className="max-w-xl mx-auto px-4 pt-6 space-y-6 pb-12">
            
            {/* Language */}
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

            {/* Notifications */}
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

            {/* About */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-50">
               <div className="flex items-center gap-4 mb-4">
                <div className="bg-gray-50 p-3 rounded-2xl text-gray-600">
                  <Info className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-semibold text-lg">{t.about}</h3>
                  <p className="text-sm text-gray-500">SubRadar v1.3.3.4</p>
                </div>
              </div>
              <p className="text-sm text-gray-400 leading-relaxed">
                {t.aboutDesc}
              </p>
            </div>
            
            <div className="h-10"></div>
          </div>
        </div>
        
        <div className="absolute left-0 top-0 bottom-0 w-4 bg-gradient-to-r from-black/5 to-transparent pointer-events-none" />
      </div>

      {/* Subscription Form Modal */}
      {isFormOpen && (
        <SubscriptionForm 
          initialData={editingSub}
          onSave={handleSaveSubscription} 
          onCancel={closeForm}
          onDelete={handleDeleteSubscription}
          t={t}
          showDeleteConfirm={isDeleteConfirmOpen}
          setShowDeleteConfirm={setIsDeleteConfirmOpen}
        />
      )}
    </div>
  );
};

export default App;