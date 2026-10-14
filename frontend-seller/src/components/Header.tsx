"use client";
import Link from "next/link";
import { useEffect, useState } from "react";
import { clearSession, readCart, readSession, Session } from "@/lib/api";

export default function Header() {
  const [session, setSession] = useState<Session | null>(null);
  const [count, setCount] = useState(0);
  useEffect(() => {
    const refresh = () => {
      setSession(readSession());
      setCount(readCart().reduce((sum, item) => sum + item.quantity, 0));
    };
    refresh();
    window.addEventListener("shopsphere-session-change", refresh);
    window.addEventListener("shopsphere-cart-change", refresh);
    window.addEventListener("storage", refresh);
    return () => {
      window.removeEventListener("shopsphere-session-change", refresh);
      window.removeEventListener("shopsphere-cart-change", refresh);
      window.removeEventListener("storage", refresh);
    };
  }, []);
  return <header className="nav">
    <Link className="brand" href="/">shopsphere<span>.</span></Link>
    <nav className="navlinks"><Link href="/#products">Shop</Link><Link href="/cart">Cart ({count})</Link>{session?.role === "ADMIN" && <Link href="/admin">Admin</Link>}</nav>
    <div className="navActions">{session ? <><span className="userPill">{session.email}</span><button className="textButton" onClick={() => { clearSession(); window.location.href = "/"; }}>Sign out</button></> : <><Link className="textButton" href="/login">Sign in</Link><Link className="smallButton" href="/register">Create account</Link></>}</div>
  </header>;
}
