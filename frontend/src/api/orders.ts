import { apiClient } from './client';
import { ApiResponse } from './auth';

export interface OrderLineItem {
  id: string;
  shopifyLineItemId: number;
  title: string;
  variantTitle?: string;
  sku?: string;
  quantity: number;
  fulfillableQuantity: number;
  fulfilledQuantity: number;
  price: number;
  totalDiscount: number;
  requiresShipping: boolean;
  taxable: boolean;
}

export interface FulfillmentLineItem {
  id: string;
  quantity: number;
  orderLineItemId: string;
  title?: string;
  sku?: string;
}

export interface Fulfillment {
  id: string;
  shopifyFulfillmentId: number;
  status: string;
  trackingCompany?: string;
  trackingNumber?: string;
  trackingNumbers?: string[];
  trackingUrl?: string;
  trackingUrls?: string[];
  service?: string;
  shipmentStatus?: string;
  estimatedDeliveryAt?: string;
  deliveredAt?: string;
  shopifyCreatedAt?: string;
  items: FulfillmentLineItem[];
}

export interface OrderTransaction {
  id: string;
  shopifyTransactionId: number;
  parentId?: number;
  gateway?: string;
  kind: string;
  status: string;
  amount: number;
  currency: string;
  paymentMethodName?: string;
  errorCode?: string;
  errorMessage?: string;
  processedAt?: string;
}

export interface Order {
  id: string;
  shopifyOrderId: number;
  orderNumber: string;
  name: string;
  email?: string;
  phone?: string;
  financialStatus: string;
  fulfillmentStatus: string;
  currency: string;
  subtotalPrice: number;
  totalDiscounts: number;
  totalTax: number;
  totalShipping: number;
  totalPrice: number;
  cancelledAt?: string;
  cancelReason?: string;
  customerFirstName?: string;
  customerLastName?: string;
  customerEmail?: string;
  shippingAddress?: string;
  billingAddress?: string;
  tags?: string[];
  note?: string;
  shopifyCreatedAt?: string;
  shopifyUpdatedAt?: string;
  syncedAt: string;
  lineItems: OrderLineItem[];
  fulfillments: Fulfillment[];
  transactions: OrderTransaction[];
}

export interface OrderStats {
  totalOrders: number;
  unfulfilledOrders: number;
  partiallyFulfilledOrders: number;
  fulfilledOrders: number;
  paidOrders: number;
  pendingOrders: number;
  totalSales: number;
  abandonedCheckoutsCount: number;
  recoveredCheckoutsCount: number;
  totalAbandonedValue: number;
}

export interface AbandonedCheckout {
  id: string;
  shopifyCheckoutId: number;
  cartToken?: string;
  email?: string;
  phone?: string;
  customerName?: string;
  subtotalPrice: number;
  totalPrice: number;
  currency: string;
  abandonedCheckoutUrl?: string;
  recoveryStatus: string;
  completedAt?: string;
  lineItems?: string;
  shopifyCreatedAt?: string;
  syncedAt: string;
}

export const ordersApi = {
  async getOrders(params?: {
    query?: string;
    financialStatus?: string;
    fulfillmentStatus?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: Order[]; totalElements: number; totalPages: number }> {
    const res = await apiClient.get<ApiResponse<any>>('/orders', { params });
    return res.data.data;
  },

  async getOrderById(id: string): Promise<Order> {
    const res = await apiClient.get<ApiResponse<Order>>(`/orders/${id}`);
    return res.data.data;
  },

  async getStats(): Promise<OrderStats> {
    const res = await apiClient.get<ApiResponse<OrderStats>>('/orders/stats');
    return res.data.data;
  },

  async getAbandonedCheckouts(params?: {
    recoveryStatus?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: AbandonedCheckout[]; totalElements: number; totalPages: number }> {
    const res = await apiClient.get<ApiResponse<any>>('/abandoned-checkouts', { params });
    return res.data.data;
  },

  async getAbandonedStats(): Promise<Record<string, number>> {
    const res = await apiClient.get<ApiResponse<Record<string, number>>>('/abandoned-checkouts/stats');
    return res.data.data;
  },
};
