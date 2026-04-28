package com.banco.workflow.controller;

import com.banco.workflow.dto.FuncionarioWorkflowDtos;
import com.banco.workflow.service.FuncionarioWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/workflow/funcionario")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','REVISOR','GERENTE')")
public class FuncionarioWorkflowController {

    private final FuncionarioWorkflowService funcionarioWorkflowService;

    @GetMapping("/bandeja")
    public ResponseEntity<List<FuncionarioWorkflowDtos.TareaDTO>> obtenerBandejaTareas() {
        return ResponseEntity.ok(funcionarioWorkflowService.getBandejaActual());
    }

    @GetMapping("/tareas/{tareaId}")
    public ResponseEntity<FuncionarioWorkflowDtos.TareaDetalleDTO> obtenerDetalleTarea(@PathVariable String tareaId) {
        return ResponseEntity.ok(funcionarioWorkflowService.getDetalleTarea(tareaId));
    }

    @PostMapping("/tareas/{tareaId}/reclamar")
    public ResponseEntity<FuncionarioWorkflowDtos.TareaDetalleDTO> reclamarTarea(@PathVariable String tareaId) {
        return ResponseEntity.ok(funcionarioWorkflowService.reclamarTarea(tareaId));
    }

    @PostMapping("/tareas/{tareaId}/borrador")
    public ResponseEntity<FuncionarioWorkflowDtos.GuardadoBorradorDTO> guardarBorrador(
            @PathVariable String tareaId,
            @RequestBody FuncionarioWorkflowDtos.GuardarBorradorRequest request) {
        return ResponseEntity.ok(funcionarioWorkflowService.guardarBorrador(tareaId, request.getValoresFormulario()));
    }

    @PostMapping("/tareas/{tareaId}/solicitar-correccion")
    public ResponseEntity<FuncionarioWorkflowDtos.CorreccionDTO> solicitarCorreccion(
            @PathVariable String tareaId,
            @RequestBody FuncionarioWorkflowDtos.SolicitarCorreccionRequest request) {
        return ResponseEntity.ok(funcionarioWorkflowService.solicitarCorreccion(tareaId, request.getMotivo(), request.getTargetNodeId()));
    }

    /**
     * CU-17: listar nodos humanos previos posibles para devolución por corrección.
     */
    @GetMapping("/tareas/{tareaId}/correccion/targets")
    public ResponseEntity<List<FuncionarioWorkflowDtos.CorrectionTargetDTO>> correctionTargets(@PathVariable String tareaId) {
        return ResponseEntity.ok(funcionarioWorkflowService.listCorrectionTargets(tareaId));
    }

    @PostMapping("/tareas/{tareaId}/completar")
    public ResponseEntity<FuncionarioWorkflowDtos.ResultadoDerivacionDTO> completarTareaYDerivar(
            @PathVariable String tareaId,
            @RequestBody FuncionarioWorkflowDtos.CompletarTareaRequest request) {
        return ResponseEntity.ok(funcionarioWorkflowService.completarTarea(tareaId, request.getValoresFormulario()));
    }

    @GetMapping("/tramites/{instanciaId}/historial")
    public ResponseEntity<FuncionarioWorkflowDtos.HistorialTramiteDTO> obtenerHistorial(@PathVariable String instanciaId) {
        return ResponseEntity.ok(funcionarioWorkflowService.getHistorial(instanciaId));
    }

    @GetMapping("/tramites/buscar")
    public ResponseEntity<List<FuncionarioWorkflowDtos.BuscarTramiteDTO>> buscarTramites(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String workflowInstanceId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String clienteDni,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String estado,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String fechaDesde,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String fechaHasta) {
        return ResponseEntity.ok(funcionarioWorkflowService.buscarTramites(workflowInstanceId, clienteDni, estado, fechaDesde, fechaHasta));
    }
}
