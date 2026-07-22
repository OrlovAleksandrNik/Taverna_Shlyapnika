import { prisma } from "../db.js";
import { audit } from "./audit.js";

type PrivacyEntityType = "GameSignup" | "ServiceRequest" | "ContactRequest";

export async function withdrawConsent(input: {
  entityType: PrivacyEntityType;
  requestId: string;
  anonymize?: boolean;
}) {
  const now = new Date();
  const common = {
    consentWithdrawnAt: now,
    dataUseStoppedAt: now,
    ...(input.anonymize ? { anonymizedAt: now } : {})
  };

  if (input.entityType === "GameSignup") {
    await prisma.gameSignup.update({
      where: { id: input.requestId },
      data: input.anonymize
        ? {
            ...common,
            playerName: "Отозвано",
            contact: `withdrawn:${input.requestId}`,
            comment: null,
            status: "cancelled"
          }
        : common
    });
  }

  if (input.entityType === "ServiceRequest") {
    await prisma.serviceRequest.update({
      where: { id: input.requestId },
      data: input.anonymize
        ? {
            ...common,
            name: "Отозвано",
            contact: `withdrawn:${input.requestId}`,
            city: null,
            comment: null,
            status: "closed"
          }
        : common
    });
  }

  if (input.entityType === "ContactRequest") {
    await prisma.contactRequest.update({
      where: { id: input.requestId },
      data: input.anonymize
        ? {
            ...common,
            name: "Отозвано",
            contact: `withdrawn:${input.requestId}`,
            topic: null,
            comment: "Данные обезличены по отзыву согласия.",
            status: "withdrawn"
          }
        : common
    });
  }

  await audit(undefined, "privacy.consent_withdrawn", input.entityType, input.requestId, {
    anonymize: Boolean(input.anonymize)
  });

  return { ok: true, entityType: input.entityType, requestId: input.requestId, anonymized: Boolean(input.anonymize) };
}
