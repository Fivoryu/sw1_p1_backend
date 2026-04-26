package com.banco.workflow.dto;

import com.banco.workflow.model.BpmnNode;
import com.banco.workflow.model.DocumentUpload;
import com.banco.workflow.model.FormDefinition;
import com.banco.workflow.model.ProcessInstance;
import com.banco.workflow.model.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FuncionarioWorkflowDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TareaDTO {
        private String id;
        private String workflowInstanceId;
        private String nodoId;
        private String nombreTarea;
        private String departamentoAsignado;
        private String usuarioAsignado;
        private String estado;
        private LocalDateTime fechaCreacion;
        private LocalDateTime fechaVencimiento;
        private String prioridad;
        private String formularioId;
        private String clienteNombre;
        private String clienteDni;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TareaDetalleDTO {
        private TareaDTO tarea;
        private ProcessInstance tramite;
        private FormDefinition formulario;
        private Map<String, BpmnNode> workflowGraph;
        private Map<String, Object> datosActuales;
        private Map<String, Object> borradorActual;
        private List<FormularioCompletoDTO> formulariosPrevios;
        private List<DocumentoResumenDTO> documentos;
        private List<String> activeNodeIds;
        private boolean canClaim;
        private boolean canEdit;
        private boolean claimedByCurrentUser;
        private boolean blockedByOtherUser;
        private String accessState;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FormularioCompletoDTO {
        private String formularioId;
        private String tareaId;
        private String nodoId;
        private String usuarioId;
        private LocalDateTime fecha;
        private String estado;
        private Map<String, Object> valores;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResultadoDerivacionDTO {
        private boolean success;
        private String message;
        private String estadoInstancia;
        private List<TareaDTO> nuevasTareas;
        private List<String> activeNodeIds;
        private Map<String, Object> contextoDatos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistorialTramiteDTO {
        private String instanciaId;
        private String policyName;
        private String estado;
        private List<ProcessInstance.HistoryEntry> historialNodos;
        private List<FormularioCompletoDTO> formulariosCompletados;
        private List<DocumentoResumenDTO> documentos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompletarTareaRequest {
        private Map<String, Object> valoresFormulario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuardarBorradorRequest {
        private Map<String, Object> valoresFormulario;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SolicitarCorreccionRequest {
        private String motivo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentoResumenDTO {
        private String id;
        private String taskId;
        private String nombreArchivo;
        private String mimeType;
        private String estado;
        private LocalDateTime fecha;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuscarTramiteDTO {
        private String instanciaId;
        private String policyName;
        private String estado;
        private String clienteNombre;
        private String clienteDni;
        private String departamentoActual;
        private LocalDateTime fechaInicio;
        private LocalDateTime ultimaActualizacion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuardadoBorradorDTO {
        private boolean success;
        private String message;
        private TareaDetalleDTO detalle;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CorreccionDTO {
        private boolean success;
        private String message;
        private String instanciaId;
        private String nodoDevueltoId;
        private String nodoDevueltoNombre;
    }

    public static TareaDTO fromTask(Task task) {
        return TareaDTO.builder()
                .id(task.getId())
                .workflowInstanceId(task.getProcessInstanceId())
                .nodoId(task.getNodeId())
                .nombreTarea(task.getNodeName())
                .departamentoAsignado(task.getDepartmentAssigned() != null ? task.getDepartmentAssigned() : task.getCandidateRole())
                .usuarioAsignado(task.getAssignee())
                .estado(task.getStatus())
                .fechaCreacion(task.getCreatedAt())
                .fechaVencimiento(task.getDueDate())
                .prioridad(task.getPriority())
                .formularioId(task.getFormId())
                .clienteNombre(task.getCustomerName())
                .clienteDni(task.getCustomerDni())
                .build();
    }

    public static DocumentoResumenDTO fromDocument(DocumentUpload documentUpload) {
        return DocumentoResumenDTO.builder()
                .id(documentUpload.getId())
                .taskId(documentUpload.getTaskId())
                .nombreArchivo(documentUpload.getFileName())
                .mimeType(documentUpload.getMimeType())
                .estado(documentUpload.getStatus())
                .fecha(documentUpload.getUploadedAt())
                .build();
    }
}
