import React, { useState, useEffect } from 'react';
import { Subscription, BillingCycle, Currency } from '../types';
import { X, Check, Calendar, DollarSign, RefreshCw, Wallet, Clock } from 'lucide-react';
import { Translation } from '../utils/translations';

interface Props {
  initialData?: Subscription | null;
  onSave: (sub: Omit<Subscription, 'id' | 'createdAt'>) => void;
  onCancel: () => void;
  onDelete?: (id: string) => void;
  t: Translation;
}

const SubscriptionForm: React.FC<Props> = ({ initialData, onSave, onCancel, onDelete, t }) => {
  const [name, setName] = useState('');
  const [price, setPrice] = useState('');
  const [currency, setCurrency] = useState<Currency>('CNY');
  const [cycle, setCycle] = useState<BillingCycle>(BillingCycle.Monthly);
  const [nextBillingDate, setNextBillingDate] = useState('');
  
  // New optional fields
  const [startDate, setStartDate] = useState('');
  const [accountBalance, setAccountBalance] = useState('');

  useEffect(() => {
    if (initialData) {
      setName(initialData.name);
      setPrice(initialData.price.toString());
      setCurrency(initialData.currency);
      setCycle(initialData.cycle);
      setNextBillingDate(initialData.nextBillingDate);
      setStartDate(initialData.startDate || '');
      setAccountBalance(initialData.accountBalance ? initialData.accountBalance.toString() : '');
    } else {
      // Default to today
      setNextBillingDate(new Date().toISOString().split('T')[0]);
      // Default currency is already CNY via useState init
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
      nextBillingDate,
      startDate: startDate || undefined,
      accountBalance: accountBalance ? parseFloat(accountBalance) : undefined
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center">
      {/* Backdrop for Tablet/Desktop */}
      <div 
        className="absolute inset-0 bg-black/30 backdrop-blur-sm transition-opacity" 
        onClick={onCancel}
      />

      {/* Modal Container */}
      <div className="relative w-full h-full md:h-auto md:max-h-[85vh] md:max-w-lg md:rounded-3xl bg-white shadow-2xl flex flex-col animate-in slide-in-from-bottom-4 md:zoom-in-95 duration-300 overflow-hidden">
        
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-100 shrink-0">
          <button 
            onClick={onCancel}
            className="p-2 rounded-full hover:bg-gray-100 transition-colors"
          >
            <X className="w-6 h-6 text-gray-600" />
          </button>
          <h2 className="text-lg font-medium text-gray-800">
            {initialData ? t.editSub : t.newSub}
          </h2>
          <div className="w-10" /> {/* Spacer for centering */}
        </div>

        {/* Scrollable Form Content */}
        <form onSubmit={handleSubmit} className="flex-1 p-6 space-y-6 overflow-y-auto">
          
          {/* Name Input */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 ml-1">{t.serviceName}</label>
            <div className="bg-surface-variant rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Netflix"
                className="w-full bg-transparent border-none outline-none text-xl text-gray-800 placeholder-gray-400"
                autoFocus={!initialData}
                required
              />
            </div>
          </div>

          {/* Currency & Price Input Group */}
          <div className="grid grid-cols-3 gap-3">
              {/* Currency */}
              <div className="col-span-1 space-y-2">
                  <label className="text-sm font-medium text-primary-600 ml-1">{t.currency}</label>
                  <div className="bg-surface-variant rounded-2xl p-1 flex">
                      <button
                          type="button"
                          onClick={() => setCurrency('CNY')}
                          className={`flex-1 rounded-xl text-sm font-bold py-2 transition-all ${currency === 'CNY' ? 'bg-white text-primary-700 shadow-sm' : 'text-gray-400'}`}
                      >
                          ¥
                      </button>
                      <button
                          type="button"
                          onClick={() => setCurrency('USD')}
                          className={`flex-1 rounded-xl text-sm font-bold py-2 transition-all ${currency === 'USD' ? 'bg-white text-primary-700 shadow-sm' : 'text-gray-400'}`}
                      >
                          $
                      </button>
                  </div>
              </div>

              {/* Price */}
              <div className="col-span-2 space-y-2">
              <label className="text-sm font-medium text-primary-600 ml-1">{t.price}</label>
              <div className="flex items-center bg-surface-variant rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
                  <span className="text-gray-500 mr-2 font-medium">{currency === 'CNY' ? '¥' : '$'}</span>
                  <input
                  type="number"
                  step="0.01"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  placeholder="0.00"
                  className="w-full bg-transparent border-none outline-none text-xl text-gray-800 placeholder-gray-400"
                  required
                  />
              </div>
              </div>
          </div>

          {/* Cycle Selection */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 ml-1">{t.billingCycle}</label>
            <div className="flex gap-3">
              {[BillingCycle.Monthly, BillingCycle.Yearly].map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setCycle(c)}
                  className={`flex-1 py-3 px-4 rounded-2xl border text-sm font-medium transition-all duration-200 flex items-center justify-center gap-2
                    ${cycle === c 
                      ? 'bg-primary-100 border-primary-200 text-primary-800 shadow-sm' 
                      : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                >
                  <RefreshCw className={`w-4 h-4 ${cycle === c ? 'animate-spin-slow' : ''}`} />
                  {c === BillingCycle.Monthly ? t.monthly : t.yearly}
                </button>
              ))}
            </div>
          </div>

          {/* Date Input */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-primary-600 ml-1">{t.nextBillingDate}</label>
            <div className="flex items-center bg-surface-variant rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Calendar className="w-5 h-5 text-gray-500 mr-2" />
              <input
                type="date"
                value={nextBillingDate}
                onChange={(e) => setNextBillingDate(e.target.value)}
                className="w-full bg-transparent border-none outline-none text-lg text-gray-800"
                required
              />
            </div>
          </div>

          <div className="h-px bg-gray-100 my-4" />

          {/* Optional Fields Section */}
          
          {/* Start Date (Optional) */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-400 ml-1">{t.startDate}</label>
            <div className="flex items-center bg-surface-variant/50 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Clock className="w-5 h-5 text-gray-400 mr-2" />
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full bg-transparent border-none outline-none text-gray-600"
              />
            </div>
          </div>

          {/* Account Balance (Optional) */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-400 ml-1">{t.accountBalance}</label>
            <div className="flex items-center bg-surface-variant/50 rounded-2xl px-4 py-3 focus-within:ring-2 focus-within:ring-primary-500 transition-all">
              <Wallet className="w-5 h-5 text-gray-400 mr-2" />
              <span className="text-gray-400 mr-1 font-medium">{currency === 'CNY' ? '¥' : '$'}</span>
              <input
                type="number"
                step="0.01"
                value={accountBalance}
                onChange={(e) => setAccountBalance(e.target.value)}
                placeholder="0.00"
                className="w-full bg-transparent border-none outline-none text-gray-600"
              />
            </div>
          </div>

        </form>

        {/* Footer Actions */}
        <div className="p-4 bg-white border-t border-gray-100 shrink-0 flex flex-col gap-3">
          <button
            onClick={handleSubmit}
            disabled={!name || !price || !nextBillingDate}
            className="w-full py-4 bg-primary-600 text-white rounded-full font-medium text-lg shadow-lg shadow-primary-200 disabled:opacity-50 disabled:shadow-none active:scale-[0.98] transition-all flex items-center justify-center gap-2"
          >
            <Check className="w-5 h-5" />
            {t.save}
          </button>
          
          {initialData && onDelete && (
            <button
              onClick={() => {
                if (window.confirm(t.confirmDelete)) {
                  onDelete(initialData.id);
                }
              }}
              className="w-full py-3 text-red-500 font-medium rounded-full hover:bg-red-50 transition-colors"
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