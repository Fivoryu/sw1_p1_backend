package com.banco.workflow.service;

import com.banco.workflow.model.BpmnNode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WorkflowGeneratorService {

    /**
     * Genera código TypeScript a partir de nodos BPMN parseados
     */
    public String generateWorkflowCode(String workflowName, Map<String, BpmnNode> nodes) {
        StringBuilder code = new StringBuilder();

        // Header
        code.append("import { proxyActivities, defineSignal, defineQuery } from '@temporalio/workflow';\n");
        code.append("import * as activities from './activities';\n");
        code.append("\n");

        // Export interface
        code.append("export interface ").append(toPascalCase(workflowName)).append("Input {\n");
        code.append("  [key: string]: any;\n");
        code.append("}\n\n");

        code.append("export interface ").append(toPascalCase(workflowName)).append("Output {\n");
        code.append("  status: string;\n");
        code.append("  data?: any;\n");
        code.append("}\n\n");

        // Workflow function
        code.append("const act = proxyActivities<typeof activities>({\n");
        code.append("  startToCloseTimeout: '1 minute',\n");
        code.append("  scheduleToCloseTimeout: '5 minutes',\n");
        code.append("  retry: { initialInterval: '1s', maximumAttempts: 3 },\n");
        code.append("});\n\n");

        code.append("export async function ").append(toCamelCase(workflowName)).append("(\n");
        code.append("  input: ").append(toPascalCase(workflowName)).append("Input\n");
        code.append("): Promise<").append(toPascalCase(workflowName)).append("Output> {\n\n");

        code.append("  const variables = { ...input };\n");
        code.append("  let currentNodeId = '").append(findStartNode(nodes)).append("';\n\n");

        code.append("  // Main workflow loop\n");
        code.append("  while (currentNodeId) {\n");
        code.append("    const node = nodes['");
        code.append(workflowName).append("'][currentNodeId];\n\n");

        code.append("    if (!node) break;\n\n");

        code.append("    switch (node.type) {\n");
        code.append("      case 'UserTask':\n");
        code.append("        const result = await act.executeTask(currentNodeId, node, variables);\n");
        code.append("        variables[currentNodeId + '_result'] = result;\n");
        code.append("        Object.assign(variables, result);\n");
        code.append("        currentNodeId = determineNextNode(node, variables);\n");
        code.append("        break;\n\n");

        code.append("      case 'ExclusiveGateway':\n");
        code.append("        currentNodeId = evaluateGateway(node, variables);\n");
        code.append("        break;\n\n");

        code.append("      case 'ParallelGateway':\n");
        code.append("        // Ejecutar tareas en paralelo\n");
        code.append("""
                        const parallelTasks = node.outgoingTransitions.map(t => act.executeTask(t.toNodeId, nodes[t.toNodeId], variables));
                        const parallelResults = await Promise.all(parallelTasks);
                        """);
        code.append("        currentNodeId = findJoinNode(node);\n");
        code.append("        break;\n\n");

        code.append("      case 'EndEvent':\n");
        code.append("        return {\n");
        code.append("          status: 'COMPLETED',\n");
        code.append("          data: variables\n");
        code.append("        };\n\n");

        code.append("      default:\n");
        code.append("        currentNodeId = node.outgoingTransitions?.[0]?.toNodeId;\n");
        code.append("    }\n");
        code.append("  }\n\n");

        code.append("  return {\n");
        code.append("    status: 'COMPLETED',\n");
        code.append("    data: variables\n");
        code.append("  };\n");
        code.append("}\n\n");

        // Helper functions
        code.append(generateHelperFunctions());

        return code.toString();
    }

    private String generateHelperFunctions() {
        return """
                function determineNextNode(node: any, variables: any): string | null {
                  if (!node.outgoingTransitions || node.outgoingTransitions.length === 0) return null;
                  if (node.outgoingTransitions.length === 1) return node.outgoingTransitions[0].toNodeId;
                  
                  // Multiple outgoing with conditions
                  for (const transition of node.outgoingTransitions) {
                    if (!transition.conditionExpression) continue;
                    if (evaluateCondition(transition.conditionExpression, variables)) {
                      return transition.toNodeId;
                    }
                  }
                  
                  // Default flow
                  const defaultFlow = node.outgoingTransitions.find(t => t.defaultFlow);
                  return defaultFlow?.toNodeId || null;
                }
                
                function evaluateGateway(node: any, variables: any): string {
                  for (const transition of node.outgoingTransitions) {
                    if (evaluateCondition(transition.conditionExpression, variables)) {
                      return transition.toNodeId;
                    }
                  }
                  return node.outgoingTransitions[0]?.toNodeId;
                }
                
                function evaluateCondition(expr: string, variables: any): boolean {
                  try {
                    // eslint-disable-next-line no-new-func
                    return Function('variables', 'return ' + expr)(variables);
                  } catch (e) {
                    return false;
                  }
                }
                
                function findJoinNode(node: any): string {
                  // Simplification: seguir una transición
                  return node.outgoingTransitions?.[0]?.toNodeId || null;
                }
                """;
    }

    private String findStartNode(Map<String, BpmnNode> nodes) {
        return nodes.values().stream()
                .filter(n -> "StartEvent".equals(n.getType()))
                .map(BpmnNode::getId)
                .findFirst()
                .orElse("");
    }

    private String toCamelCase(String str) {
        String[] words = str.split("_");
        StringBuilder result = new StringBuilder(words[0].toLowerCase());
        for (int i = 1; i < words.length; i++) {
            result.append(words[i].substring(0, 1).toUpperCase())
                    .append(words[i].substring(1).toLowerCase());
        }
        return result.toString();
    }

    private String toPascalCase(String str) {
        String camelCase = toCamelCase(str);
        return camelCase.substring(0, 1).toUpperCase() + camelCase.substring(1);
    }
}
