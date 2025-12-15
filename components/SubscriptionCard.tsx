import React, { useState } from 'react';
import { Subscription, BillingCycle, Language } from '../types';
import { Calendar, RefreshCw } from 'lucide-react';
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

  let statusColor = "bg-surface-variant border-transparent";
  let textColor = "text-gray-900";
  let dateColor = "text-gray-600";

  if (isToday) {
    statusColor = "bg-amber-100 border-amber-200";
    textColor = "text-amber-900";
    dateColor = "text-amber-700 font-semibold";
  } else if (isPast) {
    statusColor = "bg-red-50 border-red-100";
    textColor = "text-red-900";
    dateColor = "text-red-600 font-semibold";
  }

  const locale = language === 'zh' ? 'zh-CN' : 'en-US';
  const dateStr = nextDate.toLocaleDateString(locale, { month: 'short', day: 'numeric', year: 'numeric' });

  const getStatusText = () => {
    if (isToday) return t.dueToday;
    if (isPast) return t.overdue.replace('{days}', Math.abs(daysDiff).toString());
    return t.inDays.replace('{days}', daysDiff.toString());
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
            <span className="text-xs text-gray-500 font-medium">
              / {subscription.cycle === BillingCycle.Monthly ? t.mo : t.yr}
            </span>
          </div>

          {/* Account Balance Display */}
          {subscription.accountBalance !== undefined && subscription.accountBalance !== null && (
            <div className="text-xs text-gray-500 font-medium flex items-center gap-1 mt-0.5">
              <span>{t.balanceLabel}:</span>
              <span className="text-gray-700">{currencySymbol}{subscription.accountBalance.toFixed(2)}</span>
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
            {!isToday && !isPast && <Calendar className="w-3.5 h-3.5 text-gray-400" />}
            <span className={`text-base ${dateColor}`}>
              {dateStr}
            </span>
          </div>
          
          <span className={`text-xs font-medium mt-1 ${isToday ? 'text-amber-600' : isPast ? 'text-red-500' : 'text-gray-400'}`}>
            {getStatusText()}
          </span>
        </div>
      </div>

      {/* Renew Action Button (Only visible if Today or Past) */}
      {showRenewAction && (
        <div className="mt-4 pt-3 border-t border-black/5 flex justify-end">
          <button
            onClick={handleRenewClick}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-semibold transition-all shadow-sm
              ${isRenewing 
                ? 'bg-green-100 text-green-700 scale-95' 
                : 'bg-white text-gray-700 hover:bg-gray-50 active:bg-gray-100'
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