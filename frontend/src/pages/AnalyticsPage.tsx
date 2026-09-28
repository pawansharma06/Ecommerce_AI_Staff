import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { analyticsApi, GenerateReportRequest, ExecutiveReportResponse } from '../api/analytics';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  Button,
  Chip,
  Stack,
  CircularProgress,
  Alert,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  LinearProgress,
} from '@mui/material';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import MemoryIcon from '@mui/icons-material/Memory';
import AssessmentIcon from '@mui/icons-material/Assessment';
import SpeedIcon from '@mui/icons-material/Speed';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LightbulbOutlinedIcon from '@mui/icons-material/LightbulbOutlined';
import RefreshIcon from '@mui/icons-material/Refresh';

export const AnalyticsPage: React.FC = () => {
  const [salesDays, setSalesDays] = useState<number>(14);

  // Report Modal State
  const [reportOpen, setReportOpen] = useState(false);
  const [reportTimeframe, setReportTimeframe] = useState('Last 30 Days');
  const [reportFocus, setReportFocus] = useState('General Business & AI Performance');
  const [reportPrompt, setReportPrompt] = useState('');
  const [generatedReport, setGeneratedReport] = useState<ExecutiveReportResponse | null>(null);

  const { data: overview, refetch: refetchOverview } = useQuery({
    queryKey: ['analyticsOverview'],
    queryFn: analyticsApi.getOverview,
  });

  const { data: salesTrends, isLoading: loadingTrends, refetch: refetchTrends } = useQuery({
    queryKey: ['salesTrends', salesDays],
    queryFn: () => analyticsApi.getSalesTrends(salesDays),
  });

  const { data: aiMetrics, isLoading: loadingAi, refetch: refetchAi } = useQuery({
    queryKey: ['aiMetrics'],
    queryFn: analyticsApi.getAiMetrics,
  });

  const generateReportMutation = useMutation({
    mutationFn: (req: GenerateReportRequest) => analyticsApi.generateReport(req),
    onSuccess: (data) => {
      setGeneratedReport(data);
    },
  });

  const handleOpenReport = () => {
    setReportOpen(true);
    if (!generatedReport) {
      generateReportMutation.mutate({
        timeframe: reportTimeframe,
        focusArea: reportFocus,
        customPrompt: reportPrompt,
      });
    }
  };

  const handleRefreshAll = () => {
    refetchOverview();
    refetchTrends();
    refetchAi();
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 6 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <AssessmentIcon fontSize="large" color="primary" /> Commerce & AI Analytics
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Real-time business performance, revenue trends, AI agent token economics, and automated executive intelligence
          </Typography>
        </Box>
        <Stack direction="row" spacing={2}>
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={handleRefreshAll}>
            Refresh
          </Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<SmartToyIcon />}
            onClick={handleOpenReport}
          >
            Generate AI Executive Brief
          </Button>
        </Stack>
      </Box>

      {/* KPI Overview Grid */}
      <Grid container spacing={2} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">Total Revenue (GMV)</Typography>
                  <Typography variant="h5" fontWeight="bold">
                    ${overview ? overview.totalGmv.toLocaleString('en-US', { minimumFractionDigits: 2 }) : '0.00'}
                  </Typography>
                </Box>
                <AttachMoneyIcon color="primary" sx={{ fontSize: 36, bgcolor: 'primary.light', p: 0.5, borderRadius: 2, color: 'white' }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">Total Orders / AOV</Typography>
                  <Typography variant="h5" fontWeight="bold">
                    {overview?.totalOrders ?? 0} <Typography component="span" variant="body2" color="text.secondary">(${overview?.averageOrderValue ?? 0} AOV)</Typography>
                  </Typography>
                </Box>
                <ShoppingBagIcon color="secondary" sx={{ fontSize: 36, bgcolor: 'secondary.light', p: 0.5, borderRadius: 2, color: 'white' }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">AI Sessions / Messages</Typography>
                  <Typography variant="h5" fontWeight="bold">
                    {overview?.totalAiConversations ?? 0} <Typography component="span" variant="body2" color="text.secondary">({overview?.totalAiMessages ?? 0} msgs)</Typography>
                  </Typography>
                </Box>
                <SmartToyIcon color="success" sx={{ fontSize: 36, bgcolor: 'success.light', p: 0.5, borderRadius: 2, color: 'white' }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2}>
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Box>
                  <Typography variant="caption" color="text.secondary">Total Tokens / Est. Cost</Typography>
                  <Typography variant="h5" fontWeight="bold">
                    {overview ? overview.totalTokensConsumed.toLocaleString() : 0} <Typography component="span" variant="body2" color="text.secondary">(${overview?.estimatedLlmCostUsd ?? '0.00'})</Typography>
                  </Typography>
                </Box>
                <MemoryIcon color="warning" sx={{ fontSize: 36, bgcolor: 'warning.light', p: 0.5, borderRadius: 2, color: 'white' }} />
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Main Analytics Sections */}
      <Grid container spacing={3}>
        {/* Sales Trends Chart / Table */}
        <Grid item xs={12} md={7}>
          <Paper sx={{ p: 3, borderRadius: 2, height: '100%' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <TrendingUpIcon color="primary" /> Revenue & Order Trajectory
              </Typography>
              <TextField
                select
                size="small"
                value={salesDays}
                onChange={(e) => setSalesDays(Number(e.target.value))}
                sx={{ width: 130 }}
              >
                <MenuItem value={7}>Last 7 Days</MenuItem>
                <MenuItem value={14}>Last 14 Days</MenuItem>
                <MenuItem value={30}>Last 30 Days</MenuItem>
              </TextField>
            </Box>

            {loadingTrends ? (
              <Box sx={{ py: 6, textAlign: 'center' }}>
                <CircularProgress size={32} />
              </Box>
            ) : !salesTrends || salesTrends.length === 0 ? (
              <Alert severity="info">No sales activity recorded for this period.</Alert>
            ) : (
              <TableContainer sx={{ maxHeight: 380 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Date</TableCell>
                      <TableCell align="right">Orders</TableCell>
                      <TableCell align="right">Revenue ($)</TableCell>
                      <TableCell align="right">Avg Order ($)</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {salesTrends.map((pt) => (
                      <TableRow key={pt.date} hover>
                        <TableCell sx={{ fontWeight: 'medium' }}>{pt.date}</TableCell>
                        <TableCell align="right">
                          <Chip label={pt.orderCount} size="small" variant="outlined" color={pt.orderCount > 0 ? 'primary' : 'default'} />
                        </TableCell>
                        <TableCell align="right" sx={{ fontWeight: 'bold', color: pt.revenue > 0 ? 'success.main' : 'text.secondary' }}>
                          ${pt.revenue.toFixed(2)}
                        </TableCell>
                        <TableCell align="right">${pt.averageOrderValue.toFixed(2)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Paper>
        </Grid>

        {/* AI Performance & Tool Distribution */}
        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 3, borderRadius: 2, height: '100%' }}>
            <Typography variant="h6" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <SpeedIcon color="secondary" /> AI Token & Tool Performance
            </Typography>

            {loadingAi ? (
              <Box sx={{ py: 6, textAlign: 'center' }}>
                <CircularProgress size={32} />
              </Box>
            ) : (
              <Stack spacing={2.5}>
                {/* Latency Stats */}
                <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2 }}>
                  <Typography variant="caption" color="text.secondary" fontWeight="bold">REASONING LATENCY PROFILE</Typography>
                  <Grid container spacing={2} sx={{ mt: 0.5 }}>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">Average Latency</Typography>
                      <Typography variant="h6" fontWeight="bold">{aiMetrics?.avgLatencyMs ?? 0} ms</Typography>
                    </Grid>
                    <Grid item xs={6}>
                      <Typography variant="body2" color="text.secondary">p95 Latency</Typography>
                      <Typography variant="h6" fontWeight="bold">{aiMetrics?.p95LatencyMs ?? 0} ms</Typography>
                    </Grid>
                  </Grid>
                </Box>

                {/* Tool Invocations */}
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight="bold">FREQUENT TOOL INVOCATIONS</Typography>
                  {aiMetrics?.toolInvocations && Object.keys(aiMetrics.toolInvocations).length > 0 ? (
                    <Stack spacing={1} sx={{ mt: 1 }}>
                      {Object.entries(aiMetrics.toolInvocations).slice(0, 4).map(([tool, count]) => (
                        <Box key={tool}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                            <Typography variant="caption" fontFamily="monospace">{tool}</Typography>
                            <Typography variant="caption" fontWeight="bold">{count} calls</Typography>
                          </Box>
                          <LinearProgress variant="determinate" value={Math.min(100, (count / 15) * 100)} sx={{ height: 6, borderRadius: 3 }} />
                        </Box>
                      ))}
                    </Stack>
                  ) : (
                    <Typography variant="body2" sx={{ color: '#94a3b8', fontSize: '0.8rem', mt: 1 }}>
                      No tool executions recorded yet.
                    </Typography>
                  )}
                </Box>

                {/* Channel Distribution */}
                <Box>
                  <Typography variant="caption" color="text.secondary" fontWeight="bold">CHANNEL SESSIONS BREAKDOWN</Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 1, flexWrap: 'wrap', gap: 1 }}>
                    {aiMetrics?.channelDistribution && Object.entries(aiMetrics.channelDistribution).map(([channel, count]) => (
                      <Chip
                        key={channel}
                        label={`${channel}: ${count}`}
                        size="small"
                        color={count > 0 ? 'primary' : 'default'}
                        variant={count > 0 ? 'filled' : 'outlined'}
                      />
                    ))}
                  </Stack>
                </Box>
              </Stack>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* AI Executive Report Generator Dialog */}
      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SmartToyIcon color="primary" /> ShopAI Executive Commerce Intelligence Brief
        </DialogTitle>
        <DialogContent dividers>
          {/* Report Configuration Bar */}
          <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2, mb: 3 }}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Timeframe"
                  value={reportTimeframe}
                  onChange={(e) => setReportTimeframe(e.target.value)}
                >
                  <MenuItem value="Last 7 Days">Last 7 Days</MenuItem>
                  <MenuItem value="Last 14 Days">Last 14 Days</MenuItem>
                  <MenuItem value="Last 30 Days">Last 30 Days</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  select
                  fullWidth
                  size="small"
                  label="Focus Area"
                  value={reportFocus}
                  onChange={(e) => setReportFocus(e.target.value)}
                >
                  <MenuItem value="General Business & AI Performance">General Performance</MenuItem>
                  <MenuItem value="Revenue & Conversion Growth">Revenue & Conversion</MenuItem>
                  <MenuItem value="Operational Fulfillment & Risks">Fulfillment & Risks</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  fullWidth
                  size="small"
                  label="Custom Directives"
                  placeholder="e.g. Focus on cart recovery"
                  value={reportPrompt}
                  onChange={(e) => setReportPrompt(e.target.value)}
                />
              </Grid>
            </Grid>
          </Box>

          {generateReportMutation.isPending ? (
            <Box sx={{ py: 6, textAlign: 'center' }}>
              <CircularProgress size={40} />
              <Typography variant="body2" sx={{ mt: 2 }} color="text.secondary">
                Analyzing revenue data, order fulfillment queues, cart recovery metrics, and AI assistant traces...
              </Typography>
            </Box>
          ) : generatedReport ? (
            <Stack spacing={3}>
              <Box sx={{ p: 2.5, bgcolor: 'primary.50', borderRadius: 2, borderLeft: '4px solid #1976d2' }}>
                <Typography variant="subtitle2" color="primary" fontWeight="bold">EXECUTIVE SUMMARY</Typography>
                <Typography variant="body1" sx={{ mt: 1 }}>
                  {generatedReport.executiveSummary}
                </Typography>
              </Box>

              <Box>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <CheckCircleOutlineIcon color="success" fontSize="small" /> Key Business Highlights
                </Typography>
                <Stack spacing={1}>
                  {generatedReport.keyHighlights.map((hl, idx) => (
                    <Typography key={idx} variant="body2" color="text.secondary">
                      • {hl}
                    </Typography>
                  ))}
                </Stack>
              </Box>

              {generatedReport.operationalAlerts.length > 0 && (
                <Box>
                  <Typography variant="subtitle2" fontWeight="bold" color="warning.main" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <WarningAmberIcon color="warning" fontSize="small" /> Operational Alerts & Risks
                  </Typography>
                  <Stack spacing={1}>
                    {generatedReport.operationalAlerts.map((alt, idx) => (
                      <Typography key={idx} variant="body2" color="text.secondary">
                        • {alt}
                      </Typography>
                    ))}
                  </Stack>
                </Box>
              )}

              <Box>
                <Typography variant="subtitle2" fontWeight="bold" color="primary" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <LightbulbOutlinedIcon color="primary" fontSize="small" /> Strategic AI Action Recommendations
                </Typography>
                <Stack spacing={1}>
                  {generatedReport.strategicRecommendations.map((rec, idx) => (
                    <Typography key={idx} variant="body2" color="text.secondary">
                      • {rec}
                    </Typography>
                  ))}
                </Stack>
              </Box>
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => generateReportMutation.mutate({ timeframe: reportTimeframe, focusArea: reportFocus, customPrompt: reportPrompt })}
            disabled={generateReportMutation.isPending}
            startIcon={<RefreshIcon />}
          >
            Regenerate
          </Button>
          <Button onClick={() => setReportOpen(false)} variant="contained">
            Done
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};
