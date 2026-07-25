FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package.json
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
ENV PORT=80
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY docker-entrypoint.d/40-control-runtime-config.sh /docker-entrypoint.d/40-control-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-control-runtime-config.sh
EXPOSE 80
