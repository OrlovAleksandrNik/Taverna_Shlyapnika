export const CONSENT_VERSION = "1.0";
export const PRIVACY_POLICY_VERSION = "1.0";
export const CONSENT_REQUIRED_CODE = "CONSENT_REQUIRED";

export type ConsentFormType = "game-booking" | "service-request" | "contact";

export type ConsentInput = {
  consentGiven?: boolean;
  consentVersion?: string;
  privacyPolicyVersion?: string;
};

export class ConsentRequiredError extends Error {
  code = CONSENT_REQUIRED_CODE;

  constructor() {
    super("Заявка не отправлена: необходимо согласие на обработку персональных данных.");
    this.name = "ConsentRequiredError";
  }
}

export function requireConsent(input: ConsentInput | undefined, formType: ConsentFormType) {
  if (
    !input?.consentGiven ||
    input.consentVersion !== CONSENT_VERSION ||
    input.privacyPolicyVersion !== PRIVACY_POLICY_VERSION
  ) {
    throw new ConsentRequiredError();
  }

  return {
    consentGiven: true,
    consentVersion: input.consentVersion,
    privacyPolicyVersion: input.privacyPolicyVersion,
    consentedAt: new Date(),
    formType
  };
}
