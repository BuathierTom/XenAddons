export interface SessionUser {
  email: string | null;
  emailVerified: boolean | null;
  id: string;
  minecraftUsername: string | null;
  minecraftUuid: string;
  name: string | null;
}

export interface AppSession {
  createdAt: string;
  expiresAt: string;
  id: string;
  user: SessionUser;
}

export interface SessionStore {
  create(user: SessionUser): AppSession;
  delete(sessionId: string): void;
  get(sessionId: string): AppSession | null;
}

export interface McidRequestedCode {
  expiresAt: string | null;
  message: string;
  userId: string;
}

export interface McidVerifiedIdentity {
  userId: string;
  username: string;
}

export interface McidClient {
  requestCode(input: { uuid: string }): Promise<McidRequestedCode>;
  verifyCode(input: { code: string; uuid: string }): Promise<McidVerifiedIdentity>;
}
