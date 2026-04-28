# AWS deployment

Este repositorio queda preparado para desplegarse en AWS App Runner usando una imagen publicada en Amazon ECR.

## Recursos AWS necesarios

- Un repositorio de Amazon ECR
- Un servicio de AWS App Runner configurado para leer la imagen desde ECR
- Un rol IAM para GitHub Actions con permisos sobre ECR y App Runner

## Variables de GitHub Actions

Configura estas `Repository variables`:

- `AWS_REGION`: region AWS, por ejemplo `us-east-1`
- `ECR_REPOSITORY`: nombre del repositorio ECR
- `APP_RUNNER_SERVICE_ARN`: ARN del servicio App Runner

Configura este `Repository secret`:

- `AWS_ROLE_ARN`: rol IAM asumido por GitHub Actions via OIDC

## Variables de entorno del servicio App Runner

- `MONGODB_URI`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `TEMPORAL_TARGET`
- `WORKFLOW_AI_BASE_URL`
- `PORT` opcional, por defecto `8080`

## Flujo de despliegue

- Cada push a `main` ejecuta `.github/workflows/aws-deploy.yml`
- El workflow compila la app, construye la imagen Docker y la publica en ECR
- Luego dispara `aws apprunner start-deployment` para que App Runner tome la imagen `latest`
