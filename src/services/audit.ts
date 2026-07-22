import { prisma } from "../db.js";

export async function audit(userId: string | undefined, action: string, entityType: string, entityId?: string, details?: unknown) {
  await prisma.auditLog.create({
    data: {
      userId,
      action,
      entityType,
      entityId,
      details: details === undefined ? undefined : (details as object)
    }
  });
}
