import React, { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Card,
  CardContent,
  Grid,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Slider,
} from '@mui/material';
import {
  Build as BuildIcon,
  PlayArrow as PlayArrowIcon,
  Memory as MemoryIcon,
  Psychology as PsychologyIcon,
  AttachMoney as MoneyIcon,
  Speed as SpeedIcon,
  Warning as WarningIcon,
} from '@mui/icons-material';
import { useQuery, useMutation } from '@tanstack/react-query';
import { toolsApi, ToolDefinitionDto, ToolExecutionResponse } from '../api/tools';
import { llmApi, LlmGenerateResponse } from '../api/llm';

export const ToolsPage: React.FC = () => {
  const [tab, setTab] = useState(0);

  // Tool Test State
  const [selectedTool, setSelectedTool] = useState<ToolDefinitionDto | null>(null);
  const [testParamsJson, setTestParamsJson] = useState('{}');
  const [executionResponse, setExecutionResponse] = useState<ToolExecutionResponse | null>(null);
  const [testDialogOpen, setTestDialogOpen] = useState(false);

  // LLM Test State
  const [systemPrompt, setSystemPrompt] = useState('You are ShopAI Assistant for e-commerce intelligence.');
  const [userPrompt, setUserPrompt] = useState('Search for winter coats and summarize return policy.');
  const [temperature, setTemperature] = useState(0.7);
  const [llmResult, setLlmResult] = useState<LlmGenerateResponse | null>(null);

  // Queries
  const { data: tools = [], isLoading: toolsLoading } = useQuery({
    queryKey: ['tools'],
    queryFn: () => toolsApi.getTools(),
  });

  const { data: llmStatus, refetch: refetchStatus } = useQuery({
    queryKey: ['llmStatus'],
    queryFn: () => llmApi.getStatus(),
    refetchInterval: 10000,
  });

  const { data: llmLogs = [], refetch: refetchLogs } = useQuery({
    queryKey: ['llmLogs'],
    queryFn: () => llmApi.getLogs(),
    refetchInterval: 10000,
  });

  // Mutations
  const executeToolMutation = useMutation({
    mutationFn: ({ toolName, params }: { toolName: string; params: Record<string, any> }) =>
      toolsApi.executeTool(toolName, params, 'ManualToolTester'),
    onSuccess: (data) => {
      setExecutionResponse(data);
    },
  });

  const generateLlmMutation = useMutation({
    mutationFn: () => llmApi.generate(userPrompt, systemPrompt, 'InteractiveConsole', temperature),
    onSuccess: (data) => {
      setLlmResult(data);
      refetchLogs();
      refetchStatus();
    },
  });

  const handleOpenTest = (tool: ToolDefinitionDto) => {
    setSelectedTool(tool);
    setExecutionResponse(null);

    // Generate initial template based on schema properties
    const sampleParams: Record<string, any> = {};
    if (tool.parametersSchema?.properties) {
      Object.keys(tool.parametersSchema.properties).forEach((key) => {
        const prop = tool.parametersSchema.properties[key];
        if (key === 'orderNumber') sampleParams[key] = '#1001';
        else if (key === 'email') sampleParams[key] = 'customer@example.com';
        else if (key === 'query') sampleParams[key] = 'winter boots';
        else if (key === 'amount') sampleParams[key] = 25.0;
        else if (key === 'reason') sampleParams[key] = 'Customer return request';
        else if (prop.type === 'integer' || prop.type === 'number') sampleParams[key] = 1;
        else if (prop.type === 'boolean') sampleParams[key] = true;
        else sampleParams[key] = 'example';
      });
    }
    setTestParamsJson(JSON.stringify(sampleParams, null, 2));
    setTestDialogOpen(true);
  };

  const handleRunExecution = () => {
    if (!selectedTool) return;
    try {
      const parsed = JSON.parse(testParamsJson);
      executeToolMutation.mutate({ toolName: selectedTool.name, params: parsed });
    } catch (e: any) {
      alert('Invalid JSON parameters: ' + e.message);
    }
  };

  const getRiskChip = (risk: string) => {
    switch (risk) {
      case 'CRITICAL':
        return <Chip label="CRITICAL RISK" size="small" sx={{ bgcolor: '#d32f2f', color: '#fff', fontWeight: 600 }} />;
      case 'HIGH':
        return <Chip label="HIGH RISK" size="small" sx={{ bgcolor: '#ed6c02', color: '#fff', fontWeight: 600 }} />;
      case 'MEDIUM':
        return <Chip label="MEDIUM RISK" size="small" color="warning" variant="outlined" />;
      default:
        return <Chip label="LOW RISK" size="small" color="info" variant="outlined" />;
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <BuildIcon sx={{ fontSize: 32, color: 'primary.main' }} />
            <Typography variant="h4" fontWeight={700}>
              Tool Registry & LLM Subsystem
            </Typography>
          </Box>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            AI Agent tool catalog, risk evaluation engine, and multi-provider LLM gateway telemetry.
          </Typography>
        </Box>
      </Box>

      {/* Navigation Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={tab} onChange={(_, v) => setTab(v)} textColor="primary" indicatorColor="primary">
          <Tab icon={<BuildIcon />} iconPosition="start" label="Agent Tool Catalog" />
          <Tab icon={<PsychologyIcon />} iconPosition="start" label="LLM Gateway & Telemetry" />
        </Tabs>
      </Paper>

      {/* Tab 0: Tool Catalog */}
      {tab === 0 && (
        <Box>
          {toolsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Grid container spacing={3}>
              {tools.map((tool) => (
                <Grid item xs={12} md={6} lg={4} key={tool.name}>
                  <Card variant="outlined" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                    <CardContent sx={{ flexGrow: 1, p: 2.5 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
                        <Typography variant="h6" fontWeight={700} sx={{ fontFamily: 'monospace' }}>
                          {tool.name}
                        </Typography>
                        {getRiskChip(tool.riskLevel)}
                      </Box>

                      <Typography variant="body2" color="text.secondary" sx={{ minHeight: 40, mb: 2 }}>
                        {tool.description}
                      </Typography>

                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
                        <Chip label={`Perm: ${tool.requiredPermission}`} size="small" variant="outlined" />
                        {tool.requiresHumanApproval && (
                          <Chip label="Requires Approval" size="small" color="warning" icon={<WarningIcon />} />
                        )}
                      </Box>

                      <Paper
                        variant="outlined"
                        sx={{
                          p: 1,
                          bgcolor: 'grey.50',
                          fontFamily: 'monospace',
                          fontSize: '0.75rem',
                          maxHeight: 100,
                          overflow: 'auto',
                        }}
                      >
                        {JSON.stringify(tool.parametersSchema, null, 2)}
                      </Paper>
                    </CardContent>

                    <Box sx={{ p: 2, pt: 0 }}>
                      <Button
                        fullWidth
                        variant="contained"
                        size="small"
                        startIcon={<PlayArrowIcon />}
                        onClick={() => handleOpenTest(tool)}
                      >
                        Test / Execute
                      </Button>
                    </Box>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Box>
      )}

      {/* Tab 1: LLM Gateway & Telemetry */}
      {tab === 1 && (
        <Box>
          {/* Status Metric Cards */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card variant="outlined">
                <CardContent sx={{ p: 2.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <PsychologyIcon color="primary" />
                    <Typography variant="subtitle2" color="text.secondary">
                      Active Provider
                    </Typography>
                  </Box>
                  <Typography variant="h5" fontWeight={700}>
                    {llmStatus?.activeProvider || 'LOCAL_FALLBACK'}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Model: {llmStatus?.activeModel || 'deterministic-sim'}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card variant="outlined">
                <CardContent sx={{ p: 2.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <MemoryIcon color="secondary" />
                    <Typography variant="subtitle2" color="text.secondary">
                      Total Tokens
                    </Typography>
                  </Box>
                  <Typography variant="h5" fontWeight={700}>
                    {llmStatus?.totalTokensUsed?.toLocaleString() || 0}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Prompt + Completion tokens
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card variant="outlined">
                <CardContent sx={{ p: 2.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <MoneyIcon color="success" />
                    <Typography variant="subtitle2" color="text.secondary">
                      Estimated Cost
                    </Typography>
                  </Box>
                  <Typography variant="h5" fontWeight={700}>
                    ${Number(llmStatus?.totalCostUsd || 0).toFixed(4)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    USD cumulative expenditure
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} sm={6} md={3}>
              <Card variant="outlined">
                <CardContent sx={{ p: 2.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                    <SpeedIcon color="warning" />
                    <Typography variant="subtitle2" color="text.secondary">
                      Provider Readiness
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', mt: 0.5 }}>
                    <Chip
                      label="OpenAI"
                      size="small"
                      color={llmStatus?.openaiConfigured ? 'success' : 'default'}
                    />
                    <Chip
                      label="Gemini"
                      size="small"
                      color={llmStatus?.geminiConfigured ? 'success' : 'default'}
                    />
                    <Chip
                      label="Claude"
                      size="small"
                      color={llmStatus?.anthropicConfigured ? 'success' : 'default'}
                    />
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          {/* Test Prompt Generator */}
          <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Test LLM Prompt Generation
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="System Prompt"
                  value={systemPrompt}
                  onChange={(e) => setSystemPrompt(e.target.value)}
                  size="small"
                  multiline
                  rows={2}
                  sx={{ mb: 2 }}
                />
                <TextField
                  fullWidth
                  label="User Prompt"
                  value={userPrompt}
                  onChange={(e) => setUserPrompt(e.target.value)}
                  size="small"
                  multiline
                  rows={3}
                  sx={{ mb: 2 }}
                />
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
                  <Typography variant="caption">Temperature: {temperature}</Typography>
                  <Slider
                    value={temperature}
                    min={0.0}
                    max={1.0}
                    step={0.1}
                    onChange={(_, v) => setTemperature(v as number)}
                    sx={{ width: 150 }}
                  />
                </Box>
                <Button
                  variant="contained"
                  startIcon={<PlayArrowIcon />}
                  disabled={generateLlmMutation.isPending}
                  onClick={() => generateLlmMutation.mutate()}
                >
                  {generateLlmMutation.isPending ? 'Generating...' : 'Generate Response'}
                </Button>
              </Grid>

              <Grid item xs={12} md={6}>
                <Typography variant="caption" fontWeight={600}>
                  LLM Response Output:
                </Typography>
                <Paper
                  variant="outlined"
                  sx={{
                    p: 2,
                    mt: 1,
                    minHeight: 180,
                    maxHeight: 250,
                    overflow: 'auto',
                    bgcolor: 'grey.50',
                    fontFamily: 'monospace',
                    fontSize: '0.85rem',
                  }}
                >
                  {generateLlmMutation.isPending ? (
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'text.secondary' }}>
                      <CircularProgress size={16} /> Generating inference...
                    </Box>
                  ) : llmResult ? (
                    <div>
                      <div style={{ marginBottom: 8, color: '#1976d2', fontWeight: 600 }}>
                        [{llmResult.providerName} / {llmResult.modelName}] ({llmResult.totalTokens} tokens)
                      </div>
                      <div style={{ whiteSpace: 'pre-wrap' }}>{llmResult.content}</div>
                    </div>
                  ) : (
                    <span style={{ color: '#999' }}>Response will appear here.</span>
                  )}
                </Paper>
              </Grid>
            </Grid>
          </Paper>

          {/* Telemetry Logs Table */}
          <Paper variant="outlined">
            <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Typography variant="h6" fontWeight={700}>
                Recent LLM Telemetry Logs
              </Typography>
              <Button size="small" onClick={() => refetchLogs()}>
                Refresh Logs
              </Button>
            </Box>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Timestamp</TableCell>
                  <TableCell>Agent</TableCell>
                  <TableCell>Provider</TableCell>
                  <TableCell>Model</TableCell>
                  <TableCell align="right">Prompt Tk</TableCell>
                  <TableCell align="right">Comp Tk</TableCell>
                  <TableCell align="right">Total Tk</TableCell>
                  <TableCell align="right">Latency</TableCell>
                  <TableCell align="right">Cost ($)</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {llmLogs.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} align="center" sx={{ py: 3, color: 'text.secondary' }}>
                      No LLM usage recorded yet.
                    </TableCell>
                  </TableRow>
                ) : (
                  llmLogs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell>{new Date(log.createdAt).toLocaleTimeString()}</TableCell>
                      <TableCell>
                        <Chip label={log.agentName} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell>{log.provider}</TableCell>
                      <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{log.model}</TableCell>
                      <TableCell align="right">{log.promptTokens}</TableCell>
                      <TableCell align="right">{log.completionTokens}</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 600 }}>
                        {log.totalTokens}
                      </TableCell>
                      <TableCell align="right">{log.latencyMs}ms</TableCell>
                      <TableCell align="right">${Number(log.costUsd || 0).toFixed(6)}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </Paper>
        </Box>
      )}

      {/* Tool Execution Dialog */}
      <Dialog open={testDialogOpen} onClose={() => setTestDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          Execute Tool: <strong>{selectedTool?.name}</strong>
        </DialogTitle>
        <DialogContent>
          {selectedTool && (
            <Box sx={{ mt: 1 }}>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                {selectedTool.description}
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, my: 1 }}>
                {getRiskChip(selectedTool.riskLevel)}
                {selectedTool.requiresHumanApproval && (
                  <Chip label="Requires Approval Gate" size="small" color="warning" />
                )}
              </Box>

              <Typography variant="subtitle2" sx={{ mt: 2, mb: 0.5 }}>
                JSON Input Parameters:
              </Typography>
              <TextField
                fullWidth
                multiline
                rows={5}
                value={testParamsJson}
                onChange={(e) => setTestParamsJson(e.target.value)}
                sx={{ fontFamily: 'monospace' }}
              />

              {executionResponse && (
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" gutterBottom>
                    Execution Response:
                  </Typography>
                  <Alert
                    severity={
                      executionResponse.status === 'SUCCESS'
                        ? 'success'
                        : executionResponse.status === 'PENDING_APPROVAL'
                        ? 'warning'
                        : 'error'
                    }
                    sx={{ mb: 1 }}
                  >
                    <strong>Status: {executionResponse.status}</strong>
                    {executionResponse.message && ` - ${executionResponse.message}`}
                  </Alert>
                  <Paper
                    variant="outlined"
                    sx={{
                      p: 1.5,
                      bgcolor: 'grey.50',
                      fontFamily: 'monospace',
                      fontSize: '0.8rem',
                      maxHeight: 200,
                      overflow: 'auto',
                    }}
                  >
                    <pre style={{ margin: 0 }}>{JSON.stringify(executionResponse, null, 2)}</pre>
                  </Paper>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestDialogOpen(false)}>Close</Button>
          <Button
            variant="contained"
            color="primary"
            startIcon={<PlayArrowIcon />}
            disabled={executeToolMutation.isPending}
            onClick={handleRunExecution}
          >
            {executeToolMutation.isPending ? 'Executing...' : 'Run Tool'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
