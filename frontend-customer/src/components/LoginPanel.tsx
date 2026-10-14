"use client";

import { FormEvent, useState } from "react";
import { login } from "@/lib/api";

export default function LoginPanel() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setBusy(true);
    try {
      const result = await login(email, password);
      // Demo only: do not persist bearer tokens in localStorage for production.
      setMessage(`Signed in as ${result.email}. Role: ${result.role}.`);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to sign in.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="loginCard">
      <p className="eyebrow">ACCOUNT ACCESS</p>
      <h3>Welcome back.</h3>
      <p className="loginHint">Sign in to test the Auth Service API.</p>
      <form onSubmit={handleSubmit}>
        <label htmlFor="email">Email address</label>
        <input id="email" type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="you@example.com"/>
        <label htmlFor="password">Password</label>
        <input id="password" type="password" autoComplete="current-password" minLength={8} required value={password} onChange={e => setPassword(e.target.value)} placeholder="At least 8 characters"/>
        <button className="primaryButton full" disabled={busy}>{busy ? "Signing in…" : "Sign in"} <span>→</span></button>
      </form>
      {message && <p className="formMessage" role="status">{message}</p>}
      <p className="finePrint">Demo login only. Production session and token storage must be hardened.</p>
    </div>
  );
}
