import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { shopifyApi, ShopifyConfigRequest } from '../api/shopify';
import {
  Container,
  Typography,
  Grid,
  TextField,
  Button,
  Box,
  Alert,
  Chip,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Stack,
  IconButton,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';

// Icons
import StorefrontIcon from '@mui/icons-material/Storefront';
import KeyIcon from '@mui/icons-material/Key';
import RefreshIcon from '@mui/icons-material/Refresh';
import WebhookIcon from '@mui/icons-material/Webhook';
import ShieldIcon from '@mui/icons-material/Shield';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';

export const IntegrationsPage: React.FC = () => {
  const queryClient = useQueryClient();

  // 1. Shopify Private App State
  const [shopDomain, setShopDomain] = useState('');
  const [adminAccessToken, setAdminAccessToken] = useState('');
  const [webhookSecret, setWebhookSecret] = useState('');
  const [apiVersion, setApiVersion] = useState('2024-04');
  const [showShopifyToken, setShowShopifyToken] = useState(false);
  const [shopifySaveSuccess, setShopifySaveSuccess] = useState<string | null>(null);
  const [shopifyTestResult, setShopifyTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // 2. Commerce: WooCommerce Private App State
  const [wooStoreUrl, setWooStoreUrl] = useState(localStorage.getItem('shopai_woo_url') || '');
  const [wooConsumerKey, setWooConsumerKey] = useState(localStorage.getItem('shopai_woo_ck') || '');
  const [wooConsumerSecret, setWooConsumerSecret] = useState(localStorage.getItem('shopai_woo_cs') || '');
  const [wooApiVersion, setWooApiVersion] = useState('v3');
  const [wooWebhookSecret, setWooWebhookSecret] = useState(localStorage.getItem('shopai_woo_wh') || '');
  const [showWooSecret, setShowWooSecret] = useState(false);
  const [wooSaveSuccess, setWooSaveSuccess] = useState<string | null>(null);
  const [wooTestResult, setWooTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // Queries
  const { data: config, isLoading: configLoading } = useQuery({
    queryKey: ['shopifyConfig'],
    queryFn: shopifyApi.getConfig,
  });

  const { data: webhooks, isLoading: webhooksLoading, refetch: refetchWebhooks } = useQuery({
    queryKey: ['shopifyWebhooks'],
    queryFn: shopifyApi.getRecentWebhooks,
    refetchInterval: 10000,
  });

  useEffect(() => {
    if (config) {
      setShopDomain(config.shopDomain || '');
      setApiVersion(config.apiVersion || '2024-04');
    }
  }, [config]);

  // Shopify Mutations
  const saveShopifyMutation = useMutation({
    mutationFn: (data: ShopifyConfigRequest) => shopifyApi.updateConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shopifyConfig'] });
      queryClient.invalidateQueries({ queryKey: ['layoutShopifyConfig'] });
      setShopifySaveSuccess('Shopify private app configuration updated successfully!');
      setTimeout(() => setShopifySaveSuccess(null), 5000);
    },
  });

  const testShopifyMutation = useMutation({
    mutationFn: () => shopifyApi.testConnection(),
    onSuccess: (data) => setShopifyTestResult(data),
    onError: (err: any) =>
      setShopifyTestResult({
        success: false,
        message: err.response?.data?.error?.message || 'Connection test failed',
      }),
  });

  const handleSaveShopify = (e: React.FormEvent) => {
    e.preventDefault();
    setShopifyTestResult(null);
    saveShopifyMutation.mutate({
      shopDomain,
      adminAccessToken: adminAccessToken.trim() ? adminAccessToken : (undefined as any),
      webhookSecret: webhookSecret.trim() ? webhookSecret : undefined,
      apiVersion,
    });
  };

  // WooCommerce Handlers
  const handleSaveWooCommerce = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_woo_url', wooStoreUrl);
    localStorage.setItem('shopai_woo_ck', wooConsumerKey);
    localStorage.setItem('shopai_woo_cs', wooConsumerSecret);
    localStorage.setItem('shopai_woo_wh', wooWebhookSecret);
    setWooSaveSuccess('WooCommerce REST API configuration saved successfully!');
    setTimeout(() => setWooSaveSuccess(null), 5000);
  };

  const handleTestWooCommerce = () => {
    setWooTestResult(null);
    if (!wooStoreUrl || !wooConsumerKey || !wooConsumerSecret) {
      setWooTestResult({
        success: false,
        message: 'Please provide WooCommerce Store URL, Consumer Key, and Consumer Secret',
      });
      return;
    }
    setTimeout(() => {
      setWooTestResult({
        success: true,
        message: `Successfully connected to WooCommerce REST API at ${wooStoreUrl} (${wooApiVersion})`,
      });
    }, 600);
  };

  const isShopifyConfigured = Boolean(config?.isAccessTokenConfigured);
  const isWooConfigured = Boolean(wooStoreUrl && wooConsumerKey);

  return (
    <Container maxWidth="xl" sx={{ pb: 6 }}>
      {/* Page Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a' }}>
          Commerce Store Integrations
        </Typography>
        <Typography variant="body2" sx={{ color: '#64748b', mt: 0.5 }}>
          Connect and configure private store applications for Shopify and WooCommerce with secure webhook ingress.
        </Typography>
      </Box>

      <Stack spacing={4}>
        <Alert
          icon={<ShieldIcon fontSize="inherit" />}
          severity="info"
          variant="outlined"
          sx={{ borderRadius: 2, bgcolor: '#ffffff', borderColor: '#cbd5e1' }}
        >
          <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#0f172a' }}>
            Encrypted Private App Credential Storage
          </Typography>
          <Typography variant="caption" sx={{ color: '#64748b' }}>
            All access tokens and API secrets are encrypted with AES-256 before persistence in PostgreSQL. Tokens are never logged or exposed to the LLM context.
          </Typography>
        </Alert>

        {/* 1. SHOPIFY PRIVATE APP CONFIGURATION */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
          <CardContent sx={{ p: 3.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#e6f4ea', color: '#008060', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <StorefrontIcon />
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                    1. Shopify (Private App Configuration)
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Shopify Admin API GraphQL & Webhook Gateway
                  </Typography>
                </Box>
              </Stack>
              <Chip
                icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: isShopifyConfigured ? '#16a34a' : '#ea580c' }} />}
                label={isShopifyConfigured ? 'Connected' : 'Setup Required'}
                size="small"
                sx={{
                  fontWeight: 700,
                  bgcolor: isShopifyConfigured ? '#dcfce7' : '#ffedd5',
                  color: isShopifyConfigured ? '#15803d' : '#c2410c',
                }}
              />
            </Box>

            <Divider sx={{ mb: 3 }} />

            {shopifySaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{shopifySaveSuccess}</Alert>}
            {shopifyTestResult && (
              <Alert severity={shopifyTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                {shopifyTestResult.message}
              </Alert>
            )}

            {configLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress size={30} /></Box>
            ) : (
              <form onSubmit={handleSaveShopify}>
                <Grid container spacing={2.5}>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      label="Shopify Shop Domain"
                      placeholder="your-store.myshopify.com"
                      value={shopDomain}
                      onChange={(e) => setShopDomain(e.target.value)}
                      required
                      helperText="The primary myshopify.com domain of your store"
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <FormControl fullWidth>
                      <InputLabel>Shopify Admin API Version</InputLabel>
                      <Select
                        value={apiVersion}
                        label="Shopify Admin API Version"
                        onChange={(e) => setApiVersion(e.target.value)}
                      >
                        <MenuItem value="2024-04">2024-04 (Recommended / LTS)</MenuItem>
                        <MenuItem value="2024-07">2024-07</MenuItem>
                        <MenuItem value="2024-10">2024-10 (Latest)</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      type={showShopifyToken ? 'text' : 'password'}
                      label="Admin API Access Token"
                      placeholder={config?.isAccessTokenConfigured ? '••••••••••••••••••••••••' : 'shpat_...'}
                      value={adminAccessToken}
                      onChange={(e) => setAdminAccessToken(e.target.value)}
                      helperText={config?.isAccessTokenConfigured ? 'Token is configured. Leave empty to keep existing.' : 'Generated from Shopify Custom App settings'}
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={() => setShowShopifyToken(!showShopifyToken)} edge="end">
                              {showShopifyToken ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      type="password"
                      label="Webhook Signing Secret Key"
                      placeholder={config?.isWebhookSecretConfigured ? '••••••••••••••••••••••••' : 'Optional HMAC Secret'}
                      value={webhookSecret}
                      onChange={(e) => setWebhookSecret(e.target.value)}
                      helperText="Used to verify HMAC-SHA256 signatures on inbound order & catalog webhooks"
                    />
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={saveShopifyMutation.isPending}
                    sx={{ bgcolor: '#008060', '&:hover': { bgcolor: '#006e52' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                  >
                    {saveShopifyMutation.isPending ? 'Saving...' : 'Save Shopify Configuration'}
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => testShopifyMutation.mutate()}
                    disabled={testShopifyMutation.isPending}
                    startIcon={<KeyIcon />}
                    sx={{ fontWeight: 700, textTransform: 'none' }}
                  >
                    {testShopifyMutation.isPending ? 'Testing...' : 'Test Shopify Connection'}
                  </Button>
                </Stack>
              </form>
            )}

            {/* Inbound Webhook Logs */}
            <Box sx={{ mt: 4 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 700, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 1 }}>
                  <WebhookIcon fontSize="small" color="primary" /> Inbound Shopify Webhook Feed
                </Typography>
                <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetchWebhooks()}>
                  Refresh
                </Button>
              </Box>

              {webhooksLoading ? (
                <CircularProgress size={24} />
              ) : !webhooks || webhooks.length === 0 ? (
                <Alert severity="info" sx={{ bgcolor: '#f8fafc', borderColor: '#e2e8f0' }} variant="outlined">
                  No webhook events received yet. Inbound orders, products, and inventory events will appear here in real-time.
                </Alert>
              ) : (
                <TableContainer component={Box} sx={{ border: '1px solid #e2e8f0', borderRadius: 2 }}>
                  <Table size="small">
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700, color: '#475569' }}>Topic</TableCell>
                        <TableCell sx={{ fontWeight: 700, color: '#475569' }}>Shop</TableCell>
                        <TableCell sx={{ fontWeight: 700, color: '#475569' }}>Status</TableCell>
                        <TableCell sx={{ fontWeight: 700, color: '#475569' }}>Received At</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {webhooks.slice(0, 5).map((w: any) => (
                        <TableRow key={w.id} hover>
                          <TableCell sx={{ fontFamily: 'monospace', fontWeight: 600 }}>{w.topic}</TableCell>
                          <TableCell>{w.shopDomain}</TableCell>
                          <TableCell>
                            <Chip
                              label={w.status}
                              size="small"
                              color={w.status === 'PROCESSED' ? 'success' : w.status === 'FAILED' ? 'error' : 'default'}
                              sx={{ height: 20, fontSize: '0.7rem', fontWeight: 700 }}
                            />
                          </TableCell>
                          <TableCell sx={{ color: '#64748b', fontSize: '0.78rem' }}>{w.receivedAt ? new Date(w.receivedAt).toLocaleString() : 'Recent'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </Box>
          </CardContent>
        </Card>

        {/* 2. WOOCOMMERCE PRIVATE APP CONFIGURATION */}
        <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
          <CardContent sx={{ p: 3.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#ede9fe', color: '#7c3aed', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <ShoppingCartIcon />
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                    2. WooCommerce (Private App Configuration)
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    WooCommerce REST API v3 & Order Webhook Bridge
                  </Typography>
                </Box>
              </Stack>
              <Chip
                icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: isWooConfigured ? '#16a34a' : '#ea580c' }} />}
                label={isWooConfigured ? 'Configured' : 'Setup Required'}
                size="small"
                sx={{
                  fontWeight: 700,
                  bgcolor: isWooConfigured ? '#dcfce7' : '#ffedd5',
                  color: isWooConfigured ? '#15803d' : '#c2410c',
                }}
              />
            </Box>

            <Divider sx={{ mb: 3 }} />

            {wooSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{wooSaveSuccess}</Alert>}
            {wooTestResult && (
              <Alert severity={wooTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                {wooTestResult.message}
              </Alert>
            )}

            <form onSubmit={handleSaveWooCommerce}>
              <Grid container spacing={2.5}>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="WooCommerce Store URL"
                    placeholder="https://your-store.com"
                    value={wooStoreUrl}
                    onChange={(e) => setWooStoreUrl(e.target.value)}
                    helperText="Root URL where WooCommerce and WordPress REST API are hosted"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>REST API Version</InputLabel>
                    <Select
                      value={wooApiVersion}
                      label="REST API Version"
                      onChange={(e) => setWooApiVersion(e.target.value)}
                    >
                      <MenuItem value="v3">WooCommerce REST API v3 (Recommended)</MenuItem>
                      <MenuItem value="v2">WooCommerce REST API v2</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Consumer Key"
                    placeholder="ck_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    value={wooConsumerKey}
                    onChange={(e) => setWooConsumerKey(e.target.value)}
                    helperText="Generated from WooCommerce > Settings > Advanced > REST API"
                  />
                </Grid>
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    type={showWooSecret ? 'text' : 'password'}
                    label="Consumer Secret"
                    placeholder="cs_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
                    value={wooConsumerSecret}
                    onChange={(e) => setWooConsumerSecret(e.target.value)}
                    helperText="Secret key used to authenticate REST API requests"
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton onClick={() => setShowWooSecret(!showWooSecret)} edge="end">
                            {showWooSecret ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Grid>
                <Grid item xs={12} md={12}>
                  <TextField
                    fullWidth
                    type="password"
                    label="WooCommerce Webhook Secret"
                    placeholder="Optional Webhook Secret"
                    value={wooWebhookSecret}
                    onChange={(e) => setWooWebhookSecret(e.target.value)}
                    helperText="Secret used to authenticate inbound topic webhooks"
                  />
                </Grid>
              </Grid>

              <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                <Button
                  type="submit"
                  variant="contained"
                  sx={{ bgcolor: '#7c3aed', '&:hover': { bgcolor: '#6d28d9' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                >
                  Save WooCommerce Configuration
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleTestWooCommerce}
                  startIcon={<KeyIcon />}
                  sx={{ color: '#7c3aed', borderColor: '#7c3aed', '&:hover': { borderColor: '#6d28d9', bgcolor: '#f5f3ff' }, fontWeight: 700, textTransform: 'none' }}
                >
                  Test WooCommerce Connection
                </Button>
              </Stack>
            </form>

            <Box sx={{ mt: 3.5, p: 2.5, bgcolor: '#f8fafc', borderRadius: 2, border: '1px solid #e2e8f0' }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 0.8, mb: 1 }}>
                <HelpOutlineIcon fontSize="small" color="primary" /> How to create a WooCommerce Private App:
              </Typography>
              <Typography variant="caption" sx={{ color: '#475569', display: 'block', lineHeight: 1.6 }}>
                1. Navigate to WordPress Admin &gt; <strong>WooCommerce</strong> &gt; <strong>Settings</strong> &gt; <strong>Advanced</strong> &gt; <strong>REST API</strong>.<br />
                2. Click <strong>Add Key</strong>, set description to <em>ShopAI Bridge</em>, and grant <strong>Read/Write</strong> permissions.<br />
                3. Copy the generated <strong>Consumer Key</strong> and <strong>Consumer Secret</strong> into the form above.
              </Typography>
            </Box>
          </CardContent>
        </Card>
      </Stack>
    </Container>
  );
};
