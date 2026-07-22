package by.taverna.shlyapnika.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicPostRateLimitFilter extends OncePerRequestFilter {
  private static final long WINDOW_MS = 60_000;
  private static final int MAX_REQUESTS_PER_WINDOW = 12;

  private final Clock clock;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public PublicPostRateLimitFilter() {
    this(Clock.systemUTC());
  }

  PublicPostRateLimitFilter(Clock clock) {
    this.clock = clock;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isProtectedPost(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (!allowed(key(request))) {
      response.setStatus(429);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"error\":\"Слишком много заявок. Попробуйте немного позже.\",\"message\":\"Слишком много заявок. Попробуйте немного позже.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private boolean isProtectedPost(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
    var path = request.getRequestURI();
    return "/api/game-signups".equals(path) || "/api/service-requests".equals(path);
  }

  private boolean allowed(String key) {
    var now = clock.millis();
    var bucket = buckets.compute(key, (ignored, current) -> {
      if (current == null || now - current.windowStartedAt > WINDOW_MS) return new Bucket(now, 1);
      return new Bucket(current.windowStartedAt, current.count + 1);
    });
    return bucket.count <= MAX_REQUESTS_PER_WINDOW;
  }

  private String key(HttpServletRequest request) {
    var forwardedFor = request.getHeader("x-forwarded-for");
    var ip = forwardedFor == null || forwardedFor.isBlank()
        ? request.getRemoteAddr()
        : forwardedFor.split(",", 2)[0].trim();
    var userAgent = request.getHeader("user-agent");
    return ip + "|" + (userAgent == null ? "" : Integer.toHexString(userAgent.hashCode()));
  }

  private record Bucket(long windowStartedAt, int count) {
  }
}
