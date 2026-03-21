FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci --only=production --ignore-scripts

FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --ignore-scripts
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /app

# Install curl for healthchecks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy built application
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

# Create logs directory with proper permissions
RUN mkdir -p /logs && chown app:app /logs

USER app
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
