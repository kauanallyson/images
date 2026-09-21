# Image Processing Service

Backend de portfólio baseado no [projeto image-processing do roadmap.sh](https://roadmap.sh/projects/image-processing-service):
upload de imagens e download (add geração de QRCode e Watermark mais tarde).
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

Base path: `/api/v1/images`. Imagens são identificadas pelo SHA-256 (hex) do arquivo.

| método | path                  | sucesso | notas                                         |
|--------|-----------------------|---------|-----------------------------------------------|
| POST   | /api/v1/images        | 201     | upload multipart                              |
| GET    | /api/v1/images        | 200     | lista paginada (`page`, `size`, `sort`)       |
| GET    | /api/v1/images/{hash} | 200     | retorna a imagem com URL de download          |
| DELETE | /api/v1/images/{hash} | 204     | deleta a imagem                               |

### Upload

`POST /api/v1/images` (`multipart/form-data`):

- header `X-File-SHA256`: SHA-256 (hex) do arquivo
- part `file`: o arquivo (`image/jpeg`, `image/png` ou `image/webp`)

```bash
curl -X POST http://localhost:8080/api/v1/images \
  -H "X-File-SHA256: $(sha256sum photo.jpg | cut -d' ' -f1)" \
  -F "file=@photo.jpg"
```

Resposta do upload e do `GET /{hash}`:
```json
{
  "uri": "https://...",
  "fileName": "photo.jpg"
}
```

### Listagem

`GET /api/v1/images`:
```json
{
  "content": [
    {
      "hash": "...",
      "fileName": "photo.jpg",
      "contentType": "image/jpeg",
      "createdAt": "2000-01-01T12:00:00.000+00:00",
      "updatedAt": "2000-01-01T12:00:00.000+00:00"
    }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

### Erros

Formato `application/problem+json`

| status | quando                                          |
|--------|-------------------------------------------------|
| 400    | header, part ou arquivo ausente/inválido        |
| 404    | imagem não encontrada                           |
| 413    | arquivo muito grande                            |
| 415    | tipo de mídia não permitido                     |
| 422    | hash não confere com o arquivo                  |
| 502    | falha no storage                                |
