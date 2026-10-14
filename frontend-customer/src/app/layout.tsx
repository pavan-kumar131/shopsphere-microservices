import type { Metadata } from "next";
import "./globals.css";
import Header from "@/components/Header";
export const metadata: Metadata = { title: "ShopSphere — considered everyday goods", description: "Shop useful objects, thoughtfully chosen." };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><Header />{children}<footer className="siteFooter"><a className="brand" href="/">shopsphere<span>.</span></a><p>Thoughtful essentials for everyday life.</p><span>Learning project · Demo checkout only</span></footer></body></html>;
}
