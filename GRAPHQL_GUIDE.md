# GraphQL API - Workflow Engine

## 🔗 Endpoints

- **GraphQL Query/Mutation**: `http://localhost:8080/api/graphql`
- **GraphiQL (IDE)**: `http://localhost:8080/api/graphiql`
- **Schema Introspection**: `http://localhost:8080/api/graphql?query={__schema{types{name}}}`

## 📚 Ejemplos de Queries

### Obtener Procesos de un Usuario

```graphql
query GetProcesses {
  processes(userId: "user-123") {
    id
    policyName
    status
    initiatedAt
    completedAt
    history {
      nodeId
      nodeName
      status
      timestamp
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "processes": [
      {
        "id": "proc-001",
        "policyName": "Loan Application",
        "status": "RUNNING",
        "initiatedAt": "2024-04-20T10:00:00Z",
        "completedAt": null,
        "history": [
          {
            "nodeId": "start-event",
            "nodeName": "Iniciar Solicitud",
            "status": "COMPLETED",
            "timestamp": "2024-04-20T10:00:00Z"
          }
        ]
      }
    ]
  }
}
```

### Obtener Tareas de un Usuario

```graphql
query GetTasks {
  tasks(assignee: "officer-456") {
    id
    nodeName
    status
    createdAt
    dueDate
    processInstanceId
    formData
    requiredDocuments
  }
}
```

### Obtener Notificaciones

```graphql
query GetNotifications {
  notifications(userId: "user-123", limit: 20) {
    id
    title
    body
    type
    read
    createdAt
  }
}
```

## ✏️ Ejemplos de Mutations

### Completar una Tarea

```graphql
mutation CompleteTask {
  completeTask(input: {
    taskId: "task-001"
    result: {
      approvedAmount: 10000
      interest: 5.5
      term: 24
    }
    comments: "Tarea completada satisfactoriamente"
  }) {
    success
    message
    task {
      id
      status
      completedAt
    }
    nextTasks {
      id
      nodeName
      assignee
    }
  }
}
```

**Response:**
```json
{
  "data": {
    "completeTask": {
      "success": true,
      "message": "Task completed successfully",
      "task": {
        "id": "task-001",
        "status": "COMPLETED",
        "completedAt": "2024-04-20T11:30:00Z"
      },
      "nextTasks": [
        {
          "id": "task-002",
          "nodeName": "Document Validation",
          "assignee": "officer-789"
        }
      ]
    }
  }
}
```

### Cargar un Documento

```graphql
mutation UploadDocument {
  uploadDocument(input: {
    processInstanceId: "proc-001"
    fileName: "loan_agreement.pdf"
    fileData: "JVBERi0xLjQK..." # Base64 encoded
    mimeType: "application/pdf"
  }) {
    success
    message
    document {
      id
      fileName
      status
      uploadedAt
      url
    }
  }
}
```

### Iniciar un Nuevo Proceso

```graphql
mutation StartProcess {
  startProcess(input: {
    policyId: "policy-loan-001"
    clientId: "client-555"
    variables: {
      loanAmount: 50000
      applicantName: "Juan Pérez"
      applicantEmail: "juan@example.com"
    }
  }) {
    id
    status
    policyName
    initiatedAt
  }
}
```

### Marcar Notificación como Leída

```graphql
mutation MarkAsRead {
  markNotificationAsRead(id: "notif-123") {
    success
    message
  }
}
```

### Rechazar una Tarea

```graphql
mutation RejectTask {
  rejectTask(taskId: "task-001", reason: "Información incompleta") {
    success
    message
    task {
      id
      status
    }
  }
}
```

## 🔐 Autenticación

GraphQL usa JWT tokens iguales que REST API. Incluir el token en el header:

```bash
curl -X POST http://localhost:8080/api/graphql \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"query":"{ processes(userId:\"user-123\") { id status } }"}'
```

## 📱 Desde la App Mobile

La app Flutter usará `graphql_flutter`:

```dart
// Query
final result = await _client.query(
  QueryOptions(
    document: gql(getProcessesQuery),
    variables: {'userId': userId},
  ),
);

// Mutation
final response = await _client.mutate(
  MutationOptions(
    document: gql(completeTaskMutation),
    variables: {
      'taskId': taskId,
      'result': result,
    },
  ),
);
```

## 🐛 Debugging

### Ver GraphiQL en el navegador

1. Abrir http://localhost:8080/api/graphiql
2. Escribir queries en la sección izquierda
3. Presionar Ctrl+Enter para ejecutar
4. Ver respuesta en la sección derecha

### Logs de GraphQL

Los logs aparecen en consola con nivel DEBUG:

```
2024-04-20 10:15:30 DEBUG org.springframework.graphql : Executing query: getProcesses
2024-04-20 10:15:30 DEBUG org.springframework.graphql : Query result: 200ms
```

## 📊 Introspection Query

Para obtener el schema completo:

```graphql
{
  __schema {
    types {
      name
      description
      fields {
        name
        type {
          name
        }
      }
    }
  }
}
```

## ⚠️ Error Handling

Los errores de GraphQL se devuelven en la sección `errors`:

```json
{
  "data": null,
  "errors": [
    {
      "message": "Process not found: proc-999",
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "path": ["process"]
    }
  ]
}
```

## 🔄 Subscriptions (WebSocket)

Para actualizaciones en tiempo real:

```graphql
subscription OnTaskUpdated {
  taskUpdated(taskId: "task-001") {
    id
    status
    updatedAt
  }
}
```

### Conectar desde cliente:

```dart
// Flutter
final subscription = await _client.subscribe(
  SubscriptionOptions(
    document: gql(onTaskUpdatedSubscription),
    variables: {'taskId': taskId},
  ),
).listen((event) {
  print('Task updated: ${event.data}');
});
```

## 📝 Best Practices

1. **Siempre especificar campos requeridos**: Los campos sin `!` son opcionales
2. **Usar variables para seguridad**: No concatenar strings en queries
3. **Paginar resultados**: Usar parámetro `limit` para grandes datasets
4. **Cachear localmente**: Usar Hive en mobile para datos offline
5. **Manejo de errores**: Siempre revisar `success` en respuestas

## 🚀 Deployment

### Producción

Cambiar en `application-prod.yml`:

```yaml
spring:
  graphql:
    graphiql:
      enabled: false  # Deshabilitar IDE en producción
    web:
      cors:
        allowed-origins: "https://yourdomain.com"
        allowed-methods: POST
```

### CORS Configuration

Para permitir la app móvil/web:

```yaml
spring:
  graphql:
    web:
      cors:
        allowed-origins:
          - "http://10.0.2.2:3000"  # Emulador Android
          - "https://app.banco.com"  # App móvil en producción
          - "https://admin.banco.com"  # Web admin
        allowed-methods: GET,POST,OPTIONS
        allow-credentials: true
        max-age: 3600
```

---

**Última actualización**: Abril 20, 2026
