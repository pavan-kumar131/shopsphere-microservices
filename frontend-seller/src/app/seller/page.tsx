'use client';
import { useState } from 'react';
import { readSession } from '@/lib/api';

export default function SellerPage() {
  const [message, setMessage] = useState('Sign in with a seller account to manage your catalogue.');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState('');
  const [category, setCategory] = useState('');
  const [stock, setStock] = useState('1');
  const [file, setFile] = useState<File | null>(null);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const session = readSession();
    if (!session) { setMessage('Please sign in first.'); return; }
    if (session.role !== 'SELLER' && session.role !== 'ADMIN') { setMessage('This account does not have seller permissions.'); return; }
    try {
      let imageUrl: string | null = null;
      if (file) {
        const upload = await fetch('/api/products/image-upload-url', { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ fileName: file.name, contentType: file.type }) });
        if (!upload.ok) throw new Error(await upload.text());
        const target = await upload.json();
        const put = await fetch(target.uploadUrl, { method: 'PUT', headers: { 'Content-Type': file.type }, body: file });
        if (!put.ok) throw new Error('S3 image upload failed');
        imageUrl = target.imageUrl;
      }
      const response = await fetch('/api/products', { method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' }, body: JSON.stringify({ name, description, price: Number(price), category, imageUrl, stock: Number(stock) }) });
      if (!response.ok) throw new Error(await response.text());
      setMessage('Product created successfully.'); setName(''); setDescription(''); setPrice(''); setCategory(''); setStock('1'); setFile(null);
    } catch (err) { setMessage(err instanceof Error ? err.message : 'Could not save product'); }
  }
  return <main className="container" style={{maxWidth:760,margin:'2rem auto',padding:'1rem'}}><h1>Seller dashboard</h1><p>Register/sign in as a seller, then add products. Images are uploaded directly to private S3 using a short-lived presigned URL.</p><form onSubmit={submit} style={{display:'grid',gap:12}}><label>Product name<input required value={name} onChange={e=>setName(e.target.value)} /></label><label>Description<textarea required value={description} onChange={e=>setDescription(e.target.value)} /></label><label>Price (INR)<input type="number" min="0.01" step="0.01" required value={price} onChange={e=>setPrice(e.target.value)} /></label><label>Category<input required value={category} onChange={e=>setCategory(e.target.value)} /></label><label>Stock<input type="number" min="0" required value={stock} onChange={e=>setStock(e.target.value)} /></label><label>Product image<input type="file" accept="image/jpeg,image/png,image/webp" onChange={e=>setFile(e.target.files?.[0] ?? null)} /></label><button type="submit">Create product</button></form><p role="status">{message}</p></main>;
}
