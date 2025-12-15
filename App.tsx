import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Plus, Settings, CreditCard, Bell, Info, ArrowLeft, Languages } from 'lucide-react';
import { App as CapacitorApp } from '@capacitor/app';
import { Subscription, AppSettings, Language, BillingCycle } from './types';
import * as storage from './services/storageService';
import SubscriptionForm from './components/SubscriptionForm';
import SubscriptionCard from './components/SubscriptionCard';
import { getTranslation } from './utils/translations';

// Changed View type logic to boolean state for settings overlay
const App: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [settings, setSettings] = useState<AppSettings>({ notificationsEnabled: false, language: 'en' });
  const [isSettingsOpen, setIsSettingsOpen] = useState(false); // Replaced currentView with this
  const [isFormOpen, setIsFormOpen] = useState(false);
  // Lifted state for delete confirmation to handle hardware back button correctly
  const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);
  const [editingSub, setEditingSub] = useState<Subscription | null>(null);

  // Gesture State
  const [dragX, setDragX] = useState(0);
  const [isDragging, setIsDragging] = useState(false);
  const touchStartX = useRef<number | null>(null);
  const settingsContentRef = useRef<HTMLDivElement>(null);

  // Refs for back button listener
  const stateRef = useRef({ isSettingsOpen, isFormOpen, isDeleteConfirmOpen });

  useEffect(() => {
    setSubscriptions(storage.getSubscriptions());
    setSettings(storage.getSettings());
  }, []);

  useEffect(() => {
    stateRef.current = { isSettingsOpen, isFormOpen, isDeleteConfirmOpen };
  }, [isSettingsOpen, isFormOpen, isDeleteConfirmOpen]);

  // Hardware Back Button
  useEffect(() => {
    const handleBackButton = async () => {
      CapacitorApp.addListener('backButton', ({ canGoBack }) => {
        const { isSettingsOpen, isFormOpen, isDeleteConfirmOpen } = stateRef.current;

        if (isDeleteConfirmOpen) {
          // Priority 1: Close delete confirmation (return to edit form)
          setIsDeleteConfirmOpen(false);
        } else if (isFormOpen) {
          // Priority 2: Close form
          setIsFormOpen(false);
          setEditingSub(null);
        } else if (isSettingsOpen) {
          // Priority 3: Close settings
          setIsSettingsOpen(false);
          setDragX(0); // Reset drag
        } else {
          // Priority 4: Exit App
          CapacitorApp.exitApp();
        }
      });
    };
    handleBackButton();
    return () => { CapacitorApp.removeAllListeners(); };
  }, []);

  // --- Gesture Logic ---

  const onTouchStart = (e: React.TouchEvent) => {
    // Only enable gesture if we are near the left edge (optional, currently enabled for whole screen)
    touchStartX.current = e.targetTouches[0].clientX;
    setIsDragging(true);
  };

  const onTouchMove = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return;
    
    const currentX = e.targetTouches[0].clientX;
    const delta = currentX - touchStartX.current;

    // Only allow dragging to the right (positive delta)
    if (delta > 0) {
      setDragX(delta);
    }
  };

  const onTouchEnd = () => {
    setIsDragging(false);
    touchStartX.current = null;

    const threshold = window.innerWidth * 0.3; // Close if dragged 30% of screen
    
    if (dragX > threshold) {
      // Complete the slide out
      setIsSettingsOpen(false);
      setDragX(0); // Reset for next time (transition will handle the exit visually)
    } else {
      // Snap back
      setDragX(0);
    }
  };

  const t = getTranslation(settings.language);

  const sortedSubscriptions = useMemo(() => {
    return [...subscriptions].sort((a, b) => {
      return new Date(a.nextBillingDate).getTime() - new Date(b.nextBillingDate).getTime();
    });
  }, [subscriptions]);

  const monthlyCosts = useMemo(() => {
    return subscriptions.reduce((acc, sub) => {
      const amount = sub.cycle === 'Monthly' ? sub.price : sub.price / 12;
      const currency = sub.currency || 'USD';
      if (currency === 'CNY') acc.CNY += amount;
      else acc.USD += amount;
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
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    setIsFormOpen(false);
    setEditingSub(null);
    setIsDeleteConfirmOpen(false);
  };

  const handleDeleteSubscription = (id: string) => {
    const newSubs = subscriptions.filter(s => s.id !== id);
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
    setIsFormOpen(false);
    setEditingSub(null);
    setIsDeleteConfirmOpen(false);
  };

  const handleRenewSubscription = (sub: Subscription) => {
    const currentDate = new Date(sub.nextBillingDate);
    
    if (sub.cycle === BillingCycle.Monthly) {
      currentDate.setMonth(currentDate.getMonth() + 1);
    } else {
      currentDate.setFullYear(currentDate.getFullYear() + 1);
    }

    // Format back to YYYY-MM-DD
    const newDateStr = currentDate.toISOString().split('T')[0];
    
    const updatedSub = { ...sub, nextBillingDate: newDateStr };
    const newSubs = subscriptions.map(s => s.id === sub.id ? updatedSub : s);
    
    setSubscriptions(newSubs);
    storage.saveSubscriptions(newSubs);
  };

  const handleToggleNotification = () => {
    const newSettings = { ...settings, notificationsEnabled: !settings.notificationsEnabled };
    setSettings(newSettings);
    storage.saveSettings(newSettings);
    if (newSettings.notificationsEnabled && 'Notification' in window && Notification.permission !== 'granted') {
      Notification.requestPermission();
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

  return (
    <div className="min-h-screen bg-surface text-gray-900 font-sans selection:bg-primary-200 overflow-hidden relative">
      
      {/* ================= MAIN LIST VIEW (Always Rendered) ================= */}
      {/* We apply a slight scale/dim effect when settings are open for a nice depth effect */}
      <div 
        className={`h-full transition-all duration-300 ${isSettingsOpen ? 'scale-[0.92] opacity-50 bg-gray-100 rounded-3xl overflow-hidden cursor-pointer' : ''}`}
        style={{ transformOrigin: 'center top' }}
        onClick={() => isSettingsOpen && setIsSettingsOpen(false)}
      >
        <header className="sticky top-0 z-10 bg-surface/90 backdrop-blur-md border-b border-gray-100">
          <div className="max-w-5xl mx-auto px-5 py-4 flex justify-between items-center">
            <div className="flex flex-col">
              <h1 className="text-2xl font-bold bg-gradient-to-br from-primary-700 to-primary-500 bg-clip-text text-transparent">
                {t.appName}
              </h1>
              <span className="text-xs text-gray-500 font-medium">
                {totalDisplay} {t.totalMonthly}
              </span>
            </div>
            
            <button 
              onClick={(e) => { e.stopPropagation(); setIsSettingsOpen(true); }}
              className="p-3 rounded-full hover:bg-surface-variant text-gray-600 active:scale-90 transition-transform"
            >
              <Settings className="w-6 h-6" />
            </button>
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
      
      {/* 
        Floating Action Button moved OUTSIDE the scaled main view 
        This prevents it from jumping/scaling when settings are opened.
        It sits at z-20, while settings overlay is z-30, so settings will cover it.
      */}
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
        {/* Settings Header - Now part of the overlay so it moves with the slide */}
        <div className="sticky top-0 z-10 bg-surface/90 backdrop-blur-md border-b border-gray-100 shrink-0">
          <div className="max-w-5xl mx-auto px-5 py-4 flex items-center gap-3">
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
                  <p className="text-sm text-gray-500">SubRadar v1.3.3.1</p>
                </div>
              </div>
              <p className="text-sm text-gray-400 leading-relaxed">
                {t.aboutDesc}
              </p>
            </div>
            
            <div className="h-10"></div>
          </div>
        </div>
        
        {/* Left edge shadow hint during drag */}
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