FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN corepack enable && pnpm install --frozen-lockfile

FROM deps AS build
COPY prisma ./prisma
COPY src ./src
COPY tsconfig.json ./
RUN pnpm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY prisma ./prisma
COPY assets ./assets
COPY data ./data
COPY masters ./masters
COPY *.html ./
COPY styles.css script.js ./
EXPOSE 4177
CMD ["sh", "-c", "pnpm prisma:deploy && node dist/index.js"]
