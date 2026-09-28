import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ordersApi, Order } from '../api/orders';
import { syncApi } from '../api/sync';
import { OrderDetailDialog } from '../components/OrderDetailDialog';
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
  IconButton,
  Button,
  CircularProgress,
  Pagination,
  Stack,
  Tooltip,
  Alert,
  Snackbar,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import SyncIcon from '@mui/icons-material/Sync';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

export const OrdersPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const [financialFilter, setFinancialFilter] = useState('ALL');
  const [fulfillmentFilter, setFulfillmentFilter] = useState('ALL');
  const [page, setPage] = useState(1);
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  const { data: statsData } = useQuery({
    queryKey: ['orderStats'],
    queryFn: ordersApi.getStats,
  });

  const { data: ordersData, isLoading } = useQuery({
    queryKey: ['orders', search, financialFilter, fulfillmentFilter, page],
    queryFn: () =>
      ordersApi.getOrders({
        query: search || undefined,
        financialStatus: financialFilter !== 'ALL' ? financialFilter : undefined,
        fulfillmentStatus: fulfillmentFilter !== 'ALL' ? fulfillmentFilter : undefined,
        page: page - 1,
        size: 10,
      }),
  });

  const syncMutation = useMutation({
    mutationFn: syncApi.triggerOrderSync,
    onSuccess: (data) => {
      setSnackbarMessage(data.message || 'Shopify Orders sync triggered successfully');
      queryClient.invalidateQueries({ queryKey: ['orders'] });
      queryClient.invalidateQueries({ queryKey: ['orderStats'] });
    },
    onError: () => {
      setSnackbarMessage('Failed to trigger orders sync');
    },
  });

  const handleOpenDetail = (order: Order) => {
    setSelectedOrder(order);
    setDetailOpen(true);
  };

  const getFulfillmentChip = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'FULFILLED':
        return <Chip label="Fulfilled" color="success" size="small" />;
      case 'PARTIALLY_FULFILLED':
        return <Chip label="Partial" color="warning" size="small" />;
      case 'UNFULFILLED':
      default:
        return <Chip label="Unfulfilled" color="error" size="small" variant="outlined" />;
    }
  };

  const getFinancialChip = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'PAID':
        return <Chip label="Paid" color="success" size="small" />;
      case 'REFUNDED':
        return <Chip label="Refunded" color="secondary" size="small" />;
      case 'PARTIALLY_REFUNDED':
        return <Chip label="Partial Refund" color="secondary" size="small" />;
      case 'PENDING':
      default:
        return <Chip label={status || 'Pending'} color="warning" size="small" variant="outlined" />;
    }
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 6 }}>
      {/* Header & Sync Action */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold">
            Orders & Shipments
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Multi-carrier fulfillment tracking, payment transactions, and Shopify order stream
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={syncMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SyncIcon />}
          onClick={() => syncMutation.mutate()}
          disabled={syncMutation.isPending}
        >
          {syncMutation.isPending ? 'Syncing...' : 'Sync Orders'}
        </Button>
      </Box>

      {/* Metrics Cards */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <ShoppingBagIcon color="primary" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOTAL ORDERS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" sx={{ mt: 0.5 }}>
                {statsData?.totalOrders || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AttachMoneyIcon color="success" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">TOTAL SALES</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 0.5 }}>
                ${statsData?.totalSales != null ? statsData.totalSales.toFixed(2) : '0.00'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <WarningAmberIcon color="error" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">UNFULFILLED</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mt: 0.5 }}>
                {statsData?.unfulfilledOrders || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={6} sm={3}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <LocalShippingIcon color="primary" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">FULFILLED</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" sx={{ mt: 0.5 }}>
                {statsData?.fulfilledOrders || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filters and Search Bar */}
      <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={5}>
            <TextField
              fullWidth
              size="small"
              placeholder="Search by order #, customer, or email..."
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
          </Grid>

          <Grid item xs={12} md={7}>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{ alignItems: 'center' }}>
              <Typography variant="caption" fontWeight="bold" color="text.secondary">
                Fulfillment:
              </Typography>
              {['ALL', 'UNFULFILLED', 'PARTIALLY_FULFILLED', 'FULFILLED'].map((f) => (
                <Chip
                  key={f}
                  label={f.replace('_', ' ')}
                  size="small"
                  clickable
                  color={fulfillmentFilter === f ? 'primary' : 'default'}
                  onClick={() => {
                    setFulfillmentFilter(f);
                    setPage(1);
                  }}
                />
              ))}

              <Typography variant="caption" fontWeight="bold" color="text.secondary" sx={{ ml: 1 }}>
                Payment:
              </Typography>
              {['ALL', 'PAID', 'PENDING', 'REFUNDED'].map((f) => (
                <Chip
                  key={f}
                  label={f}
                  size="small"
                  clickable
                  color={financialFilter === f ? 'secondary' : 'default'}
                  onClick={() => {
                    setFinancialFilter(f);
                    setPage(1);
                  }}
                />
              ))}
            </Stack>
          </Grid>
        </Grid>
      </Paper>

      {/* Orders Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 6 }}>
          <CircularProgress />
        </Box>
      ) : (!ordersData?.content || ordersData.content.length === 0) ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>
          No orders found matching the filter criteria. Click "Sync Orders" above to synchronize from your Shopify store.
        </Alert>
      ) : (
        <Paper elevation={1} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: 'action.hover' }}>
              <TableRow>
                <TableCell>Order #</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Payment</TableCell>
                <TableCell>Fulfillment</TableCell>
                <TableCell>Items</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell align="center">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {ordersData.content.map((order) => (
                <TableRow key={order.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="bold" color="primary.main">
                      {order.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      #{order.orderNumber}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {order.shopifyCreatedAt ? new Date(order.shopifyCreatedAt).toLocaleDateString() : '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {order.customerFirstName || ''} {order.customerLastName || 'Guest'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {order.customerEmail || order.email || '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>{getFinancialChip(order.financialStatus)}</TableCell>
                  <TableCell>{getFulfillmentChip(order.fulfillmentStatus)}</TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {order.lineItems?.length || 0} item{order.lineItems?.length !== 1 ? 's' : ''}
                    </Typography>
                    {order.fulfillments && order.fulfillments.length > 0 && (
                      <Typography variant="caption" color="success.main" sx={{ display: 'block' }}>
                        {order.fulfillments.length} package{order.fulfillments.length !== 1 ? 's' : ''}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight="bold">
                      ${order.totalPrice != null ? order.totalPrice.toFixed(2) : '0.00'}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {order.currency}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <Tooltip title="View Order & Tracking Details">
                      <IconButton size="small" color="primary" onClick={() => handleOpenDetail(order)}>
                        <VisibilityIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          {ordersData.totalPages > 1 && (
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'center' }}>
              <Pagination
                count={ordersData.totalPages}
                page={page}
                onChange={(_, p) => setPage(p)}
                color="primary"
              />
            </Box>
          )}
        </Paper>
      )}

      {/* Order Detail Modal */}
      <OrderDetailDialog
        order={selectedOrder}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
      />

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
