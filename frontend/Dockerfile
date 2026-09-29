# --- build ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ENV VITE_API_BASE_URL=/api-proxy
RUN npm run build

# --- serve ---
FROM nginx:1.29-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# The Estonian dictionary as a plain file: the api renders crawler text for the static pages
# (about, methodology, sources...) from it, so bots read exactly what people read.
COPY --from=build /app/src/i18n/locales/et.json /usr/share/nginx/html/i18n/et.json
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
