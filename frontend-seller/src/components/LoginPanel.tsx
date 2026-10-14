"use client";

import { FormEvent, useRef, useState } from "react";
import Script from "next/script";
import { googleLogin, login, saveSession } from "@/lib/api";

declare global {
  interface Window {
    google?: { accounts: { id: {
      initialize: (options: { client_id: string; callback: (response: { credential: string }) => void }) => void;
      renderButton: (element: HTMLElement, options: Record<string, unknown>) => void;
    } } };
  }
}

const SITE = "seller" as const;
const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";

export default function LoginPanel() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const googleButton = useRef<HTMLDivElement>(null);

  function initializeGoogle() {
    if (!GOOGLE_CLIENT_ID || !window.google || !googleButton.current) return;
    googleButton.current.replaceChildren();
    window.google.accounts.id.initialize({
      client_id: GOOGLE_CLIENT_ID,
      callback: async response => {
        setBusy(true); setMessage("");
        try {
          const session = await googleLogin(response.credential, SITE);
          saveSession(session);
          setMessage(`Signed in with Google as ${session.email}. Role: ${session.role}.`);
        } catch (error) { setMessage(error instanceof Error ? error.message : "Google sign-in failed."); }
        finally { setBusy(false); }
      }
    });
    window.google.accounts.id.renderButton(googleButton.current, { theme: "outline", size: "large", shape: "rectangular", text: "continue_with", width: 300 });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setMessage(""); setBusy(true);
    try {
      const result = await login(email, password);
      saveSession(result);
      setMessage(`Signed in as ${result.email}. Role: ${result.role}.`);
    } catch (error) { setMessage(error instanceof Error ? error.message : "Unable to sign in."); }
    finally { setBusy(false); }
  }

  return (
    <div className="loginCard">
      {GOOGLE_CLIENT_ID && <Script src="https://accounts.google.com/gsi/client" strategy="afterInteractive" onLoad={initializeGoogle} />}
      <p className="eyebrow">ACCOUNT ACCESS</p>
      <h3>Welcome back.</h3>
      <p className="loginHint">Sign in to ShopSphere seller.</p>
      {GOOGLE_CLIENT_ID ? <div ref={googleButton} aria-label="Continue with Google" /> : <p className="finePrint">Google sign-in is disabled until NEXT_PUBLIC_GOOGLE_CLIENT_ID is configured.</p>}
      <form onSubmit={handleSubmit}>
        <label htmlFor="email">Email address</label>
        <input id="email" type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com"/>
        <label htmlFor="password">Password</label>
        <input id="password" type="password" autoComplete="current-password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 8 characters"/>
        <button className="primaryButton full" disabled={busy}>{busy ? "Signing in…" : "Sign in"} <span>→</span></button>
      </form>
      {message && <p className="formMessage" role="status">{message}</p>}
      <p className="finePrint">Use Google Identity Services with a configured OAuth Web client ID. Admin access is allow-listed by IAM.</p>
    </div>
  );
}
