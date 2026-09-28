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
  Divider,
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Refresh as RefreshIcon,
  Gavel as GavelIcon,
  Warning as WarningIcon,
  Security as SecurityIcon,
  Bolt as BoltIcon,
  Code as CodeIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toolsApi, ActionApprovalDto } from '../api/tools';

export const ApprovalsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedTab, setSelectedTab] = useState<string>('PENDING');
  const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
  const [selectedAction, setSelectedAction] = useState<ActionApprovalDto | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);

  const { data: approvalsData, isLoading, refetch } = useQuery({
    queryKey: ['approvals', selectedTab],
    queryFn: () => toolsApi.getApprovals(selectedTab === 'ALL' ? undefined : selectedTab, 0, 50),
    refetchInterval: 10000,
  });

  const { data: pendingCount } = useQuery({
    queryKey: ['approvalsPendingCount'],
    queryFn: () => toolsApi.getPendingCount(),
    refetchInterval: 5000,
  });

  const approveMutation = useMutation({
    mutationFn: (id: string) => toolsApi.approveAction(id),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['approvals'] });
      queryClient.invalidateQueries({ queryKey: ['approvalsPendingCount'] });
      setActionSuccessMsg(`Action ${data.toolName} successfully approved and executed!`);
      setTimeout(() => setActionSuccessMsg(null), 5000);
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: string; reason: string }) => toolsApi.rejectAction(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['approvals'] });
      queryClient.invalidateQueries({ queryKey: ['approvalsPendingCount'] });
      setRejectDialogOpen(false);
      setRejectionReason('');
      setActionSuccessMsg('Action rejected successfully.');
      setTimeout(() => setActionSuccessMsg(null), 5000);
    },
  });

  const handleOpenReject = (action: ActionApprovalDto) => {
    setSelectedAction(action);
    setRejectionReason('');
    setRejectDialogOpen(true);
  };

  const handleOpenDetail = (action: ActionApprovalDto) => {
    setSelectedAction(action);
    setDetailDialogOpen(true);
  };

  const handleConfirmReject = () => {
    if (selectedAction) {
      rejectMutation.mutate({ id: selectedAction.id, reason: rejectionReason });
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

  const getStatusChip = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Chip label="PENDING APPROVAL" size="small" color="warning" icon={<WarningIcon />} />;
      case 'APPROVED':
      case 'EXECUTED':
        return <Chip label="EXECUTED" size="small" color="success" icon={<CheckCircleIcon />} />;
      case 'REJECTED':
        return <Chip label="REJECTED" size="small" color="error" icon={<CancelIcon />} />;
      case 'FAILED':
        return <Chip label="FAILED" size="small" color="error" />;
      default:
        return <Chip label={status} size="small" />;
    }
  };

  const actions = approvalsData?.content || [];

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <GavelIcon sx={{ fontSize: 32, color: 'primary.main' }} />
            <Typography variant="h4" fontWeight={700}>
              AI Action Approvals
            </Typography>
            {pendingCount !== undefined && pendingCount > 0 && (
              <Chip label={`${pendingCount} Pending`} color="error" size="small" sx={{ fontWeight: 700 }} />
            )}
          </Box>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Human-in-the-Loop authorization gate for sensitive AI operations (Rule 14: Dangerous actions require human approval).
          </Typography>
        </Box>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => refetch()}
        >
          Refresh Queue
        </Button>
      </Box>

      {actionSuccessMsg && (
        <Alert severity="success" sx={{ mb: 3 }} onClose={() => setActionSuccessMsg(null)}>
          {actionSuccessMsg}
        </Alert>
      )}

      {/* Tabs */}
      <Paper sx={{ mb: 3 }}>
        <Tabs
          value={selectedTab}
          onChange={(_, v) => setSelectedTab(v)}
          textColor="primary"
          indicatorColor="primary"
        >
          <Tab
            value="PENDING"
            label={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <span>Pending Approval</span>
                {pendingCount !== undefined && pendingCount > 0 && (
                  <Chip label={pendingCount} size="small" color="error" sx={{ height: 20, fontSize: '0.75rem' }} />
                )}
              </Box>
            }
          />
          <Tab value="ALL" label="All Requests" />
          <Tab value="EXECUTED" label="Executed" />
          <Tab value="REJECTED" label="Rejected" />
        </Tabs>
      </Paper>

      {/* Content */}
      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 8 }}>
          <CircularProgress />
        </Box>
      ) : actions.length === 0 ? (
        <Paper sx={{ p: 6, textAlign: 'center' }}>
          <SecurityIcon sx={{ fontSize: 64, color: 'text.disabled', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            No action requests in this category.
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            When autonomous AI agents trigger high-risk actions (e.g. refunds or inventory modifications), they will appear here awaiting your explicit approval.
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={2}>
          {actions.map((action) => (
            <Grid item xs={12} key={action.id}>
              <Card
                variant="outlined"
                sx={{
                  borderLeft: action.status === 'PENDING' ? '4px solid #ed6c02' : undefined,
                  bgcolor: action.status === 'PENDING' ? 'rgba(237, 108, 2, 0.02)' : 'inherit',
                }}
              >
                <CardContent sx={{ p: 2.5 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                      <BoltIcon color="primary" />
                      <Typography variant="h6" fontWeight={600}>
                        {action.toolName}
                      </Typography>
                      {getRiskChip(action.riskLevel)}
                      {getStatusChip(action.status)}
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {new Date(action.createdAt).toLocaleString()}
                    </Typography>
                  </Box>

                  <Grid container spacing={2} sx={{ mb: 2 }}>
                    <Grid item xs={12} md={3}>
                      <Typography variant="caption" color="text.secondary">
                        Agent / Requester:
                      </Typography>
                      <Typography variant="body2" fontWeight={500}>
                        {action.agentName} ({action.requestedBy})
                      </Typography>
                    </Grid>

                    <Grid item xs={12} md={6}>
                      <Typography variant="caption" color="text.secondary">
                        Action Parameters:
                      </Typography>
                      <Paper
                        variant="outlined"
                        sx={{
                          p: 1,
                          mt: 0.5,
                          bgcolor: 'grey.50',
                          fontFamily: 'monospace',
                          fontSize: '0.8rem',
                          maxHeight: 80,
                          overflow: 'auto',
                        }}
                      >
                        {JSON.stringify(action.inputPayload, null, 2)}
                      </Paper>
                    </Grid>

                    <Grid item xs={12} md={3} sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 1 }}>
                      {action.status === 'PENDING' ? (
                        <>
                          <Button
                            variant="contained"
                            color="success"
                            size="small"
                            startIcon={<CheckCircleIcon />}
                            disabled={approveMutation.isPending}
                            onClick={() => approveMutation.mutate(action.id)}
                          >
                            Approve & Execute
                          </Button>
                          <Button
                            variant="outlined"
                            color="error"
                            size="small"
                            startIcon={<CancelIcon />}
                            disabled={rejectMutation.isPending}
                            onClick={() => handleOpenReject(action)}
                          >
                            Reject
                          </Button>
                        </>
                      ) : (
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<CodeIcon />}
                          onClick={() => handleOpenDetail(action)}
                        >
                          View Results
                        </Button>
                      )}
                    </Grid>
                  </Grid>

                  {action.rejectionReason && (
                    <Alert severity="error" sx={{ mt: 1, py: 0.5 }}>
                      <strong>Rejection Reason:</strong> {action.rejectionReason} (by {action.approvedBy})
                    </Alert>
                  )}
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Reject Dialog */}
      <Dialog open={rejectDialogOpen} onClose={() => setRejectDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Reject Action Request</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            Provide a reason for rejecting the action <strong>{selectedAction?.toolName}</strong> requested by {selectedAction?.agentName}.
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Rejection Reason"
            value={rejectionReason}
            onChange={(e) => setRejectionReason(e.target.value)}
            placeholder="e.g. Order is past the 30-day return window or was flagged for manual review."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRejectDialogOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleConfirmReject}
            disabled={rejectMutation.isPending}
          >
            {rejectMutation.isPending ? 'Rejecting...' : 'Confirm Reject'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Detail Dialog */}
      <Dialog open={detailDialogOpen} onClose={() => setDetailDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Action Request Execution Details</DialogTitle>
        <DialogContent>
          {selectedAction && (
            <Box sx={{ mt: 1 }}>
              <Typography variant="subtitle2" gutterBottom>
                Tool: <strong>{selectedAction.toolName}</strong> ({selectedAction.riskLevel})
              </Typography>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Status: {selectedAction.status} | Resolved by: {selectedAction.approvedBy || 'N/A'}
              </Typography>
              <Divider sx={{ my: 1.5 }} />

              <Typography variant="caption" fontWeight={600}>
                Input Parameters:
              </Typography>
              <Paper variant="outlined" sx={{ p: 1.5, my: 1, bgcolor: 'grey.50', fontFamily: 'monospace', fontSize: '0.85rem' }}>
                <pre style={{ margin: 0 }}>{JSON.stringify(selectedAction.inputPayload, null, 2)}</pre>
              </Paper>

              <Typography variant="caption" fontWeight={600} sx={{ mt: 2, display: 'block' }}>
                Execution Result / Output:
              </Typography>
              <Paper variant="outlined" sx={{ p: 1.5, my: 1, bgcolor: 'grey.50', fontFamily: 'monospace', fontSize: '0.85rem' }}>
                <pre style={{ margin: 0 }}>{JSON.stringify(selectedAction.executionResult, null, 2)}</pre>
              </Paper>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
