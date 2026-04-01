export interface AppAuthSession {
  session: {
    id: string;
    expiresAt: Date | string;
  };
  user: {
    email: string;
    emailVerified: boolean;
    id: string;
    image?: string | null;
    minecraftUsername?: string | null;
    minecraftUuid?: string | null;
    name: string;
  };
}

export interface AppAuth {
  api: {
    getSession: (input: { headers: Headers }) => Promise<AppAuthSession | null>;
  };
  handler: (request: Request) => Promise<Response>;
}
