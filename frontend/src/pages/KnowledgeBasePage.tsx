import React, { useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  Tabs,
  Tab,
  TextField,
  Button,
  Chip,
  IconButton,
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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Slider,
  CircularProgress,
  Alert,
  Tooltip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Psychology as PsychologyIcon,
  Storage as StorageIcon,
  MenuBook as MenuBookIcon,
  Person as PersonIcon,
  ExpandMore as ExpandMoreIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ragApi, SearchRequest, CreateDocumentRequest, CreateCustomerMemoryRequest } from '../api/rag';

export const KnowledgeBasePage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState(0);

  // Search Playground State
  const [searchQuery, setSearchQuery] = useState('');
  const [searchCustomerEmail, setSearchCustomerEmail] = useState('');
  const [searchDocType, setSearchDocType] = useState('ALL');
  const [searchMinScore, setSearchMinScore] = useState<number>(0.4);
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Create Document Dialog State
  const [docDialogOpen, setDocDialogOpen] = useState(false);
  const [newDocTitle, setNewDocTitle] = useState('');
  const [newDocType, setNewDocType] = useState('POLICY');
  const [newDocContent, setNewDocContent] = useState('');
  const [newDocEmail, setNewDocEmail] = useState('');

  // Customer Memory State
  const [memoryEmail, setMemoryEmail] = useState('');
  const [searchedMemoryEmail, setSearchedMemoryEmail] = useState('');
  const [memoryDialogOpen, setMemoryDialogOpen] = useState(false);
  const [newMemCategory, setNewMemCategory] = useState('PREFERENCE');
  const [newMemKey, setNewMemKey] = useState('');
  const [newMemValue, setNewMemValue] = useState('');
  const [newMemConfidence, setNewMemConfidence] = useState<number>(0.95);

  // Notification Toast
  const [actionMessage, setActionMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  // Queries
  const { data: stats, isLoading: statsLoading, refetch: refetchStats } = useQuery({
    queryKey: ['ragStats'],
    queryFn: ragApi.getStats,
  });

  const { data: documentsData, isLoading: docsLoading, refetch: refetchDocs } = useQuery({
    queryKey: ['ragDocuments'],
    queryFn: () => ragApi.getDocuments({ size: 50 }),
  });

  const { data: memoriesData, isLoading: memoriesLoading, refetch: refetchMemories } = useQuery({
    queryKey: ['customerMemories', searchedMemoryEmail],
    queryFn: () => ragApi.getCustomerMemories(searchedMemoryEmail),
    enabled: !!searchedMemoryEmail,
  });

  // Re-index Mutation
  const reindexMutation = useMutation({
    mutationFn: ragApi.triggerReindex,
    onSuccess: (data) => {
      setActionMessage({ type: 'success', text: data.message || 'Vector re-indexing started in background!' });
      queryClient.invalidateQueries({ queryKey: ['ragStats'] });
      queryClient.invalidateQueries({ queryKey: ['ragDocuments'] });
    },
    onError: (err: any) => {
      setActionMessage({ type: 'error', text: err.response?.data?.message || 'Failed to trigger re-indexing' });
    },
  });

  // Create Document Mutation
  const createDocMutation = useMutation({
    mutationFn: (doc: CreateDocumentRequest) => ragApi.createDocument(doc),
    onSuccess: () => {
      setActionMessage({ type: 'success', text: 'Document indexed successfully into vector database!' });
      setDocDialogOpen(false);
      setNewDocTitle('');
      setNewDocContent('');
      setNewDocEmail('');
      refetchDocs();
      refetchStats();
    },
    onError: (err: any) => {
      setActionMessage({ type: 'error', text: err.response?.data?.message || 'Failed to index document' });
    },
  });

  // Delete Document Mutation
  const deleteDocMutation = useMutation({
    mutationFn: (id: string) => ragApi.deleteDocument(id),
    onSuccess: () => {
      setActionMessage({ type: 'success', text: 'Knowledge document removed.' });
      refetchDocs();
      refetchStats();
    },
    onError: (err: any) => {
      setActionMessage({ type: 'error', text: err.response?.data?.message || 'Failed to delete document' });
    },
  });

  // Create Customer Memory Mutation
  const createMemoryMutation = useMutation({
    mutationFn: (mem: CreateCustomerMemoryRequest) => ragApi.createCustomerMemory(mem),
    onSuccess: () => {
      setActionMessage({ type: 'success', text: 'Customer memory fact recorded and vectorized!' });
      setMemoryDialogOpen(false);
      setNewMemKey('');
      setNewMemValue('');
      if (searchedMemoryEmail) refetchMemories();
      refetchStats();
    },
    onError: (err: any) => {
      setActionMessage({ type: 'error', text: err.response?.data?.message || 'Failed to save customer memory' });
    },
  });

  // Delete Memory Mutation
  const deleteMemoryMutation = useMutation({
    mutationFn: (id: string) => ragApi.deleteCustomerMemory(id),
    onSuccess: () => {
      setActionMessage({ type: 'success', text: 'Customer memory removed.' });
      if (searchedMemoryEmail) refetchMemories();
      refetchStats();
    },
  });

  // Handle Search Execution
  const handleExecuteSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    setSearchError(null);
    try {
      const req: SearchRequest = {
        query: searchQuery.trim(),
        customerEmail: searchCustomerEmail.trim() || undefined,
        documentType: searchDocType === 'ALL' ? undefined : searchDocType,
        minScore: searchMinScore,
        limit: 10,
      };
      const res = await ragApi.search(req);
      setSearchResults(res);
    } catch (err: any) {
      setSearchError(err.response?.data?.message || 'Search execution failed');
    } finally {
      setIsSearching(false);
    }
  };

  const handleCustomerMemoryLookup = (e: React.FormEvent) => {
    e.preventDefault();
    setSearchedMemoryEmail(memoryEmail.trim());
  };
  return (
    <Box sx={{ p: { xs: 2, md: 4 } }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700, display: 'flex', alignItems: 'center', gap: 1 }}>
            <PsychologyIcon color="primary" fontSize="large" />
            Vector Knowledge Base & Memory
          </Typography>
          <Typography variant="body2" color="text.secondary">
            PostgreSQL 16 pgvector HNSW indexing, multi-provider embeddings, and customer personalization memory.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              refetchStats();
              refetchDocs();
              if (searchedMemoryEmail) refetchMemories();
            }}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            color="secondary"
            startIcon={reindexMutation.isPending ? <CircularProgress size={18} color="inherit" /> : <StorageIcon />}
            disabled={reindexMutation.isPending}
            onClick={() => reindexMutation.mutate()}
          >
            {reindexMutation.isPending ? 'Re-indexing...' : 'Re-index Knowledge'}
          </Button>
        </Box>
      </Box>

      {/* Action Notification */}
      {actionMessage && (
        <Alert
          severity={actionMessage.type}
          onClose={() => setActionMessage(null)}
          sx={{ mb: 3 }}
        >
          {actionMessage.text}
        </Alert>
      )}

      {/* Stats Summary Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                Indexed Documents
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main', my: 0.5 }}>
                {statsLoading ? <CircularProgress size={24} /> : stats?.totalDocuments ?? 0}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {stats?.totalChunks ?? 0} vectorized text chunks
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                Customer Memories
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main', my: 0.5 }}>
                {statsLoading ? <CircularProgress size={24} /> : stats?.totalCustomerMemories ?? 0}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Personalized context facts
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                Vector Index Status
              </Typography>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, my: 0.5 }}>
                <CheckCircleIcon color="success" fontSize="small" />
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  HNSW Cosine Active
                </Typography>
              </Box>
              <Typography variant="caption" color="text.secondary">
                {stats?.vectorDimensions ?? 1536} dimensions (PostgreSQL 16)
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="overline" color="text.secondary" fontWeight={600}>
                Active Embedding Engine
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 700, color: 'info.main', my: 0.5 }}>
                {stats?.activeEmbeddingProvider?.toUpperCase() ?? 'LOCAL'}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {stats?.activeEmbeddingModel ?? 'text-embedding-3-small'}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Paper elevation={2} sx={{ borderRadius: 2, overflow: 'hidden' }}>
        <Tabs
          value={activeTab}
          onChange={(_, v) => setActiveTab(v)}
          indicatorColor="primary"
          textColor="primary"
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', px: 2, pt: 1 }}
        >
          <Tab icon={<SearchIcon />} iconPosition="start" label="Semantic Search Playground" />
          <Tab icon={<MenuBookIcon />} iconPosition="start" label="Store Knowledge Documents" />
          <Tab icon={<PersonIcon />} iconPosition="start" label="Customer Memory Inspector" />
        </Tabs>
        {/* TAB 0: Semantic Search Playground */}
        {activeTab === 0 && (
          <Box sx={{ p: 3 }}>
            <Typography variant="h6" fontWeight={600} gutterBottom>
              Vector Similarity Search & Customer Context Retrieval
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              Test real-time pgvector cosine similarity matching across products, order histories, abandoned carts, policies, and customer facts.
            </Typography>

            <Box component="form" onSubmit={handleExecuteSearch} sx={{ mb: 4 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} md={5}>
                  <TextField
                    fullWidth
                    label="Semantic Search Query"
                    placeholder="e.g. warm waterproof winter jacket or return policy"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    required
                  />
                </Grid>

                <Grid item xs={12} sm={6} md={3}>
                  <TextField
                    fullWidth
                    label="Customer Email (Scope Context)"
                    placeholder="e.g. alice@example.com (optional)"
                    value={searchCustomerEmail}
                    onChange={(e) => setSearchCustomerEmail(e.target.value)}
                    helperText="Isolates private customer orders/carts"
                  />
                </Grid>

                <Grid item xs={12} sm={6} md={2}>
                  <FormControl fullWidth>
                    <InputLabel>Document Type</InputLabel>
                    <Select
                      value={searchDocType}
                      label="Document Type"
                      onChange={(e) => setSearchDocType(e.target.value)}
                    >
                      <MenuItem value="ALL">All Documents</MenuItem>
                      <MenuItem value="PRODUCT">Products</MenuItem>
                      <MenuItem value="ORDER_HISTORY">Orders & Tracking</MenuItem>
                      <MenuItem value="ABANDONED_CHECKOUT">Abandoned Carts</MenuItem>
                      <MenuItem value="POLICY">Store Policies</MenuItem>
                      <MenuItem value="FAQ">FAQs</MenuItem>
                      <MenuItem value="CUSTOM">Custom Notes</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} md={2} sx={{ display: 'flex', alignItems: 'center' }}>
                  <Button
                    fullWidth
                    size="large"
                    variant="contained"
                    type="submit"
                    disabled={isSearching}
                    startIcon={isSearching ? <CircularProgress size={20} color="inherit" /> : <SearchIcon />}
                    sx={{ height: 56 }}
                  >
                    {isSearching ? 'Searching...' : 'Search'}
                  </Button>
                </Grid>

                <Grid item xs={12} md={6}>
                  <Typography variant="caption" color="text.secondary" gutterBottom>
                    Cosine Similarity Threshold: {(searchMinScore * 100).toFixed(0)}%
                  </Typography>
                  <Slider
                    value={searchMinScore}
                    min={0.1}
                    max={0.95}
                    step={0.05}
                    onChange={(_, val) => setSearchMinScore(val as number)}
                    valueLabelDisplay="auto"
                    valueLabelFormat={(v) => `${(v * 100).toFixed(0)}%`}
                  />
                </Grid>
              </Grid>
            </Box>

            {searchError && (
              <Alert severity="error" sx={{ mb: 3 }}>
                {searchError}
              </Alert>
            )}

            {/* Results */}
            <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
              Matches ({searchResults.length})
            </Typography>

            {searchResults.length === 0 && !isSearching && (
              <Box sx={{ p: 4, textAlign: 'center', bgcolor: 'action.hover', borderRadius: 2 }}>
                <Typography color="text.secondary">
                  No matching chunks found. Try lowering the similarity threshold or entering a different query.
                </Typography>
              </Box>
            )}

            <Grid container spacing={2}>
              {searchResults.map((item, idx) => (
                <Grid item xs={12} key={item.id || idx}>
                  <Card variant="outlined" sx={{ borderRadius: 2 }}>
                    <CardContent>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1, flexWrap: 'wrap', gap: 1 }}>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle1" fontWeight={700}>
                            {item.documentTitle}
                          </Typography>
                          <Chip
                            label={item.documentType}
                            size="small"
                            color={
                              item.documentType === 'PRODUCT' ? 'primary' :
                              item.documentType === 'ORDER_HISTORY' ? 'secondary' :
                              item.documentType === 'ABANDONED_CHECKOUT' ? 'warning' : 'default'
                            }
                          />
                          {item.customerEmail && (
                            <Chip label={`Customer: ${item.customerEmail}`} size="small" variant="outlined" />
                          )}
                        </Box>
                        <Chip
                          label={`Match: ${(item.similarityScore * 100).toFixed(1)}%`}
                          color={item.similarityScore > 0.75 ? 'success' : item.similarityScore > 0.5 ? 'info' : 'default'}
                          size="small"
                          sx={{ fontWeight: 700 }}
                        />
                      </Box>

                      <Paper sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1, mb: 1.5, whiteSpace: 'pre-wrap', fontFamily: 'inherit' }}>
                        <Typography variant="body2">
                          {item.content}
                        </Typography>
                      </Paper>

                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="caption" color="text.secondary">
                          Tokens: {item.tokenCount} | Chunk ID: {item.id?.substring(0, 8)}...
                        </Typography>
                        {item.metadata && Object.keys(item.metadata).length > 0 && (
                          <Accordion disableGutters elevation={0} sx={{ bgcolor: 'transparent', '&:before': { display: 'none' } }}>
                            <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ p: 0, minHeight: 0 }}>
                              <Typography variant="caption" color="primary">View Chunk Metadata</Typography>
                            </AccordionSummary>
                            <AccordionDetails sx={{ p: 1, bgcolor: 'background.paper', borderRadius: 1, border: 1, borderColor: 'divider' }}>
                              <pre style={{ margin: 0, fontSize: 11 }}>{JSON.stringify(item.metadata, null, 2)}</pre>
                            </AccordionDetails>
                          </Accordion>
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          </Box>
        )}
        {/* TAB 1: Store Knowledge Documents */}
        {activeTab === 1 && (
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
              <Typography variant="h6" fontWeight={600}>
                Store Knowledge Documents ({documentsData?.totalElements ?? 0})
              </Typography>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setDocDialogOpen(true)}
              >
                Add Knowledge Doc
              </Button>
            </Box>

            {docsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
            ) : (
              <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                <Table>
                  <TableHead>
                    <TableRow sx={{ bgcolor: 'action.hover' }}>
                      <TableCell sx={{ fontWeight: 700 }}>Title</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Type</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Customer Scope</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Chunks</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>Updated</TableCell>
                      <TableCell align="right" sx={{ fontWeight: 700 }}>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {documentsData?.content?.map((doc) => (
                      <TableRow key={doc.id} hover>
                        <TableCell>
                          <Typography variant="body2" fontWeight={600}>{doc.title}</Typography>
                          <Typography variant="caption" color="text.secondary" sx={{ display: 'block', maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                            {doc.content}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip label={doc.documentType} size="small" />
                        </TableCell>
                        <TableCell>
                          {doc.customerEmail ? <Chip label={doc.customerEmail} size="small" variant="outlined" /> : <Typography variant="caption" color="text.secondary">Global / Store</Typography>}
                        </TableCell>
                        <TableCell>{doc.chunkCount ?? 1}</TableCell>
                        <TableCell>
                          <Chip label={doc.status} size="small" color={doc.status === 'INDEXED' ? 'success' : 'warning'} />
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption">{new Date(doc.updatedAt).toLocaleString()}</Typography>
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="Delete Document">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => {
                                if (window.confirm(`Delete document "${doc.title}"?`)) {
                                  deleteDocMutation.mutate(doc.id);
                                }
                              }}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                    {(!documentsData?.content || documentsData.content.length === 0) && (
                      <TableRow>
                        <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                          <Typography color="text.secondary">No documents indexed yet. Click "Re-index Knowledge" to ingest catalog and orders.</Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        )}

        {/* TAB 2: Customer Memory Inspector */}
        {activeTab === 2 && (
          <Box sx={{ p: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 3, flexWrap: 'wrap', gap: 2 }}>
              <Box>
                <Typography variant="h6" fontWeight={600}>
                  Customer Context & Learned Memory Facts
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Inspect and manage dynamic personalized facts (sizes, preferences, notes, dietary constraints) stored for each customer.
                </Typography>
              </Box>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setMemoryDialogOpen(true)}
              >
                Add Customer Fact
              </Button>
            </Box>

            <Box component="form" onSubmit={handleCustomerMemoryLookup} sx={{ display: 'flex', gap: 2, mb: 4, maxWidth: 600 }}>
              <TextField
                fullWidth
                label="Search Customer Email"
                placeholder="e.g. customer@example.com"
                value={memoryEmail}
                onChange={(e) => setMemoryEmail(e.target.value)}
                required
              />
              <Button variant="contained" type="submit" startIcon={<SearchIcon />} sx={{ px: 3 }}>
                Lookup
              </Button>
            </Box>

            {searchedMemoryEmail && (
              <Box>
                <Typography variant="subtitle1" fontWeight={700} sx={{ mb: 2 }}>
                  Memories for: <Chip label={searchedMemoryEmail} color="primary" />
                </Typography>

                {memoriesLoading ? (
                  <CircularProgress />
                ) : memoriesData && memoriesData.length > 0 ? (
                  <Grid container spacing={2}>
                    {memoriesData.map((mem) => (
                      <Grid item xs={12} sm={6} md={4} key={mem.id}>
                        <Card variant="outlined" sx={{ borderRadius: 2 }}>
                          <CardContent>
                            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                              <Chip label={mem.category} size="small" color="secondary" />
                              <IconButton
                                size="small"
                                color="error"
                                onClick={() => deleteMemoryMutation.mutate(mem.id)}
                              >
                                <DeleteIcon fontSize="small" />
                              </IconButton>
                            </Box>
                            <Typography variant="subtitle2" fontWeight={700} color="text.secondary">
                              {mem.memoryKey}
                            </Typography>
                            <Typography variant="body1" sx={{ my: 1, fontWeight: 500 }}>
                              {mem.memoryValue}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Confidence: {((mem.confidenceScore ?? 1.0) * 100).toFixed(0)}% | Recorded: {new Date(mem.createdAt).toLocaleDateString()}
                            </Typography>
                          </CardContent>
                        </Card>
                      </Grid>
                    ))}
                  </Grid>
                ) : (
                  <Alert severity="info">No personalized memory facts recorded for this customer yet.</Alert>
                )}
              </Box>
            )}
          </Box>
        )}
      </Paper>
      {/* Add Document Dialog */}
      <Dialog open={docDialogOpen} onClose={() => setDocDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add Knowledge Document</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <TextField
            label="Document Title"
            fullWidth
            value={newDocTitle}
            onChange={(e) => setNewDocTitle(e.target.value)}
            required
          />
          <FormControl fullWidth>
            <InputLabel>Document Type</InputLabel>
            <Select
              value={newDocType}
              label="Document Type"
              onChange={(e) => setNewDocType(e.target.value)}
            >
              <MenuItem value="POLICY">Store Policy (Returns, Shipping, Privacy)</MenuItem>
              <MenuItem value="FAQ">Frequently Asked Question</MenuItem>
              <MenuItem value="CUSTOM">Custom Store Note / Guide</MenuItem>
            </Select>
          </FormControl>
          <TextField
            label="Customer Email (Optional - leave empty for store-wide)"
            fullWidth
            value={newDocEmail}
            onChange={(e) => setNewDocEmail(e.target.value)}
          />
          <TextField
            label="Document Content"
            fullWidth
            multiline
            rows={5}
            value={newDocContent}
            onChange={(e) => setNewDocContent(e.target.value)}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDocDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!newDocTitle.trim() || !newDocContent.trim() || createDocMutation.isPending}
            onClick={() => createDocMutation.mutate({
              title: newDocTitle.trim(),
              content: newDocContent.trim(),
              documentType: newDocType,
              customerEmail: newDocEmail.trim() || undefined,
            })}
          >
            {createDocMutation.isPending ? 'Indexing...' : 'Save & Index'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Add Memory Dialog */}
      <Dialog open={memoryDialogOpen} onClose={() => setMemoryDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Record Customer Memory Fact</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
          <TextField
            label="Customer Email"
            fullWidth
            value={searchedMemoryEmail || memoryEmail}
            onChange={(e) => setMemoryEmail(e.target.value)}
            required
          />
          <FormControl fullWidth>
            <InputLabel>Memory Category</InputLabel>
            <Select
              value={newMemCategory}
              label="Memory Category"
              onChange={(e) => setNewMemCategory(e.target.value)}
            >
              <MenuItem value="PREFERENCE">Preference (Size, Color, Material)</MenuItem>
              <MenuItem value="FEEDBACK">Customer Feedback / Sentiments</MenuItem>
              <MenuItem value="NOTE">Agent / Merchant Note</MenuItem>
              <MenuItem value="ORDER_ISSUE">Special Order Request / Note</MenuItem>
            </Select>
          </FormControl>
          <TextField
            label="Memory Key / Topic"
            placeholder="e.g. shoe_size, preferred_color, vegan_materials"
            fullWidth
            value={newMemKey}
            onChange={(e) => setNewMemKey(e.target.value)}
            required
          />
          <TextField
            label="Memory Value / Fact"
            placeholder="e.g. Customer wears size 10 US and prefers black or navy"
            fullWidth
            multiline
            rows={2}
            value={newMemValue}
            onChange={(e) => setNewMemValue(e.target.value)}
            required
          />
          <Box>
            <Typography variant="caption" color="text.secondary">
              Confidence Score: {(newMemConfidence * 100).toFixed(0)}%
            </Typography>
            <Slider
              value={newMemConfidence}
              min={0.5}
              max={1.0}
              step={0.05}
              onChange={(_, v) => setNewMemConfidence(v as number)}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setMemoryDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!newMemKey.trim() || !newMemValue.trim() || createMemoryMutation.isPending}
            onClick={() => createMemoryMutation.mutate({
              customerEmail: (searchedMemoryEmail || memoryEmail).trim(),
              category: newMemCategory,
              key: newMemKey.trim(),
              value: newMemValue.trim(),
              confidenceScore: newMemConfidence,
            })}
          >
            {createMemoryMutation.isPending ? 'Saving...' : 'Record Memory'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};