import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import {
  playgroundApi,
  PlaygroundTraceRequest,
  PlaygroundTraceResponse,
  PromptPreviewResponse,
} from '../api/playground';
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
  TextField,
  MenuItem,
  Tabs,
  Tab,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Slider,
  Card,
  CardContent,
} from '@mui/material';
import ScienceIcon from '@mui/icons-material/Science';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import VisibilityIcon from '@mui/icons-material/Visibility';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import BuildCircleIcon from '@mui/icons-material/BuildCircle';
import MemoryIcon from '@mui/icons-material/Memory';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import HubIcon from '@mui/icons-material/Hub';
import TextSnippetIcon from '@mui/icons-material/TextSnippet';
import TimerIcon from '@mui/icons-material/Timer';

export const PlaygroundPage: React.FC = () => {
  // Input State
  const [agentType, setAgentType] = useState('CUSTOMER_SUPPORT');
  const [channel, setChannel] = useState('WEB_CHAT');
  const [customerEmail, setCustomerEmail] = useState('');
  const [userQuery, setUserQuery] = useState('');
  const [temperature, setTemperature] = useState(0.7);
  const [maxSteps, setMaxSteps] = useState(5);

  // Output State
  const [activeTab, setActiveTab] = useState(0);
  const [traceResult, setTraceResult] = useState<PlaygroundTraceResponse | null>(null);
  const [previewResult, setPreviewResult] = useState<PromptPreviewResponse | null>(null);

  const traceMutation = useMutation({
    mutationFn: (req: PlaygroundTraceRequest) => playgroundApi.executeTrace(req),
    onSuccess: (data) => {
      setTraceResult(data);
      setPreviewResult(null);
      setActiveTab(0);
    },
  });

  const previewMutation = useMutation({
    mutationFn: () => playgroundApi.previewPrompt({
      agentType,
      customerEmail: customerEmail || undefined,
      userQuery,
    }),
    onSuccess: (data) => {
      setPreviewResult(data);
      setActiveTab(1);
    },
  });

  const handleRunTrace = () => {
    traceMutation.mutate({
      agentType,
      channel,
      customerEmail: customerEmail || undefined,
      userQuery,
      temperature,
      maxSteps,
    });
  };

  const handleQuickPrompt = (q: string) => {
    setUserQuery(q);
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 6 }}>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ScienceIcon fontSize="large" color="secondary" /> Agent Playground & Debug Workbench
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Simulate autonomous agent reasoning loops, inspect dynamic Vector & Graph RAG context injection, and trace tool executions with step-level telemetry
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Left Workbench Controls */}
        <Grid item xs={12} md={4.5}>
          <Paper sx={{ p: 3, borderRadius: 2 }}>
            <Typography variant="h6" fontWeight="bold" sx={{ mb: 2 }}>
              Execution Configuration
            </Typography>

            <Stack spacing={2.5}>
              <TextField
                select
                fullWidth
                size="small"
                label="Agent Persona"
                value={agentType}
                onChange={(e) => setAgentType(e.target.value)}
              >
                <MenuItem value="CUSTOMER_SUPPORT">Customer Support Concierge</MenuItem>
                <MenuItem value="ADMIN_COPILOT">Merchant Admin Copilot</MenuItem>
                <MenuItem value="STOREFRONT_SALES">Storefront Sales Advisor</MenuItem>
              </TextField>

              <TextField
                select
                fullWidth
                size="small"
                label="Channel Touchpoint"
                value={channel}
                onChange={(e) => setChannel(e.target.value)}
              >
                <MenuItem value="WEB_CHAT">Web Chat Widget</MenuItem>
                <MenuItem value="WHATSAPP">WhatsApp Cloud API</MenuItem>
                <MenuItem value="EMAIL">Email Support</MenuItem>
                <MenuItem value="STOREFRONT_WIDGET">Storefront Embedded</MenuItem>
              </TextField>

              <TextField
                fullWidth
                size="small"
                label="Simulated Customer Email (Context Injection)"
                value={customerEmail}
                onChange={(e) => setCustomerEmail(e.target.value)}
                placeholder="customer@example.com"
                helperText="Injects customer order history, memory preferences, and graph relationships"
              />

              <Box>
                <Typography variant="caption" color="text.secondary" fontWeight="bold">
                  QUICK PROMPT TEMPLATES
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 0.5, flexWrap: 'wrap', gap: 0.5 }}>
                  <Chip
                    label="Order Status"
                    size="small"
                    onClick={() => handleQuickPrompt('What is the current status of my order?')}
                  />
                  <Chip
                    label="Product Catalog"
                    size="small"
                    onClick={() => handleQuickPrompt('What products are currently available in the catalog?')}
                  />
                  <Chip
                    label="Return Policy"
                    size="small"
                    onClick={() => handleQuickPrompt('What is the store return and refund policy?')}
                  />
                  <Chip
                    label="Admin Stock Analysis"
                    size="small"
                    onClick={() => {
                      setAgentType('ADMIN_COPILOT');
                      handleQuickPrompt('What products are low on inventory or need reordering?');
                    }}
                  />
                </Stack>
              </Box>

              <TextField
                fullWidth
                multiline
                rows={3}
                size="small"
                label="User Query / Inbound Message"
                value={userQuery}
                onChange={(e) => setUserQuery(e.target.value)}
              />

              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="caption" color="text.secondary">Temperature: {temperature}</Typography>
                </Box>
                <Slider
                  value={temperature}
                  min={0.0}
                  max={1.0}
                  step={0.1}
                  onChange={(_, val) => setTemperature(val as number)}
                  size="small"
                />
              </Box>

              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Typography variant="caption" color="text.secondary">Max Reasoning Steps: {maxSteps}</Typography>
                </Box>
                <Slider
                  value={maxSteps}
                  min={1}
                  max={10}
                  step={1}
                  onChange={(_, val) => setMaxSteps(val as number)}
                  size="small"
                />
              </Box>

              <Stack direction="row" spacing={1.5}>
                <Button
                  variant="contained"
                  color="secondary"
                  fullWidth
                  startIcon={traceMutation.isPending ? <CircularProgress size={20} color="inherit" /> : <PlayArrowIcon />}
                  onClick={handleRunTrace}
                  disabled={traceMutation.isPending || !userQuery.trim()}
                >
                  Run Trace
                </Button>
                <Button
                  variant="outlined"
                  fullWidth
                  startIcon={previewMutation.isPending ? <CircularProgress size={20} /> : <VisibilityIcon />}
                  onClick={() => previewMutation.mutate()}
                  disabled={previewMutation.isPending}
                >
                  Preview Prompt
                </Button>
              </Stack>
            </Stack>
          </Paper>
        </Grid>

        {/* Right Execution & Trace Output */}
        <Grid item xs={12} md={7.5}>
          <Paper sx={{ p: 3, borderRadius: 2, minHeight: 520 }}>
            {/* Top Telemetry Header if Trace Exists */}
            {traceResult && (
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 2, mb: 2.5 }}>
                <Grid container spacing={2} alignItems="center">
                  <Grid item xs={4}>
                    <Typography variant="caption" color="text.secondary">Total Latency</Typography>
                    <Typography variant="h6" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      <TimerIcon fontSize="small" color="primary" /> {traceResult.totalLatencyMs} ms
                    </Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" color="text.secondary">Total Tokens</Typography>
                    <Typography variant="h6" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      <MemoryIcon fontSize="small" color="warning" /> {traceResult.totalTokens}
                    </Typography>
                  </Grid>
                  <Grid item xs={4}>
                    <Typography variant="caption" color="text.secondary">Tools Executed</Typography>
                    <Typography variant="h6" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                      <BuildCircleIcon fontSize="small" color="success" /> {traceResult.totalToolsExecuted} calls
                    </Typography>
                  </Grid>
                </Grid>
              </Box>
            )}

            <Tabs value={activeTab} onChange={(_, val) => setActiveTab(val)} sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
              <Tab label="Reasoning Trace Steps" />
              <Tab label="Prompt & Injected Context" />
              <Tab label="Final Output" />
            </Tabs>

            {/* TAB 0: Step-by-Step Reasoning Trace */}
            {activeTab === 0 && (
              <Box>
                {traceMutation.isPending ? (
                  <Box sx={{ py: 8, textAlign: 'center' }}>
                    <CircularProgress size={40} color="secondary" />
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                      Executing multi-step reasoning loop with tool execution engine...
                    </Typography>
                  </Box>
                ) : !traceResult ? (
                  <Box sx={{ py: 8, textAlign: 'center' }}>
                    <ScienceIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                    <Typography variant="body1" color="text.secondary">
                      No trace executed yet. Click <strong>Run Trace</strong> to simulate an agent reasoning cycle.
                    </Typography>
                  </Box>
                ) : (
                  <Stack spacing={2}>
                    {traceResult.traceSteps.map((step) => (
                      <Card key={step.stepIndex} variant="outlined" sx={{ borderRadius: 2 }}>
                        <CardContent sx={{ pb: 1.5 }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                            <Chip label={`Step ${step.stepIndex}`} size="small" color="primary" />
                            <Typography variant="caption" color="text.secondary">
                              {step.stepLatencyMs} ms | {step.stepTokens} tokens
                            </Typography>
                          </Box>

                          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mb: 1.5 }}>
                            {step.assistantThought}
                          </Typography>

                          {/* Tool Calls */}
                          {step.toolCalls && step.toolCalls.length > 0 && (
                            <Stack spacing={1}>
                              {step.toolCalls.map((tc, idx) => (
                                <Accordion key={idx} variant="outlined" sx={{ bgcolor: 'background.default' }}>
                                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                      <BuildCircleIcon fontSize="small" color="secondary" />
                                      <Typography variant="caption" fontWeight="bold" fontFamily="monospace">
                                        {tc.toolName}
                                      </Typography>
                                      <Chip label={`${tc.executionMs}ms`} size="small" variant="outlined" />
                                      {tc.requiresApproval && <Chip label="Requires Approval" size="small" color="warning" />}
                                    </Box>
                                  </AccordionSummary>
                                  <AccordionDetails>
                                    <Typography variant="caption" color="text.secondary" fontWeight="bold">ARGUMENTS:</Typography>
                                    <Box component="pre" sx={{ p: 1, bgcolor: 'background.paper', borderRadius: 1, fontSize: '0.75rem', overflowX: 'auto' }}>
                                      {JSON.stringify(tc.arguments, null, 2)}
                                    </Box>
                                    <Typography variant="caption" color="text.secondary" fontWeight="bold" sx={{ mt: 1, display: 'block' }}>RESULT:</Typography>
                                    <Box component="pre" sx={{ p: 1, bgcolor: 'background.paper', borderRadius: 1, fontSize: '0.75rem', overflowX: 'auto' }}>
                                      {tc.resultOutput}
                                    </Box>
                                  </AccordionDetails>
                                </Accordion>
                              ))}
                            </Stack>
                          )}
                        </CardContent>
                      </Card>
                    ))}
                  </Stack>
                )}
              </Box>
            )}

            {/* TAB 1: Prompt & Injected Context */}
            {activeTab === 1 && (
              <Box>
                {previewMutation.isPending ? (
                  <Box sx={{ py: 8, textAlign: 'center' }}>
                    <CircularProgress size={32} />
                  </Box>
                ) : (
                  <Stack spacing={2.5}>
                    {/* Rendered Prompt */}
                    <Box>
                      <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <TextSnippetIcon color="primary" fontSize="small" /> Rendered System Prompt
                      </Typography>
                      <Box
                        component="pre"
                        sx={{
                          p: 2,
                          bgcolor: 'background.default',
                          borderRadius: 2,
                          fontSize: '0.8rem',
                          maxHeight: 250,
                          overflowY: 'auto',
                          whiteSpace: 'pre-wrap',
                          fontFamily: 'monospace',
                        }}
                      >
                        {previewResult ? previewResult.renderedPrompt : traceResult?.injectedContext?.systemPrompt || 'No prompt preview generated.'}
                      </Box>
                    </Box>

                    {/* Injected Customer Memory */}
                    <Box>
                      <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <MemoryIcon color="secondary" fontSize="small" /> Injected Customer Memory
                      </Typography>
                      {previewResult?.injectedMemories && previewResult.injectedMemories.length > 0 ? (
                        <Stack spacing={0.5}>
                          {previewResult.injectedMemories.map((m, idx) => (
                            <Chip key={idx} label={`[${m.category}] ${m.key}: ${m.value}`} size="small" variant="outlined" />
                          ))}
                        </Stack>
                      ) : (
                        <Typography variant="caption" color="text.secondary">No customer profile preferences injected.</Typography>
                      )}
                    </Box>

                    {/* Injected RAG Chunks */}
                    <Box>
                      <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                        <HubIcon color="success" fontSize="small" /> Vector Knowledge Base Matches
                      </Typography>
                      {previewResult?.injectedRagChunks && previewResult.injectedRagChunks.length > 0 ? (
                        <Stack spacing={1}>
                          {previewResult.injectedRagChunks.map((c, idx) => (
                            <Box key={idx} sx={{ p: 1.5, bgcolor: 'background.default', borderRadius: 1 }}>
                              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                                <Typography variant="caption" fontWeight="bold">{c.documentTitle}</Typography>
                                <Chip label={`Score: ${(c.similarityScore * 100).toFixed(1)}%`} size="small" color="primary" />
                              </Box>
                              <Typography variant="caption" color="text.secondary">{c.contentSnippet}</Typography>
                            </Box>
                          ))}
                        </Stack>
                      ) : (
                        <Typography variant="caption" color="text.secondary">No vector knowledge chunks retrieved.</Typography>
                      )}
                    </Box>
                  </Stack>
                )}
              </Box>
            )}

            {/* TAB 2: Final Output */}
            {activeTab === 2 && (
              <Box>
                {traceResult ? (
                  <Box sx={{ p: 3, bgcolor: 'primary.50', borderRadius: 2, borderLeft: '4px solid #9c27b0' }}>
                    <Typography variant="subtitle2" color="secondary" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <CheckCircleOutlineIcon fontSize="small" /> Final Assistant Response
                    </Typography>
                    <Typography variant="body1" sx={{ mt: 1.5, whiteSpace: 'pre-wrap' }}>
                      {traceResult.finalResponse}
                    </Typography>
                  </Box>
                ) : (
                  <Alert severity="info">Run a trace to inspect the final response.</Alert>
                )}
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
};
