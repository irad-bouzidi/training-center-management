# --- Build stage ---
FROM node:20-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Baked into the built JS at compile time - Vite only reads import.meta.env.*
# from build-time env, so this must be an ARG/ENV, not a runtime env var.
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# --- Dev stage: Vite dev server with HMR, source bind-mounted over this at
# runtime (see docker-compose.override.yml.example). Not used in production -
# selected explicitly via `build.target: dev`.
FROM node:20-alpine AS dev
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# --- Runtime stage (default target) ---
FROM nginx:alpine AS runtime
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
