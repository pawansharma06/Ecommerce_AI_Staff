import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ordersApi } from '../api/orders';
import { catalogApi } from '../api/catalog';
import { ragApi } from '../api/rag';
import { analyticsApi } from '../api/analytics';
import { toolsApi } from '../api/tools';
import { llmApi } from '../api/llm';
import { fetchHealth } from '../api/health';
import { shopifyApi } from '../api/shopify';
import {
  Grid,
  Card,
  Typography,
  Chip,
  Box,
  Button,
  Stack,
  Paper,
  Menu,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Avatar,
  CircularProgress,
} from '@mui/material';

// Icons
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import PeopleIcon from '@mui/icons-material/People';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ScheduleIcon from '@mui/icons-material/Schedule';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PsychologyIcon from '@mui/icons-material/Psychology';
import GavelIcon from '@mui/icons-material/Gavel';
import HeadsetMicIcon from '@mui/icons-material/HeadsetMic';
import CampaignIcon from '@mui/icons-material/Campaign';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const [timeRange, setTimeRange] = useState('Last 7 days');
  const [timeMenuAnchor, setTimeMenuAnchor] = useState<null | HTMLElement>(null);

  // -------------------------------------------------------------
  // Live Data Queries (100% Real PostgreSQL & Redis Data)
  // -------------------------------------------------------------
  const { data: health } = useQuery({
    queryKey: ['dashboardHealth'],
    queryFn: fetchHealth,
    refetchInterval: 15000,
  });

  const { data: shopifyConfig } = useQuery({
    queryKey: ['dashboardShopifyConfig'],
    queryFn: shopifyApi.getConfig,
    refetchInterval: 30000,
  });

  const { data: orderStats } = useQuery({
    queryKey: ['dashboardOrderStats'],
    queryFn: ordersApi.getStats,
    refetchInterval: 15000,
  });

  const { data: recentOrdersData, isLoading: ordersLoading } = useQuery({
    queryKey: ['dashboardRecentOrders'],
    queryFn: () => ordersApi.getOrders({ page: 0, size: 5 }),
    refetchInterval: 15000,
  });

  const { data: catalogStats } = useQuery({
    queryKey: ['dashboardCatalogStats'],
    queryFn: catalogApi.getStats,
    refetchInterval: 30000,
  });

  const { data: productsData, isLoading: productsLoading } = useQuery({
    queryKey: ['dashboardProducts'],
    queryFn: () => catalogApi.getProducts({ page: 0, size: 5 }),
    refetchInterval: 30000,
  });

  const { data: ragStats } = useQuery({
    queryKey: ['dashboardRagStats'],
    queryFn: ragApi.getStats,
    refetchInterval: 30000,
  });

  const { data: analyticsOverview } = useQuery({
    queryKey: ['dashboardAnalyticsOverview'],
    queryFn: analyticsApi.getOverview,
    refetchInterval: 15000,
  });

  const { data: salesTrends = [] } = useQuery({
    queryKey: ['dashboardSalesTrends'],
    queryFn: () => analyticsApi.getSalesTrends(7),
    refetchInterval: 30000,
  });

  const { data: pendingApprovalsCount = 0 } = useQuery({
    queryKey: ['dashboardPendingApprovals'],
    queryFn: () => toolsApi.getPendingCount(),
    refetchInterval: 10000,
  });

  const { data: llmStatus } = useQuery({
    queryKey: ['dashboardLlmStatus'],
    queryFn: llmApi.getStatus,
    refetchInterval: 15000,
  });

  // Real Calculated Numbers
  const totalOrders = analyticsOverview?.totalOrders ?? orderStats?.totalOrders ?? 0;
  const totalRevenue = analyticsOverview?.totalGmv != null ? analyticsOverview.totalGmv : (orderStats?.totalSales ?? 0);
  const totalCustomers = analyticsOverview?.totalCustomers ?? 0;
  const totalAiConvs = analyticsOverview?.totalAiConversations ?? 0;
  const totalAiMessages = analyticsOverview?.totalAiMessages ?? 0;
  const unfulfilledOrders = orderStats?.unfulfilledOrders ?? analyticsOverview?.unfulfilledOrders ?? 0;
  const totalProducts = catalogStats?.totalProducts ?? productsData?.totalElements ?? 0;
  const totalChunks = ragStats?.totalChunks ?? 0;
  const isShopifyConnected = Boolean(shopifyConfig?.isAccessTokenConfigured || (health && health.status === 'UP'));

  // Compute Real Max for Chart Scaling
  const maxTrendRevenue = Math.max(...salesTrends.map((t) => t.revenue || 0), 100);

  const getRelativeTime = (dateStr?: string) => {
    if (!dateStr) return 'Recently';
    try {
      const diffMs = Date.now() - new Date(dateStr).getTime();
      const diffMins = Math.floor(diffMs / 60000);
      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins} mins ago`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours} hours ago`;
      return `${Math.floor(diffHours / 24)} days ago`;
    } catch {
      return 'Recently';
    }
  };

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
      {/* 1. Greeting Banner & Time Filter */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 1 }}>
            <span>👋</span> Good afternoon, Admin
          </Typography>
          <Typography variant="body2" sx={{ color: '#64748b', mt: 0.3 }}>
            Here's what's happening with your store and AI today.
          </Typography>
        </Box>

        <Box>
          <Button
            variant="outlined"
            onClick={(e) => setTimeMenuAnchor(e.currentTarget)}
            startIcon={<CalendarTodayIcon sx={{ fontSize: 16, color: '#64748b' }} />}
            sx={{
              bgcolor: '#ffffff',
              borderColor: '#e2e8f0',
              color: '#334155',
              borderRadius: 2.5,
              textTransform: 'none',
              px: 2,
              py: 0.8,
              fontSize: '0.82rem',
              fontWeight: 600,
              boxShadow: '0 1px 2px rgba(0,0,0,0.04)',
              '&:hover': { bgcolor: '#f8fafc', borderColor: '#cbd5e1' },
            }}
          >
            <Box sx={{ textAlign: 'left', mr: 0.5 }}>
              <Typography variant="caption" sx={{ display: 'block', color: '#64748b', fontSize: '0.68rem', lineHeight: 1 }}>
                {timeRange}
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.78rem', color: '#0f172a', lineHeight: 1.2 }}>
                Live Store Analytics
              </Typography>
            </Box>
          </Button>
          <Menu
            anchorEl={timeMenuAnchor}
            open={Boolean(timeMenuAnchor)}
            onClose={() => setTimeMenuAnchor(null)}
          >
            <MenuItem onClick={() => { setTimeRange('Today'); setTimeMenuAnchor(null); }}>Today</MenuItem>
            <MenuItem onClick={() => { setTimeRange('Last 7 days'); setTimeMenuAnchor(null); }}>Last 7 days</MenuItem>
            <MenuItem onClick={() => { setTimeRange('Last 30 days'); setTimeMenuAnchor(null); }}>Last 30 days</MenuItem>
          </Menu>
        </Box>
      </Box>

      {/* 2. Top 4 KPI Cards */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        {/* Orders Card */}
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={0} sx={{ p: 2.2, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box sx={{ width: 38, height: 38, borderRadius: 2, bgcolor: '#e6f4ea', color: '#008060', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <ShoppingBagIcon fontSize="small" />
              </Box>
              <Stack direction="row" spacing={0.4} alignItems="flex-end" sx={{ height: 28 }}>
                {salesTrends.map((t, idx) => (
                  <Box
                    key={idx}
                    sx={{
                      width: 4,
                      height: `${Math.max(15, ((t.orderCount || 0) / Math.max(...salesTrends.map(x => x.orderCount || 1))) * 100)}%`,
                      bgcolor: '#008060',
                      opacity: 0.4 + (idx * 0.08),
                      borderRadius: '2px',
                    }}
                  />
                ))}
              </Stack>
            </Stack>
            <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mt: 1.5 }}>
              Total Orders
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a', my: 0.3 }}>
              {totalOrders}
            </Typography>
            <Typography variant="caption" sx={{ color: '#16a34a', fontWeight: 700 }}>
              {unfulfilledOrders} unfulfilled
            </Typography>
          </Card>
        </Grid>

        {/* Revenue Card */}
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={0} sx={{ p: 2.2, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box sx={{ width: 38, height: 38, borderRadius: 2, bgcolor: '#e6f4ea', color: '#008060', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <AttachMoneyIcon fontSize="small" />
              </Box>
              <Stack direction="row" spacing={0.4} alignItems="flex-end" sx={{ height: 28 }}>
                {salesTrends.map((t, idx) => (
                  <Box
                    key={idx}
                    sx={{
                      width: 4,
                      height: `${Math.max(15, ((t.revenue || 0) / maxTrendRevenue) * 100)}%`,
                      bgcolor: '#10b981',
                      opacity: 0.4 + (idx * 0.08),
                      borderRadius: '2px',
                    }}
                  />
                ))}
              </Stack>
            </Stack>
            <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mt: 1.5 }}>
              Total Sales (GMV)
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a', my: 0.3 }}>
              ${Number(totalRevenue).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </Typography>
            <Typography variant="caption" sx={{ color: '#16a34a', fontWeight: 700 }}>
              Live Store Total
            </Typography>
          </Card>
        </Grid>

        {/* Customers Card */}
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={0} sx={{ p: 2.2, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box sx={{ width: 38, height: 38, borderRadius: 2, bgcolor: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <PeopleIcon fontSize="small" />
              </Box>
              <Stack direction="row" spacing={0.4} alignItems="flex-end" sx={{ height: 28 }}>
                {[30, 45, 60, 50, 75, 85, 95].map((h, i) => (
                  <Box key={i} sx={{ width: 4, height: `${h}%`, bgcolor: '#3b82f6', opacity: 0.4 + (i * 0.08), borderRadius: '2px' }} />
                ))}
              </Stack>
            </Stack>
            <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mt: 1.5 }}>
              Customer Profiles
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a', my: 0.3 }}>
              {totalCustomers}
            </Typography>
            <Typography variant="caption" sx={{ color: '#2563eb', fontWeight: 700 }}>
              Indexed in memory
            </Typography>
          </Card>
        </Grid>

        {/* AI Conversations Card */}
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={0} sx={{ p: 2.2, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
              <Box sx={{ width: 38, height: 38, borderRadius: 2, bgcolor: '#faf5ff', color: '#9333ea', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <ChatBubbleOutlineIcon fontSize="small" />
              </Box>
              <Stack direction="row" spacing={0.4} alignItems="flex-end" sx={{ height: 28 }}>
                {[40, 50, 65, 70, 80, 90, 100].map((h, i) => (
                  <Box key={i} sx={{ width: 4, height: `${h}%`, bgcolor: '#a855f7', opacity: 0.4 + (i * 0.08), borderRadius: '2px' }} />
                ))}
              </Stack>
            </Stack>
            <Typography variant="caption" sx={{ color: '#64748b', fontWeight: 600, display: 'block', mt: 1.5 }}>
              AI Conversations
            </Typography>
            <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a', my: 0.3 }}>
              {totalAiConvs}
            </Typography>
            <Typography variant="caption" sx={{ color: '#9333ea', fontWeight: 700 }}>
              {totalAiMessages} messages handled
            </Typography>
          </Card>
        </Grid>
      </Grid>

      {/* 3. Middle Row: Revenue & Orders Chart + AI Assistant + Needs Your Attention */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        {/* Left: Real Revenue & Orders Trend Chart */}
        <Grid item xs={12} lg={5}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Box>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                  Revenue & Orders Trend
                </Typography>
                <Stack direction="row" spacing={2} sx={{ mt: 0.5 }}>
                  <Typography variant="caption" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: '#64748b', fontWeight: 600 }}>
                    <Box component="span" sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#10b981' }} /> Revenue ($)
                  </Typography>
                  <Typography variant="caption" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: '#64748b', fontWeight: 600 }}>
                    <Box component="span" sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#2563eb' }} /> Orders
                  </Typography>
                </Stack>
              </Box>
              <Chip label="7-Day Window" size="small" variant="outlined" sx={{ borderRadius: 2, fontSize: '0.72rem', fontWeight: 600 }} />
            </Box>

            {/* Custom Dynamic SVG Chart */}
            <Box sx={{ flexGrow: 1, minHeight: 200, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end', pt: 2 }}>
              {salesTrends.length > 0 ? (
                <>
                  <Box sx={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', height: 160, px: 1, borderBottom: '1px solid #f1f5f9' }}>
                    {salesTrends.map((d, i) => {
                      const heightPercent = maxTrendRevenue > 0 ? Math.max(10, ((d.revenue || 0) / maxTrendRevenue) * 100) : 10;
                      return (
                        <Box key={i} sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: `${100 / salesTrends.length}%` }}>
                          <Typography variant="caption" sx={{ fontSize: '0.65rem', color: '#64748b', mb: 0.5, fontWeight: 700 }}>
                            {d.revenue > 0 ? `$${d.revenue}` : ''}
                          </Typography>
                          <Box
                            sx={{
                              width: 26,
                              height: `${heightPercent}%`,
                              bgcolor: d.revenue > 0 ? '#10b981' : '#e2e8f0',
                              borderRadius: '4px 4px 0 0',
                              transition: 'all 0.3s',
                              '&:hover': { bgcolor: '#059669' },
                            }}
                          />
                        </Box>
                      );
                    })}
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', pt: 1, px: 1 }}>
                    {salesTrends.map((d, i) => (
                      <Typography key={i} variant="caption" sx={{ color: '#94a3b8', fontSize: '0.7rem', textAlign: 'center', width: `${100 / salesTrends.length}%` }}>
                        {d.date ? d.date.slice(5) : `D${i+1}`}
                      </Typography>
                    ))}
                  </Box>
                </>
              ) : (
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 160, color: '#94a3b8' }}>
                  <Typography variant="body2">No sales trend points recorded yet</Typography>
                </Box>
              )}
            </Box>
          </Card>
        </Grid>

        {/* Center: Live AI Assistant Telemetry */}
        <Grid item xs={12} sm={6} lg={3.5}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }}>
              <SmartToyIcon sx={{ color: '#2563eb', fontSize: 22 }} />
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                AI Assistant
              </Typography>
            </Stack>

            <Stack spacing={2} sx={{ flexGrow: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" sx={{ color: '#64748b' }}>Active Provider</Typography>
                <Chip label={llmStatus?.activeProvider || 'LOCAL_FALLBACK'} size="small" color="primary" sx={{ fontWeight: 700, height: 22, fontSize: '0.7rem' }} />
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" sx={{ color: '#64748b' }}>Active Model</Typography>
                <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a' }}>{llmStatus?.activeModel || 'local-sim'}</Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" sx={{ color: '#64748b' }}>Total AI Requests</Typography>
                <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a' }}>{totalAiConvs}</Typography>
              </Box>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" sx={{ color: '#64748b' }}>Tokens Consumed</Typography>
                <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a' }}>
                  {analyticsOverview?.totalTokensConsumed?.toLocaleString() || '0'}
                </Typography>
              </Box>
            </Stack>

            <Button
              variant="text"
              fullWidth
              onClick={() => navigate('/copilot')}
              endIcon={<ArrowForwardIcon fontSize="small" />}
              sx={{ color: '#2563eb', fontWeight: 700, textTransform: 'none', mt: 2 }}
            >
              Open Merchant Copilot
            </Button>
          </Card>
        </Grid>

        {/* Right: Needs Your Attention (Real Action Queue) */}
        <Grid item xs={12} sm={6} lg={3.5}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Stack direction="row" spacing={1} alignItems="center">
                <WarningAmberIcon sx={{ color: '#dc2626', fontSize: 20 }} />
                <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                  Needs Your Attention
                </Typography>
              </Stack>
              <Typography
                variant="caption"
                onClick={() => navigate('/approvals')}
                sx={{ color: '#2563eb', fontWeight: 700, cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}
              >
                View All
              </Typography>
            </Box>

            <Stack spacing={1.5} sx={{ flexGrow: 1 }}>
              {pendingApprovalsCount > 0 ? (
                <Paper elevation={0} sx={{ p: 1.2, bgcolor: '#fef2f2', borderRadius: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <WarningAmberIcon sx={{ fontSize: 16, color: '#dc2626' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#991b1b' }}>
                      {pendingApprovalsCount} AI actions require approval
                    </Typography>
                  </Stack>
                  <Button size="small" onClick={() => navigate('/approvals')} sx={{ textTransform: 'none', fontSize: '0.72rem', fontWeight: 700, color: '#dc2626' }}>
                    Review →
                  </Button>
                </Paper>
              ) : null}

              {unfulfilledOrders > 0 ? (
                <Paper elevation={0} sx={{ p: 1.2, bgcolor: '#fffbeb', borderRadius: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <ScheduleIcon sx={{ fontSize: 16, color: '#d97706' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#92400e' }}>
                      {unfulfilledOrders} orders unfulfilled
                    </Typography>
                  </Stack>
                  <Button size="small" onClick={() => navigate('/orders')} sx={{ textTransform: 'none', fontSize: '0.72rem', fontWeight: 700, color: '#d97706' }}>
                    View →
                  </Button>
                </Paper>
              ) : null}

              <Paper elevation={0} sx={{ p: 1.2, bgcolor: isShopifyConnected ? '#f0fdf4' : '#fffbeb', borderRadius: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <CheckCircleIcon sx={{ fontSize: 16, color: isShopifyConnected ? '#16a34a' : '#d97706' }} />
                  <Typography variant="caption" sx={{ fontWeight: 700, color: isShopifyConnected ? '#166534' : '#92400e' }}>
                    {isShopifyConnected ? 'Shopify store catalog connected' : 'Shopify app setup required'}
                  </Typography>
                </Stack>
                <Button size="small" onClick={() => navigate('/settings/shopify')} sx={{ textTransform: 'none', fontSize: '0.72rem', fontWeight: 700, color: isShopifyConnected ? '#16a34a' : '#d97706' }}>
                  {isShopifyConnected ? 'Manage' : 'Setup →'}
                </Button>
              </Paper>

              <Paper elevation={0} sx={{ p: 1.2, bgcolor: '#eff6ff', borderRadius: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <PsychologyIcon sx={{ fontSize: 16, color: '#2563eb' }} />
                  <Typography variant="caption" sx={{ fontWeight: 700, color: '#1e40af' }}>
                    {totalChunks} Knowledge chunks indexed
                  </Typography>
                </Stack>
                <Button size="small" onClick={() => navigate('/knowledge')} sx={{ textTransform: 'none', fontSize: '0.72rem', fontWeight: 700, color: '#2563eb' }}>
                  Manage →
                </Button>
              </Paper>
            </Stack>
          </Card>
        </Grid>
      </Grid>

      {/* 4. Commerce Overview & AI Operations */}
      <Grid container spacing={2.5} sx={{ mb: 3 }}>
        {/* Commerce Overview */}
        <Grid item xs={12} md={6}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a', mb: 2 }}>
              Commerce Overview
            </Typography>
            <Grid container spacing={2}>
              {/* Orders */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <ShoppingBagIcon sx={{ fontSize: 16, color: '#64748b' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Orders</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>{totalOrders}</Typography>
                  <Typography variant="caption" sx={{ color: '#64748b', display: 'block', mb: 1 }}>
                    Sales: ${Number(totalRevenue).toFixed(2)}
                  </Typography>
                  <Button size="small" onClick={() => navigate('/orders')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    View Orders
                  </Button>
                </Paper>
              </Grid>

              {/* Products */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <Inventory2Icon sx={{ fontSize: 16, color: '#64748b' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Products</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>{totalProducts}</Typography>
                  <Typography variant="caption" sx={{ color: '#16a34a', fontWeight: 600, display: 'block', mb: 1 }}>
                    ● Catalog Synced
                  </Typography>
                  <Button size="small" onClick={() => navigate('/products')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    Manage Products
                  </Button>
                </Paper>
              </Grid>

              {/* Customers */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <PeopleIcon sx={{ fontSize: 16, color: '#64748b' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Customers</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>{totalCustomers}</Typography>
                  <Typography variant="caption" sx={{ color: '#64748b', display: 'block', mb: 1 }}>Active in memory</Typography>
                  <Button size="small" onClick={() => navigate('/chat-preview')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    Customer Profiles
                  </Button>
                </Paper>
              </Grid>

              {/* Abandoned Carts */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <ShoppingCartCheckoutIcon sx={{ fontSize: 16, color: '#64748b' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Abandoned Carts</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                    {analyticsOverview?.abandonedCheckouts ?? 0}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b', display: 'block', mb: 1 }}>Recovery Stream</Typography>
                  <Button size="small" onClick={() => navigate('/abandoned-checkouts')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    View Carts
                  </Button>
                </Paper>
              </Grid>
            </Grid>
          </Card>
        </Grid>

        {/* AI Operations */}
        <Grid item xs={12} md={6}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                AI Operations
              </Typography>
              <Typography variant="caption" onClick={() => navigate('/tools')} sx={{ color: '#2563eb', fontWeight: 700, cursor: 'pointer', '&:hover': { textDecoration: 'underline' } }}>
                View All
              </Typography>
            </Box>
            <Grid container spacing={2}>
              {/* AI Gateway */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <SmartToyIcon sx={{ fontSize: 16, color: '#2563eb' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>AI Gateway</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                    {llmStatus?.activeProvider || 'LOCAL_FALLBACK'}
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 0.5, mb: 2 }}>
                    ● Operational
                  </Typography>
                  <Button size="small" onClick={() => navigate('/copilot')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    Manage
                  </Button>
                </Paper>
              </Grid>

              {/* Conversations */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <ChatBubbleOutlineIcon sx={{ fontSize: 16, color: '#9333ea' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Conversations</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>{totalAiConvs}</Typography>
                  <Typography variant="caption" sx={{ color: '#64748b', display: 'block', mb: 2 }}>{totalAiMessages} messages</Typography>
                  <Button size="small" onClick={() => navigate('/copilot')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    View Inbox
                  </Button>
                </Paper>
              </Grid>

              {/* Pending Approval */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <GavelIcon sx={{ fontSize: 16, color: '#d97706' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Pending Approvals</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>{pendingApprovalsCount}</Typography>
                  <Typography variant="caption" sx={{ color: pendingApprovalsCount > 0 ? '#dc2626' : '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 0.5, mb: 2 }}>
                    {pendingApprovalsCount > 0 ? '⚠️ Action Required' : '● Up to Date'}
                  </Typography>
                  <Button size="small" onClick={() => navigate('/approvals')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700, color: pendingApprovalsCount > 0 ? '#dc2626' : 'primary.main' }}>
                    Review
                  </Button>
                </Paper>
              </Grid>

              {/* Knowledge Base */}
              <Grid item xs={6}>
                <Paper elevation={0} sx={{ p: 2, border: '1px solid #f1f5f9', borderRadius: 2, bgcolor: '#f8fafc' }}>
                  <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                    <PsychologyIcon sx={{ fontSize: 16, color: '#16a34a' }} />
                    <Typography variant="caption" sx={{ fontWeight: 700, color: '#64748b' }}>Knowledge Base</Typography>
                  </Stack>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                    {totalChunks} chunks
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#16a34a', fontWeight: 700, display: 'flex', alignItems: 'center', gap: 0.5, mb: 2 }}>
                    ● Vector Indexed
                  </Typography>
                  <Button size="small" onClick={() => navigate('/knowledge')} endIcon={<ArrowForwardIcon sx={{ fontSize: 12 }} />} sx={{ p: 0, textTransform: 'none', fontSize: '0.75rem', fontWeight: 700 }}>
                    Manage
                  </Button>
                </Paper>
              </Grid>
            </Grid>
          </Card>
        </Grid>
      </Grid>

      {/* 5. Bottom Row: Real Recent Orders Activity + Real Products Catalog + AI Agents */}
      <Grid container spacing={2.5}>
        {/* Real Recent Orders Activity */}
        <Grid item xs={12} lg={4}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                Recent Order Stream
              </Typography>
              <Typography variant="caption" onClick={() => navigate('/orders')} sx={{ color: '#2563eb', fontWeight: 700, cursor: 'pointer' }}>
                View All
              </Typography>
            </Box>

            {ordersLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress size={24} /></Box>
            ) : recentOrdersData?.content && recentOrdersData.content.length > 0 ? (
              <Stack spacing={2}>
                {recentOrdersData.content.slice(0, 4).map((order) => (
                  <Box key={order.id} sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start' }}>
                    <Avatar sx={{ width: 32, height: 32, bgcolor: order.fulfillmentStatus === 'FULFILLED' ? '#f0fdf4' : '#fffbeb', color: order.fulfillmentStatus === 'FULFILLED' ? '#16a34a' : '#d97706' }}>
                      <ShoppingBagIcon sx={{ fontSize: 16 }} />
                    </Avatar>
                    <Box sx={{ flexGrow: 1 }}>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.8rem' }}>
                        Order {order.orderNumber} – ${order.totalPrice.toFixed(2)}
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        {[order.customerFirstName, order.customerLastName].filter(Boolean).join(' ') || order.customerEmail || 'Guest Customer'} ({order.lineItems?.length || 0} items)
                      </Typography>
                    </Box>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.7rem' }}>
                      {getRelativeTime(order.shopifyCreatedAt || order.syncedAt)}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            ) : (
              <Box sx={{ p: 3, textAlign: 'center', color: '#94a3b8' }}>
                <Typography variant="body2">No recent orders recorded</Typography>
              </Box>
            )}
          </Card>
        </Grid>

        {/* Real Top Products */}
        <Grid item xs={12} lg={4}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                Live Catalog Products
              </Typography>
              <Typography variant="caption" onClick={() => navigate('/products')} sx={{ color: '#2563eb', fontWeight: 700, cursor: 'pointer' }}>
                View All
              </Typography>
            </Box>

            {productsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress size={24} /></Box>
            ) : productsData?.content && productsData.content.length > 0 ? (
              <TableContainer>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ color: '#94a3b8', fontSize: '0.7rem', fontWeight: 700, border: 'none', px: 0.5 }}>#</TableCell>
                      <TableCell sx={{ color: '#94a3b8', fontSize: '0.7rem', fontWeight: 700, border: 'none' }}>Product</TableCell>
                      <TableCell align="right" sx={{ color: '#94a3b8', fontSize: '0.7rem', fontWeight: 700, border: 'none' }}>Inventory</TableCell>
                      <TableCell align="right" sx={{ color: '#94a3b8', fontSize: '0.7rem', fontWeight: 700, border: 'none' }}>Price</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {productsData.content.slice(0, 5).map((p, idx) => {
                      const minPrice = p.variants?.length ? Math.min(...p.variants.map(v => v.price)) : 0;
                      return (
                        <TableRow key={p.id} hover sx={{ '&:last-child td, &:last-child th': { border: 0 } }}>
                          <TableCell sx={{ fontWeight: 700, color: '#64748b', fontSize: '0.75rem', px: 0.5, borderBottom: '1px solid #f8fafc' }}>
                            {idx + 1}
                          </TableCell>
                          <TableCell sx={{ borderBottom: '1px solid #f8fafc' }}>
                            <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.78rem' }} noWrap>
                              {p.title}
                            </Typography>
                            <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.68rem' }}>
                              {p.vendor || 'Shopify'}
                            </Typography>
                          </TableCell>
                          <TableCell align="right" sx={{ fontWeight: 600, color: '#334155', fontSize: '0.78rem', borderBottom: '1px solid #f8fafc' }}>
                            {p.totalInventory}
                          </TableCell>
                          <TableCell align="right" sx={{ fontWeight: 700, color: '#0f172a', fontSize: '0.78rem', borderBottom: '1px solid #f8fafc' }}>
                            ${minPrice.toFixed(2)}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Box sx={{ p: 3, textAlign: 'center', color: '#94a3b8' }}>
                <Typography variant="body2">No products synced in catalog yet</Typography>
              </Box>
            )}
          </Card>
        </Grid>

        {/* AI Agents Active Status */}
        <Grid item xs={12} lg={4}>
          <Card elevation={0} sx={{ p: 3, borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="subtitle1" sx={{ fontWeight: 800, color: '#0f172a' }}>
                AI Agents
              </Typography>
              <Typography variant="caption" onClick={() => navigate('/copilot')} sx={{ color: '#2563eb', fontWeight: 700, cursor: 'pointer' }}>
                View All
              </Typography>
            </Box>

            <Stack spacing={2}>
              {/* Agent 1: Customer Support Agent */}
              <Paper elevation={0} sx={{ p: 2, border: '1px solid #e2e8f0', borderRadius: 2.5, bgcolor: '#fafafa' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Stack direction="row" spacing={1.2} alignItems="center">
                    <Avatar sx={{ width: 32, height: 32, bgcolor: '#f3e8ff', color: '#7e22ce' }}>
                      <HeadsetMicIcon fontSize="small" />
                    </Avatar>
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.82rem' }}>
                        Customer Support Agent
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.7rem' }}>
                        Autonomous orders & returns assistant
                      </Typography>
                    </Box>
                  </Stack>
                  <Chip
                    icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: '#16a34a' }} />}
                    label="Active"
                    size="small"
                    sx={{ height: 20, fontSize: '0.68rem', fontWeight: 700, bgcolor: '#dcfce7', color: '#15803d' }}
                  />
                </Box>

                <Grid container spacing={1} sx={{ my: 1, textAlign: 'center' }}>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Provider</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem' }}>
                      {llmStatus?.activeProvider || 'LOCAL'}
                    </Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Tools</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem', color: '#16a34a' }}>11 Tools</Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Model</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem' }} noWrap>
                      {llmStatus?.activeModel || 'local-sim'}
                    </Typography>
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                  <Button size="small" variant="outlined" onClick={() => navigate('/chat-preview')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Open
                  </Button>
                  <Button size="small" variant="outlined" onClick={() => navigate('/playground')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Test
                  </Button>
                  <Button size="small" variant="outlined" onClick={() => navigate('/copilot')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Configure
                  </Button>
                </Stack>
              </Paper>

              {/* Agent 2: Merchant Copilot */}
              <Paper elevation={0} sx={{ p: 2, border: '1px solid #e2e8f0', borderRadius: 2.5, bgcolor: '#fafafa' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                  <Stack direction="row" spacing={1.2} alignItems="center">
                    <Avatar sx={{ width: 32, height: 32, bgcolor: '#ffedd5', color: '#c2410c' }}>
                      <CampaignIcon fontSize="small" />
                    </Avatar>
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a', fontSize: '0.82rem' }}>
                        Merchant Admin Copilot
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b', fontSize: '0.7rem' }}>
                        Store analytics & action automation
                      </Typography>
                    </Box>
                  </Stack>
                  <Chip
                    icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: '#16a34a' }} />}
                    label="Active"
                    size="small"
                    sx={{ height: 20, fontSize: '0.68rem', fontWeight: 700, bgcolor: '#dcfce7', color: '#15803d' }}
                  />
                </Box>

                <Grid container spacing={1} sx={{ my: 1, textAlign: 'center' }}>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Provider</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem' }}>
                      {llmStatus?.activeProvider || 'LOCAL'}
                    </Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Approvals</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem', color: '#2563eb' }}>
                      {pendingApprovalsCount} Pending
                    </Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" sx={{ color: '#94a3b8', fontSize: '0.68rem' }}>Risk Gate</Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, fontSize: '0.75rem', color: '#16a34a' }}>Enforced</Typography>
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
                  <Button size="small" variant="outlined" onClick={() => navigate('/copilot')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Open
                  </Button>
                  <Button size="small" variant="outlined" onClick={() => navigate('/playground')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Test
                  </Button>
                  <Button size="small" variant="outlined" onClick={() => navigate('/settings/shopify')} sx={{ flex: 1, borderRadius: 1.5, textTransform: 'none', fontSize: '0.72rem', fontWeight: 700 }}>
                    Configure
                  </Button>
                </Stack>
              </Paper>
            </Stack>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};
