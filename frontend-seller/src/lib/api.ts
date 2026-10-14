export const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type Product = {
  id: number;
  name: string;
  description: string;
  price: number;
  category: string;
  imageUrl?: string | null;
  stock?: number;
};

export type Session = {
  accessToken: string;
  tokenType: string;
  userId: number;
  email: string;
  role: "CUSTOMER" | "ADMIN" | string;
  expiresInSeconds?: number;
};

export type CartItem = { product: Product; quantity: number };

async function parseResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  let data: any = {};
  if (text) {
    try { data = JSON.parse(text); } catch { data = { message: text }; }
  }
  if (!response.ok) {
    const detail = data.detail ?? data.message ?? data.error ?? data.title;
    throw new Error(typeof detail === "string" ? detail : `Request failed (${response.status})`);
  }
  return data as T;
}

export async function getProducts(): Promise<Product[]> {
  return parseResponse<Product[]>(await fetch(`${API_BASE}/api/products`, { cache: "no-store" }));
}

export async function getProduct(id: string | number): Promise<Product> {
  return parseResponse<Product>(await fetch(`${API_BASE}/api/products/${id}`, { cache: "no-store" }));
}

export async function register(email: string, password: string, role: "CUSTOMER" | "SELLER" = "CUSTOMER") {
  return parseResponse<{id:number;email:string;role:string}>(await fetch(`${API_BASE}/api/auth/register`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password, role })
  }));
}

export async function login(email: string, password: string): Promise<Session> {
  return parseResponse<Session>(await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password })
  }));
}

<<<<<<< HEAD
export async function googleLogin(idToken: string, site: "customer" | "seller" | "admin"): Promise<Session> {
  return parseResponse<Session>(await fetch(`${API_BASE}/api/auth/google`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ idToken, site })
  }));
}

=======
>>>>>>> origin/main
export async function createProduct(input: Omit<Product, "id">, token: string) {
  return parseResponse<Product>(await fetch(`${API_BASE}/api/products`, {
    method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }, body: JSON.stringify(input)
  }));
}

export async function updateProduct(id: number, input: Omit<Product, "id">, token: string) {
  return parseResponse<Product>(await fetch(`${API_BASE}/api/products/${id}`, {
    method: "PUT", headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` }, body: JSON.stringify(input)
  }));
}

export async function deleteProduct(id: number, token: string) {
  return parseResponse<void>(await fetch(`${API_BASE}/api/products/${id}`, {
    method: "DELETE", headers: { Authorization: `Bearer ${token}` }
  }));
}

export function readSession(): Session | null {
  if (typeof window === "undefined") return null;
  try { return JSON.parse(localStorage.getItem("shopsphere_session") || "null") as Session | null; }
  catch { return null; }
}
export function saveSession(session: Session) {
  localStorage.setItem("shopsphere_session", JSON.stringify(session));
  window.dispatchEvent(new Event("shopsphere-session-change"));
}
export function clearSession() {
  localStorage.removeItem("shopsphere_session");
  window.dispatchEvent(new Event("shopsphere-session-change"));
}
export function readCart(): CartItem[] {
  if (typeof window === "undefined") return [];
  try { return JSON.parse(localStorage.getItem("shopsphere_cart") || "[]") as CartItem[]; }
  catch { return []; }
}
export function saveCart(cart: CartItem[]) {
  localStorage.setItem("shopsphere_cart", JSON.stringify(cart));
  window.dispatchEvent(new Event("shopsphere-cart-change"));
}
export function addToCart(product: Product) {
  const cart = readCart();
  const existing = cart.find(item => item.product.id === product.id);
  if (existing) existing.quantity += 1;
  else cart.push({ product, quantity: 1 });
  saveCart(cart);
}

export type ShopOrder = { id: number; customerId: number; status: string; total: number; itemsJson: string; paymentReference?: string; shippingStatus: string; createdAt: string };
export async function createOrder(items: CartItem[], token: string, idempotencyKey: string): Promise<ShopOrder> {
  return parseResponse<ShopOrder>(await fetch(`${API_BASE}/api/orders`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}`, "Idempotency-Key": idempotencyKey },
    body: JSON.stringify({ items: items.map(i => ({ productId: i.product.id, quantity: i.quantity })) })
  }));
}
export async function paySandbox(orderId: number, token: string): Promise<ShopOrder> {
  return parseResponse<ShopOrder>(await fetch(`${API_BASE}/api/orders/${orderId}/payment-sandbox?outcome=success`, {
    method: "POST", headers: { Authorization: `Bearer ${token}` }
  }));
}
export async function getMyOrders(token: string): Promise<ShopOrder[]> {
  return parseResponse<ShopOrder[]>(await fetch(`${API_BASE}/api/orders`, { headers: { Authorization: `Bearer ${token}` }, cache: "no-store" }));
}
