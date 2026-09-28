import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { catalogApi, Product } from '../api/catalog';
import { syncApi } from '../api/sync';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  TextField,
  InputAdornment,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Paper,
  Chip,
  Button,
  CircularProgress,
  Pagination,
  Alert,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Avatar,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import SyncIcon from '@mui/icons-material/Sync';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import LayersIcon from '@mui/icons-material/Layers';
import VisibilityIcon from '@mui/icons-material/Visibility';

export const ProductsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(1);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  const { data: statsData } = useQuery({
    queryKey: ['catalogStats'],
    queryFn: catalogApi.getStats,
  });

  const { data: productsData, isLoading } = useQuery({
    queryKey: ['products', search, page],
    queryFn: () =>
      catalogApi.getProducts({
        query: search || undefined,
        page: page - 1,
        size: 10,
      }),
  });

  const syncMutation = useMutation({
    mutationFn: syncApi.triggerCatalogSync,
    onSuccess: (data) => {
      setSnackbarMessage(data.message || 'Shopify Catalog sync triggered successfully');
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['catalogStats'] });
    },
    onError: () => {
      setSnackbarMessage('Failed to trigger catalog sync');
    },
  });

  const handleOpenDetail = (product: Product) => {
    setSelectedProduct(product);
    setDetailOpen(true);
  };

  const getStatusChip = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'ACTIVE':
        return <Chip label="Active" color="success" size="small" />;
      case 'DRAFT':
        return <Chip label="Draft" color="warning" size="small" variant="outlined" />;
      case 'ARCHIVED':
      default:
        return <Chip label={status || 'Archived'} color="default" size="small" />;
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 6 }}>
      {/* Header & Sync Action */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Products & Catalog
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Shopify store catalog, multi-variants, pricing, and live inventory sync
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={syncMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SyncIcon />}
          onClick={() => syncMutation.mutate()}
          disabled={syncMutation.isPending}
        >
          {syncMutation.isPending ? 'Syncing...' : 'Sync Catalog'}
        </Button>
      </Box>

      {/* Metrics Cards */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Inventory2Icon color="primary" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOTAL PRODUCTS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" sx={{ mt: 0.5 }}>
                {statsData?.totalProducts || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CheckCircleIcon color="success" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">ACTIVE PRODUCTS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 0.5 }}>
                {statsData?.activeProducts || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <LayersIcon color="secondary" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOTAL VARIANTS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" sx={{ mt: 0.5 }}>
                {statsData?.totalInventory || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <WarningIcon color="warning" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">LOW STOCK / DRAFT</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="warning.main" sx={{ mt: 0.5 }}>
                {statsData?.draftProducts || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Search Bar */}
      <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <TextField
          fullWidth
          size="small"
          placeholder="Search products by title, vendor, or handle..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(1);
          }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          }}
        />
      </Paper>

      {/* Products Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 6 }}>
          <CircularProgress />
        </Box>
      ) : (!productsData?.content || productsData.content.length === 0) ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>
          No products found. Click "Sync Catalog" above to import products from your connected Shopify store.
        </Alert>
      ) : (
        <Paper elevation={1} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: 'action.hover' }}>
              <TableRow>
                <TableCell>Product</TableCell>
                <TableCell>Vendor</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Variants</TableCell>
                <TableCell>Total Inventory</TableCell>
                <TableCell>Price Range</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {productsData.content.map((product) => {
                const prices = product.variants?.map((v) => v.price) || [0];
                const minPrice = prices.length > 0 ? Math.min(...prices) : 0;
                const maxPrice = prices.length > 0 ? Math.max(...prices) : 0;
                const firstImg = product.variants?.find((v) => v.imageUrl)?.imageUrl;

                return (
                  <TableRow key={product.id} hover>
                    <TableCell>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Avatar
                          src={firstImg}
                          variant="rounded"
                          sx={{ width: 44, height: 44, bgcolor: 'primary.light' }}
                        >
                          <Inventory2Icon fontSize="small" />
                        </Avatar>
                        <Box>
                          <Typography variant="body2" fontWeight="bold">
                            {product.title}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            handle: {product.handle}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">{product.vendor || '—'}</Typography>
                    </TableCell>
                    <TableCell>{getStatusChip(product.status)}</TableCell>
                    <TableCell>
                      <Chip label={`${product.variants?.length || 0} variants`} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={`${product.totalInventory || 0} in stock`}
                        size="small"
                        color={product.totalInventory > 0 ? 'success' : 'error'}
                        variant={product.totalInventory > 0 ? 'filled' : 'outlined'}
                      />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {minPrice === maxPrice
                          ? `$${minPrice.toFixed(2)}`
                          : `$${minPrice.toFixed(2)} - $${maxPrice.toFixed(2)}`}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Button
                        size="small"
                        variant="outlined"
                        startIcon={<VisibilityIcon fontSize="small" />}
                        onClick={() => handleOpenDetail(product)}
                      >
                        Variants
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>

          {/* Pagination */}
          {productsData.totalPages > 1 && (
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'center' }}>
              <Pagination
                count={productsData.totalPages}
                page={page}
                onChange={(_, p) => setPage(p)}
                color="primary"
              />
            </Box>
          )}
        </Paper>
      )}

      {/* Product Detail Modal */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        {selectedProduct && (
          <>
            <DialogTitle>
              <Typography variant="h6" fontWeight="bold">
                {selectedProduct.title}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Shopify Product ID: {selectedProduct.shopifyProductId} • Handle: {selectedProduct.handle}
              </Typography>
            </DialogTitle>
            <DialogContent dividers>
              <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>
                Product Variants ({selectedProduct.variants?.length || 0})
              </Typography>
              <Table size="small">
                <TableHead sx={{ bgcolor: 'action.hover' }}>
                  <TableRow>
                    <TableCell>Variant Title</TableCell>
                    <TableCell>SKU</TableCell>
                    <TableCell>Barcode</TableCell>
                    <TableCell align="right">Price</TableCell>
                    <TableCell align="right">Inventory</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {selectedProduct.variants?.map((v) => (
                    <TableRow key={v.id}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {v.title}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" fontFamily="monospace">
                          {v.sku || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption">{v.barcode || '—'}</Typography>
                      </TableCell>
                      <TableCell align="right">${v.price != null ? v.price.toFixed(2) : '0.00'}</TableCell>
                      <TableCell align="right">
                        <Chip
                          label={`${v.inventoryQuantity} qty`}
                          size="small"
                          color={v.inventoryQuantity > 0 ? 'success' : 'error'}
                          variant="outlined"
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </DialogContent>
            <DialogActions sx={{ px: 3, py: 2 }}>
              <Button onClick={() => setDetailOpen(false)} variant="contained">
                Close
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>

      {/* Feedback Snackbar */}
      <Snackbar
        open={Boolean(snackbarMessage)}
        autoHideDuration={4000}
        onClose={() => setSnackbarMessage(null)}
        message={snackbarMessage}
      />
    </Container>
  );
};
