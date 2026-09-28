# ShopAI Agent Architecture

> Implementation: Phases 8-10

## Agent Lifecycle

```
User Message
    |
Conversation Engine (session, history, context window management)
    |
Intent Detection
    |
Context Planning
    |        |            |
Graph RAG  Vector RAG  Tool Selection
    |        |            |
         Context Fusion
              |
         LLM (via LLMProvider)
              |
         Tool Execution (if needed, via ToolEngine)
              |
         Approval Gate (if high/critical risk action)
              |
         Response Generation
              |
         Audit Log
```

## Core Entities

- `Agent` — Configuration, persona, tools, LLM settings
- `AgentVersion` — Versioned snapshots of agent configuration
- `AgentTool` — Which tools are granted to an agent
- `AgentExecution` — A single execution run
- `AgentContext` — The assembled context for a run
- `AgentMemory` — Persistent memory across conversations

## Tool Registry

The `ToolRegistry` maintains metadata for every available tool:

| Field | Description |
|---|---|
| name | Unique tool identifier |
| description | What the tool does |
| version | Tool version |
| permission | Required permission string |
| riskLevel | LOW / MEDIUM / HIGH / CRITICAL |
| requiresApproval | Whether human approval is needed |
| inputSchema | JSON Schema for tool input |
| outputSchema | JSON Schema for tool output |

## Action Approval Flow

```
Agent proposes action
    |
Risk Evaluator
    |
 LOW/MEDIUM? ─── Execute immediately ─── Audit
    |
HIGH/CRITICAL? ─── Create ActionRequest ─── Notify Admin
                         |
                  Admin approves/rejects
                         |
                      Execute ─── Audit
```

## Risk Levels

| Tool | Risk Level |
|---|---|
| get_product | LOW |
| search_products | LOW |
| get_order_status | LOW |
| update_product | MEDIUM |
| update_inventory | MEDIUM |
| bulk_update | HIGH |
| refund_order | HIGH |
| delete_product | CRITICAL |

## Customer Agent Tools

search_products, get_product, get_product_variant, get_customer,
get_order, get_order_status, get_fulfillment, get_tracking,
get_shipping_policy, get_return_policy, create_cart, add_cart_item,
update_cart, get_checkout_url

## Admin Agent Tools

search_products, create_product, update_product, get_inventory,
update_inventory, search_orders, get_order, update_order,
search_customers, get_customer, generate_report, analyze_sales,
analyze_inventory, analyze_products

## Memory Levels

1. Conversation memory — current session
2. Customer context — cross-session customer data
3. Agent memory — agent-level persistent knowledge
4. Long-term business knowledge — shared across agents

## AI Safety Rules

- LLM cannot execute arbitrary SQL.
- LLM cannot execute arbitrary code.
- LLM cannot access environment variables or secrets.
- LLM cannot access Shopify credentials.
- LLM can REQUEST tool execution.
- The application decides if the tool is authorized to execute.
- All tool executions are recorded in audit_logs.