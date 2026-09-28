import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Grid,
  Chip,
  Divider,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Paper,
  Stack,
  Link,
  Card,
  CardContent,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import PaymentIcon from '@mui/icons-material/Payment';
import PersonIcon from '@mui/icons-material/Person';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import { Order } from '../api/orders';

interface OrderDetailDialogProps {
  order: Order | null;
  open: boolean;
  onClose: () => void;
}

export const OrderDetailDialog: React.FC<OrderDetailDialogProps> = ({ order, open, onClose }) => {
  if (!order) return null;

  const parseAddress = (addressJson?: string) => {
    if (!addressJson) return null;
    try {
      return JSON.parse(addressJson);
    } catch {
      return null;
    }
  };

  const shippingAddr = parseAddress(order.shippingAddress);
  const billingAddr = parseAddress(order.billingAddress);

  const getFulfillmentChip = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'FULFILLED':
        return <Chip label="Fulfilled" color="success" size="small" />;
      case 'PARTIALLY_FULFILLED':
        return <Chip label="Partially Fulfilled" color="warning" size="small" />;
      case 'UNFULFILLED':
      default:
        return <Chip label="Unfulfilled" color="error" size="small" variant="outlined" />;
    }
  };

  const getFinancialChip = (status: string) => {
    switch (status?.toUpperCase()) {
      case 'PAID':
        return <Chip label="Paid" color="success" size="small" />;
      case 'PARTIALLY_REFUNDED':
      case 'REFUNDED':
        return <Chip label={status} color="secondary" size="small" />;
      case 'PENDING':
      default:
        return <Chip label={status || 'Pending'} color="warning" size="small" variant="outlined" />;
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle sx={{ pb: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h5" fontWeight="bold">
              Order {order.name}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Shopify Order ID: {order.shopifyOrderId} • Placed:{' '}
              {order.shopifyCreatedAt ? new Date(order.shopifyCreatedAt).toLocaleString() : 'N/A'}
            </Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            {getFinancialChip(order.financialStatus)}
            {getFulfillmentChip(order.fulfillmentStatus)}
          </Stack>
        </Box>
      </DialogTitle>

      <DialogContent dividers>
        <Grid container spacing={3}>
          {/* Customer & Address Summary */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <PersonIcon fontSize="small" color="primary" /> Customer Details
                </Typography>
                <Typography variant="body2" fontWeight="medium">
                  {order.customerFirstName || ''} {order.customerLastName || 'Guest Customer'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {order.customerEmail || order.email || 'No email provided'}
                </Typography>
                {order.phone && (
                  <Typography variant="body2" color="text.secondary">
                    Phone: {order.phone}
                  </Typography>
                )}

                {shippingAddr && (
                  <Box sx={{ mt: 1.5 }}>
                    <Typography variant="caption" fontWeight="bold" color="text.secondary">
                      SHIPPING ADDRESS:
                    </Typography>
                    <Typography variant="body2">
                      {shippingAddr.address1}, {shippingAddr.city}, {shippingAddr.province} {shippingAddr.zip},{' '}
                      {shippingAddr.country}
                    </Typography>
                  </Box>
                )}

                {billingAddr && (
                  <Box sx={{ mt: 1.5 }}>
                    <Typography variant="caption" fontWeight="bold" color="text.secondary">
                      BILLING ADDRESS:
                    </Typography>
                    <Typography variant="body2">
                      {billingAddr.address1}, {billingAddr.city}, {billingAddr.province} {billingAddr.zip},{' '}
                      {billingAddr.country}
                    </Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>

          {/* Financial Summary */}
          <Grid item xs={12} md={6}>
            <Card variant="outlined" sx={{ height: '100%' }}>
              <CardContent>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <PaymentIcon fontSize="small" color="primary" /> Financial Breakdown
                </Typography>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2" color="text.secondary">Subtotal:</Typography>
                  <Typography variant="body2">${order.subtotalPrice != null ? order.subtotalPrice.toFixed(2) : '0.00'}</Typography>
                </Box>
                {order.totalDiscounts > 0 && (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                    <Typography variant="body2" color="text.secondary">Discounts:</Typography>
                    <Typography variant="body2" color="error.main">-${order.totalDiscounts.toFixed(2)}</Typography>
                  </Box>
                )}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2" color="text.secondary">Shipping:</Typography>
                  <Typography variant="body2">${order.totalShipping != null ? order.totalShipping.toFixed(2) : '0.00'}</Typography>
                </Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Typography variant="body2" color="text.secondary">Tax:</Typography>
                  <Typography variant="body2">${order.totalTax != null ? order.totalTax.toFixed(2) : '0.00'}</Typography>
                </Box>
                <Divider sx={{ my: 1 }} />
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="subtitle2" fontWeight="bold">Total ({order.currency}):</Typography>
                  <Typography variant="subtitle1" fontWeight="bold" color="primary.main">
                    ${order.totalPrice != null ? order.totalPrice.toFixed(2) : '0.00'}
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          {/* Line Items */}
          <Grid item xs={12}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>
              Ordered Items ({order.lineItems?.length || 0})
            </Typography>
            <Paper variant="outlined">
              <Table size="small">
                <TableHead sx={{ bgcolor: 'action.hover' }}>
                  <TableRow>
                    <TableCell>Product / Item</TableCell>
                    <TableCell>SKU</TableCell>
                    <TableCell align="right">Price</TableCell>
                    <TableCell align="center">Qty</TableCell>
                    <TableCell align="right">Total</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {order.lineItems?.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {item.title}
                        </Typography>
                        {item.variantTitle && (
                          <Typography variant="caption" color="text.secondary">
                            Variant: {item.variantTitle}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="caption" fontFamily="monospace">
                          {item.sku || '—'}
                        </Typography>
                      </TableCell>
                      <TableCell align="right">${item.price != null ? item.price.toFixed(2) : '0.00'}</TableCell>
                      <TableCell align="center">
                        <Chip label={`x${item.quantity}`} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                        ${((item.price || 0) * (item.quantity || 1)).toFixed(2)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
          </Grid>

          {/* Fulfillments & Tracking Section */}
          <Grid item xs={12}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <LocalShippingIcon color="primary" fontSize="small" /> Multi-Shipment & Tracking Updates ({order.fulfillments?.length || 0})
            </Typography>
            {(!order.fulfillments || order.fulfillments.length === 0) ? (
              <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', color: 'text.secondary' }}>
                <HourglassEmptyIcon sx={{ fontSize: 32, mb: 0.5 }} />
                <Typography variant="body2">No fulfillment packages generated yet. Order is unfulfilled.</Typography>
              </Paper>
            ) : (
              <Stack spacing={2}>
                {order.fulfillments.map((f, idx) => (
                  <Paper key={f.id} variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5 }}>
                      <Box>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle2" fontWeight="bold">
                            Shipment #{idx + 1}
                          </Typography>
                          <Chip label={f.status} size="small" color="success" />
                          {f.shipmentStatus && <Chip label={f.shipmentStatus} size="small" variant="outlined" />}
                        </Box>
                        <Typography variant="caption" color="text.secondary">
                          Shopify Fulfillment ID: {f.shopifyFulfillmentId} • Created:{' '}
                          {f.shopifyCreatedAt ? new Date(f.shopifyCreatedAt).toLocaleString() : 'N/A'}
                        </Typography>
                      </Box>
                      {f.deliveredAt && (
                        <Chip
                          icon={<CheckCircleOutlineIcon />}
                          label={`Delivered: ${new Date(f.deliveredAt).toLocaleDateString()}`}
                          color="success"
                          variant="outlined"
                          size="small"
                        />
                      )}
                    </Box>

                    <Grid container spacing={2}>
                      <Grid item xs={12} sm={6}>
                        <Typography variant="caption" color="text.secondary" fontWeight="bold">
                          CARRIER / SERVICE:
                        </Typography>
                        <Typography variant="body2">
                          {f.trackingCompany || 'Standard Carrier'} {f.service ? `(${f.service})` : ''}
                        </Typography>
                      </Grid>

                      <Grid item xs={12} sm={6}>
                        <Typography variant="caption" color="text.secondary" fontWeight="bold">
                          TRACKING DETAILS:
                        </Typography>
                        {f.trackingNumber ? (
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Typography variant="body2" fontFamily="monospace" fontWeight="bold">
                              {f.trackingNumber}
                            </Typography>
                            {f.trackingUrl && (
                              <Link href={f.trackingUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.85rem' }}>
                                Track Shipment <OpenInNewIcon fontSize="inherit" />
                              </Link>
                            )}
                          </Box>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            No tracking number assigned
                          </Typography>
                        )}
                      </Grid>
                    </Grid>
                  </Paper>
                ))}
              </Stack>
            )}
          </Grid>

          {/* Transactions Ledger */}
          <Grid item xs={12}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
              <PaymentIcon color="primary" fontSize="small" /> Payment Transactions & Gateway Ledger ({order.transactions?.length || 0})
            </Typography>
            {(!order.transactions || order.transactions.length === 0) ? (
              <Paper variant="outlined" sx={{ p: 2, textAlign: 'center', color: 'text.secondary' }}>
                <Typography variant="body2">No transaction records logged.</Typography>
              </Paper>
            ) : (
              <Paper variant="outlined">
                <Table size="small">
                  <TableHead sx={{ bgcolor: 'action.hover' }}>
                    <TableRow>
                      <TableCell>Kind / Action</TableCell>
                      <TableCell>Gateway / Method</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell align="right">Amount</TableCell>
                      <TableCell>Processed At</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {order.transactions.map((tx) => (
                      <TableRow key={tx.id}>
                        <TableCell>
                          <Chip label={tx.kind} size="small" variant="outlined" color="primary" />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {tx.paymentMethodName || tx.gateway || 'Shopify Payments'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={tx.status}
                            size="small"
                            color={tx.status === 'SUCCESS' ? 'success' : 'error'}
                          />
                        </TableCell>
                        <TableCell align="right" sx={{ fontWeight: 'bold' }}>
                          ${tx.amount != null ? tx.amount.toFixed(2) : '0.00'} {tx.currency}
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" color="text.secondary">
                            {tx.processedAt ? new Date(tx.processedAt).toLocaleString() : 'N/A'}
                          </Typography>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Paper>
            )}
          </Grid>
        </Grid>
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} variant="contained">
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};
