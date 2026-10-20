package com.shopsphere.gateway;
import io.jsonwebtoken.Claims; import io.jsonwebtoken.Jwts; import io.jsonwebtoken.security.Keys; import java.util.Base64; import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value; import org.springframework.cloud.gateway.filter.GatewayFilterChain; import org.springframework.cloud.gateway.filter.GlobalFilter; import org.springframework.core.Ordered; import org.springframework.http.HttpMethod; import org.springframework.http.HttpStatus; import org.springframework.http.server.reactive.ServerHttpRequest; import org.springframework.stereotype.Component; import org.springframework.web.server.ServerWebExchange; import reactor.core.publisher.Mono;
@Component public class JwtGatewayFilter implements GlobalFilter, Ordered {
 private final SecretKey key;
 public JwtGatewayFilter(@Value("${security.jwt.secret}") String secret){byte[] b=Base64.getDecoder().decode(secret);if(b.length<32)throw new IllegalArgumentException("JWT_SECRET must decode to at least 32 bytes");key=Keys.hmacShaKeyFor(b);}
 @Override public int getOrder(){return -100;}
 @Override public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain){ServerHttpRequest req=exchange.getRequest();String path=req.getURI().getPath();HttpMethod method=req.getMethod();
  boolean publicAuth=path.equals("/api/auth/register")||path.equals("/api/auth/login")||path.equals("/api/auth/google"); boolean publicProduct=path.startsWith("/api/products")&&HttpMethod.GET.equals(method); boolean health=path.equals("/actuator/health");
  if(publicAuth||publicProduct||health||HttpMethod.OPTIONS.equals(method))return chain.filter(exchange);
  try {String h=req.getHeaders().getFirst("Authorization");if(h==null||!h.startsWith("Bearer "))throw new IllegalArgumentException();Claims c=Jwts.parser().verifyWith(key).build().parseSignedClaims(h.substring(7)).getPayload();String role=c.get("role",String.class);if(c.getSubject()==null||c.getExpiration()==null||c.getExpiration().getTime()<=System.currentTimeMillis())throw new IllegalArgumentException();
   if(path.startsWith("/api/products")&&(HttpMethod.POST.equals(method)||HttpMethod.PUT.equals(method)||HttpMethod.PATCH.equals(method)||HttpMethod.DELETE.equals(method))){boolean ok="ADMIN".equals(role)||(HttpMethod.POST.equals(method)&&"SELLER".equals(role));if(!ok)return reject(exchange,HttpStatus.FORBIDDEN);}
   if(path.startsWith("/api/orders")&&HttpMethod.POST.equals(method)&&path.contains("/shipping-sandbox")&& !"ADMIN".equals(role))return reject(exchange,HttpStatus.FORBIDDEN);
   if(path.startsWith("/api/payments")&&path.endsWith("/confirm-sandbox")&& !"ADMIN".equals(role))return reject(exchange,HttpStatus.FORBIDDEN);
   if(path.startsWith("/api/notifications")&& !"ADMIN".equals(role))return reject(exchange,HttpStatus.FORBIDDEN);
   return chain.filter(exchange);
  }catch(Exception ex){return reject(exchange,HttpStatus.UNAUTHORIZED);}
 }
 private Mono<Void> reject(ServerWebExchange e,HttpStatus status){e.getResponse().setStatusCode(status);return e.getResponse().setComplete();}
}
