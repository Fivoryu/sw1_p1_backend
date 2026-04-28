package com.banco.workflow.repository;

import com.banco.workflow.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    void deleteByProcessInstanceId(String processInstanceId);
    List<Task> findByAssignee(String assignee);
    List<Task> findByCandidateRole(String candidateRole);
    List<Task> findByCandidateRoleAndStatus(String candidateRole, String status);
    List<Task> findByDepartmentAssigned(String departmentAssigned);
    List<Task> findByDepartmentAssignedAndStatus(String departmentAssigned, String status);
    List<Task> findByAssigneeAndStatus(String assignee, String status);
    List<Task> findByProcessInstanceId(String processInstanceId);
    List<Task> findByStatus(String status);
    List<Task> findByProcessInstanceIdAndStatus(String processInstanceId, String status);
}
