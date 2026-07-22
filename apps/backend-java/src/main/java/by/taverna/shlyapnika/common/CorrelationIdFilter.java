package by.taverna.shlyapnika.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
  public static final String HEADER = "x-request-id";
  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var requestId = request.getHeader(HEADER);
    if (requestId == null || requestId.isBlank()) requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    response.setHeader(HEADER, requestId);
    var started = System.nanoTime();
    try {
      filterChain.doFilter(request, response);
    } finally {
      var durationMs = (System.nanoTime() - started) / 1_000_000;
      log.info("request method={} path={} status={} durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs
      );
      MDC.remove("requestId");
    }
  }
}
