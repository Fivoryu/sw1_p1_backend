# AWS deployment

Este repositorio se despliega con una **imagen Docker** publicada en **Amazon ECR** y un servicio **ECS/Fargate** (o equivalente) que use esa imagen. El `Dockerfile` en la raíz del repo empaqueta la aplicacion Spring Boot 21 en un JAR y lo ejecuta con el JRE.

## Recursos AWS necesarios

- Un repositorio de Amazon ECR
- Un cluster y servicio ECS (Fargate) que usen la imagen `latest` (u otra etiqueta)
- Un rol IAM para GitHub Actions con permisos sobre ECR y ECS

## Variables de GitHub Actions

Configura estas `Repository variables`:

- `AWS_REGION`: region AWS, por ejemplo `us-east-1`
- `ECR_REPOSITORY`: nombre del repositorio ECR
- `ECS_CLUSTER`: nombre del cluster ECS
- `ECS_SERVICE`: nombre del servicio ECS

Configura este `Repository secret`:

- `AWS_ROLE_ARN`: rol IAM asumido por GitHub Actions via OIDC

## Variables de entorno del contenedor (ECS / tarea)

- `MONGODB_URI`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `TEMPORAL_TARGET`
- `WORKFLOW_AI_BASE_URL`
- `PORT` opcional, por defecto `8080`

## Flujo de despliegue

- Cada push a `main` ejecuta `.github/workflows/aws-deploy.yml`
- El workflow construye la imagen Docker (Maven dentro del `Dockerfile`) y la publica en ECR
- Luego ejecuta `aws ecs update-service --force-new-deployment` para que el servicio tome la nueva imagen
