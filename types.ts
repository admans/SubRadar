export enum BillingCycle {
  Monthly = 'Monthly',
  Yearly = 'Yearly'
}

export type Language = 'en' | 'zh';

export type Currency = 'CNY' | 'USD';

export interface Subscription {
  id: string;
  name: string;
  price: number;
  currency: Currency;
  cycle: BillingCycle;
  nextBillingDate: string; // ISO Date String YYYY-MM-DD
  startDate?: string; // Optional ISO Date String
  accountBalance?: number; // Optional balance
  createdAt: number;
}

export interface AppSettings {
  notificationsEnabled: boolean;
  language: Language;
}