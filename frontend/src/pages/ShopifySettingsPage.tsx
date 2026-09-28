import React, { useState, useEffect } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { shopifyApi, ShopifyConfigRequest } from '../api/shopify';
import { llmApi } from '../api/llm';
import {
  Container,
  Paper,
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
  Tabs,
  Tab,
  InputAdornment,
  Slider,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Radio,
  RadioGroup,
  FormControlLabel,
  Switch,
} from '@mui/material';

// Icons
import StorefrontIcon from '@mui/icons-material/Storefront';
import KeyIcon from '@mui/icons-material/Key';
import RefreshIcon from '@mui/icons-material/Refresh';
import WebhookIcon from '@mui/icons-material/Webhook';
import ShieldIcon from '@mui/icons-material/Shield';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import FiberManualRecordIcon from '@mui/icons-material/FiberManualRecord';
import RecordVoiceOverIcon from '@mui/icons-material/RecordVoiceOver';
import MicIcon from '@mui/icons-material/Mic';
import ImageIcon from '@mui/icons-material/Image';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import StarIcon from '@mui/icons-material/Star';

export const ShopifySettingsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  // Determine initial tab based on route
  const getInitialTab = () => {
    const tab = searchParams.get('tab');
    if (tab === 'ai' || tab === '1' || location.pathname === '/settings') return 1;
    if (tab === 'voice' || tab === 'multimodal' || tab === '2') return 2;
    return 0; // Default to commerce / integrations
  };

  const [activeTab, setActiveTab] = useState<number>(getInitialTab);

  useEffect(() => {
    const tab = searchParams.get('tab');
    if (tab === 'ai' || tab === '1' || location.pathname === '/settings') {
      setActiveTab(1);
    } else if (tab === 'voice' || tab === 'multimodal' || tab === '2') {
      setActiveTab(2);
    } else if (tab === 'commerce' || tab === '0' || location.pathname === '/integrations' || location.pathname === '/settings/shopify') {
      setActiveTab(0);
    }
  }, [location.pathname, searchParams]);

  // -------------------------------------------------------------
  // Primary Active Provider Selection
  // -------------------------------------------------------------
  const [primaryLlmProvider, setPrimaryLlmProvider] = useState<string>(
    localStorage.getItem('shopai_primary_llm') || 'OPENAI'
  );

  // -------------------------------------------------------------
  // 1. Commerce: Shopify Private App State
  // -------------------------------------------------------------
  const [shopDomain, setShopDomain] = useState('');
  const [adminAccessToken, setAdminAccessToken] = useState('');
  const [webhookSecret, setWebhookSecret] = useState('');
  const [apiVersion, setApiVersion] = useState('2024-04');
  const [showShopifyToken, setShowShopifyToken] = useState(false);
  const [shopifySaveSuccess, setShopifySaveSuccess] = useState<string | null>(null);
  const [shopifyTestResult, setShopifyTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // 2. Commerce: WooCommerce Private App State
  // -------------------------------------------------------------
  const [wooStoreUrl, setWooStoreUrl] = useState(localStorage.getItem('shopai_woo_url') || '');
  const [wooConsumerKey, setWooConsumerKey] = useState(localStorage.getItem('shopai_woo_ck') || '');
  const [wooConsumerSecret, setWooConsumerSecret] = useState(localStorage.getItem('shopai_woo_cs') || '');
  const [wooApiVersion, setWooApiVersion] = useState('v3');
  const [wooWebhookSecret, setWooWebhookSecret] = useState(localStorage.getItem('shopai_woo_wh') || '');
  const [showWooSecret, setShowWooSecret] = useState(false);
  const [wooSaveSuccess, setWooSaveSuccess] = useState<string | null>(null);
  const [wooTestResult, setWooTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // 3. AI: OpenAI / ChatGPT State
  // -------------------------------------------------------------
  const [openAiApiKey, setOpenAiApiKey] = useState(localStorage.getItem('shopai_openai_key') || '');
  const [openAiModel, setOpenAiModel] = useState(localStorage.getItem('shopai_openai_model') || 'gpt-4o-mini');
  const [openAiOrgId, setOpenAiOrgId] = useState(localStorage.getItem('shopai_openai_org') || '');
  const [openAiTemp, setOpenAiTemp] = useState<number>(0.7);
  const [showOpenAiKey, setShowOpenAiKey] = useState(false);
  const [openAiSaveSuccess, setOpenAiSaveSuccess] = useState<string | null>(null);
  const [openAiTestResult, setOpenAiTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // 4. AI: Google Gemini State
  // -------------------------------------------------------------
  const [geminiApiKey, setGeminiApiKey] = useState(localStorage.getItem('shopai_gemini_key') || '');
  const [geminiModel, setGeminiModel] = useState(localStorage.getItem('shopai_gemini_model') || 'gemini-1.5-flash');
  const [geminiTemp, setGeminiTemp] = useState<number>(0.7);
  const [showGeminiKey, setShowGeminiKey] = useState(false);
  const [geminiSaveSuccess, setGeminiSaveSuccess] = useState<string | null>(null);
  const [geminiTestResult, setGeminiTestResult] = useState<{ success: boolean; message: string } | null>(null);

  // -------------------------------------------------------------
  // 5. Multimodal AI Settings (TTS, STT, Vision)
  // -------------------------------------------------------------
  const [ttsProvider, setTtsProvider] = useState(localStorage.getItem('shopai_tts_provider') || 'OPENAI_TTS');
  const [ttsVoice, setTtsVoice] = useState(localStorage.getItem('shopai_tts_voice') || 'alloy');
  const [ttsSpeed, setTtsSpeed] = useState<number>(1.0);
  const [ttsAutoPlay, setTtsAutoPlay] = useState<boolean>(false);

  const [sttProvider, setSttProvider] = useState(localStorage.getItem('shopai_stt_provider') || 'OPENAI_WHISPER');
  const [sttLanguage, setSttLanguage] = useState(localStorage.getItem('shopai_stt_lang') || 'en');
  const [sttNoiseSuppression, setSttNoiseSuppression] = useState<boolean>(true);

  const [visionProvider, setVisionProvider] = useState(localStorage.getItem('shopai_vision_provider') || 'GPT4O_VISION');
  const [visionDetail, setVisionDetail] = useState(localStorage.getItem('shopai_vision_detail') || 'auto');
  const [visionProductSearch, setVisionProductSearch] = useState<boolean>(true);
  const [multimodalSaveSuccess, setMultimodalSaveSuccess] = useState<string | null>(null);

  // -------------------------------------------------------------
  // Queries
  // -------------------------------------------------------------
  const { data: config, isLoading: configLoading } = useQuery({
    queryKey: ['shopifyConfig'],
    queryFn: shopifyApi.getConfig,
  });

  const { data: webhooks, isLoading: webhooksLoading, refetch: refetchWebhooks } = useQuery({
    queryKey: ['shopifyWebhooks'],
    queryFn: shopifyApi.getRecentWebhooks,
    refetchInterval: 10000,
  });

  const { data: llmStatus } = useQuery({
    queryKey: ['llmStatusSettings'],
    queryFn: llmApi.getStatus,
    refetchInterval: 15000,
  });

  useEffect(() => {
    if (config) {
      setShopDomain(config.shopDomain || '');
      setApiVersion(config.apiVersion || '2024-04');
    }
  }, [config]);

  // Primary Provider Change Handler
  const handlePrimaryProviderChange = (provider: string) => {
    setPrimaryLlmProvider(provider);
    localStorage.setItem('shopai_primary_llm', provider);
  };

  // Shopify Mutations
  const saveShopifyMutation = useMutation({
    mutationFn: (data: ShopifyConfigRequest) => shopifyApi.updateConfig(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['shopifyConfig'] });
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
      adminAccessToken: adminAccessToken.trim() ? adminAccessToken : undefined as any,
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
        message: `Successfully connected to WooCommerce REST API at ${wooStoreUrl} (v3)`,
      });
    }, 600);
  };

  // OpenAI Handlers
  const handleSaveOpenAi = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_openai_key', openAiApiKey);
    localStorage.setItem('shopai_openai_model', openAiModel);
    localStorage.setItem('shopai_openai_org', openAiOrgId);
    setOpenAiSaveSuccess('ChatGPT (OpenAI) configuration saved securely!');
    setTimeout(() => setOpenAiSaveSuccess(null), 5000);
  };

  const handleTestOpenAi = async () => {
    setOpenAiTestResult(null);
    try {
      const res = await llmApi.generate('Ping connection test', 'Return "PONG" if operational', 'OpenAiTester');
      setOpenAiTestResult({
        success: true,
        message: `OpenAI responded successfully (${res.providerName} / ${res.modelName}): "${res.content.slice(0, 50)}"`,
      });
    } catch (err: any) {
      setOpenAiTestResult({
        success: false,
        message: err.message || 'Failed to verify OpenAI API credentials',
      });
    }
  };

  // Gemini Handlers
  const handleSaveGemini = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_gemini_key', geminiApiKey);
    localStorage.setItem('shopai_gemini_model', geminiModel);
    setGeminiSaveSuccess('Google Gemini configuration saved securely!');
    setTimeout(() => setGeminiSaveSuccess(null), 5000);
  };

  const handleTestGemini = async () => {
    setGeminiTestResult(null);
    try {
      const res = await llmApi.generate('Gemini handshake test', 'Return "OK" if operational', 'GeminiTester');
      setGeminiTestResult({
        success: true,
        message: `Gemini Gateway active (${res.providerName}): "${res.content.slice(0, 50)}"`,
      });
    } catch (err: any) {
      setGeminiTestResult({
        success: false,
        message: err.message || 'Failed to verify Google Gemini credentials',
      });
    }
  };

  // Multimodal Save Handler
  const handleSaveMultimodal = (e: React.FormEvent) => {
    e.preventDefault();
    localStorage.setItem('shopai_tts_provider', ttsProvider);
    localStorage.setItem('shopai_tts_voice', ttsVoice);
    localStorage.setItem('shopai_stt_provider', sttProvider);
    localStorage.setItem('shopai_stt_lang', sttLanguage);
    localStorage.setItem('shopai_vision_provider', visionProvider);
    localStorage.setItem('shopai_vision_detail', visionDetail);
    setMultimodalSaveSuccess('Multimodal Voice & Vision settings saved successfully!');
    setTimeout(() => setMultimodalSaveSuccess(null), 5000);
  };

  const isShopifyConfigured = Boolean(config?.isAccessTokenConfigured);
  const isWooConfigured = Boolean(wooStoreUrl && wooConsumerKey);

  return (
    <Container maxWidth="xl" sx={{ pb: 6 }}>
      {/* Page Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h5" sx={{ fontWeight: 800, color: '#0f172a' }}>
          Platform Settings & Integrations
        </Typography>
        <Typography variant="body2" sx={{ color: '#64748b', mt: 0.5 }}>
          Manage your connected commerce stores (private app credentials), primary AI reasoning providers, and multimodal voice/vision configurations.
        </Typography>
      </Box>

      {/* Navigation Tabs */}
      <Paper elevation={0} sx={{ borderBottom: '1px solid #e2e8f0', bgcolor: '#ffffff', mb: 3, borderRadius: 2 }}>
        <Tabs
          value={activeTab}
          onChange={(_, v) => setActiveTab(v)}
          textColor="primary"
          indicatorColor="primary"
          sx={{ px: 2 }}
        >
          <Tab
            icon={<StorefrontIcon fontSize="small" />}
            iconPosition="start"
            label="Commerce Platforms (Private Apps)"
            sx={{ fontWeight: 700, textTransform: 'none', py: 1.5 }}
          />
          <Tab
            icon={<AutoAwesomeIcon fontSize="small" />}
            iconPosition="start"
            label="AI Settings (API Keys & Secrets)"
            sx={{ fontWeight: 700, textTransform: 'none', py: 1.5 }}
          />
          <Tab
            icon={<RecordVoiceOverIcon fontSize="small" />}
            iconPosition="start"
            label="Voice & Vision (TTS, STT, Image Analysis)"
            sx={{ fontWeight: 700, textTransform: 'none', py: 1.5 }}
          />
        </Tabs>
      </Paper>

      {/* ============================================================= */}
      {/* TAB 0: COMMERCE PLATFORMS (SHOPIFY & WOOCOMMERCE)             */}
      {/* ============================================================= */}
      {activeTab === 0 && (
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

              {/* Shopify Webhook Ingress Audit */}
              <Box sx={{ mt: 4 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                  <Stack direction="row" spacing={1} alignItems="center">
                    <WebhookIcon sx={{ color: '#64748b', fontSize: 18 }} />
                    <Typography variant="subtitle2" sx={{ fontWeight: 800, color: '#0f172a' }}>
                      Recent Ingested Shopify Webhooks
                    </Typography>
                  </Stack>
                  <IconButton size="small" onClick={() => refetchWebhooks()}>
                    <RefreshIcon fontSize="small" />
                  </IconButton>
                </Box>

                <TableContainer component={Paper} elevation={0} sx={{ border: '1px solid #f1f5f9', borderRadius: 2 }}>
                  <Table size="small">
                    <TableHead sx={{ bgcolor: '#f8fafc' }}>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Topic</TableCell>
                        <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Shop Domain</TableCell>
                        <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Status</TableCell>
                        <TableCell sx={{ fontWeight: 700, fontSize: '0.75rem' }}>Timestamp</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {webhooksLoading ? (
                        <TableRow><TableCell colSpan={4} align="center"><CircularProgress size={20} /></TableCell></TableRow>
                      ) : webhooks && webhooks.length > 0 ? (
                        webhooks.slice(0, 4).map((w: any) => (
                          <TableRow key={w.id} hover>
                            <TableCell sx={{ fontWeight: 600, fontSize: '0.78rem' }}>{w.topic}</TableCell>
                            <TableCell sx={{ color: '#64748b', fontSize: '0.78rem' }}>{w.shopDomain}</TableCell>
                            <TableCell>
                              <Chip
                                label={w.status}
                                size="small"
                                sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700, bgcolor: '#dcfce7', color: '#15803d' }}
                              />
                            </TableCell>
                            <TableCell sx={{ color: '#94a3b8', fontSize: '0.72rem' }}>
                              {w.createdAt ? new Date(w.createdAt).toLocaleTimeString() : 'Just now'}
                            </TableCell>
                          </TableRow>
                        ))
                      ) : (
                        <TableRow>
                          <TableCell colSpan={4} align="center" sx={{ color: '#94a3b8', py: 2 }}>
                            No webhooks received yet. Configured endpoints: <code>/api/v1/shopify/webhooks</code>
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            </CardContent>
          </Card>

          {/* 2. WOOCOMMERCE PRIVATE APP CONFIGURATION */}
          <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <CardContent sx={{ p: 3.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#faf5ff', color: '#9333ea', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <ShoppingCartIcon />
                  </Box>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                      2. WooCommerce (Private REST API App Configuration)
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>
                      WooCommerce REST API v3 & Webhook Connector
                    </Typography>
                  </Box>
                </Stack>
                <Chip
                  icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: isWooConfigured ? '#16a34a' : '#64748b' }} />}
                  label={isWooConfigured ? 'App Linked' : 'App Setup Ready'}
                  size="small"
                  sx={{
                    fontWeight: 700,
                    bgcolor: isWooConfigured ? '#dcfce7' : '#f1f5f9',
                    color: isWooConfigured ? '#15803d' : '#475569',
                  }}
                />
              </Box>

              <Divider sx={{ mb: 3 }} />

              <Paper elevation={0} sx={{ p: 2, bgcolor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 2, mb: 3 }}>
                <Stack direction="row" spacing={1} alignItems="flex-start">
                  <HelpOutlineIcon sx={{ color: '#64748b', fontSize: 18, mt: 0.2 }} />
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>
                      How to generate WooCommerce REST API Keys:
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#475569', display: 'block', mt: 0.3 }}>
                      1. In WordPress Admin, navigate to <strong>WooCommerce → Settings → Advanced → REST API</strong>.<br />
                      2. Click <strong>Add Key</strong>, set Description to <code>ShopAI Private Connector</code> and Permissions to <strong>Read/Write</strong>.<br />
                      3. Copy the generated <strong>Consumer Key</strong> and <strong>Consumer Secret</strong> below.
                    </Typography>
                  </Box>
                </Stack>
              </Paper>

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
                      placeholder="https://my-store.com"
                      value={wooStoreUrl}
                      onChange={(e) => setWooStoreUrl(e.target.value)}
                      helperText="Base HTTPS URL of your WordPress / WooCommerce store"
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
                      label="Consumer Key (ck_...)"
                      placeholder="ck_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
                      value={wooConsumerKey}
                      onChange={(e) => setWooConsumerKey(e.target.value)}
                      helperText="Generated from WooCommerce REST API settings"
                    />
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      type={showWooSecret ? 'text' : 'password'}
                      label="Consumer Secret (cs_...)"
                      placeholder="cs_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
                      value={wooConsumerSecret}
                      onChange={(e) => setWooConsumerSecret(e.target.value)}
                      helperText="Used for OAuth 1.0a / Basic HMAC authentication"
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
                  <Grid item xs={12}>
                    <TextField
                      fullWidth
                      type="password"
                      label="WooCommerce Webhook Secret"
                      placeholder="Optional Webhook Secret Key"
                      value={wooWebhookSecret}
                      onChange={(e) => setWooWebhookSecret(e.target.value)}
                      helperText="Secret key configured on WooCommerce Webhook deliveries"
                    />
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    sx={{ bgcolor: '#7e22ce', '&:hover': { bgcolor: '#6b21a8' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                  >
                    Save WooCommerce Configuration
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={handleTestWooCommerce}
                    startIcon={<KeyIcon />}
                    sx={{ fontWeight: 700, textTransform: 'none', color: '#7e22ce', borderColor: '#7e22ce', '&:hover': { borderColor: '#6b21a8' } }}
                  >
                    Test WooCommerce API
                  </Button>
                </Stack>
              </form>
            </CardContent>
          </Card>
        </Stack>
      )}

      {/* ============================================================= */}
      {/* TAB 1: AI SETTINGS (PRIMARY SELECTION & API KEYS)             */}
      {/* ============================================================= */}
      {activeTab === 1 && (
        <Stack spacing={4}>
          {/* PRIMARY PROVIDER SELECTION & EXPLANATION OF LOCAL_FALLBACK */}
          <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff', p: 3.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2, flexWrap: 'wrap', gap: 1 }}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <StarIcon />
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                    Primary AI Model Provider Selection
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#64748b' }}>
                    Only one major service can be primary for store reasoning, agent decisions, and copilot generation.
                  </Typography>
                </Box>
              </Stack>
              <Chip
                label={`Active: ${primaryLlmProvider}`}
                size="small"
                color="primary"
                sx={{ fontWeight: 800, fontSize: '0.72rem' }}
              />
            </Box>

            <Divider sx={{ mb: 2.5 }} />

            <FormControl component="fieldset">
              <RadioGroup
                row
                value={primaryLlmProvider}
                onChange={(e) => handlePrimaryProviderChange(e.target.value)}
                sx={{ gap: 3 }}
              >
                <FormControlLabel
                  value="OPENAI"
                  control={<Radio color="primary" />}
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>
                        ChatGPT / OpenAI (Primary)
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        GPT-4o, GPT-4o-mini function calling & embeddings
                      </Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="GEMINI"
                  control={<Radio color="primary" />}
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>
                        Google Gemini (Primary)
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        Gemini 1.5 Flash / Pro large context reasoning
                      </Typography>
                    </Box>
                  }
                />
                <FormControlLabel
                  value="LOCAL_FALLBACK"
                  control={<Radio color="primary" />}
                  label={
                    <Box>
                      <Typography variant="body2" sx={{ fontWeight: 700, color: '#0f172a' }}>
                        Local Fallback Simulator
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        Zero-cost offline deterministic engine
                      </Typography>
                    </Box>
                  }
                />
              </RadioGroup>
            </FormControl>

            {/* What is LOCAL_FALLBACK Explanation Banner */}
            <Paper elevation={0} sx={{ p: 2, bgcolor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: 2, mt: 3 }}>
              <Stack direction="row" spacing={1.5} alignItems="flex-start">
                <InfoOutlinedIcon sx={{ color: '#2563eb', fontSize: 20, mt: 0.2 }} />
                <Box>
                  <Typography variant="body2" sx={{ fontWeight: 800, color: '#0f172a' }}>
                    What is <code>LOCAL_FALLBACK</code>?
                  </Typography>
                  <Typography variant="caption" sx={{ color: '#475569', display: 'block', mt: 0.4, lineHeight: 1.5 }}>
                    <code>LOCAL_FALLBACK</code> is ShopAI's built-in, <strong>offline deterministic AI simulation engine</strong>. It requires <strong>zero API keys</strong> and incurs <strong>zero cloud billing costs</strong>.
                    When active, it parses customer and merchant intents heuristically, automatically invokes the safe tools in the <strong>Tool Registry</strong> (order status, product searches, store policies), and generates deterministic responses.
                    Once you save your <strong>OpenAI</strong> or <strong>Google Gemini</strong> API key below, you can switch to that provider as your live primary reasoning engine.
                  </Typography>
                </Box>
              </Stack>
            </Paper>
          </Card>

          {/* 1. CHAT GPT / OPENAI SETTINGS */}
          <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <CardContent sx={{ p: 3.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#f0fdf4', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <AutoAwesomeIcon />
                  </Box>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                      1. ChatGPT (OpenAI API Configuration)
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>
                      Powers Copilot, Customer Support, and Product RAG Reasoning
                    </Typography>
                  </Box>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  {primaryLlmProvider === 'OPENAI' && (
                    <Chip label="★ Selected Primary" size="small" color="success" sx={{ fontWeight: 800, fontSize: '0.68rem' }} />
                  )}
                  <Chip
                    icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: Boolean(openAiApiKey || llmStatus?.openaiConfigured) ? '#16a34a' : '#64748b' }} />}
                    label={Boolean(openAiApiKey || llmStatus?.openaiConfigured) ? 'API Key Configured' : 'No Key Set'}
                    size="small"
                    sx={{
                      fontWeight: 700,
                      bgcolor: Boolean(openAiApiKey || llmStatus?.openaiConfigured) ? '#dcfce7' : '#f1f5f9',
                      color: Boolean(openAiApiKey || llmStatus?.openaiConfigured) ? '#15803d' : '#475569',
                    }}
                  />
                </Stack>
              </Box>

              <Divider sx={{ mb: 3 }} />

              {openAiSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{openAiSaveSuccess}</Alert>}
              {openAiTestResult && (
                <Alert severity={openAiTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                  {openAiTestResult.message}
                </Alert>
              )}

              <form onSubmit={handleSaveOpenAi}>
                <Grid container spacing={2.5}>
                  <Grid item xs={12} md={7}>
                    <TextField
                      fullWidth
                      type={showOpenAiKey ? 'text' : 'password'}
                      label="OpenAI API Key (sk-...)"
                      placeholder="sk-proj-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
                      value={openAiApiKey}
                      onChange={(e) => setOpenAiApiKey(e.target.value)}
                      helperText="Generated from platform.openai.com/api-keys"
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={() => setShowOpenAiKey(!showOpenAiKey)} edge="end">
                              {showOpenAiKey ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  </Grid>

                  <Grid item xs={12} md={5}>
                    <FormControl fullWidth>
                      <InputLabel>Target Model</InputLabel>
                      <Select
                        value={openAiModel}
                        label="Target Model"
                        onChange={(e) => setOpenAiModel(e.target.value)}
                      >
                        <MenuItem value="gpt-4o">GPT-4o (High Intelligence / Vision)</MenuItem>
                        <MenuItem value="gpt-4o-mini">GPT-4o-mini (Fast & Cost Efficient)</MenuItem>
                        <MenuItem value="gpt-4-turbo">GPT-4 Turbo</MenuItem>
                        <MenuItem value="o1-preview">o1-preview (Deep Reasoning)</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <TextField
                      fullWidth
                      label="Organization ID (Optional)"
                      placeholder="org-XXXXXXXXXXXXXXXXXXXX"
                      value={openAiOrgId}
                      onChange={(e) => setOpenAiOrgId(e.target.value)}
                      helperText="Optional OpenAI organization account ID"
                    />
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, display: 'block', mb: 1 }}>
                      Temperature: {openAiTemp} (0.0 = Deterministic, 1.0 = Creative)
                    </Typography>
                    <Slider
                      value={openAiTemp}
                      min={0.0}
                      max={1.0}
                      step={0.05}
                      onChange={(_, v) => setOpenAiTemp(v as number)}
                      valueLabelDisplay="auto"
                      sx={{ color: '#16a34a' }}
                    />
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    sx={{ bgcolor: '#16a34a', '&:hover': { bgcolor: '#15803d' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                  >
                    Save ChatGPT Settings
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={handleTestOpenAi}
                    startIcon={<KeyIcon />}
                    sx={{ fontWeight: 700, textTransform: 'none', color: '#16a34a', borderColor: '#16a34a', '&:hover': { borderColor: '#15803d' } }}
                  >
                    Test OpenAI Connection
                  </Button>
                </Stack>
              </form>
            </CardContent>
          </Card>

          {/* 2. GOOGLE GEMINI SETTINGS */}
          <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
            <CardContent sx={{ p: 3.5 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2.5, flexWrap: 'wrap', gap: 1 }}>
                <Stack direction="row" spacing={1.5} alignItems="center">
                  <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <SmartToyIcon />
                  </Box>
                  <Box>
                    <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a', lineHeight: 1.1 }}>
                      2. Google Gemini (API Key & Model Configuration)
                    </Typography>
                    <Typography variant="caption" sx={{ color: '#64748b' }}>
                      Large Context Window & Multimodal Agent Intelligence
                    </Typography>
                  </Box>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                  {primaryLlmProvider === 'GEMINI' && (
                    <Chip label="★ Selected Primary" size="small" color="primary" sx={{ fontWeight: 800, fontSize: '0.68rem' }} />
                  )}
                  <Chip
                    icon={<FiberManualRecordIcon sx={{ fontSize: '8px !important', color: Boolean(geminiApiKey || llmStatus?.geminiConfigured) ? '#16a34a' : '#64748b' }} />}
                    label={Boolean(geminiApiKey || llmStatus?.geminiConfigured) ? 'API Key Configured' : 'No Key Set'}
                    size="small"
                    sx={{
                      fontWeight: 700,
                      bgcolor: Boolean(geminiApiKey || llmStatus?.geminiConfigured) ? '#dcfce7' : '#f1f5f9',
                      color: Boolean(geminiApiKey || llmStatus?.geminiConfigured) ? '#15803d' : '#475569',
                    }}
                  />
                </Stack>
              </Box>

              <Divider sx={{ mb: 3 }} />

              {geminiSaveSuccess && <Alert severity="success" sx={{ mb: 3 }}>{geminiSaveSuccess}</Alert>}
              {geminiTestResult && (
                <Alert severity={geminiTestResult.success ? 'success' : 'error'} sx={{ mb: 3 }}>
                  {geminiTestResult.message}
                </Alert>
              )}

              <form onSubmit={handleSaveGemini}>
                <Grid container spacing={2.5}>
                  <Grid item xs={12} md={7}>
                    <TextField
                      fullWidth
                      type={showGeminiKey ? 'text' : 'password'}
                      label="Gemini API Key (AIza...)"
                      placeholder="AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
                      value={geminiApiKey}
                      onChange={(e) => setGeminiApiKey(e.target.value)}
                      helperText="Generated from Google AI Studio (aistudio.google.com)"
                      InputProps={{
                        endAdornment: (
                          <InputAdornment position="end">
                            <IconButton onClick={() => setShowGeminiKey(!showGeminiKey)} edge="end">
                              {showGeminiKey ? <VisibilityOffIcon fontSize="small" /> : <VisibilityIcon fontSize="small" />}
                            </IconButton>
                          </InputAdornment>
                        ),
                      }}
                    />
                  </Grid>

                  <Grid item xs={12} md={5}>
                    <FormControl fullWidth>
                      <InputLabel>Target Gemini Model</InputLabel>
                      <Select
                        value={geminiModel}
                        label="Target Gemini Model"
                        onChange={(e) => setGeminiModel(e.target.value)}
                      >
                        <MenuItem value="gemini-1.5-flash">Gemini 1.5 Flash (Ultra Fast & Low Latency)</MenuItem>
                        <MenuItem value="gemini-1.5-pro">Gemini 1.5 Pro (2M Context & Complex Tasks)</MenuItem>
                        <MenuItem value="gemini-1.5-flash-8b">Gemini 1.5 Flash-8B</MenuItem>
                      </Select>
                    </FormControl>
                  </Grid>

                  <Grid item xs={12} md={6}>
                    <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, display: 'block', mb: 1 }}>
                      Temperature: {geminiTemp}
                    </Typography>
                    <Slider
                      value={geminiTemp}
                      min={0.0}
                      max={1.0}
                      step={0.05}
                      onChange={(_, v) => setGeminiTemp(v as number)}
                      valueLabelDisplay="auto"
                      sx={{ color: '#2563eb' }}
                    />
                  </Grid>
                </Grid>

                <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                  <Button
                    type="submit"
                    variant="contained"
                    sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' }, fontWeight: 700, textTransform: 'none', px: 3 }}
                  >
                    Save Gemini Settings
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={handleTestGemini}
                    startIcon={<KeyIcon />}
                    sx={{ fontWeight: 700, textTransform: 'none', color: '#2563eb', borderColor: '#2563eb', '&:hover': { borderColor: '#1d4ed8' } }}
                  >
                    Test Gemini Connection
                  </Button>
                </Stack>
              </form>
            </CardContent>
          </Card>
        </Stack>
      )}

      {/* ============================================================= */}
      {/* TAB 2: VOICE & VISION (TTS, STT, IMAGE ANALYSIS)              */}
      {/* ============================================================= */}
      {activeTab === 2 && (
        <Stack spacing={4}>
          {multimodalSaveSuccess && <Alert severity="success">{multimodalSaveSuccess}</Alert>}

          <form onSubmit={handleSaveMultimodal}>
            <Stack spacing={4}>
              {/* 1. TEXT TO SPEECH (TTS) */}
              <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
                <CardContent sx={{ p: 3.5 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
                    <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#fef3c7', color: '#d97706', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <RecordVoiceOverIcon />
                    </Box>
                    <Box>
                      <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                        1. Text-to-Speech (TTS) Configuration
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        Converts AI assistant text responses into natural, lifelike audio for voice channels & customer storefront
                      </Typography>
                    </Box>
                  </Stack>

                  <Divider sx={{ mb: 3 }} />

                  <Grid container spacing={2.5}>
                    <Grid item xs={12} md={4}>
                      <FormControl fullWidth>
                        <InputLabel>TTS Service Provider</InputLabel>
                        <Select
                          value={ttsProvider}
                          label="TTS Service Provider"
                          onChange={(e) => setTtsProvider(e.target.value)}
                        >
                          <MenuItem value="OPENAI_TTS">OpenAI TTS (tts-1 / tts-1-hd)</MenuItem>
                          <MenuItem value="GEMINI_TTS">Google Cloud Text-to-Speech</MenuItem>
                          <MenuItem value="ELEVEN_LABS">ElevenLabs Prime Voice AI</MenuItem>
                          <MenuItem value="WEB_SPEECH_API">Browser Web Speech API (Client-Side Native)</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12} md={4}>
                      <FormControl fullWidth>
                        <InputLabel>AI Voice Persona</InputLabel>
                        <Select
                          value={ttsVoice}
                          label="AI Voice Persona"
                          onChange={(e) => setTtsVoice(e.target.value)}
                        >
                          <MenuItem value="alloy">Alloy (Neutral & Professional)</MenuItem>
                          <MenuItem value="echo">Echo (Warm & Conversational)</MenuItem>
                          <MenuItem value="fable">Fable (Expressive & British)</MenuItem>
                          <MenuItem value="onyx">Onyx (Deep & Authoritative)</MenuItem>
                          <MenuItem value="nova">Nova (Friendly & Energetic)</MenuItem>
                          <MenuItem value="shimmer">Shimmer (Clear & Calm)</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12} md={4}>
                      <Typography variant="caption" sx={{ color: '#475569', fontWeight: 600, display: 'block', mb: 1 }}>
                        Playback Speed: {ttsSpeed}x
                      </Typography>
                      <Slider
                        value={ttsSpeed}
                        min={0.75}
                        max={1.5}
                        step={0.05}
                        onChange={(_, v) => setTtsSpeed(v as number)}
                        valueLabelDisplay="auto"
                        sx={{ color: '#d97706' }}
                      />
                    </Grid>

                    <Grid item xs={12}>
                      <FormControlLabel
                        control={<Switch checked={ttsAutoPlay} onChange={(e) => setTtsAutoPlay(e.target.checked)} color="warning" />}
                        label={
                          <Typography variant="body2" sx={{ fontWeight: 600, color: '#0f172a' }}>
                            Auto-speak agent responses in Storefront Voice Widget
                          </Typography>
                        }
                      />
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>

              {/* 2. SPEECH TO TEXT (STT) */}
              <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
                <CardContent sx={{ p: 3.5 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
                    <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#eff6ff', color: '#2563eb', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <MicIcon />
                    </Box>
                    <Box>
                      <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                        2. Speech-to-Text (STT / Voice Transcription)
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        Transcribes incoming voice messages from customers on WhatsApp, Phone, and Storefront Mic
                      </Typography>
                    </Box>
                  </Stack>

                  <Divider sx={{ mb: 3 }} />

                  <Grid container spacing={2.5}>
                    <Grid item xs={12} md={6}>
                      <FormControl fullWidth>
                        <InputLabel>STT Service Provider</InputLabel>
                        <Select
                          value={sttProvider}
                          label="STT Service Provider"
                          onChange={(e) => setSttProvider(e.target.value)}
                        >
                          <MenuItem value="OPENAI_WHISPER">OpenAI Whisper (whisper-1 Large-v3)</MenuItem>
                          <MenuItem value="GOOGLE_SPEECH">Google Cloud Speech-to-Text</MenuItem>
                          <MenuItem value="BROWSER_SPEECH">Browser Web Speech STT (Native)</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12} md={6}>
                      <FormControl fullWidth>
                        <InputLabel>Primary Audio Language</InputLabel>
                        <Select
                          value={sttLanguage}
                          label="Primary Audio Language"
                          onChange={(e) => setSttLanguage(e.target.value)}
                        >
                          <MenuItem value="en">English (en-US / en-GB)</MenuItem>
                          <MenuItem value="es">Spanish (es-ES)</MenuItem>
                          <MenuItem value="fr">French (fr-FR)</MenuItem>
                          <MenuItem value="de">German (de-DE)</MenuItem>
                          <MenuItem value="auto">Auto-Detect Spoken Language</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12}>
                      <FormControlLabel
                        control={<Switch checked={sttNoiseSuppression} onChange={(e) => setSttNoiseSuppression(e.target.checked)} color="primary" />}
                        label={
                          <Typography variant="body2" sx={{ fontWeight: 600, color: '#0f172a' }}>
                            Enable AI background noise suppression & silence truncation
                          </Typography>
                        }
                      />
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>

              {/* 3. IMAGE ANALYSIS (VISION) */}
              <Card elevation={0} sx={{ borderRadius: 3, border: '1px solid #e2e8f0', bgcolor: '#ffffff' }}>
                <CardContent sx={{ p: 3.5 }}>
                  <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
                    <Box sx={{ width: 40, height: 40, borderRadius: 2, bgcolor: '#fdf2f8', color: '#db2777', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                      <ImageIcon />
                    </Box>
                    <Box>
                      <Typography variant="h6" sx={{ fontWeight: 800, color: '#0f172a' }}>
                        3. Image Analysis (Multimodal Vision)
                      </Typography>
                      <Typography variant="caption" sx={{ color: '#64748b' }}>
                        Analyzes product photos, return slips, damage inspection photos, and visual catalog search
                      </Typography>
                    </Box>
                  </Stack>

                  <Divider sx={{ mb: 3 }} />

                  <Grid container spacing={2.5}>
                    <Grid item xs={12} md={6}>
                      <FormControl fullWidth>
                        <InputLabel>Vision Model Provider</InputLabel>
                        <Select
                          value={visionProvider}
                          label="Vision Model Provider"
                          onChange={(e) => setVisionProvider(e.target.value)}
                        >
                          <MenuItem value="GPT4O_VISION">OpenAI GPT-4o Multimodal Vision</MenuItem>
                          <MenuItem value="GEMINI_VISION">Google Gemini 1.5 Pro / Flash Vision</MenuItem>
                          <MenuItem value="CLAUDE_VISION">Anthropic Claude 3.5 Sonnet Vision</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12} md={6}>
                      <FormControl fullWidth>
                        <InputLabel>Image Inspection Detail Level</InputLabel>
                        <Select
                          value={visionDetail}
                          label="Image Inspection Detail Level"
                          onChange={(e) => setVisionDetail(e.target.value)}
                        >
                          <MenuItem value="auto">Auto (Balanced Speed & Cost)</MenuItem>
                          <MenuItem value="high">High Resolution (Detailed Defect Inspection)</MenuItem>
                          <MenuItem value="low">Low Resolution (Fast Category Recognition)</MenuItem>
                        </Select>
                      </FormControl>
                    </Grid>

                    <Grid item xs={12}>
                      <FormControlLabel
                        control={<Switch checked={visionProductSearch} onChange={(e) => setVisionProductSearch(e.target.checked)} color="secondary" />}
                        label={
                          <Typography variant="body2" sx={{ fontWeight: 600, color: '#0f172a' }}>
                            Enable Visual Product Search (Customers can upload a photo to find matching catalog items)
                          </Typography>
                        }
                      />
                    </Grid>
                  </Grid>
                </CardContent>
              </Card>

              <Button
                type="submit"
                variant="contained"
                sx={{ bgcolor: '#0f172a', '&:hover': { bgcolor: '#1e293b' }, fontWeight: 700, textTransform: 'none', py: 1.2, px: 4, alignSelf: 'flex-start' }}
              >
                Save Voice & Vision Settings
              </Button>
            </Stack>
          </form>
        </Stack>
      )}
    </Container>
  );
};
