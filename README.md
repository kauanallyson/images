# Image Processing Service

Backend de portfólio baseado no [projeto image-processing do roadmap.sh](https://roadmap.sh/projects/image-processing-service):
upload de imagens e download (add geração de QRCode e Watermark depois).
Angular, Spring Boot, S3.

## Rodando localmente

```bash
cp .env.example .env
docker compose up -d
```

## Stack

- Angular 22
- Spring Boot
- AWS SDK

## API

| método   | path                | sucesso | notas                       |
|----------|---------------------|---------|-----------------------------|
| POST     | /api/v1/images      | 201     | faz upload da imagem        |
| GET      | /api/v1/images      | 200     | retorna todas com paginação |
| GET      | /api/v1/images/{id} | 200     | retorna a imagem por id     |
| DELETE   | /images/{id}        | 204     | deleta a imagem             |

DTO de resposta de imagem:
```json
{
  "uuid": "uuid",
  "originalName": "photo.jpg",
  "contentType": "image/jpeg",
  "uri": "https://...",
  "createdAt": "2000-01-01T12:00:00Z"
}
```