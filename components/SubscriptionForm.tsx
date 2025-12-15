import React, { useState, useEffect, useRef } from 'react';
import { Subscription, BillingCycle, Currency, CycleUnit } from '../types';
import { X, Check, Calendar, RefreshCw, Wallet, Clock, Trash2, Image as ImageIcon, Upload } from 'lucide-react';
import { Translation } from '../utils/translations';

interface Props {
  initialData?: Subscription | null;
  onSave: (sub: Omit<Subscription, 'id' | 'createdAt'>) => void;
  onCancel: () => void;
  onDelete?: (id: string) => void;
  t: Translation;
  // Lifted state props
  showDeleteConfirm: boolean;
  setShowDeleteConfirm: (show: boolean) => void;
}

const SubscriptionForm: React.FC<Props> = ({ 
  initialData, 
  onSave, 
  onCancel, 
  onDelete, 
  t, 
  showDeleteConfirm, 
  setShowDeleteConfirm 
}) => {
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [currency, setCurrency] = useState<Currency>('CNY');
  
  // Cycle State
  const [cycle, setCycle] = useState<BillingCycle>(BillingCycle.Monthly);
  const [customDuration, setCustomDuration] = useState<string>('1');
  const [customUnit, setCustomUnit] = useState<CycleUnit>('month');

  const [nextBillingDate, setNextBillingDate] = useState('');
  
  // Optional Fields
  const [startDate, setStartDate] = useState('');
  const [accountBalance, setAccountBalance] = useState('');
  
  // Notes & Image
  const [notes, setNotes] = useState('');
  const [image, setImage] = useState<string | undefined>(undefined);
  const [isImageViewerOpen, setIsImageViewerOpen] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (initialData) {
      setName(initialData.name);
      setPrice(initialData.price.toString());
      setCurrency(initialData.currency);
      setCycle(initialData.cycle);
      setNextBillingDate(initialData.nextBillingDate);
      setStartDate(initialData.startDate || '');
      setAccountBalance(initialData.accountBalance ? initialData.accountBalance.toString() : '');
      
      // Custom cycle data
      if (initialData.cycle === BillingCycle.Custom) {
        setCustomDuration(initialData.customCycleDuration?.toString() || '1');
        setCustomUnit(initialData.customCycleUnit || 'month');
      }

      setNotes(initialData.notes || '');
      setImage(initialData.image);
    } else {
      setNextBillingDate(new Date().toISOString().split('T')[0]);
    }
  }, [initialData]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !price || !nextBillingDate) return;

    onSave({
      name,
      price: parseFloat(price),
      currency,
      cycle,
      customCycleDuration: cycle === BillingCycle.Custom ? parseInt(customDuration) : undefined,
      customCycleUnit: cycle === BillingCycle.Custom ? customUnit : undefined,
      nextBillingDate,
      startDate: startDate || undefined,
      accountBalance: accountBalance ? parseFloat(accountBalance) : undefined,
      notes: notes || undefined,
      image: image || undefined
    });
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      // Basic client-side compression via canvas to avoid 5MB localStorage limit
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          const canvas = document.createElement('canvas');
          const MAX_WIDTH = 800; // Resize large images
          const scaleSize = MAX_WIDTH / img.width;
          canvas.width = MAX_WIDTH;
          canvas.height = img.height * scaleSize;
          
          const ctx = canvas.getContext('2d');
          ctx?.drawImage(img, 0, 0, canvas.width, canvas.height);
          
          // Compress to JPEG 0.7 quality
          const dataUrl = canvas.toDataURL('image/jpeg', 0.7);
          setImage(dataUrl);
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };

  const cycleOptions = [
    { value: BillingCycle.Monthly, label: t.monthly },
    { value: BillingCycle.Quarterly, label: t.quarterly },
    { value: BillingCycle.Yearly, label: t.yearly },
    { value: BillingCycle.Custom, label: t.custom },
  ];

  const unitOptions = [
    { value: 'day', label: t.day },
    { value: 'week', label: t.week },
    { value: 'month', label: t.month },
    { value: 'year', label: t.year },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/30 backdrop-blur-sm transition-opacity" 
        onClick={onCancel}
      />

      {/* Modal */}
      <div className="relative w-full h-full md:h-auto md:max-h-[85vh] md:max-w-lg md:rounded-3xl bg-white dark:bg-slate-900 shadow-2xl flex flex-col animate-in slide-in-from-bottom-4 md:zoom-in-95 duration-300 overflow-hidden transition-colors md:pt-0">
        
        {/* Full Screen Image Viewer Overlay */}
        {isImageViewerOpen && image && (
          <div 
            className="absolute inset-0 z-[60] bg-black/95 flex items-center justify-center animate-in fade-in duration-200"
            onClick={(e) => {
              e.stopPropagation();
              setIsImageViewerOpen(false);
            }}
          >
             <div className="relative w-full h-full p-4 flex items-center justify-center">
                <img 
                  src={image} 
                  alt="Full view" 
                  className="max-w-full max-h-full object-contain"
                />
                <button 
                  type="button"
                  onClick={() => setIsImageViewerOpen(false)}
                  className="absolute top-6 right-6 p-2 bg-white/10 rounded-full text-white backdrop-blur-sm hover:bg-white/20 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
             </div>
          </div>
        )}

        {/* Delete Confirmation */}
        {showDeleteConfirm && (
          <div 
            className="absolute inset-0 z-50 bg-white/90 dark:bg-slate-950/90 backdrop-blur-sm flex items-center justify-center p-6 animate-in fade-in duration-200"
            onClick={(e) => {
              e.stopPropagation();
              setShowDeleteConfirm(false);
            }}
          >
            <div 
              className="w-full max-w-sm bg-white dark:bg-slate-900 rounded-3xl shadow-xl border border-gray-100 dark:border-slate-800 p-6 flex flex-col items-center text-center"
              onClick={(e) => e.stopPropagation()} 
            >
              <div className="w-12 h-12 bg-red-50 dark:bg-red-900/20 rounded-full flex items-center justify-center mb-4 text-red-500">
                <Trash2 className="w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white mb-2">{t.delete}?</h3>
              <p className="text-gray-500 dark:text-gray-400 mb-6 leading-relaxed">{t.confirmDelete}</p>
              
              <div className="grid grid-cols-2 gap-3 w-full">
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  className="py-3 px-4 rounded-xl bg-gray-100 dark:bg-slate-800 text-gray-700 dark:text-gray-300 font-bold hover:bg-gray-200 dark:hover:bg-slate-700 transition-colors"
                >
                  {t.cancel}
                </button>
                <button
                  onClick={() => {
                    if (initialData && onDelete) onDelete(initialData.id);
                  }}
                  className="py-3 px-4 rounded-xl bg-red-500 text-white font-bold shadow-lg shadow-red-200 dark:shadow-red-900/30 hover:bg-red-600 transition-all"
                >
                  {t.confirm}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-100 dark:border-slate-800 shrink-0">
          <button 
            onClick={onCancel}
            className="p-2 rounded-full hover:bg-gray-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-6 h-6 text-gray-600 dark:text-gray-300" />
          </button>
          <h2 className="text-lg font-medium text-gray-800 dark:text-white">
            {initialData ? t.editSub : t.newSub}
          </h2>
          <div className="w-10" /> 
        </div>

        {/* Form Content */}
        <form onSubmit={handleSubmit} className="flex-1 p-6 space-y-6 overflow-y-auto">
          
          {/* Name - NO AUTO FOCUS */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 dark:text-primary-400 ml-1">{t.serviceName}</label>
            <div className="bg-surface-variant dark:bg-slate-800 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t.namePlaceholder}
                className="w-full bg-transparent border-none outline-none text-xl text-gray-800 dark:text-white placeholder-gray-400 dark:placeholder-gray-500"
                // autoFocus removed here
                required
              />
            </div>
          </div>

          {/* Price & Currency */}
          <div className="grid grid-cols-3 gap-3">
              <div className="col-span-1 space-y-2">
                  <label className="text-sm font-medium text-primary-600 dark:text-primary-400 ml-1">{t.currency}</label>
                  <div className="bg-surface-variant dark:bg-slate-800 rounded-2xl p-1 flex">
                      <button
                          type="button"
                          onClick={() => setCurrency('CNY')}
                          className={`flex-1 rounded-xl text-sm font-bold py-2 transition-all ${currency === 'CNY' ? 'bg-white dark:bg-slate-700 text-primary-700 dark:text-white shadow-sm' : 'text-gray-400'}`}
                      >
                          ¥
                      </button>
                      <button
                          type="button"
                          onClick={() => setCurrency('USD')}
                          className={`flex-1 rounded-xl text-sm font-bold py-2 transition-all ${currency === 'USD' ? 'bg-white dark:bg-slate-700 text-primary-700 dark:text-white shadow-sm' : 'text-gray-400'}`}
                      >
                          $
                      </button>
                  </div>
              </div>

              <div className="col-span-2 space-y-2">
              <label className="text-sm font-medium text-primary-600 dark:text-primary-400 ml-1">{t.price}</label>
              <div className="flex items-center bg-surface-variant dark:bg-slate-800 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
                  <span className="text-gray-500 dark:text-gray-400 mr-2 font-medium">{currency === 'CNY' ? '¥' : '$'}</span>
                  <input
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  placeholder="0.00"
                  className="w-full bg-transparent border-none outline-none text-xl text-gray-800 dark:text-white placeholder-gray-400 dark:placeholder-gray-500"
                  required
                  />
              </div>
              </div>
          </div>

          {/* Billing Cycle - SCROLLABLE or GRID */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 dark:text-primary-400 ml-1">{t.billingCycle}</label>
            <div className="grid grid-cols-2 gap-2">
              {cycleOptions.map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setCycle(opt.value)}
                  className={`py-3 px-2 rounded-xl border text-xs font-medium transition-all duration-200 flex items-center justify-center gap-1.5
                    ${cycle === opt.value 
                      ? 'bg-primary-100 dark:bg-primary-900/30 border-primary-200 dark:border-primary-800 text-primary-800 dark:text-primary-200 shadow-sm' 
                      : 'bg-white dark:bg-slate-800 border-gray-200 dark:border-slate-700 text-gray-600 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-slate-700'
                    }`}
                >
                  <RefreshCw className={`w-3.5 h-3.5 ${cycle === opt.value ? 'animate-spin-slow' : ''}`} />
                  {opt.label}
                </button>
              ))}
            </div>
            
            {/* Custom Cycle Input */}
            {cycle === BillingCycle.Custom && (
              <div className="flex gap-3 mt-2 animate-in slide-in-from-top-2 fade-in">
                 <div className="flex items-center bg-surface-variant dark:bg-slate-800 rounded-2xl px-4 py-2 flex-1">
                   <span className="text-gray-500 dark:text-gray-400 mr-2 text-sm">{t.every}</span>
                   <input 
                      type="number"
                      min="1"
                      value={customDuration}
                      onChange={(e) => setCustomDuration(e.target.value)}
                      className="w-full bg-transparent border-none outline-none text-gray-800 dark:text-white font-medium text-center"
                   />
                 </div>
                 <select 
                    value={customUnit}
                    onChange={(e) => setCustomUnit(e.target.value as CycleUnit)}
                    className="bg-surface-variant dark:bg-slate-800 rounded-2xl px-4 py-2 flex-1 border-none outline-none text-gray-800 dark:text-white font-medium appearance-none"
                 >
                   {unitOptions.map(u => (
                     <option key={u.value} value={u.value}>{u.label}</option>
                   ))}
                 </select>
              </div>
            )}
          </div>

          {/* Date Input */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 dark:text-primary-400 ml-1">{t.nextBillingDate}</label>
            <div className="flex items-center bg-surface-variant dark:bg-slate-800 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Calendar className="w-5 h-5 text-gray-500 dark:text-gray-400 mr-2" />
              <input
                type="date"
                value={nextBillingDate}
                onChange={(e) => setNextBillingDate(e.target.value)}
                className="w-full bg-transparent border-none outline-none text-lg text-gray-800 dark:text-white"
                required
              />
            </div>
          </div>
          
          <div className="h-px bg-gray-100 dark:bg-slate-800 my-4" />

          {/* Notes & Images */}
          <div className="space-y-3">
             <label className="text-sm font-medium text-gray-500 dark:text-gray-400 ml-1">{t.notes}</label>
             <textarea 
               value={notes}
               onChange={(e) => setNotes(e.target.value)}
               placeholder={t.notesPlaceholder}
               className="w-full bg-surface-variant/50 dark:bg-slate-800/50 rounded-2xl p-4 text-gray-700 dark:text-gray-200 min-h-[100px] border-none outline-none focus:ring-2 focus:ring-primary-200 resize-none placeholder-gray-400 dark:placeholder-gray-600"
             />
             
             {/* Image Preview & Upload */}
             <div className="flex gap-3 overflow-x-auto pb-2">
               {image && (
                 <div className="relative w-24 h-24 shrink-0 rounded-xl overflow-hidden border border-gray-200 dark:border-slate-700 group">
                    <img 
                      src={image} 
                      alt="Attachment" 
                      className="w-full h-full object-cover cursor-pointer hover:opacity-90 transition-opacity" 
                      onClick={() => setIsImageViewerOpen(true)}
                    />
                    <button 
                      type="button"
                      onClick={() => setImage(undefined)}
                      className="absolute top-1 right-1 bg-black/50 text-white rounded-full p-1 hover:bg-black/70 transition-colors"
                    >
                      <X className="w-3 h-3" />
                    </button>
                 </div>
               )}
               
               <button 
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="w-24 h-24 shrink-0 rounded-xl border-2 border-dashed border-gray-300 dark:border-slate-700 flex flex-col items-center justify-center text-gray-400 dark:text-gray-500 hover:border-primary-400 dark:hover:border-primary-500 hover:text-primary-500 dark:hover:text-primary-400 transition-colors bg-gray-50 dark:bg-slate-800/50"
               >
                 <Upload className="w-6 h-6 mb-1" />
                 <span className="text-[10px] font-medium">{t.addImage}</span>
               </button>
               <input 
                 ref={fileInputRef}
                 type="file"
                 accept="image/*"
                 className="hidden"
                 onChange={handleImageUpload}
               />
             </div>
          </div>

          <div className="h-px bg-gray-100 dark:bg-slate-800 my-4" />

          {/* Optional Fields */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-400 dark:text-gray-500 ml-1">{t.startDate}</label>
            <div className="flex items-center bg-surface-variant/50 dark:bg-slate-800/50 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Clock className="w-5 h-5 text-gray-400 dark:text-gray-500 mr-2" />
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full bg-transparent border-none outline-none text-gray-600 dark:text-gray-300"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-400 dark:text-gray-500 ml-1">{t.accountBalance}</label>
            <div className="flex items-center bg-surface-variant/50 dark:bg-slate-800/50 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Wallet className="w-5 h-5 text-gray-400 dark:text-gray-500 mr-2" />
              <span className="text-gray-400 dark:text-gray-500 mr-1 font-medium">{currency === 'CNY' ? '¥' : '$'}</span>
              <input
                type="number"
                inputMode="decimal"
                step="0.01"
                value={accountBalance}
                onChange={(e) => setAccountBalance(e.target.value)}
                placeholder="0.00"
                className="w-full bg-transparent border-none outline-none text-gray-600 dark:text-gray-300 placeholder-gray-400 dark:placeholder-gray-600"
              />
            </div>
          </div>

        </form>

        {/* Footer */}
        <div className="p-4 bg-white dark:bg-slate-900 border-t border-gray-100 dark:border-slate-800 shrink-0 flex flex-col gap-3">
          <button
            onClick={handleSubmit}
            disabled={!name || !price || !nextBillingDate}
            className="w-full py-4 bg-primary-600 text-white rounded-full font-medium text-lg shadow-lg shadow-primary-200 dark:shadow-black/50 disabled:opacity-50 disabled:shadow-none active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            <Check className="w-5 h-5" />
            {t.save}
          </button>
          
          {initialData && onDelete && (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="w-full py-3 text-red-500 font-medium rounded-full hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
            >
              {t.delete}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default SubscriptionForm;
