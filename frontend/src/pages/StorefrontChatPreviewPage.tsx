import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { agentApi, ConversationDto, MessageDto } from '../api/agent';
import {
  Container,
  Grid,
  Paper,
  Typography,
  Box,
  TextField,
  Button,
  Chip,
  Avatar,
  Stack,
  CircularProgress,
  Card,
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import PersonIcon from '@mui/icons-material/Person';
import StorefrontIcon from '@mui/icons-material/Storefront';
import PsychologyIcon from '@mui/icons-material/Psychology';
import RestartAltIcon from '@mui/icons-material/RestartAlt';

export const StorefrontChatPreviewPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [customerMode, setCustomerMode] = useState<'GUEST' | 'REGISTERED'>('GUEST');
  const [customEmail, setCustomEmail] = useState<string>('');
  const [currentConversation, setCurrentConversation] = useState<ConversationDto | null>(null);
  const [messageInput, setMessageInput] = useState<string>('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const activeEmail = customerMode === 'REGISTERED' ? customEmail : '';

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Fetch or create a conversation for this session
  const createConversationMutation = useMutation({
    mutationFn: () =>
      agentApi.createConversation({
        title: `Storefront Chat - ${activeEmail || 'Guest'}`,
        agentType: 'CUSTOMER_SUPPORT',
        channel: 'STOREFRONT_WIDGET',
        customerEmail: activeEmail || undefined,
        metadata: {
          customerName: activeEmail || 'Storefront Visitor',
          simulatorMode: true,
        },
      }),
    onSuccess: (newConv) => {
      setCurrentConversation(newConv);
    },
  });

  // Fetch full conversation details when active
  const { data: conversationDetails, refetch: refetchConversation } = useQuery({
    queryKey: ['storefrontConversation', currentConversation?.id],
    queryFn: () => (currentConversation ? agentApi.getConversation(currentConversation.id) : null),
    enabled: !!currentConversation,
    refetchInterval: 5000,
  });

  const sendMessageMutation = useMutation({
    mutationFn: (message: string) => {
      if (!currentConversation) throw new Error('No active conversation');
      return agentApi.sendMessage(currentConversation.id, message, activeEmail || undefined);
    },
    onSuccess: () => {
      setMessageInput('');
      refetchConversation();
      queryClient.invalidateQueries({ queryKey: ['storefrontConversation'] });
    },
  });

  // Automatically start a new chat session when customer changes
  useEffect(() => {
    createConversationMutation.mutate();
  }, [customerMode, customEmail]);

  useEffect(() => {
    scrollToBottom();
  }, [conversationDetails?.messages, sendMessageMutation.isPending]);

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!messageInput.trim() || sendMessageMutation.isPending || !currentConversation) return;
    sendMessageMutation.mutate(messageInput.trim());
  };

  const quickPrompts = [
    'What is your store return and refund policy?',
    'What products are currently available in your catalog?',
    'What are your shipping rates and estimated delivery times?',
    'Can you help me find the best items for my needs?',
  ];

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      {/* Header Banner */}
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="h4" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <StorefrontIcon color="primary" sx={{ fontSize: 36 }} /> Storefront Customer Assistant Simulator
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Simulate the customer-facing AI widget with dynamic Customer Memory injection, Vector RAG knowledge, and tool execution.
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RestartAltIcon />}
          onClick={() => createConversationMutation.mutate()}
          disabled={createConversationMutation.isPending}
        >
          Reset Session
        </Button>
      </Box>

      <Grid container spacing={3}>
        {/* Left Side: Customer Identity & Injected Context */}
        <Grid item xs={12} md={4}>
          <Paper elevation={2} sx={{ p: 3, borderRadius: 3, mb: 3 }}>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
              <PersonIcon color="primary" /> Simulated Customer Identity
            </Typography>

            <Typography variant="caption" color="text.secondary" sx={{ mb: 1.5, display: 'block' }}>
              Test how the AI assistant responds to anonymous visitors vs registered customers:
            </Typography>

            <Stack spacing={1.5} sx={{ mb: 2.5 }}>
              <Card
                variant="outlined"
                sx={{
                  p: 1.5,
                  cursor: 'pointer',
                  borderRadius: 2,
                  borderColor: customerMode === 'GUEST' ? 'primary.main' : 'divider',
                  bgcolor: customerMode === 'GUEST' ? 'primary.50' : 'background.paper',
                  transition: '0.2s',
                  '&:hover': { borderColor: 'primary.light' },
                }}
                onClick={() => {
                  setCustomerMode('GUEST');
                  setCustomEmail('');
                }}
              >
                <Typography variant="subtitle2" fontWeight="bold">
                  Guest / Anonymous Shopper (Default)
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
                  Simulates a first-time store visitor without prior order history or saved preferences.
                </Typography>
              </Card>

              <Card
                variant="outlined"
                sx={{
                  p: 1.5,
                  cursor: 'pointer',
                  borderRadius: 2,
                  borderColor: customerMode === 'REGISTERED' ? 'primary.main' : 'divider',
                  bgcolor: customerMode === 'REGISTERED' ? 'primary.50' : 'background.paper',
                  transition: '0.2s',
                  '&:hover': { borderColor: 'primary.light' },
                }}
                onClick={() => setCustomerMode('REGISTERED')}
              >
                <Typography variant="subtitle2" fontWeight="bold">
                  Registered Store Customer
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: customerMode === 'REGISTERED' ? 1.5 : 0 }}>
                  Injects customer memory, order history, and relationship graph context.
                </Typography>

                {customerMode === 'REGISTERED' && (
                  <TextField
                    size="small"
                    fullWidth
                    label="Customer Email"
                    placeholder="e.g. buyer@example.com"
                    value={customEmail}
                    onChange={(e) => setCustomEmail(e.target.value)}
                    onClick={(e) => e.stopPropagation()}
                    helperText="Enter any customer email associated with synced orders"
                  />
                )}
              </Card>
            </Stack>
          </Paper>

          {/* Active AI Context Card */}
          <Paper elevation={2} sx={{ p: 3, borderRadius: 3, bgcolor: '#fbfcfd' }}>
            <Typography variant="subtitle2" fontWeight="bold" sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1.5 }}>
              <PsychologyIcon color="secondary" /> Active Dynamic Prompt Context
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1.5 }}>
              The Agent Runtime automatically queries customer facts and vector embeddings:
            </Typography>

            <Box sx={{ bgcolor: 'background.paper', p: 2, borderRadius: 2, border: '1px solid #e0e0e0' }}>
              <Typography variant="caption" fontWeight="bold" color="primary.main" sx={{ display: 'block' }}>
                CUSTOMER MEMORY:
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                {activeEmail ? `Email: ${activeEmail}` : 'Guest mode (No customer facts loaded)'}
              </Typography>

              <Typography variant="caption" fontWeight="bold" color="secondary.main" sx={{ display: 'block' }}>
                KNOWLEDGE RETRIEVAL:
              </Typography>
              <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                Top-3 pgvector HNSW chunks dynamically retrieved on each user turn.
              </Typography>

              <Typography variant="caption" fontWeight="bold" color="warning.main" sx={{ display: 'block' }}>
                TOOL ACCESS:
              </Typography>
              <Typography variant="caption" color="text.secondary">
                search_products, check_inventory, get_order_status, query_policies
              </Typography>
            </Box>
          </Paper>
        </Grid>

        {/* Right Side: Interactive Storefront Chat Interface */}
        <Grid item xs={12} md={8}>
          <Paper
            elevation={3}
            sx={{
              height: '75vh',
              display: 'flex',
              flexDirection: 'column',
              borderRadius: 3,
              overflow: 'hidden',
              bgcolor: 'background.paper',
            }}
          >
            {/* Chat Top Bar */}
            <Box
              sx={{
                p: 2,
                px: 3,
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                <Avatar sx={{ bgcolor: 'white', color: 'primary.main' }}>
                  <SmartToyIcon />
                </Avatar>
                <Box>
                  <Typography variant="subtitle1" fontWeight="bold">
                    ShopAI Storefront Assistant
                  </Typography>
                  <Typography variant="caption" sx={{ opacity: 0.9 }}>
                    Active Persona: Customer Support & Commerce Specialist
                  </Typography>
                </Box>
              </Box>

              <Chip
                label={activeEmail ? `Logged in: ${activeEmail}` : 'Guest Shopper'}
                size="small"
                sx={{ bgcolor: 'rgba(255,255,255,0.2)', color: 'white' }}
              />
            </Box>

            {/* Messages Area */}
            <Box
              sx={{
                flexGrow: 1,
                overflowY: 'auto',
                p: 3,
                display: 'flex',
                flexDirection: 'column',
                gap: 2,
                bgcolor: '#f8f9fa',
              }}
            >
              {/* Initial Welcome Greeting */}
              <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', maxWidth: '80%' }}>
                <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                  <SmartToyIcon fontSize="small" />
                </Avatar>
                <Paper elevation={1} sx={{ p: 2, borderRadius: '4px 16px 16px 16px', bgcolor: 'white' }}>
                  <Typography variant="body2">
                    👋 Hi there! I'm your ShopAI virtual shopping assistant. How can I help you today? Ask me about product recommendations, check order status, or store policies!
                  </Typography>
                </Paper>
              </Box>

              {/* Render Messages */}
              {conversationDetails?.messages?.map((msg: MessageDto) => {
                const isUser = msg.role === 'user' || (msg.role as string) === 'USER';
                const hasTools = !!msg.toolName || (msg.toolCalls && Object.keys(msg.toolCalls).length > 0);

                return (
                  <Box
                    key={msg.id}
                    sx={{
                      display: 'flex',
                      justifyContent: isUser ? 'flex-end' : 'flex-start',
                      gap: 1.5,
                      alignItems: 'flex-start',
                    }}
                  >
                    {!isUser && (
                      <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                        <SmartToyIcon fontSize="small" />
                      </Avatar>
                    )}

                    <Box sx={{ maxWidth: '80%' }}>
                      <Paper
                        elevation={isUser ? 2 : 1}
                        sx={{
                          p: 2,
                          borderRadius: isUser ? '16px 4px 16px 16px' : '4px 16px 16px 16px',
                          bgcolor: isUser ? 'primary.main' : 'white',
                          color: isUser ? 'primary.contrastText' : 'text.primary',
                        }}
                      >
                        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', lineHeight: 1.5 }}>
                          {msg.content}
                        </Typography>

                        {/* Tool Executions Badge */}
                        {hasTools && (
                          <Box sx={{ mt: 1.5, pt: 1, borderTop: '1px dashed #e0e0e0' }}>
                            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 0.5 }}>
                              ⚡ Tool Context:
                            </Typography>
                            <Stack direction="row" spacing={1} flexWrap="wrap">
                              <Chip
                                label={msg.toolName || 'Tool Execution'}
                                size="small"
                                color="success"
                                variant="outlined"
                                sx={{ fontSize: '0.7rem' }}
                              />
                            </Stack>
                          </Box>
                        )}
                      </Paper>

                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{
                          display: 'block',
                          mt: 0.5,
                          textAlign: isUser ? 'right' : 'left',
                          fontSize: '0.7rem',
                        }}
                      >
                        {new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </Typography>
                    </Box>

                    {isUser && (
                      <Avatar sx={{ bgcolor: 'secondary.main', width: 36, height: 36 }}>
                        <PersonIcon fontSize="small" />
                      </Avatar>
                    )}
                  </Box>
                );
              })}

              {/* Loading indicator during generation */}
              {sendMessageMutation.isPending && (
                <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center' }}>
                  <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                    <SmartToyIcon fontSize="small" />
                  </Avatar>
                  <Paper elevation={1} sx={{ p: 2, borderRadius: '4px 16px 16px 16px', bgcolor: 'white', display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <CircularProgress size={18} />
                    <Typography variant="body2" color="text.secondary">
                      Thinking and consulting store catalog & knowledge base...
                    </Typography>
                  </Paper>
                </Box>
              )}

              <div ref={messagesEndRef} />
            </Box>

            {/* Quick Prompt Suggestions */}
            <Box sx={{ p: 1.5, bgcolor: 'background.paper', borderTop: '1px solid #f0f0f0', overflowX: 'auto' }}>
              <Stack direction="row" spacing={1}>
                {quickPrompts.map((prompt) => (
                  <Chip
                    key={prompt}
                    label={prompt}
                    size="small"
                    onClick={() => {
                      if (!sendMessageMutation.isPending && currentConversation) {
                        sendMessageMutation.mutate(prompt);
                      }
                    }}
                    sx={{
                      cursor: 'pointer',
                      bgcolor: '#f5f5f5',
                      '&:hover': { bgcolor: 'primary.50', color: 'primary.main' },
                    }}
                  />
                ))}
              </Stack>
            </Box>

            {/* Message Input Bar */}
            <Box
              component="form"
              onSubmit={handleSend}
              sx={{
                p: 2,
                bgcolor: 'background.paper',
                borderTop: '1px solid #e0e0e0',
                display: 'flex',
                gap: 1.5,
              }}
            >
              <TextField
                fullWidth
                size="small"
                placeholder="Ask about products, orders, shipping, or returns..."
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                disabled={sendMessageMutation.isPending || !currentConversation}
              />
              <Button
                type="submit"
                variant="contained"
                endIcon={<SendIcon />}
                disabled={!messageInput.trim() || sendMessageMutation.isPending || !currentConversation}
              >
                Send
              </Button>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
};
