import type { RequestHandler } from "express";

type Bucket = { count: number; resetAt: number };

export function rateLimit(maxRequests: number, windowMs: number): RequestHandler {
  const buckets = new Map<string, Bucket>();

  return (request, response, next) => {
    const key = request.ip || "unknown";
    const now = Date.now();
    const bucket = buckets.get(key);

    if (!bucket || bucket.resetAt < now) {
      buckets.set(key, { count: 1, resetAt: now + windowMs });
      return next();
    }

    bucket.count += 1;
    if (bucket.count > maxRequests) {
      response.status(429).json({ error: "Слишком много запросов. Попробуйте чуть позже." });
      return;
    }

    next();
  };
}
