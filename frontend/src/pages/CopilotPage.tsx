import React, { useState, useEffect, useRef } from 'react';
import {
  Box,
  Typography,
  Paper,
  Button,
  TextField,
  IconButton,
  Chip,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  CircularProgress,
  Alert,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
} from '@mui/material';
import {
  Send as SendIcon,
  Add as AddIcon,
  SmartToy as BotIcon,
  Person as PersonIcon,
  Build as BuildIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Warning as WarningIcon,
  DeleteOutline as DeleteIcon,
  Refresh as RefreshIcon,
  Bolt as BoltIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { agentApi, MessageDto } from '../api/agent';
import { toolsApi } from '../api/tools';

export const CopilotPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeConvId, setActiveConvId] = useState<string | null>(null);
  const [inputMessage, setInputMessage] = useState('');
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedPendingActionId, setSelectedPendingActionId] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // 1. Fetch Conversations
  const { data: convsData, isLoading: convsLoading, refetch: refetchConvs } = useQuery({
    queryKey: ['conversations', 'ADMIN_COPILOT'],
    queryFn: () => agentApi.listConversations('ADMIN_COPILOT', undefined, undefined, 0, 30),
    refetchInterval: 10000,
  });

  const conversations = convsData?.content || [];

  // Set default active conversation if none selected
  useEffect(() => {
    if (!activeConvId && conversations.length > 0) {
      setActiveConvId(conversations[0].id);
    }
  }, [conversations, activeConvId]);

  // 2. Fetch Active Conversation Details
  const { data: activeConv, isLoading: activeConvLoading, refetch: refetchActiveConv } = useQuery({
    queryKey: ['conversation', activeConvId],
    queryFn: () => (activeConvId ? agentApi.getConversation(activeConvId) : null),
    enabled: !!activeConvId,
    refetchInterval: 5000,
  });

  // Auto-scroll to bottom of messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeConv?.messages]);

  // Mutations
  const createConvMutation = useMutation({
    mutationFn: () =>
      agentApi.createConversation({
        title: 'New Copilot Session',
        agentType: 'ADMIN_COPILOT',
        channel: 'ADMIN_COPILOT',
      }),
    onSuccess: (newConv) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      setActiveConvId(newConv.id);
    },
  });

  const deleteConvMutation = useMutation({
    mutationFn: (id: string) => agentApi.deleteConversation(id),
    onSuccess: (_, deletedId) => {
      queryClient.invalidateQueries({ queryKey: ['conversations'] });
      if (activeConvId === deletedId) {
        setActiveConvId(null);
      }
    },
  });

  const sendMessageMutation = useMutation({
    mutationFn: ({ id, content }: { id: string; content: string }) =>
      agentApi.sendMessage(id, content),
    onSuccess: () => {
      setInputMessage('');
      refetchActiveConv();
      refetchConvs();
    },
  });

  const approveActionMutation = useMutation({
    mutationFn: (actionId: string) => toolsApi.approveAction(actionId),
    onSuccess: (data) => {
      setActionSuccessMsg(`Action ${data.toolName} approved & executed successfully!`);
      refetchActiveConv();
      setTimeout(() => setActionSuccessMsg(null), 5000);
    },
  });

  const rejectActionMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => toolsApi.rejectAction(id, reason),
    onSuccess: () => {
      setActionSuccessMsg('Action rejected successfully.');
      setRejectDialogOpen(false);
      setRejectionReason('');
      refetchActiveConv();
      setTimeout(() => setActionSuccessMsg(null), 5000);
    },
  });

  const handleSendMessage = () => {
    if (!inputMessage.trim() || !activeConvId) return;
    sendMessageMutation.mutate({ id: activeConvId, content: inputMessage.trim() });
  };

  const handleQuickPrompt = (prompt: string) => {
    if (!activeConvId) return;
    sendMessageMutation.mutate({ id: activeConvId, content: prompt });
  };

  const handleOpenReject = (actionId: string) => {
    setSelectedPendingActionId(actionId);
    setRejectionReason('');
    setRejectDialogOpen(true);
  };

  const renderMessageContent = (msg: MessageDto) => {
    if (msg.role === 'tool') {
      let parsedResult: any = msg.content;
      try {
        parsedResult = JSON.parse(msg.content);
      } catch (ignored) {}

      const isPending = parsedResult?.status === 'PENDING_APPROVAL';

      return (
        <Card
          variant="outlined"
          sx={{
            my: 1,
            bgcolor: isPending ? 'rgba(237, 108, 2, 0.05)' : 'grey.50',
            borderLeft: isPending ? '4px solid #ed6c02' : '4px solid #1976d2',
          }}
        >
          <CardContent sx={{ p: 1.5, '&:last-child': { pb: 1.5 } }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <BuildIcon fontSize="small" color={isPending ? 'warning' : 'primary'} />
                <Typography variant="caption" fontWeight={700} sx={{ fontFamily: 'monospace' }}>
                  Tool Execution: {msg.toolName || 'Tool'}
                </Typography>
                {isPending && (
                  <Chip label="REQUIRES APPROVAL" size="small" color="warning" icon={<WarningIcon />} />
                )}
              </Box>
            </Box>

            {isPending ? (
              <Box sx={{ mt: 1 }}>
                <Typography variant="body2" color="text.secondary">
                  {parsedResult.message || 'This high-risk action has been paused and requires your authorization.'}
                </Typography>
                <Box sx={{ display: 'flex', gap: 1, mt: 1.5 }}>
                  <Button
                    size="small"
                    variant="contained"
                    color="success"
                    startIcon={<CheckCircleIcon />}
                    disabled={approveActionMutation.isPending}
                    onClick={() => approveActionMutation.mutate(parsedResult.actionRequestId)}
                  >
                    Approve & Execute
                  </Button>
                  <Button
                    size="small"
                    variant="outlined"
                    color="error"
                    startIcon={<CancelIcon />}
                    disabled={rejectActionMutation.isPending}
                    onClick={() => handleOpenReject(parsedResult.actionRequestId)}
                  >
                    Reject
                  </Button>
                </Box>
              </Box>
            ) : (
              <Paper
                variant="outlined"
                sx={{
                  p: 1,
                  mt: 0.5,
                  bgcolor: 'background.paper',
                  fontFamily: 'monospace',
                  fontSize: '0.75rem',
                  maxHeight: 120,
                  overflow: 'auto',
                }}
              >
                <pre style={{ margin: 0 }}>{JSON.stringify(parsedResult, null, 2)}</pre>
              </Paper>
            )}
          </CardContent>
        </Card>
      );
    }

    return (
      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>
        {msg.content}
      </Typography>
    );
  };

  const messages = activeConv?.messages || [];

  return (
    <Box sx={{ height: 'calc(100vh - 70px)', display: 'flex', overflow: 'hidden' }}>
      {/* Left Sidebar: Sessions */}
      <Paper
        square
        sx={{
          width: 320,
          borderRight: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          flexDirection: 'column',
          bgcolor: 'grey.50',
        }}
      >
        <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Button
            fullWidth
            variant="contained"
            startIcon={<AddIcon />}
            disabled={createConvMutation.isPending}
            onClick={() => createConvMutation.mutate()}
          >
            New Copilot Session
          </Button>
        </Box>

        <Box sx={{ flexGrow: 1, overflowY: 'auto', p: 1 }}>
          {convsLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={24} />
            </Box>
          ) : conversations.length === 0 ? (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
              No active sessions. Click above to start one.
            </Typography>
          ) : (
            <List disablePadding>
              {conversations.map((conv) => (
                <ListItem
                  key={conv.id}
                  disablePadding
                  secondaryAction={
                    <IconButton
                      edge="end"
                      size="small"
                      onClick={(e) => {
                        e.stopPropagation();
                        deleteConvMutation.mutate(conv.id);
                      }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  }
                  sx={{ mb: 0.5 }}
                >
                  <ListItemButton
                    selected={conv.id === activeConvId}
                    onClick={() => setActiveConvId(conv.id)}
                    sx={{ borderRadius: 1.5 }}
                  >
                    <ListItemText
                      primary={conv.title}
                      secondary={new Date(conv.updatedAt).toLocaleTimeString()}
                      primaryTypographyProps={{
                        noWrap: true,
                        fontSize: '0.9rem',
                        fontWeight: conv.id === activeConvId ? 700 : 500,
                      }}
                      secondaryTypographyProps={{ fontSize: '0.75rem' }}
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          )}
        </Box>
      </Paper>

      {/* Main Chat Area */}
      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
        {/* Header */}
        <Box
          sx={{
            p: 2,
            borderBottom: '1px solid',
            borderColor: 'divider',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            bgcolor: 'background.paper',
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <BotIcon color="primary" sx={{ fontSize: 28 }} />
            <Box>
              <Typography variant="h6" fontWeight={700}>
                {activeConv?.title || 'Merchant Copilot'}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Autonomous Commerce Operator • Shopify & pgvector Integrated
              </Typography>
            </Box>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Button size="small" startIcon={<RefreshIcon />} onClick={() => refetchActiveConv()}>
              Refresh
            </Button>
          </Box>
        </Box>

        {actionSuccessMsg && (
          <Alert severity="success" sx={{ m: 2, mb: 0 }} onClose={() => setActionSuccessMsg(null)}>
            {actionSuccessMsg}
          </Alert>
        )}

        {/* Message Feed */}
        <Box sx={{ flexGrow: 1, overflowY: 'auto', p: 3 }}>
          {activeConvLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
              <CircularProgress />
            </Box>
          ) : !activeConvId ? (
            <Box sx={{ textAlign: 'center', py: 12 }}>
              <BotIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 1 }} />
              <Typography variant="h6" color="text.secondary">
                Select or create a Copilot session to begin.
              </Typography>
            </Box>
          ) : messages.length === 0 ? (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <BoltIcon sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
              <Typography variant="h6" fontWeight={700} gutterBottom>
                ShopAI Merchant Copilot is ready.
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Ask questions about your catalog, inspect order statuses, adjust stock, or issue refunds with human approval.
              </Typography>

              {/* Quick Prompt Suggestions */}
              <Grid container spacing={1} justifyContent="center" sx={{ maxWidth: 600, mx: 'auto' }}>
                {[
                  'Show all unfulfilled orders and tracking status',
                  'Search catalog for snowboards and check inventory levels',
                  'What is our store policy on returns and exchanges?',
                  'Check lost revenue from abandoned carts this week',
                ].map((prompt) => (
                  <Grid item xs={12} sm={6} key={prompt}>
                    <Button
                      fullWidth
                      variant="outlined"
                      size="small"
                      onClick={() => handleQuickPrompt(prompt)}
                      sx={{ textAlign: 'left', justifyContent: 'flex-start', textTransform: 'none', py: 1 }}
                    >
                      {prompt}
                    </Button>
                  </Grid>
                ))}
              </Grid>
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {messages.map((msg) => (
                <Box
                  key={msg.id}
                  sx={{
                    display: 'flex',
                    gap: 1.5,
                    alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                    maxWidth: msg.role === 'tool' ? '90%' : '80%',
                  }}
                >
                  {msg.role !== 'user' && (
                    <Box
                      sx={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        bgcolor: msg.role === 'tool' ? 'grey.200' : 'primary.light',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      {msg.role === 'tool' ? <BuildIcon fontSize="small" /> : <BotIcon fontSize="small" color="primary" />}
                    </Box>
                  )}

                  <Paper
                    elevation={msg.role === 'user' ? 2 : 1}
                    sx={{
                      p: 2,
                      borderRadius: 2.5,
                      bgcolor:
                        msg.role === 'user'
                          ? 'primary.main'
                          : msg.role === 'tool'
                          ? 'transparent'
                          : 'background.paper',
                      color: msg.role === 'user' ? '#fff' : 'text.primary',
                      border: msg.role === 'tool' ? 'none' : undefined,
                      boxShadow: msg.role === 'tool' ? 'none' : undefined,
                    }}
                  >
                    {renderMessageContent(msg)}
                    <Typography
                      variant="caption"
                      sx={{
                        display: 'block',
                        mt: 0.5,
                        textAlign: 'right',
                        opacity: 0.7,
                        fontSize: '0.7rem',
                      }}
                    >
                      {new Date(msg.createdAt).toLocaleTimeString()}
                    </Typography>
                  </Paper>

                  {msg.role === 'user' && (
                    <Box
                      sx={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        bgcolor: 'secondary.main',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#fff',
                        flexShrink: 0,
                      }}
                    >
                      <PersonIcon fontSize="small" />
                    </Box>
                  )}
                </Box>
              ))}

              {sendMessageMutation.isPending && (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mt: 1 }}>
                  <CircularProgress size={20} />
                  <Typography variant="body2" color="text.secondary">
                    Copilot is analyzing context and evaluating tools...
                  </Typography>
                </Box>
              )}
              <div ref={messagesEndRef} />
            </Box>
          )}
        </Box>

        {/* Input Bar */}
        <Box sx={{ p: 2, borderTop: '1px solid', borderColor: 'divider', bgcolor: 'background.paper' }}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField
              fullWidth
              placeholder="Ask Copilot anything about catalog, orders, stock, or store operations..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSendMessage();
                }
              }}
              disabled={!activeConvId || sendMessageMutation.isPending}
              size="small"
            />
            <Button
              variant="contained"
              endIcon={<SendIcon />}
              onClick={handleSendMessage}
              disabled={!inputMessage.trim() || !activeConvId || sendMessageMutation.isPending}
            >
              Send
            </Button>
          </Box>
        </Box>
      </Box>

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Action Request</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Please provide a rejection reason for this operation.
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Rejection Reason"
            value={rejectionReason}
            onChange={(e) => setRejectionReason(e.target.value)}
            placeholder="e.g. Order return period has expired."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={() => {
              if (selectedPendingActionId) {
                rejectActionMutation.mutate({ id: selectedPendingActionId, reason: rejectionReason });
              }
            }}
            disabled={rejectActionMutation.isPending}
          >
            Confirm Reject
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
