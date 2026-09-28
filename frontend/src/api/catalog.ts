import { apiClient } from './client';
import { ApiResponse } from './auth';

export interface ProductVariant {
  id: string;
  shopifyVariantId: number;
  title: string;
  sku?: string;
  barcode?: string;
  price: number;
  compareAtPrice?: number;
  inventoryQuantity: number;
  position: number;
  imageUrl?: string;
  requiresShipping: boolean;
}

export interface Collection {
  id: string;
  shopifyCollectionId: number;
  title: string;
  handle: string;
  description?: string;
  collectionType: string;
}

export interface Product {
  id: string;
  shopifyProductId: number;
  title: string;
  handle: string;
  description?: string;
  vendor?: string;
  productType?: string;
  tags?: string[];
  status: string;
  totalInventory: number;
  publishedAt?: string;
  shopifyCreatedAt?: string;
  shopifyUpdatedAt?: string;
  syncedAt: string;
  variants: ProductVariant[];
  collections: Collection[];
}

export interface CatalogStats {
  totalProducts: number;
  activeProducts: number;
  draftProducts: number;
  archivedProducts: number;
  totalInventory: number;
  totalCollections: number;
}

export const catalogApi = {
  async getProducts(params?: { query?: string; page?: number; size?: number }): Promise<{ content: Product[]; totalElements: number; totalPages: number }> {
    const res = await apiClient.get<ApiResponse<any>>('/products', { params });
    return res.data.data;
  },

  async getProductById(id: string): Promise<Product> {
    const res = await apiClient.get<ApiResponse<Product>>(`/products/${id}`);
    return res.data.data;
  },

  async getStats(): Promise<CatalogStats> {
    const res = await apiClient.get<ApiResponse<CatalogStats>>('/products/stats');
    return res.data.data;
  },
};
