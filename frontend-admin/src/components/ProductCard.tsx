"use client";
import Link from "next/link";
import { addToCart, Product } from "@/lib/api";
import { useState } from "react";
export default function ProductCard({ product, index = 0 }: { product: Product; index?: number }) {
  const [added, setAdded] = useState(false);
  const glyphs = ["✳", "◉", "△", "✦", "◌", "⬡"];
  return <article className="productCard">
    <Link className={`productVisual visual${index % 3}`} href={`/products/${product.id}`}>
      <span className="productNumber">{String(index + 1).padStart(2,"0")}</span>
      {product.imageUrl ? <img className="productImage" src={product.imageUrl} alt={product.name} /> : <div className="productGlyph">{glyphs[index % glyphs.length]}</div>}
      <span className="category">{product.category}</span>
    </Link>
    <div className="productInfo"><div><Link href={`/products/${product.id}`}><h3>{product.name}</h3></Link><p>{product.description}</p></div><strong>₹{Number(product.price).toLocaleString("en-IN")}</strong></div>
    <button className="addButton" type="button" onClick={() => { addToCart(product); setAdded(true); window.setTimeout(() => setAdded(false), 1300); }}>{added ? "Added to cart ✓" : "Add to cart"}<span>＋</span></button>
  </article>;
}
