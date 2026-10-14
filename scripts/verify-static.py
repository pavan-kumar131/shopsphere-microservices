#!/usr/bin/env python3
"""Offline structural checks for ShopSphere. Does not replace Docker/Maven/browser tests."""
from pathlib import Path
import json, sys, xml.etree.ElementTree as ET
try:
    import yaml
except ImportError:
    yaml = None
ROOT = Path(__file__).resolve().parents[1]
errors=[]; checks=[]
def ok(message): checks.append(message)
def fail(message): errors.append(message)
# Parse all Maven descriptors.
for p in sorted(ROOT.glob('*/pom.xml')):
    try:
        ET.parse(p); ok(f'Maven XML parses: {p.relative_to(ROOT)}')
    except Exception as e: fail(f'Invalid Maven XML {p.relative_to(ROOT)}: {e}')
# Parse all package manifests.
for p in sorted(ROOT.glob('frontend-*/package.json')):
    try:
        data=json.loads(p.read_text())
        if not {'build','start'}.issubset(data.get('scripts',{})): fail(f'Missing build/start scripts: {p.relative_to(ROOT)}')
        else: ok(f'Frontend package manifest parses: {p.relative_to(ROOT)}')
    except Exception as e: fail(f'Invalid package manifest {p.relative_to(ROOT)}: {e}')
# Compose YAML and build contexts.
compose_path=ROOT/'docker-compose.yml'
if yaml:
    try:
        compose=yaml.safe_load(compose_path.read_text()); services=compose.get('services',{})
        ok(f'Compose YAML parses ({len(services)} services)')
        for name,service in services.items():
            build=service.get('build') if isinstance(service,dict) else None
            if isinstance(build,str) and not (ROOT/build).is_dir(): fail(f'{name} build context missing: {build}')
            if isinstance(build,dict) and build.get('context') and not (ROOT/build['context']).is_dir(): fail(f'{name} build context missing: {build["context"]}')
        for name in ['customer-frontend','seller-frontend','admin-frontend','nginx','api-gateway','auth-service','product-service','order-service','payment-service','notification-service','postgres']:
            if name not in services: fail(f'Required Compose service missing: {name}')
            else: ok(f'Compose service exists: {name}')
    except Exception as e: fail(f'Invalid Compose YAML: {e}')
else: fail('PyYAML unavailable; cannot parse Compose YAML')
# Frontend contexts must be independent.
for d in ['frontend-customer','frontend-seller','frontend-admin']:
    if not (ROOT/d/'Dockerfile').exists() or not (ROOT/d/'src/app/page.tsx').exists(): fail(f'Independent frontend incomplete: {d}')
    else: ok(f'Independent frontend context exists: {d}')
# Check source invariants introduced for security.
checks_text={
 'Product ownership persisted':'seller_id' in (ROOT/'product-service/src/main/java/com/shopsphere/product/Product.java').read_text(),
 'Seller ownership enforced':'You can only manage products you own' in (ROOT/'product-service/src/main/java/com/shopsphere/product/ProductController.java').read_text(),
 'Payment owner check':'Payment belongs to another customer' in (ROOT/'payment-service/src/main/java/com/shopsphere/payment/PaymentController.java').read_text(),
 'JWT expiration checked':'getExpiration()' in (ROOT/'api-gateway/src/main/java/com/shopsphere/gateway/JwtGatewayFilter.java').read_text(),
 'Feign client present':'@FeignClient' in (ROOT/'order-service/src/main/java/com/shopsphere/orders/ProductClient.java').read_text(),
 'Atomic stock reservation query':'reserveStock' in (ROOT/'product-service/src/main/java/com/shopsphere/product/ProductRepository.java').read_text() and 'p.stock >= :quantity' in (ROOT/'product-service/src/main/java/com/shopsphere/product/ProductRepository.java').read_text(),
 'Internal service token is configured':'INTERNAL_SERVICE_TOKEN' in (ROOT/'docker-compose.yml').read_text() and 'security:\n  jwt:' in (ROOT/'product-service/src/main/resources/application.yml').read_text(),
 'Order compensates reservations':'compensate(reserved)' in (ROOT/'order-service/src/main/java/com/shopsphere/orders/OrderController.java').read_text(),
}
for label,passed in checks_text.items(): (ok if passed else fail)(label if passed else f'Missing invariant: {label}')
# XML-like brace balance sanity check for changed Java sources only.
for rel in ['product-service/src/main/java/com/shopsphere/product/Product.java','product-service/src/main/java/com/shopsphere/product/ProductController.java','product-service/src/main/java/com/shopsphere/product/JwtAdminVerifier.java','product-service/src/main/java/com/shopsphere/product/JwtAccessFilter.java','payment-service/src/main/java/com/shopsphere/payment/PaymentController.java','payment-service/src/main/java/com/shopsphere/payment/JwtAccessFilter.java','notification-service/src/main/java/com/shopsphere/notification/JwtAccessFilter.java','product-service/src/main/java/com/shopsphere/product/InternalInventoryController.java','product-service/src/main/java/com/shopsphere/product/InternalServiceVerifier.java','order-service/src/main/java/com/shopsphere/orders/OrderController.java']:
    txt=(ROOT/rel).read_text()
    if txt.count('{') != txt.count('}'): fail(f'Brace count mismatch: {rel}')
    else: ok(f'Java brace sanity: {rel}')
for c in checks: print('PASS',c)
for e in errors: print('FAIL',e)
print(f'\nStatic checks: {len(checks)} passed, {len(errors)} failed.')
sys.exit(1 if errors else 0)
