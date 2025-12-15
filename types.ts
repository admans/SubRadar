export enum BillingCycle {
  Monthly = 'Monthly',
  Quarterly = 'Quarterly',
  Yearly = 'Yearly',
  Custom = 'Custom'
}

export type CycleUnit = 'day' | 'week' | 'month' | 'year';

export type Language = 'en' | 'zh';

export type Currency = 'CNY' | 'USD';

export interface Subscription {
  id: string;
  name: string;
  price: number;
  currency: Currency;
  cycle: BillingCycle;
  
  // Custom Cycle Fields
  customCycleDuration?: number;
  customCycleUnit?: CycleUnit;

  nextBillingDate: string; // ISO Date String YYYY-MM-DD
  startDate?: string; // Optional ISO Date String
  accountBalance?: number; // Optional balance
  
  // Notes & Media
  notes?: string;
  image?: string; // Base64 string

  createdAt: number;
}

export interface AppSettings {
  notificationsEnabled: boolean;
  language: Language;
}