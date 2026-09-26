FROM node:lts-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

RUN npm run lint && npm test && npm run build

FROM nginx:stable-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080