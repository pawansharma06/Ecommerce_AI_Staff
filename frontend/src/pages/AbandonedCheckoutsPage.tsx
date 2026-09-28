import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ordersApi } from '../api/orders';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
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
  Stack,
  Alert,
  Link,
} from '@mui/material';
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';

export const AbandonedCheckoutsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(1);

  const { data: statsData } = useQuery({
    queryKey: ['orderStats'],
    queryFn: ordersApi.getStats,
  });

  const { data: checkoutsData, isLoading } = useQuery({
    queryKey: ['abandonedCheckouts', statusFilter, page],
    queryFn: () =>
      ordersApi.getAbandonedCheckouts({
        recoveryStatus: statusFilter !== 'ALL' ? statusFilter : undefined,
        page: page - 1,
        size: 10,
      }),
  });

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 6 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold">
          Abandoned Checkouts
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Track high-intent customers who left cart at checkout and initiate direct recovery links
        </Typography>
      </Box>

      {/* Metrics Cards */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={4}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <ShoppingCartCheckoutIcon color="error" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">ABANDONED CARTS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="error.main" sx={{ mt: 0.5 }}>
                {statsData?.abandonedCheckoutsCount || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <CheckCircleIcon color="success" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">RECOVERED CARTS</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="success.main" sx={{ mt: 0.5 }}>
                {statsData?.recoveredCheckoutsCount || 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card elevation={1} sx={{ borderRadius: 2 }}>
            <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <AttachMoneyIcon color="warning" fontSize="small" />
                <Typography variant="caption" color="text.secondary" fontWeight="bold">ABANDONED VALUE</Typography>
              </Box>
              <Typography variant="h5" fontWeight="bold" color="warning.main" sx={{ mt: 0.5 }}>
                ${statsData?.totalAbandonedValue != null ? statsData.totalAbandonedValue.toFixed(2) : '0.00'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Filter Chips */}
      <Paper sx={{ p: 2, mb: 3, borderRadius: 2 }}>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography variant="caption" fontWeight="bold" color="text.secondary">
            Status:
          </Typography>
          {['ALL', 'ABANDONED', 'RECOVERED'].map((s) => (
            <Chip
              key={s}
              label={s}
              size="small"
              clickable
              color={statusFilter === s ? 'primary' : 'default'}
              onClick={() => {
                setStatusFilter(s);
                setPage(1);
              }}
            />
          ))}
        </Stack>
      </Paper>

      {/* Abandoned Checkouts Table */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', my: 6 }}>
          <CircularProgress />
        </Box>
      ) : (!checkoutsData?.content || checkoutsData.content.length === 0) ? (
        <Alert severity="info" sx={{ borderRadius: 2 }}>
          No abandoned checkouts recorded. Checkouts are automatically ingested via webhooks and order synchronization.
        </Alert>
      ) : (
        <Paper elevation={1} sx={{ borderRadius: 2, overflow: 'hidden' }}>
          <Table>
            <TableHead sx={{ bgcolor: 'action.hover' }}>
              <TableRow>
                <TableCell>Customer</TableCell>
                <TableCell>Email / Phone</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Recovery Status</TableCell>
                <TableCell align="right">Cart Total</TableCell>
                <TableCell align="center">Recovery Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {checkoutsData.content.map((checkout) => (
                <TableRow key={checkout.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {checkout.customerName?.trim() || 'Guest Customer'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">{checkout.email || '—'}</Typography>
                    {checkout.phone && (
                      <Typography variant="caption" color="text.secondary">
                        {checkout.phone}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2">
                      {checkout.shopifyCreatedAt ? new Date(checkout.shopifyCreatedAt).toLocaleString() : '—'}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={checkout.recoveryStatus}
                      size="small"
                      color={checkout.recoveryStatus === 'RECOVERED' ? 'success' : 'error'}
                      variant={checkout.recoveryStatus === 'RECOVERED' ? 'filled' : 'outlined'}
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Typography variant="body2" fontWeight="bold">
                      ${checkout.totalPrice != null ? checkout.totalPrice.toFixed(2) : '0.00'} {checkout.currency}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    {checkout.abandonedCheckoutUrl ? (
                      <Button
                        size="small"
                        variant="outlined"
                        color="primary"
                        endIcon={<OpenInNewIcon fontSize="small" />}
                        component={Link}
                        href={checkout.abandonedCheckoutUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        sx={{ textTransform: 'none' }}
                      >
                        Open Cart
                      </Button>
                    ) : (
                      <Typography variant="caption" color="text.secondary">
                        —
                      </Typography>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>

          {/* Pagination */}
          {checkoutsData.totalPages > 1 && (
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'center' }}>
              <Pagination
                count={checkoutsData.totalPages}
                page={page}
                onChange={(_, p) => setPage(p)}
                color="primary"
              />
            </Box>
          )}
        </Paper>
      )}
    </Container>
  );
};
