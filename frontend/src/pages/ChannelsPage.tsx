import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { channelsApi, ChannelConfigDto, ChannelType, ChannelMessageDto } from '../api/channels';
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
  TextField,
  FormControlLabel,
  Switch,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import WhatsAppIcon from '@mui/icons-material/WhatsApp';
import EmailIcon from '@mui/icons-material/Email';
import SettingsIcon from '@mui/icons-material/Settings';
import SendIcon from '@mui/icons-material/Send';
import ChatIcon from '@mui/icons-material/Chat';
import RefreshIcon from '@mui/icons-material/Refresh';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PlayCircleFilledWhiteIcon from '@mui/icons-material/PlayCircleFilledWhite';

export const ChannelsPage: React.FC = () => {
  const queryClient = useQueryClient();

  // Channel Config Dialog State
  const [editingConfig, setEditingConfig] = useState<ChannelConfigDto | null>(null);
  const [editEnabled, setEditEnabled] = useState(false);
  const [editAutoReply, setEditAutoReply] = useState(true);
  const [editConfigData, setEditConfigData] = useState<Record<string, any>>({});

  // Simulator State
  const [simChannel, setSimChannel] = useState<ChannelType>('WHATSAPP');
  const [simSenderId, setSimSenderId] = useState('');
  const [simSenderName, setSimSenderName] = useState('');
  const [simSubject, setSimSubject] = useState('');
  const [simMessage, setSimMessage] = useState('');

  // Filter State for Message Log
  const [filterChannel, setFilterChannel] = useState<string>('ALL');

  const { data: configs, refetch: refetchConfigs } = useQuery({
    queryKey: ['channelConfigs'],
    queryFn: channelsApi.getConfigs,
  });

  const { data: messagesData, isLoading: loadingMessages, refetch: refetchMessages } = useQuery({
    queryKey: ['channelMessages', filterChannel],
    queryFn: () => channelsApi.getMessages({
      channelType: filterChannel === 'ALL' ? undefined : (filterChannel as ChannelType),
      size: 15,
    }),
    refetchInterval: 10000,
  });

  const updateConfigMutation = useMutation({
    mutationFn: (vars: { type: ChannelType; enabled: boolean; autoReply: boolean; data: Record<string, any> }) =>
      channelsApi.updateConfig(vars.type, {
        enabled: vars.enabled,
        autoReplyEnabled: vars.autoReply,
        configData: vars.data,
      }),
    onSuccess: () => {
      setEditingConfig(null);
      queryClient.invalidateQueries({ queryKey: ['channelConfigs'] });
      refetchConfigs();
    },
  });

  const simulateMutation = useMutation({
    mutationFn: channelsApi.simulateMessage,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['channelMessages'] });
      refetchMessages();
    },
  });

  const handleOpenEdit = (config: ChannelConfigDto) => {
    setEditingConfig(config);
    setEditEnabled(config.enabled);
    setEditAutoReply(config.autoReplyEnabled);
    setEditConfigData(config.configData || {});
  };

  const handleSaveConfig = () => {
    if (!editingConfig) return;
    updateConfigMutation.mutate({
      type: editingConfig.channelType,
      enabled: editEnabled,
      autoReply: editAutoReply,
      data: editConfigData,
    });
  };

  const handleSimulateSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!simMessage.trim() || simulateMutation.isPending) return;

    simulateMutation.mutate({
      channelType: simChannel,
      senderId: simSenderId,
      senderName: simSenderName,
      subject: simChannel === 'EMAIL' ? (simSubject || 'Support Inquiry') : undefined,
      message: simMessage,
    });
  };

  const quickPrompts = [
    'Where is my order #1001?',
    'What is your store return and refund policy?',
    'Recommend a snowboard that matches my riding style and size',
    'Do you have winter boots in size 10?',
  ];

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 6 }}>
      {/* Header Banner */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <ChatIcon color="primary" sx={{ fontSize: 38 }} /> Multi-Channel Commerce Adapters
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Manage external communication channels (Rule 26: "Every channel must be behind an interface"). Autonomous AI routing for WhatsApp & Email.
          </Typography>
        </Box>

        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => {
            refetchConfigs();
            refetchMessages();
          }}
        >
          Refresh Channels
        </Button>
      </Box>

      {/* Channel Cards Grid */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        {/* WhatsApp Cloud API Card */}
        {(() => {
          const waConfig = configs?.find((c) => c.channelType === 'WHATSAPP');
          const isEnabled = waConfig?.enabled ?? false;

          return (
            <Grid item xs={12} md={4}>
              <Card elevation={2} sx={{ borderRadius: 3, height: '100%', display: 'flex', flexDirection: 'column', borderLeft: '4px solid #25d366' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <WhatsAppIcon sx={{ color: '#25d366', fontSize: 30 }} />
                      <Typography variant="h6" fontWeight="bold">
                        WhatsApp Cloud API
                      </Typography>
                    </Box>
                    <Chip
                      label={isEnabled ? 'ACTIVE' : 'INACTIVE'}
                      size="small"
                      color={isEnabled ? 'success' : 'default'}
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Official Meta Cloud API webhook ingress. Autonomous order tracking, catalog search, and conversational commerce.
                  </Typography>

                  <Box sx={{ bgcolor: '#fbfcfd', p: 1.5, borderRadius: 2, border: '1px solid #eeeeee', mb: 2 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Auto-Reply Mode: <b>{waConfig?.autoReplyEnabled ? 'Autonomous AI' : 'Manual Review'}</b>
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Phone Number ID: <code>{waConfig?.configData?.phoneNumberId || 'Not configured'}</code>
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Webhook Path: <code>/api/v1/channels/whatsapp/webhook</code>
                    </Typography>
                  </Box>
                </CardContent>
                <Box sx={{ p: 2, pt: 0 }}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<SettingsIcon />}
                    onClick={() => waConfig && handleOpenEdit(waConfig)}
                    sx={{ textTransform: 'none' }}
                  >
                    Configure WhatsApp
                  </Button>
                </Box>
              </Card>
            </Grid>
          );
        })()}

        {/* Email Support Channel Card */}
        {(() => {
          const emConfig = configs?.find((c) => c.channelType === 'EMAIL');
          const isEnabled = emConfig?.enabled ?? false;

          return (
            <Grid item xs={12} md={4}>
              <Card elevation={2} sx={{ borderRadius: 3, height: '100%', display: 'flex', flexDirection: 'column', borderLeft: '4px solid #1976d2' }}>
                <CardContent sx={{ flexGrow: 1 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <EmailIcon color="primary" sx={{ fontSize: 30 }} />
                      <Typography variant="h6" fontWeight="bold">
                        Email Support Channel
                      </Typography>
                    </Box>
                    <Chip
                      label={isEnabled ? 'ACTIVE' : 'INACTIVE'}
                      size="small"
                      color={isEnabled ? 'primary' : 'default'}
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                    Inbound email webhook & thread parser. Reconstructs customer order threads and answers store policy questions.
                  </Typography>

                  <Box sx={{ bgcolor: '#fbfcfd', p: 1.5, borderRadius: 2, border: '1px solid #eeeeee', mb: 2 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Auto-Reply Mode: <b>{emConfig?.autoReplyEnabled ? 'Autonomous AI' : 'Draft / Manual Review'}</b>
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Support Address: <code>{emConfig?.configData?.supportEmail || 'support@shopai.dev'}</code>
                    </Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                      Webhook Path: <code>/api/v1/channels/email/webhook</code>
                    </Typography>
                  </Box>
                </CardContent>
                <Box sx={{ p: 2, pt: 0 }}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<SettingsIcon />}
                    onClick={() => emConfig && handleOpenEdit(emConfig)}
                    sx={{ textTransform: 'none' }}
                  >
                    Configure Email
                  </Button>
                </Box>
              </Card>
            </Grid>
          );
        })()}

        {/* Storefront Web Widget Card */}
        <Grid item xs={12} md={4}>
          <Card elevation={2} sx={{ borderRadius: 3, height: '100%', display: 'flex', flexDirection: 'column', borderLeft: '4px solid #9c27b0' }}>
            <CardContent sx={{ flexGrow: 1 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <SmartToyIcon color="secondary" sx={{ fontSize: 30 }} />
                  <Typography variant="h6" fontWeight="bold">
                    Storefront Widget SDK
                  </Typography>
                </Box>
                <Chip label="ACTIVE" size="small" color="secondary" />
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Customer-facing embedded chat widget with customer memory and shopping assistant capabilities.
              </Typography>

              <Box sx={{ bgcolor: '#fbfcfd', p: 1.5, borderRadius: 2, border: '1px solid #eeeeee', mb: 2 }}>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Channel Type: <code>STOREFRONT_WIDGET</code>
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Simulator: Available at <code>/chat-preview</code>
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Session Isolation: Single-store dedicated
                </Typography>
              </Box>
            </CardContent>
            <Box sx={{ p: 2, pt: 0 }}>
              <Button
                fullWidth
                variant="outlined"
                color="secondary"
                onClick={() => window.open('/chat-preview', '_blank')}
                sx={{ textTransform: 'none' }}
              >
                Open Assistant Preview
              </Button>
            </Box>
          </Card>
        </Grid>
      </Grid>

      {/* Main Grid: Multi-Channel Simulator & Audit Logs */}
      <Grid container spacing={3}>
        {/* Interactive Multi-Channel Simulator */}
        <Grid item xs={12} lg={5}>
          <Paper elevation={2} sx={{ p: 3, borderRadius: 3 }}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
              <PlayCircleFilledWhiteIcon color="primary" /> Interactive Multi-Channel Simulator
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 2 }}>
              Simulate incoming customer messages across WhatsApp and Email to test real-time AI tool routing and responses:
            </Typography>

            <Box component="form" onSubmit={handleSimulateSend}>
              {/* Channel Selector */}
              <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
                <Chip
                  icon={<WhatsAppIcon sx={{ color: simChannel === 'WHATSAPP' ? 'white !important' : '#25d366' }} />}
                  label="WhatsApp Simulator"
                  color={simChannel === 'WHATSAPP' ? 'success' : 'default'}
                  onClick={() => {
                    setSimChannel('WHATSAPP');
                    setSimSenderId('15551234567');
                    setSimSenderName('Jane Doe');
                  }}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                />
                <Chip
                  icon={<EmailIcon />}
                  label="Email Simulator"
                  color={simChannel === 'EMAIL' ? 'primary' : 'default'}
                  onClick={() => {
                    setSimChannel('EMAIL');
                    setSimSenderId('jane.doe@example.com');
                    setSimSenderName('Jane Doe');
                  }}
                  sx={{ cursor: 'pointer', fontWeight: 'bold' }}
                />
              </Stack>

              {/* Sender Details */}
              <Grid container spacing={1.5} sx={{ mb: 2 }}>
                <Grid item xs={6}>
                  <TextField
                    size="small"
                    fullWidth
                    label={simChannel === 'WHATSAPP' ? 'Sender Phone Number' : 'Sender Email'}
                    value={simSenderId}
                    onChange={(e) => setSimSenderId(e.target.value)}
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    size="small"
                    fullWidth
                    label="Customer Name"
                    value={simSenderName}
                    onChange={(e) => setSimSenderName(e.target.value)}
                  />
                </Grid>
              </Grid>

              {simChannel === 'EMAIL' && (
                <TextField
                  size="small"
                  fullWidth
                  label="Subject Line (e.g. Re: Order #1001)"
                  value={simSubject}
                  onChange={(e) => setSimSubject(e.target.value)}
                  sx={{ mb: 2 }}
                />
              )}

              {/* Quick Prompts */}
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                Quick Test Inquiries:
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.8, mb: 2 }}>
                {quickPrompts.map((p) => (
                  <Chip
                    key={p}
                    label={p}
                    size="small"
                    onClick={() => setSimMessage(p)}
                    sx={{ cursor: 'pointer', fontSize: '0.7rem' }}
                  />
                ))}
              </Box>

              {/* Message Input */}
              <TextField
                fullWidth
                multiline
                rows={3}
                size="small"
                label="Incoming Message Body"
                value={simMessage}
                onChange={(e) => setSimMessage(e.target.value)}
                sx={{ mb: 2 }}
              />

              <Button
                fullWidth
                type="submit"
                variant="contained"
                color={simChannel === 'WHATSAPP' ? 'success' : 'primary'}
                endIcon={simulateMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <SendIcon />}
                disabled={!simMessage.trim() || simulateMutation.isPending}
                sx={{ fontWeight: 'bold' }}
              >
                {simulateMutation.isPending ? 'Processing AI Turn...' : `Dispatch Simulated ${simChannel} Message`}
              </Button>
            </Box>

            {/* Simulation Response Preview Card */}
            {simulateMutation.data && (
              <Box sx={{ mt: 3, p: 2, bgcolor: '#f4f6f8', borderRadius: 2, border: '1px solid #e0e0e0' }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" fontWeight="bold" color="primary.main">
                    🤖 Autonomous AI Response:
                  </Typography>
                  <Chip label={`${simulateMutation.data.latencyMs}ms`} size="small" variant="outlined" />
                </Box>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mb: 1 }}>
                  {simulateMutation.data.outboundReply}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Session ID: <code>{simulateMutation.data.conversationId}</code>
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>

        {/* Message Log Table */}
        <Grid item xs={12} lg={7}>
          <Paper elevation={2} sx={{ p: 3, borderRadius: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 1 }}>
              <Typography variant="subtitle1" fontWeight="bold">
                Channel Message Audit Log
              </Typography>

              {/* Channel Filter Chips */}
              <Stack direction="row" spacing={0.8} flexWrap="wrap">
                {['ALL', 'WHATSAPP', 'EMAIL', 'STOREFRONT_WIDGET'].map((ch) => (
                  <Chip
                    key={ch}
                    label={ch}
                    size="small"
                    color={filterChannel === ch ? 'primary' : 'default'}
                    onClick={() => setFilterChannel(ch)}
                    sx={{ cursor: 'pointer', fontSize: '0.7rem' }}
                  />
                ))}
              </Stack>
            </Box>

            {loadingMessages ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', my: 4 }}>
                <CircularProgress />
              </Box>
            ) : messagesData?.content && messagesData.content.length > 0 ? (
              <TableContainer sx={{ maxHeight: 520 }}>
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell>Channel</TableCell>
                      <TableCell>Direction</TableCell>
                      <TableCell>Sender / Recipient</TableCell>
                      <TableCell>Content</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Time</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {messagesData.content.map((msg: ChannelMessageDto) => {
                      const isInbound = msg.direction === 'INBOUND';
                      return (
                        <TableRow key={msg.id} hover>
                          <TableCell>
                            <Chip
                              icon={msg.channelType === 'WHATSAPP' ? <WhatsAppIcon sx={{ fontSize: '14px !important' }} /> : <EmailIcon sx={{ fontSize: '14px !important' }} />}
                              label={msg.channelType}
                              size="small"
                              variant="outlined"
                              sx={{ fontSize: '0.65rem', height: 22 }}
                            />
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={msg.direction}
                              size="small"
                              color={isInbound ? 'primary' : 'secondary'}
                              sx={{ fontSize: '0.65rem', height: 20 }}
                            />
                          </TableCell>
                          <TableCell>
                            <Typography variant="caption" fontWeight="bold" sx={{ display: 'block' }}>
                              {isInbound ? (msg.senderName || msg.senderId) : msg.recipientId}
                            </Typography>
                          </TableCell>
                          <TableCell sx={{ maxWidth: 220 }}>
                            <Typography variant="caption" sx={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {msg.content}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={msg.status}
                              size="small"
                              color={msg.status === 'SENT' ? 'success' : msg.status === 'FAILED' ? 'error' : 'default'}
                              sx={{ fontSize: '0.65rem', height: 20 }}
                            />
                          </TableCell>
                          <TableCell>
                            <Typography variant="caption" color="text.secondary">
                              {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">
                No channel messages recorded yet. Use the simulator above to dispatch a test turn.
              </Alert>
            )}
          </Paper>
        </Grid>
      </Grid>

      {/* Channel Configuration Modal */}
      <Dialog open={!!editingConfig} onClose={() => setEditingConfig(null)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 'bold' }}>
          Configure {editingConfig?.channelType} Channel
        </DialogTitle>
        <DialogContent dividers>
          {editingConfig && (
            <Stack spacing={2.5} sx={{ mt: 1 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={editEnabled}
                    onChange={(e) => setEditEnabled(e.target.checked)}
                    color="primary"
                  />
                }
                label="Enable Channel Ingress"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={editAutoReply}
                    onChange={(e) => setEditAutoReply(e.target.checked)}
                    color="success"
                  />
                }
                label="Autonomous AI Auto-Reply (Uncheck to queue messages as Drafts)"
              />

              {editingConfig.channelType === 'WHATSAPP' && (
                <>
                  <TextField
                    size="small"
                    fullWidth
                    label="Meta Phone Number ID"
                    value={editConfigData.phoneNumberId || ''}
                    onChange={(e) => setEditConfigData({ ...editConfigData, phoneNumberId: e.target.value })}
                  />
                  <TextField
                    size="small"
                    fullWidth
                    label="Meta WhatsApp Access Token"
                    type="password"
                    value={editConfigData.accessToken || ''}
                    onChange={(e) => setEditConfigData({ ...editConfigData, accessToken: e.target.value })}
                  />
                  <TextField
                    size="small"
                    fullWidth
                    label="Webhook Verify Token (for hub.verify_token)"
                    value={editConfigData.verifyToken || ''}
                    onChange={(e) => setEditConfigData({ ...editConfigData, verifyToken: e.target.value })}
                  />
                </>
              )}

              {editingConfig.channelType === 'EMAIL' && (
                <>
                  <TextField
                    size="small"
                    fullWidth
                    label="Store Support Email Address"
                    value={editConfigData.supportEmail || ''}
                    onChange={(e) => setEditConfigData({ ...editConfigData, supportEmail: e.target.value })}
                  />
                  <TextField
                    size="small"
                    fullWidth
                    label="Sender Display Name"
                    value={editConfigData.senderName || ''}
                    onChange={(e) => setEditConfigData({ ...editConfigData, senderName: e.target.value })}
                  />
                </>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => setEditingConfig(null)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveConfig}
            disabled={updateConfigMutation.isPending}
          >
            {updateConfigMutation.isPending ? 'Saving...' : 'Save Configuration'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};
