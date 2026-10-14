"use client";
import { useEffect, useState } from "react";
import Link from "next/link";
import ProductCard from "@/components/ProductCard";
import { getProducts, Product } from "@/lib/api";
export default function HomePage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [category, setCategory] = useState("All");
  const [search, setSearch] = useState("");
  useEffect(() => { getProducts().then(setProducts).catch(e => setError(e.message || "Unable to load products")).finally(() => setLoading(false)); }, []);
  const categories = ["All", ...Array.from(new Set(products.map(p => p.category)))];
  const filtered = products.filter(p => (category === "All" || p.category === category) && `${p.name} ${p.description} ${p.category}`.toLowerCase().includes(search.toLowerCase()));
  return <main>
    <section className="hero"><div className="heroCopy"><p className="eyebrow">THE EVERYDAY EDIT · SHOPSPhERE</p><h1>Good things.<br/><em>Better living.</em></h1><p className="heroText">Thoughtful essentials for the way you live, work, and move through the world. Considered design, useful objects, no unnecessary noise.</p><a className="primaryButton" href="#products">Explore the collection <span>→</span></a><div className="trust"><span className="dot"/> Curated picks <span>·</span> Easy cart <span>·</span> Secure account access</div></div>
      <div className="heroArt"><div className="sun"/><div className="artCard cardOne"><span>01 / CARRY</span><div className="bagShape">S</div><b>Everyday<br/>backpack</b></div><div className="artCard cardTwo"><span>02 / LISTEN</span><div className="headphoneShape">◖</div><b>Sound, in<br/>your own world</b></div><div className="artLabel">LESS, BUT<br/>EXCEPTIONAL.</div></div></section>
    <section className="benefits"><div><span>01</span><strong>Thoughtful essentials</strong><small>Useful things, chosen well.</small></div><div><span>02</span><strong>Simple shopping</strong><small>Manage your cart as you go.</small></div><div><span>03</span><strong>Made for everyday</strong><small>Easy to explore, easy to use.</small></div></section>
    <section className="section" id="products"><div className="sectionHead"><div><p className="eyebrow">THE SHORTLIST</p><h2>Made for every day.</h2></div><p>Explore the catalogue and add your favourites to the cart.</p></div>
      <div className="catalogTools"><div className="searchBox"><span>⌕</span><input aria-label="Search products" placeholder="Search products…" value={search} onChange={e => setSearch(e.target.value)} /></div><div className="categoryTabs">{categories.map(c => <button key={c} className={category === c ? "categoryTab active" : "categoryTab"} onClick={() => setCategory(c)}>{c}</button>)}</div></div>
      {loading ? <div className="emptyState">Loading the collection…</div> : error ? <div className="errorState">{error}<p>Check that the API Gateway and Product Service are running.</p><button className="secondaryButton" onClick={() => { setLoading(true); setError(""); getProducts().then(setProducts).catch(e => setError(e.message)).finally(() => setLoading(false)); }}>Try again</button></div> : filtered.length ? <div className="productGrid">{filtered.map((product, i) => <ProductCard key={product.id} product={product} index={i}/>)}</div> : <div className="emptyState">No products match your search. Try another term or category.</div>}
    </section>
    <section className="storySection" id="about"><div><p className="eyebrow">BUILT WITH CARE</p><h2>Good design is<br/><em>in the details.</em></h2><p>ShopSphere is a working learning storefront powered by Next.js and Spring Boot services. Create an account, browse products, manage your cart, or sign in as an administrator to manage the catalogue.</p><Link className="primaryButton" href="/register">Create your account <span>→</span></Link></div><div className="storyStats"><div><strong>01</strong><span>Browse products</span></div><div><strong>02</strong><span>Build your cart</span></div><div><strong>03</strong><span>Manage your account</span></div></div></section>
  </main>;
}
