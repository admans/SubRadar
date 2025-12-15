import React, { useState } from 'react';
import { Subscription, BillingCycle, Language } from '../types';
import { Calendar, RefreshCw, Image as ImageIcon } from 'lucide-react';
import { Translation } from '../utils/translations';

interface Props {
  subscription: Subscription;
  onClick: (subscription: Subscription) => void;
  onRenew: (subscription: Subscription) => void;
  t: Translation;
  language: Language;
}

const SubscriptionCard: React.FC<Props> = ({ subscription, onClick, onRenew, t, language }) => {
  const [isRenewing, setIsRenewing] = useState(false);
  
  const today = new Date();
  const nextDate = new Date(subscription.nextBillingDate);
  
  // Reset time parts for accurate date comparison
  today.setHours(0, 0, 0, 0);
  const nextDateCompare = new Date(nextDate);
  nextDateCompare.setHours(0, 0, 0, 0);

  const isToday = nextDateCompare.getTime() === today.getTime();
  const isPast = nextDateCompare.getTime() < today.getTime();
  const showRenewAction = isToday || isPast;
  
  const daysDiff = Math.ceil((nextDateCompare.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

  let statusColor = "bg-surface-variant dark:bg-slate-900 border-transparent";
  let textColor = "text-gray-900 dark:text-gray-100";
  let dateColor = "text-gray-600 dark:text-gray-400";

  if (isToday) {
    statusColor = "bg-amber-100 dark:bg-amber-900/30 border-amber-200 dark:border-amber-800/50";
    textColor = "text-amber-900 dark:text-amber-100";
    dateColor = "text-amber-700 dark:text-amber-300 font-semibold";
  } else if (isPast) {
    statusColor = "bg-red-50 dark:bg-red-900/20 border-red-100 dark:border-red-900/40";
    textColor = "text-red-900 dark:text-red-100";
    dateColor = "text-red-600 dark:text-red-300 font-semibold";
  }

  const locale = language === 'zh' ? 'zh-CN' : 'en-US';
  const dateStr = nextDate.toLocaleDateString(locale, { month: 'short', day: 'numeric', year: 'numeric' });

  const getStatusText = () => {
    if (isToday) return t.dueToday;
    if (isPast) return t.overdue.replace('{days}', Math.abs(daysDiff).toString());
    return t.inDays.replace('{days}', daysDiff.toString());
  };

  const getCycleDisplay = () => {
    const { cycle, customCycleDuration, customCycleUnit } = subscription;

    if (cycle === BillingCycle.Monthly) return ` / ${t.mo}`;
    if (cycle === BillingCycle.Quarterly) return ` / ${t.qtr}`;
    if (cycle === BillingCycle.Yearly) return ` / ${t.yr}`;

    if (cycle === BillingCycle.Custom && customCycleDuration && customCycleUnit) {
      const isPlural = customCycleDuration > 1;
      let unitLabel = '';

      switch (customCycleUnit) {
        case 'day': unitLabel = isPlural ? t.days : t.day; break;
        case 'week': unitLabel = isPlural ? t.weeks : t.week; break;
        case 'month': unitLabel = isPlural ? t.months : t.month; break;
        case 'year': unitLabel = isPlural ? t.years : t.year; break;
      }

      // Convert English units to lowercase for cleaner aesthetic (e.g. "Days" -> "days")
      if (language === 'en') {
        unitLabel = unitLabel.toLowerCase();
      }

      return ` / ${customCycleDuration} ${unitLabel}`;
    }

    return ` / ${t.mo}`;
  };

  const currencySymbol = subscription.currency === 'CNY' ? '¥' : '$';

  const handleRenewClick = (e: React.MouseEvent) => {
    e.stopPropagation(); // Prevent opening the edit modal
    setIsRenewing(true);
    // Small delay for visual feedback
    setTimeout(() => {
      onRenew(subscription);
      setIsRenewing(false);
    }, 300);
  };

  return (
    <div 
      onClick={() => onClick(subscription)}
      className={`relative group p-5 rounded-3xl border ${statusColor} transition-all duration-200 active:scale-[0.98] cursor-pointer shadow-sm hover:shadow-md h-full flex flex-col justify-between`}
    >
      <div className="flex justify-between items-start">
        {/* Left Side: Name, Price, Balance */}
        <div className="flex flex-col gap-1 pr-2">
          <h3 className={`font-semibold text-lg ${textColor} leading-tight`}>{subscription.name}</h3>
          
          <div className="flex items-baseline gap-1">
            <span className={`text-xl font-bold ${textColor}`}>
              {currencySymbol}{subscription.price.toFixed(2)}
            </span>
            <span className="text-xs text-gray-500 dark:text-gray-400 font-medium whitespace-nowrap">
              {getCycleDisplay()}
            </span>
          </div>

          {/* Account Balance Display */}
          {subscription.accountBalance !== undefined && subscription.accountBalance !== null && (
            <div className="text-xs text-gray-500 dark:text-gray-400 font-medium flex items-center gap-1 mt-0.5">
              <span>{t.balanceLabel}:</span>
              <span className="text-gray-700 dark:text-gray-300">{currencySymbol}{subscription.accountBalance.toFixed(2)}</span>
            </div>
          )}

          {/* Image Attachment Indicator */}
          {subscription.image && (
             <div className="mt-1 flex items-center text-primary-600 dark:text-primary-300 text-[10px] font-medium bg-primary-50 dark:bg-primary-900/30 self-start px-1.5 py-0.5 rounded-md">
               <ImageIcon className="w-3 h-3 mr-1" />
               <span className="opacity-90">{t.image}</span> 
             </div>
          )}
        </div>
        
        {/* Right Side: Date, Status */}
        <div className="flex flex-col items-end shrink-0">
          {isToday && !isRenewing && (
            <span className="bg-amber-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm animate-pulse mb-1">
              {t.payToday}
            </span>
          )}

          <div className="flex items-center gap-1.5">
            {!isToday && !isPast && <Calendar className="w-3.5 h-3.5 text-gray-400 dark:text-gray-500" />}
            <span className={`text-base ${dateColor}`}>
              {dateStr}
            </span>
          </div>
          
          <span className={`text-xs font-medium mt-1 ${isToday ? 'text-amber-600 dark:text-amber-400' : isPast ? 'text-red-500 dark:text-red-400' : 'text-gray-400 dark:text-gray-500'}`}>
            {getStatusText()}
          </span>
        </div>
      </div>

      {/* Renew Action Button (Only visible if Today or Past) */}
      {showRenewAction && (
        <div className="mt-4 pt-3 border-t border-black/5 dark:border-white/10 flex justify-end">
          <button
            onClick={handleRenewClick}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-semibold transition-all shadow-sm
              ${isRenewing 
                ? 'bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300 scale-95' 
                : 'bg-white dark:bg-slate-800 text-gray-700 dark:text-gray-200 hover:bg-gray-50 dark:hover:bg-slate-700 active:bg-gray-100 dark:active:bg-slate-700/80'
              }`}
          >
            {isRenewing ? (
               <>
                 <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                 {t.renewed}
               </>
            ) : (
               <>
                 <RefreshCw className="w-3.5 h-3.5" />
                 {t.renew}
               </>
            )}
          </button>
        </div>
      )}
    </div>
  );
};

export default SubscriptionCard;