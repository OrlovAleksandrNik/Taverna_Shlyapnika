import { prisma } from "../db.js";
import { logger } from "../logger.js";
import { audit } from "./audit.js";
import { ConsentInput, requireConsent } from "./consent.js";
import { notifyAdmins } from "./notifications.js";

export async function createServiceRequest(input: {
  name: string;
  contact: string;
  service: string;
  serviceType?: string;
  desiredDate?: string;
  participants?: number;
  city?: string;
  comment?: string;
  consent: ConsentInput;
}) {
  const consent = requireConsent(input.consent, "service-request");

  const request = await prisma.serviceRequest.create({
    data: {
      name: input.name,
      contact: input.contact,
      service: input.service,
      desiredDate: input.desiredDate,
      participants: input.participants,
      city: input.city,
      comment: input.comment,
      ...consent
    }
  });

  await audit(undefined, "service.request", "ServiceRequest", request.id, {
    service: input.service,
    serviceType: input.serviceType,
    consentVersion: consent.consentVersion,
    privacyPolicyVersion: consent.privacyPolicyVersion
  });

  await notifyAdmins([
    "Новая заявка на услугу",
    "",
    `Услуга: ${input.service}`,
    `Имя: ${input.name}`,
    `Контакт: ${input.contact}`,
    input.desiredDate ? `Дата: ${input.desiredDate}` : "",
    input.participants ? `Участников: ${input.participants}` : "",
    input.city ? `Город: ${input.city}` : "",
    input.comment ? `Комментарий: ${input.comment}` : ""
  ].filter(Boolean).join("\n"));

  logger.info({ requestId: request.id, service: request.service }, "service request saved");
  return request;
}
